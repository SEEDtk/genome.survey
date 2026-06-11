package org.theseed.memdb.words;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.DbDefinition;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationshipType;

/**
 * A word database is used to generate a word list from templates. For each relationship or attribute, we have a template string that generates
 * a word or phrase. For the attribute, the attribute value itself; for the relationship, three templates: the source entity ID, the relationship name phrase,
 * and the target entity ID. The relationship name phrase is used to generate a word or phrase describing the relationship between the source and target entities.
 * 
 * An attribute will generally be output immediately after the entity ID.
 */
public class WordDbDefinition extends DbDefinition {

    public WordDbDefinition(File fileName) throws IOException, ParseFailureException {
        super(fileName);
    }

    @Override
    protected EntityType createEntityType(String name) {
        return new WordEntityType(name);
    }

    @Override
    protected String processRelationshipDefinition(RelationshipType rel, DbDefinition db) throws ParseFailureException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processRelationshipDefinition'");
    }

    @Override
    protected RelationshipType createRelationshipType(EntityType sourceType, String sourceIdColName,
            EntityType targetType, String targetIdColName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createRelationshipType'");
    }

    @Override
    protected DbInstance createDbInstance(List<String> typeNames) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createDbInstance'");
    }

}
