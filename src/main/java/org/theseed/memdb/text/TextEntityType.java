/**
 *
 */
package org.theseed.memdb.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.walk.WalkEntityType;

/**
 * This object represents an entity type for a text-walk database.  It will
 * have a template string for each attribute and a token counter used
 * for database stats.
 *
 * @author Bruce Parrello
 *
 */
public class TextEntityType extends WalkEntityType {

    // FIELDS
    /** list of attribute templates */
    private final List<String> attributeStrings;

    /**
     * Create a new text entity type with the specified name.
     * @param name
     */
    public TextEntityType(String name) {
        super(name);
        this.attributeStrings = new ArrayList<>(5);
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

}
