/**
 *
 */
package org.theseed.walker;

/**
 * This is the relationship type for a template text-walk database.  In addition to the base-class data,
 * it contains a template for producing the output sentence in each direction. When we process an entity
 * instance, we will use the relationship types to compute the crossings to the target entities, and then
 * attach the converse crossing to those targets.
 *
 * @author Bruce Parrello
 *
 */
public class TextRelationshipType extends RelationshipType {

    // FIELDS
    /** forward relationship template string */
    private String forwardSentence;
    /** converse relationship sentence string */
    private String converseSentence;

    /**
     * Construct a relationship type for a specified relationship.
     *
     * @param sourceType	source entity type
     * @param sourceCol		column name of source entity ID in record
     * @param targetType	target entity type
     * @param targetCol		column name of target entity ID in record
     */
    public TextRelationshipType(EntityType sourceType, String sourceCol, EntityType targetType, String targetCol) {
        super(sourceType, sourceCol, targetType, targetCol);
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

}
