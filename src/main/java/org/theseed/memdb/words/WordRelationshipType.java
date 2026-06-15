package org.theseed.memdb.words;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationBuilder;
import org.theseed.memdb.RelationshipType;

public class WordRelationshipType extends RelationshipType {

    // FIELDS
    /** source string */
    private String sourceTemplate;
    /** relationship name string */
    private String nameTemplate;
    /** target string */
    private String targetTemplate;

    /**
     * Construct a relationship type for a specified relationship.
     *
     * @param sourceType	source entity type
     * @param sourceCol		column name of source entity ID in record
     * @param targetType	target entity type
     * @param targetCol		column name of target entity ID in record
     */
    public WordRelationshipType(EntityType sourceType, String sourceCol, EntityType targetType, String targetCol) {
        super(sourceType, sourceCol, targetType, targetCol);
        this.sourceTemplate = null;
        this.nameTemplate = null;
        this.targetTemplate = null;
    }

    @Override
    protected RelationBuilder createRelationBuilder(FieldInputStream inStream)
            throws IOException, ParseFailureException {
        return new WordRelationBuilder(this, inStream);
    }

    /**
     * @return the template string for the source entity ID
     */
    public String getSourceString() {
        return this.sourceTemplate;
    }

    /**
     * @return the template string for the relationship name
     */
    public String getNameString() {
        return this.nameTemplate;
    }

    /**
     * @return the template string for the target entity ID
     */
    public String getTargetString() {
        return this.targetTemplate;
    }

    /**
     * Specify the template string for the source entity ID.
     * 
     * @param sourceTemplate    the template string for the source entity ID
     */
    public void setSourceString(String sourceTemplate) {
        this.sourceTemplate = sourceTemplate;
    }

    /**
     * Specify the template string for the relationship name.
     * 
     * @param nameTemplate    the template string for the relationship name
     */
    public void setNameString(String nameTemplate) {
        this.nameTemplate = nameTemplate;
    }

    /**
     * Specify the template string for the target entity ID.
     * 
     * @param targetTemplate    the template string for the target entity ID
     */
    public void setTargetString(String targetTemplate) {
        this.targetTemplate = targetTemplate;
    }

}
