/**
 *
 */
package org.theseed.memdb.query;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.RelationBuilder;
import org.theseed.memdb.RelationshipInstance;
import org.theseed.memdb.RelationshipType;

/**
 * Because relationships are very simple in a query-generation database, its relation-builder
 * has no extra work.
 *
 * @author Bruce Parrello
 *
 */
public class QueryRelationBuilder extends RelationBuilder {


    /**
     * Construct a relation-builder for a query-generation database.
     *
     * @param relType	relationship type
     * @param inStream	input stream for source records
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public QueryRelationBuilder(RelationshipType relType, FieldInputStream inStream)
            throws IOException, ParseFailureException {
        super(relType, inStream);
    }

    @Override
    protected RelationshipInstance getForwardInstance(DbInstance db, Record record, EntityInstance sourceInstance,
            EntityInstance targetInstance) {
        return new QueryRelationshipInstance(targetInstance);
    }

    @Override
    protected RelationshipInstance getReverseInstance(DbInstance db, Record record, EntityInstance sourceInstance,
            EntityInstance targetInstance) {
        return new QueryRelationshipInstance(sourceInstance);
    }

}
