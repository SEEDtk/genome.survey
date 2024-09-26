/**
 *
 */
package org.theseed.memdb.query;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
 * described two ways. First, by a column name in the entity type definition (which is also the
 * attribute name). Each attribute name should be on a single line below the entity header.
 * Second, by a specification in a relationship definition. This is used when a many-to-many
 * relationship contains data that has to be stored in one of the connecting entities. This
 * is almost always identifying data in a target entity that has no source file of its own. The
 * specification consists of the field name in the data file input record and the attribute name
 * in the target entity itself.
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
        // Get the relationship type as a query-relationship type.
        QueryRelationshipType qRel = (QueryRelationshipType) rel;
        // Loop through the target-attribute definitions (if any).
        String retVal = db.readNext();
        while (retVal != null && ! retVal.startsWith("#")) {
            // Here we have a target attribute definition. We put this in the map. The key is the input field name and the
            // value is the target-instance field name.
            String[] parts = StringUtils.split(retVal);
            if (parts.length != 2)
                throw new ParseFailureException("Target-field spec in relationship definition for " + qRel.toString()
                + " has an invalid format.");
            qRel.storeFieldMapping(parts[0], parts[1]);
            retVal = db.readNext();
        }
        return retVal;
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
