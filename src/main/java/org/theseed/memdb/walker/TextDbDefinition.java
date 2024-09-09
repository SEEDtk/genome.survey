/**
 *
 */
package org.theseed.memdb.walker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.DbDefinition;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationshipType;

/**
 * A text database is used to generate the random-walk text from templates, so the additional data
 * in this type of database is the template strings for the attributes and relationships. For
 * an entity definition, there is one template per attribute. For a relationship definition, there
 * is one template for each relationship direction.
 *
 * @author Bruce Parrello
 *
 */
public class TextDbDefinition extends DbDefinition {

    /**
     * Create a database definition from a definition file.
     *
     * @param fileName		name of the definition file
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public TextDbDefinition(File fileName) throws IOException, ParseFailureException {
        super(fileName);
    }

    @Override
    protected EntityType createEntityType(String name) {
        return new TextEntityType(name);
    }

    @Override
    protected String processRelationshipDefinition(RelationshipType rel, DbDefinition db) throws ParseFailureException {
        // Here we have two data lines for the relationship, each containing a template for the
        // appropriate relationship direction.
        TextRelationshipType textRel = (TextRelationshipType) rel;
        // The first line is the forward template.
        String retVal = db.readNext();
        if (retVal == null || retVal.startsWith("#"))
            throw new ParseFailureException("Missing template strings for relationship at line " + db.getLineCount() + " in definition file.");
        textRel.addFowardString(retVal);
        // The second line is the reserve template.
        retVal = db.readNext();
        if (retVal == null || retVal.startsWith("#"))
            throw new ParseFailureException("Missing reverse template string for relationship at line " + db.getLineCount() + " in definition file.");
        textRel.addReverseString(retVal);
        retVal = db.readNext();
        return retVal;
    }

    @Override
    protected RelationshipType createRelationshipType(EntityType sourceType, String sourceIdColName,
            EntityType targetType, String targetIdColName) {
        return new TextRelationshipType(sourceType, sourceIdColName, targetType, targetIdColName);
    }

    @Override
    protected DbInstance createDbInstance(List<String> typeNames) {
        return new TextDbInstance(typeNames);
    }


}
