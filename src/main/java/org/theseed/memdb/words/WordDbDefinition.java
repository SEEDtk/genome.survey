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
        // We have three lines of text-- the source ID template, the relationship name template, and the target ID template. 
        // We need to read those three lines and store them in the relationship type.
        WordRelationshipType wordRel = (WordRelationshipType) rel;
        String retVal = db.readNext();
        if (retVal == null || retVal.startsWith("#"))
            throw new ParseFailureException("Missing template strings for relationship at line " + db.getLineCount() + " in definition file.");
        wordRel.setSourceString(retVal);
        retVal = db.readNext();
        if (retVal == null || retVal.startsWith("#"))
            throw new ParseFailureException("Missing relationship name template string for relationship at line " + db.getLineCount() + " in definition file.");
        wordRel.setNameString(retVal);
        retVal = db.readNext();
        if (retVal == null || retVal.startsWith("#"))
            throw new ParseFailureException("Missing target ID template string for relationship at line " + db.getLineCount() + " in definition file.");
        wordRel.setTargetString(retVal);
        // End by reading ahead to what should be the next header.
        retVal = db.readNext();
        return retVal;
    }

    @Override
    protected RelationshipType createRelationshipType(EntityType sourceType, String sourceIdColName,
            EntityType targetType, String targetIdColName) {
        return new WordRelationshipType(sourceType, sourceIdColName, targetType, targetIdColName);
    }

    @Override
    protected DbInstance createDbInstance(List<String> typeNames) {
        return new WordDbInstance(typeNames);
    }

}