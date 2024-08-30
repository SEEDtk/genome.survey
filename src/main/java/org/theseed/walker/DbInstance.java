/**
 *
 */
package org.theseed.walker;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A database instance contains the data described by a DbDefinition.  The data is stored in a
 * massive two-level hash keyed on entity type name, each consisting of a hash of entity instances
 * keyed by ID.
 *
 * @author Bruce Parrello
 *
 */
public abstract class DbInstance {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(DbInstance.class);
    /** master entity instance table */
    private Map<String, Map<String, EntityInstance>> masterMap;
    /** list of entity type names in priority order */
    private List<String> typeNames;

    /**
     * Create a blank, empty database instance.
     *
     * @param size	number of entity types in this database
     */
    public DbInstance(List<String> types) {
        this.typeNames = types;
        this.masterMap = new TreeMap<String, Map<String, EntityInstance>>();
    }

    /**
     * Remove the specified entity instance from the master map.
     *
     * @param curr		entity instance to remove
     */
    protected void removeFromMap(EntityInstance curr) {
        String type = curr.getType();
        var entityMap = this.masterMap.get(type);
        if (entityMap != null) {
            String id = curr.getId();
            entityMap.remove(id);
            // Denote this entity instance is gone.
            curr.setDeleted();
            // If there are no more instances of this type, remove the map.
            if (entityMap.isEmpty())
                this.masterMap.remove(type);
        }
    }

    /**
     * Find the specified entity instance.
     *
     * @param type		entity type name
     * @param id		entity instance ID
     *
     * @return the desired entity instance, or NULL if it is exhausted
     */
    public EntityInstance getEntity(String type, String id) {
        EntityInstance retVal = null;
        var entityMap = this.masterMap.get(type);
        if (entityMap != null)
            retVal = entityMap.get(id);
        return retVal;
    }

    /**
     * Store a mew entity instance in this database.
     *
     * @param entityType		type of new entity
     * @param entityId			ID of new entity
     * @param entityInstance	instance to store
     */
    public void putEntity(EntityType entityType, String entityId, EntityInstance entityInstance) {
        var entityMap = this.masterMap.computeIfAbsent(entityType.getName(),
                x -> new HashMap<String, EntityInstance>());
        entityMap.put(entityId, entityInstance);
    }

    /**
     * Find or create a new entity instance.
     *
     * @param entityType		type of new entity
     * @param entityId			ID of new entity
     */
    public EntityInstance findEntity(EntityType entityType, String entityId) {
        var entityMap = this.masterMap.computeIfAbsent(entityType.getName(),
                x -> new HashMap<String, EntityInstance>());
        EntityInstance retVal = entityMap.computeIfAbsent(entityId, x -> this.createEntity(entityType, entityId));
        return retVal;
    }

    /**
     * Create a new entity instance with the specified type and ID.
     *
     * @param entityType	entity type to create
     * @param entityId		ID of entity to create
     *
     * @return the new entity instance
     */
    protected abstract EntityInstance createEntity(EntityType entityType, String entityId);

    /**
     * Return a collection of entity instances for a given entity type.
     *
     * @param 	typeName	entity type name
     *
     * @return a collection of the entity instances from the entity map
     */
    public Collection<EntityInstance> getAllEntities(String typeName) {
        Collection<EntityInstance> retVal;
        Map<String, EntityInstance> entityMap = this.masterMap.get(typeName);
        if (entityMap == null)
            retVal = Collections.emptyList();
        else
            retVal = entityMap.values();
        return retVal;
    }

    /**
     * @return the number of instances of the specified entity type
     *
     * @param typeName	name of the relevant type
     */
    public int getTypeCount(String typeName) {
        int retVal = 0;
        Map<String, EntityInstance> entityMap = this.masterMap.get(typeName);
        if (entityMap != null)
            retVal = entityMap.size();
        return retVal;
    }

    /**
     * @return the list of entity type names for this database
     */
    public List<String> getTypeNames() {
        return this.typeNames;
    }

    /**
     * @return the map of IDs to instances for the named entity type
     *
     * @param type	name of the entity type of interest
     */
    public Map<String, EntityInstance> getEntityMap(String type) {
        // The only tricky part is returning an empty map if no entity instances exist for the type.
        var retVal = this.masterMap.get(type);
        if (retVal == null)
            retVal = new TreeMap<String, EntityInstance>();
        return retVal;
    }

}
