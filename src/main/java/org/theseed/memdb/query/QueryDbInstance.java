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
 * The query-generation database contains statistical information about the attributes
 * in the data, which helps us to compute optimal queries for testing large-language models.
 *
 * @author Bruce Parrello
 *
 */
public class QueryDbInstance extends DbInstance {

    // FIELDS
    /** map of entity type name -> attribute name -> value counts */
    private Map<String, Map<String, CountMap<String>>> masterCountMap;

    /**
     * Construct a query-generation database instance.
     *
     * @param types		list of entity type names
     */
    public QueryDbInstance(List<String> types) {
        super(types);
        this.masterCountMap = new TreeMap<String, Map<String, CountMap<String>>>();
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

    /**
     * Record the values of an attribute in the attribute-count map.
     *
     * @param type		relevant entity type name
     * @param attrName	relevant attribute name
     * @param list		list of attribute values
     */
    protected void recordAttribute(String type, String attrName, List<String> list) {
        // Insure we have an attribute map for this entity type.
        Map<String, CountMap<String>> attrCountMap = this.masterCountMap.computeIfAbsent(type, x -> new TreeMap<String, CountMap<String>>());
        // Get the count map for this attribute.
        CountMap<String> countMap = attrCountMap.computeIfAbsent(attrName, x -> new CountMap<String>());
        // Count the attribute. Note that each list element is counted on its own.
        for (String value : list) {
            if (! StringUtils.isBlank(value))
                countMap.count(value);
        }
    }

}
