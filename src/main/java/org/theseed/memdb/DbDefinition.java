/**
 *
 */
package org.theseed.memdb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.LineReader;

/**
 * A database definition contains the entity and relationship types.  Its primary purpose is to
 * facilitate creating the types from the definition file.
 *
 * The definition file contains commands (denoted by "#" in the first column) and descriptions.  The
 * commands have a command name (currently "Entity" or "Relationship") immediately after the pound
 * sign, and one or more space-delimited parameters.
 *
 * The Entity command's parameters are (1) entity type name, (2) entity ID column name, (3) priority
 * number (with 0 being the lowest), and (4) a file name.  The file name should be the base name of
 * the file in each directory.  The priority is used to determine which entities should be chosen first
 * when traversing the database.
 *
 * Some entities are actually many-to-many or ternary relationships.  We don't want these put into the
 * database; rather they are used to generate relationship records in other entities.  For these,
 * we specify "null" for the ID column.  Other entities are relationships targets or sources, but
 * there are no data records associated with them. For these, the file names are missing.
 *
 * Immediately after the entity header are zero or more attribute descriptions. These vary depending on
 * the database type. These can be followed by file descriptions listing additional files that contain
 * entity data. The file definitions contain the file name and the ID attribute name.
 *
 * The relationship definitions follow each entity.  A relationship definition describes a many-to-one
 * connection between entities and contains a template for the forward direction and the reverse direction.
 * The Relationship command's parameters are (1) source entity type name, (2) column name for the source entity ID,
 * (3) target entity type name, and (4) column name for the target entity ID.  In a normal entity, the source
 * entity type name and ID column name describe the current entity itself.  Thus, a relationship in the Genome
 * entity will specify "Genome" as the source entity type and "genome_id" as the source ID column name.  In
 * the case of a many-to-many relationship entity, however, the source entity type must be a different entity.  Thus, the
 * SubsystemCell ternary relationship entity never specifies itself as a source; rather, the source is either
 * Subsystem, Genome, or Role. The relationship definition is always three lines:  the header, the forward
 * relationship descriptor, and the converse relationship descriptor.
 *
 * @author Bruce Parrello
 *
 */
public abstract class DbDefinition {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(DbDefinition.class);
    /** map of entity type names to type objects */
    private final Map<String, EntityType> entityMap;
    /** number of lines read */
    private int lineCount;
    /** number of attributes read */
    private int attrCount;
    /** number of relationships processed */
    private int relCount;
    /** iterator through the definition file */
    private Iterator<String> iter;

    /**
     * Create a database definition from a definition file.
     *
     * @param fileName		name of the definition file
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public DbDefinition(File fileName) throws IOException, ParseFailureException {
        log.info("Reading database definition from {}.", fileName);
        // Create the entity map.
        this.entityMap = new TreeMap<>();
        // Open up the input file and read the definition lines.
        try (LineReader inStream = new LineReader(fileName)) {
            // Prepare an iterator through the file.
            this.iter = inStream.iterator();
            if (! this.iter.hasNext())
                throw new IOException("No data found in database definition file " + fileName + ".");
            String line = this.iter.next();
            // Initialize our counters.
            this.lineCount = 1;
            this.attrCount = 0;
            this.relCount = 0;
            // Loop through the entity definitions.
            while (line != null) {
                if (! line.startsWith("#Entity"))
                    throw new ParseFailureException("Expecting an #Entity line at line " + lineCount + " in " + fileName + ".");
                line = this.processEntityDefinition(line);
            }
            // Release the memory for the iterator.
            this.iter = null;
        }
        log.info("{} lines read from {}.  {} entities, {} relationships, and {} attributes found.", this.lineCount, fileName,
                this.entityMap.size(), this.relCount, this.attrCount);
    }

    /**
     * Read an entity definition into the database structures.  This process stops when it hits end-of-file
     * or a new entity header.
     *
     * @param line		line containing the entity header
     *
     * @return the line containing the next entity header, or NULL if we hit end-of-file
     *
     * @throws ParseFailureException
     */
    private String processEntityDefinition(String line) throws ParseFailureException {
        String[] parms = StringUtils.split(line, " ", 5);
        if (parms.length < 4)
            throw new ParseFailureException("Entity header i line " + this.lineCount + " of definition file has fewer than 3 parameters.");
        // Find this entity type in the type map and set the ID column name.
        EntityType entity = this.findEntityType(parms[1]);
        entity.setIdColName(parms[2]);
        // Compute the entity priority.
        try {
            int prio = Integer.parseInt(parms[3]);
            entity.setPriority(prio);
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Invalid priority number \"" + parms[3] + " in line " + this.lineCount + " of definition file.");
        }
        // Store a file name if we have one.
        if (parms.length >= 5)
            entity.setFileName(parms[4]);
        // Now we need to read through the attributes.  The method will return the next header line, or NULL if we hit
        // end-of-file.
        String retVal = this.readAttributes(entity);
        // Try to read the relationships, Again, we return the next header line or NULL.
        retVal = this.readRelationships(entity, retVal);
        // Return the next entity's header line.
        return retVal;
    }

    /**
     * Find the entity type with the given name.  If none exists, it will be created.
     *
     * @param typeName		name of the entity type
     *
     * @return an entity type definition for the given type name
     */
    public EntityType findEntityType(String typeName) {
        return this.entityMap.computeIfAbsent(typeName, x -> this.createEntityType(x));
    }

    /**
     * Create a blank entity type definition for the specified type name.
     *
     * @param name		the entity type name
     *
     * @return the entity type definition
     */
    protected abstract EntityType createEntityType(String name);

    /**
     * Read the next line and update the line count.
     *
     * @return the next input line, or NULL if we are at end-of-file
     */
    public String readNext() {
        String line;
        if (this.iter.hasNext()) {
            line = this.iter.next();
            this.lineCount++;
        } else
            line = null;
        return line;
    }

    /**
     * Process the attributes of the current entity.
     *
     * @param entity	entity type to contain the attributes
     *
     * @return the first relationship header for this entity, the next entity header, or NULL for end-of-file
     */
    private String readAttributes(EntityType entity) {
        String retVal = this.readNext();
        while (retVal != null && ! retVal.startsWith("#")) {
            // Here we have an attribute line.
            entity.addAttribute(retVal);
            this.attrCount++;
            retVal = this.readNext();
        }
        // Save the last line read.  It belongs to the next groupo.
        return retVal;
    }

    /**
     * Process the relationships for the current entity.
     *
     * @param entity	entity type to contain the attributes
     * @param line		header line for the first relationship, the next entity, or NULL for end-of-file
     *
     * @return the next entity header, or NULL for end-of-file
     *
     * @throws ParseFailureException
     */
    private String readRelationships(EntityType entity, String line) throws ParseFailureException {
        String retVal = line;
        while (retVal != null && retVal.startsWith("#Relationship")) {
            // Here we have a relationship header.
            String[] parms = StringUtils.split(retVal, " ", 5);
            if (parms.length != 5)
                throw new ParseFailureException("Relationship header in line " + this.lineCount + " of definition file should have exactly 4 parameters.");
            // The header contains the source entity type name, the source ID column name, the target entity type name and
            // the target ID column name.
            EntityType sourceType = this.findEntityType(parms[1]);
            EntityType targetType = this.findEntityType(parms[3]);
            // Create the relationship and read its definition.
            RelationshipType rel = this.createRelationshipType(sourceType, parms[2], targetType, parms[4]);
            retVal = this.processRelationshipDefinition(rel, this);
            // Save the relationship.
            entity.addRelationship(rel);
            this.relCount++;
        }
        return retVal;
    }

    /**
     * This method gets control after the relationship header has been read and the relationship created.
     * The relationship definition records are processed here.
     *
     * @param rel		relationship type
     * @param db		database definition being processed
     *
     * @return the next header record, or NULL if we have reached end-of-file
     *
     * @throws ParseFailureException
     */
    protected abstract String processRelationshipDefinition(RelationshipType rel, DbDefinition db) throws ParseFailureException;

    /**
     * Create a new relationship type definition. Note that a relationship is usually defined in terms of
     * the source entity record, and the column names are from that record, implying the relationship is
     * many-to-one. For a many-to-many relationship, however, the "entity" is a table containing
     * records with both IDs.
     *
     * @param sourceType		source entity type
     * @param sourceIdColname	name of the column containing the source ID
     * @param targetType		target entity type
     * @param targetIdColName	name of the column containing the target ID
     *
     * @return the new relationship type definition
     */
    protected abstract RelationshipType createRelationshipType(EntityType sourceType, String sourceIdColName,
            EntityType targetType, String targetIdColName);

    /**
     * @return a sorted list of entity names, in priority order
     */
    public List<String> getEntityNameList() {
        List<EntityType> types = new ArrayList<>(this.entityMap.values());
        Collections.sort(types);
        return types.stream().map(x -> x.getName()).collect(Collectors.toList());
    }

    /**
     * Read the database data from a set of directories.  Each directory should contain instances of the
     * files named in the entity type definitions.  A missing file is not an error.
     *
     * @param inDirs		list of input directories
     *
     * @return a database instance containing all the data in the database
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public DbInstance readDatabase(File... inDirs) throws IOException, ParseFailureException {
        // Create the output database instance.
        List<String> typeNames = this.getEntityNameList();
        DbInstance retVal = this.createDbInstance(typeNames);
        // Initialize the subclass tracking structures.
        retVal.preProcess();
        // Set up the file and directory counters.
        int dirCount = 0;
        int fileCount = 0;
        int recordCount = 0;
        // Loop through the input directories.
        for (File inDir : inDirs) {
            dirCount++;
            log.info("Reading input directory {}: {}.", dirCount, inDir);
            // Loop through the entity types, processing the specified files.
            for (EntityType type : this.entityMap.values()) {
                String typeFileName = type.getFileName();
                if (typeFileName != null) {
                    File inFile = new File(inDir, typeFileName);
                    if (inFile.exists()) {
                        // Here we can read the entities.
                        try (FieldInputStream inStream = FieldInputStream.create(inFile)) {
                            fileCount++;
                            // The builder creates all the line templates for this entity.  Each record
                            // is applied to every template.  Setting up the templates also tells the
                            // input stream the columns we are using.
                            log.info("Processing instance data from {}.", inFile);
                            EntityBuilder builder = new EntityBuilder(type, inStream);
                            // Loop through the records, executing the builder.  This creates all the
                            // entity and relationship instances and compiles the attributes.
                            log.info("Reading instances for {} from {}.", type.getName(), inFile);
                            long lastMsg = System.currentTimeMillis();
                            int inCount = 0;
                            for (var record : inStream) {
                                inCount++;
                                builder.build(record, retVal);
                                long now = System.currentTimeMillis();
                                if (now - lastMsg >= 5000) {
                                    log.info("{} records processed in {}.", inCount, inFile);
                                    lastMsg = now;
                                }
                            }
                            log.info("{} total records processed in {}: new totals are {} entity instances, {} relationship instances.",
                                    inCount, inFile, retVal.getEntityCount(), retVal.getRelCount());
                            recordCount += inCount;
                        }
                    }
                }
            }
        }
        log.info("{} directories and {} files processed.  {} total records processed.", dirCount, fileCount, recordCount);
        retVal.postProcessEntities(this.entityMap.values());
        // Return the built database.
        return retVal;
    }

    /**
     * Create a new, empty database instance.
     *
     * @param typeNames		list of entity type names
     *
     * @return the empty database instance created
     */
    protected abstract DbInstance createDbInstance(List<String> typeNames);

    /**
     * @return the number of input lines read
     */
    public int getLineCount() {
        return this.lineCount;
    }

}
