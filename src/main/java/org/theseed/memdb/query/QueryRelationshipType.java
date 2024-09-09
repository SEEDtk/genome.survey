/**
 *
 */
package org.theseed.memdb.query;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationBuilder;
import org.theseed.memdb.RelationshipType;

/**
 * The query-generation relationship type contains no additional data.
 *
 * @author Bruce Parrello
 *
 */
public class QueryRelationshipType extends RelationshipType {

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
    }

    @Override
    protected RelationBuilder createRelationBuilder(FieldInputStream inStream)
            throws IOException, ParseFailureException {
        return new QueryRelationBuilder(this, inStream);
    }

}
