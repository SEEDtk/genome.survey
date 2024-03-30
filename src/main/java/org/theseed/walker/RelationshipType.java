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
    /** target entity type */
    private EntityType target;
    /** forward relationship template string */
    private String forwardSentence;
    /** converse relationship sentence string */
    private String converseSentence;
    /** target entity column name */
    private String targetColName;

    /**
     * Construct a relationship type for a specified relationship.
     *
     * @param targetType	target entity type
     * @param targetCol		column name of target entity ID in source column
     */
    public RelationshipType(EntityType targetType, String targetCol) {
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

}
