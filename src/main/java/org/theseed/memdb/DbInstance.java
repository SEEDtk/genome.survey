/**
 *
 */
package org.theseed.memdb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.theseed.stats.Shuffler;

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
    /** master entity instance table */
    private final Map<String, Map<String, EntityInstance>> masterMap;
    /** list of entity type names in priority order */
    private final List<String> typeNames;
    /** entity instance count */
    private int entityCount;
    /** relationship instance count */
    private int relCount;

    /**
     * Create a blank, empty database instance.
     *
     * @param size	number of entity types in this database
     */
    public DbInstance(List<String> types) {
        this.typeNames = types;
        this.masterMap = new TreeMap<>();
    }

    /**
     * Set up tracking data for the database load.
     */
    protected abstract void preProcess();

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
     * @param typeName	entity type name
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
     * Return a collection of randomly-selected entity instances for a given entity type.
     *
     * @param typeName		entity type name
     * @param limit			maximum number of instances to return (or 0 to return all)
     *
     * @return a collection of the entity instances from the entity map
     */
    public Collection<EntityInstance> getSomeEntities(String typeName, int limit) {
        Collection<EntityInstance> retVal = this.getAllEntities(typeName);
        if (limit > 0) retVal = Shuffler.selectPart(retVal, limit);
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
            retVal = new TreeMap<>();
        return retVal;
    }

    /**
     * Post-process all the entity types after a load to perform final cleanup and compute totals.
     *
     * @param entityTypes	list of entity type objects for the database
     */
    protected abstract void postProcessEntities(Collection<EntityType> entityTypes);

    /**
     * @return the number of entity instances created
     */
    public int getEntityCount() {
        return this.entityCount;
    }

    /**
     * Increment the entity instance count.
     *
     * @param entityCount 	amount to increment
     */
    protected void addEntityCount(int entityCount) {
        this.entityCount += entityCount;
    }

    /**
     * @return the number of relationship instances created
     */
    protected int getRelCount() {
        return this.relCount;
    }

    /**
     * Increment the relationship instance count.
     *
     * @param relCount 	amount to increment
     */
    protected void addRelCount(int relCount) {
        this.relCount += relCount;
    }

}
