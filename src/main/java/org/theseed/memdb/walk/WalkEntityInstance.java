package org.theseed.memdb.walk;

import java.io.PrintWriter;

import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;

/**
 * The entity instance for a walk database contains the attributes, the relationships, and an indication of whether
 * or not the instance has been deleted from the walk. The format of the attributes and relationships is determined by
 * the subclass. Abstract methods are provided to ensure that the logic for walking the database is consistent across all 
 * subclasses.
 */
public abstract class WalkEntityInstance extends EntityInstance {

    // FIELDS
    /** TRUE if this instance has been deleted */
    private boolean deleted;

    /**
     * Create a new text entity instance.
     *
     * @param type	entity type
     * @param id	ID of the instance
     */
    public WalkEntityInstance(EntityType type, String id) {
        super(type, id);
        // Start un-deleted.
        this.deleted = false;
    }

    /**
     * Reorder the attributes and relationships in this entity instance.
     */
    public abstract void shuffleAll();

    /**
     * @return TRUE if this entity instance is deleted
     */
    public boolean isDeleted() {
        return this.deleted;
    }

    /**
     * Denote that this entity instance is deleted.
     */
    public void setDeleted() {
        this.deleted = true;
    }

    /**
     * Add an attribute to this instance.
     *
     * @param attribute		attribute string to add
     */
    public abstract void addAttribute(String attribute);

    /**
     * Emit an attribute for this entity and remove it from the attribute list.
     *
     * @param writer	output writer to contain the text
     *
     * @return TRUE if an attribute was written, else FALSE
     */
    public abstract boolean popAttribute(PrintWriter writer);

    /**
     * Emit a relationship for this entity and return the target entity instance.
     *
     * @param writer	output text writer
     * @param db		controlling database instance
     *
     * @return the target entity instance, or NULL if there is none available
     */
    public abstract WalkEntityInstance popRelationship(PrintWriter writer, WalkDbInstance db);

}
