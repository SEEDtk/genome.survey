/**
 *
 */
package org.theseed.memdb.walker;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.template.LineTemplate;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;

/**
 * The text attribute builder creates a line template for each attribute, and this is then
 * used to update the entity instances.
 *
 * @author Bruce Parrello
 *
 */
public class TextAttributeBuilder extends AttributeBuilder {

    // FIELDS
    /** line template for this attribute */
    private final LineTemplate template;
    /** parent entity type */
    private final TextEntityType entityType;

    /**
     * Create the line template for this attribute builder.
     *
     * @param type				entity type for this attribute
     * @param instanceStream	input stream containing instance records
     * @param attributeString 	template string for this attribute
     *
     * @throws ParseFailureException
     * @throws IOException
     *
     */
    public TextAttributeBuilder(TextEntityType type, String attributeString, FieldInputStream instanceStream)
            throws IOException, ParseFailureException {
        // Create and save the template.
        this.template = new LineTemplate(instanceStream, attributeString, null);
        this.entityType = type;
    }

    @Override
    protected void processAttribute(DbInstance db, FieldInputStream.Record record, EntityInstance instance) {
        String attribute = template.apply(record);
        // Only process the attribute if it is non-blank. Some attributes are empty for certain entity instances
        // (for example, a genome with a missing family taxon).
        if (! StringUtils.isBlank(attribute)) {
            TextEntityInstance textInstance = (TextEntityInstance) instance;
            textInstance.addAttribute(attribute);
            TextDbInstance textDb = (TextDbInstance) db;
            long count = textDb.countTokens(attribute);
            this.entityType.countTokens(count);
        }
    }

}
