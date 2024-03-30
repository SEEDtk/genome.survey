/**
 *
 */
package org.theseed.walker;

/**
 * A relationship describes a unidirectional crossing between two entity instances.
 * It contains a sentence describing the crossing and the target entity instance
 * and is stored in the source entity instance.
 *
 * @author Bruce Parrello
 *
 */
public class RelationshipInstance {

    // FIELDS
    /** relationship crossing sentence */
    private String crossingSentence;
    /** target entity type */
    private String targetType;
    /** target entity ID */
    private String targetId;


    /**
     * Create a new relationship instance.
     *
     * @param sentence		crossing sentence
     * @param destType		target entity type name
     * @param id			target instance ID
     */
    public RelationshipInstance(String sentence, String destType, String id) {
        this.crossingSentence = sentence;
        this.targetType = destType;
        this.targetId = id;
    }

    /**
     * Convstruct a relationship instance with the specified text string and target entity.
     *
     * @param connectString		relationship description string
     * @param targetInstance	target entity instance
     */
    public RelationshipInstance(String connectString, EntityInstance targetInstance) {
        this.crossingSentence = connectString;
        this.targetType = targetInstance.getType();
        this.targetId = targetInstance.getId();
    }

    /**
     * Find the target entity instance for this relationship instance.
     *
     * @param db	controlling database instance containing the entity map
     *
     * @return the target instance, or NULL if it has been exhausted
     */
    public EntityInstance getTarget(DbInstance db) {
        return db.getEntity(this.targetType, this.targetId);
    }

    /**
     * @return the crossing sentence
     */
    public String getSentence() {
        return this.crossingSentence;
    }

}
