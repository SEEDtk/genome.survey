/**
 *
 */
package org.theseed.walker;

import java.io.PrintWriter;
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
public class DbInstance {

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
     * Write out a random walk through the database.  This is a destructive operation, as entities, relationships,
     * and attributes are deleted from the database instance as they are used.
     *
     * @param writer	output writer for the generated text
     */
    public void GenerateWalk(PrintWriter writer) {
        // Loop until we empty the type name list.
        while (! this.typeNames.isEmpty()) {
            // Get the first available entity instance.
            String typeName = this.typeNames.get(0);
            var entityMap = this.masterMap.get(typeName);
            if (entityMap == null || entityMap.isEmpty())
                this.typeNames.remove(0);
            else {
                EntityInstance first = entityMap.values().iterator().next();
                this.processEntity(writer, first);
            }
        }
    }

    /**
     * Produce the longest possible walk from the specified entity instance.
     *
     * @param writer	current output text writer
     * @param first		entity instance from which to start the walk.
     */
    private void processEntity(PrintWriter writer, EntityInstance first) {
        EntityInstance nextEntity = first;
        while (nextEntity != null) {
            // Check for an attribute to write.
            boolean found = nextEntity.popAttribute(writer);
            // Check for a relationship to write.
            EntityInstance target = nextEntity.popRelationship(writer, this);
            if (target == null && ! found) {
                // Here we have no more data on this entity, so we need to delete it.
                this.removeFromMap(nextEntity);
            }
            // Keep walking.  If the target was NULL, we will stop.
            nextEntity = target;
        }
    }

    /**
     * Remove the specified entity instance from the master map.
     *
     * @param curr		entity instance to remove
     */
    private void removeFromMap(EntityInstance curr) {
        String type = curr.getType();
        var entityMap = this.masterMap.get(type);
        if (entityMap != null) {
            String id = curr.getId();
            entityMap.remove(id);
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
        EntityInstance retVal = entityMap.computeIfAbsent(entityId, x -> new EntityInstance(entityType, entityId));
        return retVal;
    }

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

}
