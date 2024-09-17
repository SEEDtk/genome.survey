/**
 *
 */
package org.theseed.memdb.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.theseed.counters.CountMap;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;

/**
 * The query-generation database contains no private data at the current time, just
 * entities full of populated attributes and connections.
 *
 * @author Bruce Parrello
 *
 */
public class QueryDbInstance extends DbInstance {

    /**
     * Construct a query-generation database instance.
     *
     * @param types		list of entity type names
     */
    public QueryDbInstance(List<String> types) {
        super(types);
    }

    @Override
    protected void preProcess() {
        // No pre-processing is needed.
    }

    @Override
    protected EntityInstance createEntity(EntityType entityType, String entityId) {
        return new QueryEntityInstance(entityType, entityId);
    }

    @Override
    protected void postProcessEntities(Collection<EntityType> entityTypes) {
        // No post-processing is needed.
    }

}
