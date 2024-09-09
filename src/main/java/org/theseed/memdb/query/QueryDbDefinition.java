/**
 *
 */
package org.theseed.memdb.query;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.DbDefinition;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationshipType;

/**
 * This version of the in-memory database is used to generate test questions and
 * answers for LLM training. For each attribute we need to know how often each
 * value occurs, and for each entity type how many connections to each other type.
 * In the definition file, all we keep are the entity attributes. Currently, these are
 * described only by a column name (which is also the attribute name). Each attribute
 * name should be on a single line below the entity header.
 *
 * @author Bruce Parrello
 *
 */
public class QueryDbDefinition extends DbDefinition {

    /**
     * Construct a database definition for a query-generation database.
     *
     * @param fileName		name of the input file containing the definition
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public QueryDbDefinition(File fileName) throws IOException, ParseFailureException {
        super(fileName);
    }

    @Override
    protected EntityType createEntityType(String name) {
        return new QueryEntityType(name);
    }

    @Override
    protected String processRelationshipDefinition(RelationshipType rel, DbDefinition db) throws ParseFailureException {
        // There is no relationship data in this type of database.
        return db.readNext();
    }

    @Override
    protected RelationshipType createRelationshipType(EntityType sourceType, String sourceIdColName,
            EntityType targetType, String targetIdColName) {
        return new QueryRelationshipType(sourceType, sourceIdColName, targetType, targetIdColName);
    }

    @Override
    protected DbInstance createDbInstance(List<String> typeNames) {
        return new QueryDbInstance(typeNames);
    }

}
