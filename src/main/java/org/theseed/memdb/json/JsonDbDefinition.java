package org.theseed.memdb.json;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.DbDefinition;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationshipType;

/**
 * The JSON database defintion is used to generate a random-walk that outputs JSON fragments. Each time we land on an entity instance,
 * we output a small number of randomly-selected attributes in JSON format. Each time we traverse a relationship, we output a JSON object
 * describing the connected entities.
 * 
 * The relationships contain only a relationship header line, but the entity definitions contain a series of attribute lines, each of which
 * contains an attribute name, the attribute type (number, boolean, string, string list) an the name it should be given in the output
 * JSON.
 * 
 * @author Bruce Parrello
 */
public class JsonDbDefinition extends DbDefinition {

    /**
     * Create a database definition from a definition file.
     *
     * @param fileName		name of the definition file
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public JsonDbDefinition(File fileName) throws IOException, ParseFailureException {
        super(fileName);
    }

    @Override
    protected EntityType createEntityType(String name) {
        // TODO Create a JSON entity type with the specified name
        throw new UnsupportedOperationException("Unimplemented method 'createEntityType'");
    }

    @Override
    protected String processRelationshipDefinition(RelationshipType rel, DbDefinition db) throws ParseFailureException {
        // TODO read the JSON relationship definition from the DbDefinition stream
        throw new UnsupportedOperationException("Unimplemented method 'processRelationshipDefinition'");
    }

    @Override
    protected RelationshipType createRelationshipType(EntityType sourceType, String sourceIdColName,
            EntityType targetType, String targetIdColName) {
        // TODO create a JSON relationship type connecting the two specified entity types
        throw new UnsupportedOperationException("Unimplemented method 'createRelationshipType'");
    }

    @Override
    protected DbInstance createDbInstance(List<String> typeNames) {
        // TODO Create a JSON database instance with the specified entity types
        throw new UnsupportedOperationException("Unimplemented method 'createDbInstance'");
    }



}
