/**
 *
 */
package org.theseed.memdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private final String entityId;
    /** type name of this entity */
    private final String entityType;
    /** list of relationship instances */
    private final List<RelationshipInstance> connections;

    /**
     * Create a new, empty entity instance.
     *
     * @param type		type of the new entity
     * @param id		ID of the instance
     */
    public EntityInstance(EntityType type, String id) {
        this.entityId = id;
        this.entityType = type.getName();
        this.connections = new ArrayList<>();
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
     * @return the list of relationship instance
     */
    public List<RelationshipInstance> getRelationships() {
        return this.connections;
    }

    /**
     * @return the list of related entity instances of the specified type
     *
     * @param db		relevant database instance
     * @param typeName	name of the desired target entity type
     */
    public List<EntityInstance> getTargetsOfType(DbInstance db, String typeName) {
        List<EntityInstance> retVal = new ArrayList<>();
        for (RelationshipInstance connection : this.connections) {
            if (typeName.equals(connection.getTargetType()))
                retVal.add(connection.getTarget(db));
        }
        return retVal;
    }

    @Override
    public String toString() {
        return this.entityType + "[" + this.entityId + "]";
    }

    @Override
    public int hashCode() {
        int retVal = 5;
        retVal = 29 * retVal + Objects.hashCode(this.entityId);
        retVal = 29 * retVal + Objects.hashCode(this.entityType);
        return retVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EntityInstance other = (EntityInstance) obj;
        if (!Objects.equals(this.entityId, other.entityId)) {
            return false;
        }
        return Objects.equals(this.entityType, other.entityType);
    }

}
