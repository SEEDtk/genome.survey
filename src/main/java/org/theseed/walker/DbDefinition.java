/**
 *
 */
package org.theseed.walker;

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
 * The definition file contains commands (denoted by "#" in the first column) and templates.  The
 * commands have a command name (currently "Entity" or "Relationship") immediately after the pound
 * sign, and one or more space-delimited parameters.
 *
 * The Entity command's parameters are (1) entity type name, (2) entity ID column name, (3) priority
 * number (with 0 being the lowest), and (4) a file name.  The file name should be the base name of
 * the file in each directory.  The priority is used to determine which entities should be chosen first
 * when beginning a random walk.  Thus, if "Genome" has the highest priority, the walker will choose
 * Genome records to start each walk until they run out.
 *
 * Some entities are actually many-to-many or ternary relationship.  We don't want these put into the
 * database; rather they are used to generate relationship records in other entities.  For these,
 * we specify "null" for the ID column.
 *
 * Immediately after the entity header are zero or more attribute templates.  These are used to generate
 * attribute descriptions for the entity.
 *
 * The relationship definitions follow each entity.  A relationship definition describes a many-to-one
 * connection between entities and contains a template for the forward direction and the reverse direction.
 * The Relationship command's parameters are (1) source entity type name, (2) column name for the source entity ID,
 * (3) target entity type name, and (4) column name for the target entity ID.  In a normal entity, the source
 * entity type name and ID column name describe the current entity itself.  Thus, a relationship in the Genome
 * entity will specify "Genome" as the source entity type and "genome_id" as the source ID column name.  In
 * the case of a relationship entity, however, the source entity type must be a different entity.  Thus, the
 * SubsystemCell ternary relationship entity never specifies itself as a source; rather, the source is either
 * Subsystem, Genome, or Role. The relationship definition is always three lines:  the header, the forward
 * template, and the converse template.
 *
 * @author Bruce Parrello
 *
 */
public class DbDefinition {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(DbDefinition.class);
    /** map of entity type names to type objects */
    private Map<String, EntityType> entityMap;
    /** number of lines read */
    private int lineCount;
    /** number of attributes read */
    private int attrCount;
    /** number of relationships processed */
    private int relCount;

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
        this.entityMap = new TreeMap<String, EntityType>();
        // Open up the input file and read the definition lines.
        try (LineReader inStream = new LineReader(fileName)) {
            // Prepare an iterator through the file.
            Iterator<String> iter = inStream.iterator();
            if (! iter.hasNext())
                throw new IOException("No data found in database definition file " + fileName + ".");
            String line = iter.next();
            // Initialize our counters.
            this.lineCount = 1;
            this.attrCount = 0;
            this.relCount = 0;
            // Loop through the entity definitions.
            while (line != null) {
                if (! line.startsWith("#Entity"))
                    throw new ParseFailureException("Expecting an #Entity line at line " + lineCount + " in " + fileName + ".");
                line = this.processEntityDefinition(line, iter);
            }
        }
        log.info("{} lines read from {}.  {} entities, {} relationships, and {} attributes found.", this.lineCount, fileName,
                this.entityMap.size(), this.relCount, this.attrCount);
    }

    /**
     * Read an entity definition into the database structures.  This process stops when it hits end-of-file
     * or a new entity header.
     *
     * @param line		line containing the entity header
     * @param iter		input file iterator
     *
     * @return the line containing the next entity header, or NULL if we hit end-of-file
     *
     * @throws ParseFailureException
     */
    private String processEntityDefinition(String line, Iterator<String> iter) throws ParseFailureException {
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
        String retVal = this.readAttributes(entity, iter);
        // Try to read the relationships.
        retVal = this.readRelationships(entity, iter, retVal);
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
        return this.entityMap.computeIfAbsent(typeName, x -> new EntityType(x));
    }

    /**
     * Read the next line and update the line count.
     *
     * @param iter	iterator through the input file
     *
     * @return the next input line, or NULL if we are at end-of-file
     */
    private String readNext(Iterator<String> iter) {
        String line;
        if (iter.hasNext()) {
            line = iter.next();
            this.lineCount++;
        } else
            line = null;
        return line;
    }

    /**
     * Process the attributes of the current entity.
     *
     * @param entity	entity type to contain the attributes
     * @param iter		iterator through the input file
     *
     * @return the first relationship header for this entity, the next entity header, or NULL for end-of-file
     */
    private String readAttributes(EntityType entity, Iterator<String> iter) {
        String retVal = null;
        if (iter.hasNext()) {
            String line = iter.next();
            this.lineCount++;
            while (line != null && ! line.startsWith("#")) {
                // Here we have an attribute line.
                entity.addAttribute(line);
                this.attrCount++;
                line = this.readNext(iter);
            }
            // Save the last line read.  It belongs to the next groupo.
            retVal = line;
        }
        return retVal;
    }

    /**
     * Process the relationships for the current entity.
     *
     * @param entity	entity type to contain the attributes
     * @param iter		iterator through the input file
     * @param line		header line for the first relationship, the next entity, or NULL for end-of-file
     *
     * @return the next entity header, or NULL for end-of-file
     *
     * @throws ParseFailureException
     */
    private String readRelationships(EntityType entity, Iterator<String> iter, String line) throws ParseFailureException {
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
            RelationshipType rel = new RelationshipType(sourceType, parms[2], targetType, parms[4]);
            // Read the forward-direction template sentence.
            if (! iter.hasNext())
                throw new ParseFailureException("Missing template strings for relationship in line " + this.lineCount + " of definition file.");
            String forward = iter.next();
            // Read the converse-direction template sentence.
            if (! iter.hasNext())
                throw new ParseFailureException("Missing converse-direction template string for relationship in line " +
                        this.lineCount + " of defition file.");
            String converse = iter.next();
            // Record the two lines read and the fact we have a new relationship type.
            this.lineCount += 2;
            this.relCount++;
            // Store the template strings.
            rel.setTemplateStrings(forward, converse);
            // Save the relationship.
            entity.addRelationship(rel);
            // Get the next record.
            retVal = this.readNext(iter);
        }
        return retVal;
    }

    /**
     * @return a sorted list of entity names, in priority order
     */
    public List<String> getEntityNameList() {
        List<EntityType> types = new ArrayList<EntityType>(this.entityMap.values());
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
        DbInstance retVal = new DbInstance(typeNames);
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
                            log.info("Compiling templates for {}.", inFile);
                            EntityType.Builder builder = type.new Builder(inStream);
                            // Loop through the records, executing the builder.  This creates all the
                            // entity and relationship instances and compiles the attributes.
                            log.info("Reading instances for {} from {}.", type.getName(), inFile);
                            long lastMsg = System.currentTimeMillis();
                            int inCount = 0;
                            int skipCount = 0;
                            for (var record : inStream) {
                                inCount++;
                                EntityInstance newInstance = builder.build(record, retVal);
                                if (newInstance == null)
                                    skipCount++;
                                long now = System.currentTimeMillis();
                                if (now - lastMsg >= 5000) {
                                    log.info("{} records processed in {}.", inCount, inFile);
                                    lastMsg = now;
                                }
                            }
                            log.info("{} total records processed in {}: {} skipped.", inCount, inFile, skipCount);
                            recordCount += inCount;
                        }
                    }
                }
            }
        }
        log.info("{} directories and {} files processed.  {} total records processed.", dirCount, fileCount, recordCount);
        // Now we loop through the entity instances.  For each one, we shuffle the attribute and relationship lists to
        // get them in random order, and then we output the total token and instance counts for each entity type.
        long tokenTotal = 0;
        for (EntityType type : this.entityMap.values()) {
            String typeName = type.getName();
            long typeTokens = type.getTokenCount();
            tokenTotal += typeTokens;
               log.info("Entity type {} has {} instances and generated {} tokens.", typeName, retVal.getTypeCount(typeName), typeTokens);
            for (EntityInstance instance : retVal.getAllEntities(typeName))
                instance.shuffleAll();
        }
        log.info("{} total tokens generated in database.", tokenTotal);
        // Return the built database.
        return retVal;
    }

}
