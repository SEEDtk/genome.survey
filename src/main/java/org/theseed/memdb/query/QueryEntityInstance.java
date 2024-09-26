/**
 *
 */
package org.theseed.memdb.query;

import java.util.Map;
import java.util.TreeMap;

import org.theseed.io.Attribute;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;

/**
 * The query-generation entity instance contains the attribute map and a count of
 * the number of connections to other entity types.
 *
 * @author Bruce Parrello
 *
 */
public class QueryEntityInstance extends EntityInstance {

    // FIELDS
    /** map of attribute names to values */
    private Map<String, Attribute> attributes;
    /** default attribute (always false and empty */
    private static final Attribute NULL_ATTRIBUTE = new Attribute();

    /**
     * Create a query-generation entity instance of the specified type with the specified ID.
     *
     * @param type	entity type
     * @param id	ID of this instance
     */
    public QueryEntityInstance(EntityType type, String id) {
        super(type, id);
        // Create the attribute map.
        this.attributes = new TreeMap<String, Attribute>();
    }

    /**
     * Add an attribute to this entity instance.
     *
     * @param name		attrinute name
     * @param attr		attribute value holder
     */
    protected void addAttribute(String name, Attribute attr) {
        this.attributes.put(name, attr);
    }

    /**
     * @return an attribute with the specified name
     */
    public Attribute getAttribute(String name) {
        return this.attributes.getOrDefault(name, NULL_ATTRIBUTE);
    }

}
