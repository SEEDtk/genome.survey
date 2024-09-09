/**
 *
 */
package org.theseed.memdb.query;

import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.RelationshipInstance;

/**
 * The query-generation relationship instance contains no additional data.
 *
 * @author Bruce Parrello
 *
 */
public class QueryRelationshipInstance extends RelationshipInstance {

    /**
     * Construct a query-generation relationship instance for a given target entity type
     * and ID.
     *
     * @param destType	target entity instance type
     * @param id		target entity instance ID
     */
    public QueryRelationshipInstance(String destType, String id) {
        super(destType, id);
    }

    /**
     * Construct a query-generation relationship instance for a given target entity instance.
     *
     * @param targetInstance	target entity instance
     */
    public QueryRelationshipInstance(EntityInstance targetInstance) {
        super(targetInstance);
    }

}
