package org.theseed.memdb.words;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.LineTemplate;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.walk.WalkDbInstance;

/**
 * The word attribute builder creates a line template for each attribute, and this is then
 * used to update the entity instances. The line template is used to generate a word or phrase for the attribute value, and then that
 * word or phrase it added to the instance's attribute list. The attribute list then generates the next word when the entity instance
 * is processed during a walk.
 */
public class WordAttributeBuilder extends AttributeBuilder {

    // FIELDS
    /** line template for this attribute */
    private final LineTemplate template;
    /** parent entity type */
    private final WordEntityType entityType;

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
    public WordAttributeBuilder(WordEntityType wordEntityType, String attributeString,
            FieldInputStream instanceStream) throws IOException, ParseFailureException {
        this.template = new LineTemplate(instanceStream, attributeString, null);
        this.entityType = wordEntityType;
    }

    @Override
    protected void processAttribute(DbInstance db, Record record, EntityInstance instance) {
        // Get the attribute value from the template. This is a word or phrase that will be added to the instance's attribute list.
        String attribute = template.apply(record);
        // Only process the attribute if it is non-blank. Some attributes are empty for certain entity instances
        // (for example, a genome with a missing family taxon).
        if (attribute != null && ! attribute.isBlank()) {
            WordEntityInstance wordInstance = (WordEntityInstance) instance;
            wordInstance.addAttribute(attribute);
            WalkDbInstance walkDb = (WalkDbInstance) db;
            long count = walkDb.countTokens(attribute);
            this.entityType.countTokens(count);
        }
    }

}
