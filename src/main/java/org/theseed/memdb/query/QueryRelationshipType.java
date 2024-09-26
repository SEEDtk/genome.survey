/**
 *
 */
package org.theseed.memdb.query;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationBuilder;
import org.theseed.memdb.RelationshipType;

/**
 * The query-generation relationship type contains a map used to plug field values from relationship records
 * into tagret virtual entity instances. These are only necessary for many-to-many relationships stored in
 * their own tables.
 *
 * @author Bruce Parrello
 *
 */
public class QueryRelationshipType extends RelationshipType {

    // FIELDS
    /** map of input record field names to target entity field names */
    private Map<String, String> targetFieldMap;

    /**
     * Create a relationship type that connects the given source entity to the given target entity.
     *
     * @param sourceType	source entity type
     * @param sourceCol		source entity ID column
     * @param targetType	target entity type
     * @param targetCol		target entity ID column
     */
    public QueryRelationshipType(EntityType sourceType, String sourceCol, EntityType targetType, String targetCol) {
        super(sourceType, sourceCol, targetType, targetCol);
        this.targetFieldMap = new TreeMap<String, String>();
    }

    @Override
    protected RelationBuilder createRelationBuilder(FieldInputStream inStream)
            throws IOException, ParseFailureException {
        return new QueryRelationBuilder(this, inStream);
    }

    /**
     * Store a mapping from an input field name to a target entity field name.
     *
     * @param inputField	name of the input field
     * @param targetField	name of the field in the target entity instance
     */
    public void storeFieldMapping(String inputField, String targetField) {
        this.targetFieldMap.put(inputField, targetField);
    }

    /**
     * @return the field-value map
     */
    protected Map<String, String> getValueMap() {
        return this.targetFieldMap;
    }

}
