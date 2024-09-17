/**
 *
 */
package org.theseed.memdb;

/**
 * A relationship describes a unidirectional crossing between two entity instances.
 * It contains data used to find the target entity instance and is stored in the
 * source entity instance.
 *
 * @author Bruce Parrello
 *
 */
public abstract class RelationshipInstance {

    // FIELDS
    /** target entity type */
    private String targetType;
    /** target entity ID */
    private String targetId;


    /**
     * Create a new relationship instance.
     *
     * @param destType		target entity type name
     * @param id			target instance ID
     */
    public RelationshipInstance(String destType, String id) {
        this.targetType = destType;
        this.targetId = id;
    }

    /**
     * Convstruct a relationship instance with the specified target entity instance
     *
     * @param targetInstance	target entity instance
     */
    public RelationshipInstance(EntityInstance targetInstance) {
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
     * @return the target entity type
     */
    public String getTargetType() {
        return this.targetType;
    }

}
