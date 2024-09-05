/**
 *
 */
package org.theseed.memdb.walker;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationBuilder;
import org.theseed.memdb.RelationshipType;

/**
 * The text-walk relationship type contains the template strings for the two relationship directions.
 *
 * @author Bruce Parrello
 *
 */
public class TextRelationshipType extends RelationshipType {

    // FIELDS
    /** forward relation template */
    private String forwardString;
    /** converse relation template */
    private String reverseString;

    /**
     * Create a new text relationship type.
     *
     * @param sourceType	source entity type
     * @param sourceCol		source ID column
     * @param targetType	target entity type
     * @param targetCol		target ID column
     */
    public TextRelationshipType(EntityType sourceType, String sourceCol, EntityType targetType, String targetCol) {
        super(sourceType, sourceCol, targetType, targetCol);
        this.forwardString = null;
        this.reverseString = null;
    }

    /**
     * Store the forward relation template string.
     *
     * @param string	template string to store
     */
    public void addFowardString(String string) {
        this.forwardString = string;
    }

    /**
     * Store the converse relation template string.
     *
     * @param string	template string to store
     */
    public void addReverseString(String string) {
        this.reverseString = string;
    }

    @Override
    protected RelationBuilder createRelationBuilder(FieldInputStream inStream) throws IOException, ParseFailureException {
        return new TextRelationBuilder(this, inStream);
    }

    /**
     * @return the forward-direction template string
     */
    public String getForwardString() {
        return this.forwardString;
    }

    /**
     * @return the reverse-direction template String
     */
    public String getReverseString() {
        return this.reverseString;
    }

}
