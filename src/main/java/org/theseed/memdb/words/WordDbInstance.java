package org.theseed.memdb.words;

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.walk.WalkDbInstance;


/**
 * This is the instance for a word-walk database. Most of the code is in the parent class, but
 * it needs to know that the entity instances created are for word-walk entities.
 */
public class WordDbInstance extends WalkDbInstance {

    // FIELDS
    /** last phrase emitted */
    private String lastPhrase;

    /**
     * Create a new word database instance
     *
     * @param types		list of entity type names
     */
    public WordDbInstance(List<String> types) {
        super(types);
        this.lastPhrase = null;
    }

    @Override
    protected EntityInstance createEntity(EntityType entityType, String entityId) {
        // Create a word entity instance with the specified ID and type.
        return new WordEntityInstance(entityType, entityId, this);
    }

    /**
     * Write a phrase to the output stream. The phrase is skipped if it is blank or if it is identical to the last phrase emitted.
     * All strings are separated by semi-colons. If the last phrase is NULL, no semi-colon is needed.
     * 
     * @param writer
     * @param string
     */
    public void emitPhrase(PrintWriter writer, String string) {
        if (! StringUtils.isBlank(string)) {
            // The above check guarantees that the string is non-null. Also, it won't be blank or empty, so we don't 
            // have to worry about two semi-colons in a row.
            if (! string.equals(this.lastPhrase)) {
                // Here we have a new phrase.  Write it out.
                if (this.lastPhrase != null)
                    writer.print(';');
                writer.print(string);
                // Note that the last phrase can never be null after the first time through.
                this.lastPhrase = string;
            }
        }
    }

}
