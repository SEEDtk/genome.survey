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
 * The relationship and entity definitions contain a header line and then a list of attributes. The attributes consist of an output attribute
 * name, a data type (number, boolean, string, list), and then a template for generating the attribute. For the relationship, all attributes
 * are included in both directions, along with "from" and "to" fields. For the entity, we randomly select a few attributes to output.
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
        return new JsonEntityType(name);
    }

    @Override
    protected RelationshipType createRelationshipType(EntityType sourceType, String sourceIdColName,
            EntityType targetType, String targetIdColName) {
        // TODO create a JSON relationship type connecting the two specified entity types
        throw new UnsupportedOperationException("Unimplemented method 'createRelationshipType'");
    }

    @Override
    protected String processRelationshipDefinition(RelationshipType rel, DbDefinition db) throws ParseFailureException {
        // TODO read the JSON relationship definition from the DbDefinition stream
        throw new UnsupportedOperationException("Unimplemented method 'processRelationshipDefinition'");
    }

    @Override
    protected DbInstance createDbInstance(List<String> typeNames) {
        // TODO Create a JSON database instance with the specified entity types
        throw new UnsupportedOperationException("Unimplemented method 'createDbInstance'");
    }



}
