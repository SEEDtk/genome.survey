/**
 *
 */
package org.theseed.walker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.template.LineTemplate;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;


/**
 * An entity type contains instructions for reading an entity.  To read an entity, we need to know the
 * file name, the type name, the template strings for the attributes, and the definitions of the
 * relationships.  When we create an entity instance, we will use these to build the output sentences
 * for the attributes and the relationships.  In the case of the relationship, we will also need to
 * know the ID and type of the destination entity.  This information is in the relationship definition.
 * Note that some entities don't have files and are only built as a result of a relationship crossing.
 *
 * An entity type is identified solely by name.  The types are sorted by descending priority (0 is lowest)
 * followed by name.
 *
 * If the ID column name is "generated", then the entity IDs are generated at run-time.
 *
 * @author Bruce Parrello
 *
 */
public class EntityType implements Comparable<EntityType> {

    // FIELDS
    /** entity type name */
    private String name;
    /** entity input file name */
    private String fileName;
    /** ID column name */
    private String idColName;
    /** list of attribute templates */
    private List<String> attributeStrings;
    /** list of relationship definitions */
    private List<RelationshipType> relationships;
    /** instance count */
    private long instanceCount;
    /** priority */
    private int priority;
    /** token counter */
    private long tokenCount;

    /**
     * This is a helper class for building an entity instance from an entity type.
     */
    public class Builder {

        /** ID column index, or -1 for generated */
        private int idColIdx;
        /** attribute templates */
        private Collection<LineTemplate> attributeBuilders;
        /** relationship builders */
        private Collection<RelationBuilder> relationBuilders;
        /** encoding for token counter */
        private Encoding encoder;

        /**
         * Create an instance builder for this entity type on a given input stream.
         *
         * @param instanceStream	input stream for entity instances
         *
         * @throws IOException
         * @throws ParseFailureException
         */
        public Builder(FieldInputStream instanceStream) throws IOException, ParseFailureException {
            this.attributeBuilders = EntityType.this.getAttributeTemplates(instanceStream);
            this.relationBuilders = EntityType.this.getRelationshipBuilders(instanceStream);
            if (EntityType.this.idColName == null)
                this.idColIdx = -1;
            else
                this.idColIdx = instanceStream.findField(EntityType.this.idColName);
            EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
            this.encoder = registry.getEncoding(EncodingType.CL100K_BASE);
        }

        /**
         * Compute the ID of the current entity instance.
         *
         * @param record	instance input record
         */
        protected String computeId(FieldInputStream.Record record) {
             String retVal;
             long count = EntityType.this.getNewInstanceCount();
             if (this.idColIdx < 0) {
                 // Here we have a generated ID.
                 retVal = Long.toHexString(count);
             } else {
                 // Here there is a natural ID in the input record.
                 retVal = record.get(this.idColIdx);
             }
             return retVal;
        }

        /**
         * Build an entity instance from an input record.
         *
         * @param record	input record to use
         * @param db		database instance containing the entities
         *
         * @return the new entity instance, or NULL if there is none in this record
         */
        public EntityInstance build(FieldInputStream.Record record, DbInstance db) {
            EntityInstance retVal = null;
            // Get the entity ID.
            String entityId = this.computeId(record);
            // Only proceed if we have an ID for this entity.
            if (! StringUtils.isBlank(entityId)) {
                // Find or create the entity instance.
                retVal = db.findEntity(EntityType.this, entityId);
                // Loop through the attribute strings, creating the attributes.
                int tokenCount = 0;
                for (LineTemplate template : this.attributeBuilders) {
                    String attributeString = template.apply(record);
                    if (! StringUtils.isBlank(attributeString)) {
                        retVal.addAttribute(attributeString);
                        tokenCount += this.encoder.countTokens(attributeString);
                    }
                }
                // Loop through the relationship builders, creating the relationship instances
                // in each direction.
                for (RelationBuilder builder : this.relationBuilders) {
                    String forwardString = builder.getForwardString(record);
                    EntityInstance targetInstance = builder.getTarget(record, db);
                    // Only proceed if there is a target at the other end.
                    if (targetInstance != null) {
                        String converseString = builder.getConverseString(record);
                        retVal.addConnection(new RelationshipInstance(forwardString, targetInstance));
                        targetInstance.addConnection(new RelationshipInstance(converseString, retVal));
                        tokenCount += this.encoder.countTokens(forwardString) + this.encoder.countTokens(converseString);
                    }
                }
                // Update the token count.
                EntityType.this.tokenCount += tokenCount;
            }
            return retVal;
        }

    }


    /**
     * Create a new, blank entity type.
     *
     * @param name		entity type name
     */
    public EntityType(String name) {
        this.name = name;
        this.fileName = null;
        this.idColName = null;
        this.attributeStrings = new ArrayList<String>();
        this.relationships = new ArrayList<RelationshipType>();
        this.instanceCount = 0;
        this.tokenCount = 0;
        this.priority = 0;
    }

    /**
     * Increment the instance count and return it.
     *
     * @return the new instance count
     */
    public long getNewInstanceCount() {
        this.instanceCount++;
        return this.instanceCount;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EntityType)) {
            return false;
        }
        EntityType other = (EntityType) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(EntityType o) {
        int retVal = o.priority - this.priority;
        if (retVal == 0)
            retVal = this.name.compareTo(o.name);
        return retVal;
    }

    /**
     * Specify the file name.
     *
     * @param nameString	base name of the file containing entity instances
     */
    public void setFileName(String nameString) {
        this.fileName = nameString;
    }

    /**
     * Add a new attribute template string to this entity.
     *
     * @param line	line containing the template string
     */
    public void addAttribute(String line) {
        this.attributeStrings.add(line);
    }

    /**
     * Specify the name of this entity type's ID column
     *
     * @param idCol		the id column name
     */
    public void setIdColName(String idCol) {
        if (idCol.contentEquals("generated"))
            this.idColName = null;
        else
            this.idColName = idCol;
    }

    /**
     * Specify the priority of this entity.  In the random walk, lower highest priority numbers
     * will go first.
     *
     * @param prio	new priority number
     */
    public void setPriority(int prio) {
        this.priority = prio;
    }

    /**
     * @return the name of this entity type
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the name of the file containing this entity's instances
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Compute the set of attribute line templates for this entity using the specified
     * input stream.
     *
     * @param inStream		input stream for this entity's instances
     *
     * @return a collection of line templates for all the attribute sentences
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    public Collection<LineTemplate> getAttributeTemplates(FieldInputStream inStream) throws IOException, ParseFailureException {
        List<LineTemplate> retVal = new ArrayList<LineTemplate>(this.attributeStrings.size());
        for (String attributeString : this.attributeStrings) {
            LineTemplate attributeTemplate = new LineTemplate(inStream, attributeString, null);
            retVal.add(attributeTemplate);
        }
        return retVal;
    }

    /**
     * @return the name of the ID column in the entity's instance file
     */
    public String getIdColName() {
        return this.idColName;
    }

    /**
     * Create a set of relationship builders for each relationship connected to this entity.
     *
     * @param inStream	input stream for entity instances
     *
     * @return a collection of relationship builders
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    public Collection<RelationBuilder> getRelationshipBuilders(FieldInputStream inStream) throws IOException, ParseFailureException {
        List<RelationBuilder> retVal = new ArrayList<RelationBuilder>(this.relationships.size());
        for (RelationshipType relType : this.relationships) {
            RelationBuilder builder = new RelationBuilder(EntityType.this, relType, inStream);
            retVal.add(builder);
        }
        return retVal;
    }

    /**
     * @return the number of attributes for this entity type
     */
    public int getAttributeCount() {
        return this.attributeStrings.size();
    }

    /**
     * @return the number of tokens generated building this entity
     */
    public long getTokenCount() {
        return this.tokenCount;
    }

    /**
     * @return the number of instances of this entity type
     */
    public long getInstanceCount() {
        return this.instanceCount;
    }

    /**
     * Add a new relationship type to this entity type.
     *
     * @param rel	new relationship type
     */
    public void addRelationship(RelationshipType rel) {
        this.relationships.add(rel);
    }


}
