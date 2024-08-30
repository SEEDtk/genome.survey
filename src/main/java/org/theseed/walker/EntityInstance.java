/**
 *
 */
package org.theseed.walker;

import java.util.List;

import java.util.ArrayList;

/**
 * An entity instance describes a single entity occurrence.  It contains all of the relationship
 * instances that describe the connections. The sublass adds any attribute data.
 *
 * @author Bruce Parrello
 *
 */
public abstract class EntityInstance {

    // FIELDS
    /** ID of this entity */
    private String entityId;
    /** type name of this entity */
    private String entityType;
    /** list of relationship instances */
    private List<RelationshipInstance> connections;
    /** TRUE if this instance has been deleted */
    private boolean deleted;

    /**
     * Create a new, empty entity instance.
     *
     * @param type		type of the new entity
     * @param id		ID of the instance
     */
    public EntityInstance(EntityType type, String id) {
        this.entityId = id;
        this.entityType = type.getName();
        this.connections = new ArrayList<RelationshipInstance>();
    }

    /**
     * @return the type name of this entity instance
     */
    public String getType() {
        return this.entityType;
    }

    /**
     * @return the ID of this entity instance
     */
    public String getId() {
        return this.entityId;
    }

    /**
     * Add a relationship connection to this entity instance.
     *
     * @param connection	new connection to add
     */
    public void addConnection(RelationshipInstance rel) {
        this.connections.add(rel);
    }

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

}
