/**
 *
 */
package org.theseed.memdb.walker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.EntityType;

/**
 * This object represents an entity type for a text-walk database.  It will
 * have a template string for each attribute and a token counter used
 * for database stats.
 *
 * @author Bruce Parrello
 *
 */
public class TextEntityType extends EntityType {

    // FIELDS
    /** list of attribute templates */
    private final List<String> attributeStrings;
    /** token counter for all entity instances */
    private long tokenCount;

    /**
     * Create a new text entity type with the specified name.
     * @param name
     */
    public TextEntityType(String name) {
        super(name);
        this.attributeStrings = new ArrayList<>(5);
        this.tokenCount = 0;
    }

    @Override
    protected void addAttribute(String line) {
        this.attributeStrings.add(line);
    }

    @Override
    protected Collection<? extends AttributeBuilder> getAttributeBuilders(FieldInputStream instanceStream)
            throws IOException, ParseFailureException {
        List<TextAttributeBuilder> retVal = new ArrayList<>(this.attributeStrings.size());
        // Convert each attribute string into an attribute builder.
        for (String attributeString : this.attributeStrings) {
            TextAttributeBuilder attributeBuilder = new TextAttributeBuilder(this, attributeString, instanceStream);
            retVal.add(attributeBuilder);
        }
        // Return the attribute builders to the caller.
        return retVal;
    }

    /**
     * @return the number of tokens generated for this entity type
     */
    public long getTokenCount() {
        return this.tokenCount;
    }

    /**
     * Update the token count for this entity type.
     *
     * @param count		number of tokens to add
     */
    public void countTokens(long count) {
        this.tokenCount += count;
    }

}
