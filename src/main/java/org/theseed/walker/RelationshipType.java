/**
 *
 */
package org.theseed.walker;

/**
 * A relationship type describes a connection between two entity types.  It only exists
 * for the many-to-one direction.  It consists of a template for producing the output sentence
 * in each direction, the source and target entity types, and the column name
 * for the ID of the target entity.  When we process an entity instance, we will use
 * the relationship types to compute the crossings to the target entities, and then
 * attach the converse crossing to those targets.
 *
 * @author Bruce Parrello
 *
 */
public class RelationshipType {

    // FIELDS
    /** source entity type */
    private EntityType source;
    /** target entity type */
    private EntityType target;
    /** forward relationship template string */
    private String forwardSentence;
    /** converse relationship sentence string */
    private String converseSentence;
    /** source entity column name */
    private String sourceColName;
    /** target entity column name */
    private String targetColName;

    /**
     * Construct a relationship type for a specified relationship.
     *
     * @param sourceType	source entity type
     * @param sourceCol		column name of source entity ID in record
     * @param targetType	target entity type
     * @param targetCol		column name of target entity ID in record
     */
    public RelationshipType(EntityType sourceType, String sourceCol, EntityType targetType, String targetCol) {
        this.source = sourceType;
        this.sourceColName = sourceCol;
        this.target = targetType;
        this.targetColName = targetCol;
        this.forwardSentence = "";
        this.converseSentence = "";
    }

    /**
     * @return the forward-direction template string
     */
    public String getForwardSentence() {
        return this.forwardSentence;
    }

    /**
     * @return the converse-direction template string
     */
    public String getConverseSentence() {
        return this.converseSentence;
    }

    /**
     * Specify the forward and converse template strings.
     *
     * @param forward		forward-direction (many-to-one) template string
     * @param converse		converse-direction (one-to-many) template string
     */
    public void setTemplateStrings(String forward, String converse) {
        this.forwardSentence = forward;
        this.converseSentence = converse;
    }

    /**
     * @return the target entity ID column name
     */
    public String getTargetColName() {
        return this.targetColName;
    }

    /**
     * @return the target entity type
     */
    public EntityType getTargetType() {
        return this.target;
    }

    /**
     * @return the column name containing the source entity ID
     */
    public String getSourceColName() {
        return this.sourceColName;
    }

    /**
     * @return the source entity type
     */
    public EntityType getSourceType() {
        return this.source;
    }

}
