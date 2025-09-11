/**
 *
 */
package org.theseed.memdb.walker;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.template.LineTemplate;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.RelationBuilder;
import org.theseed.memdb.RelationshipInstance;

/**
 * This object stores the templates used to generate relationship instances from relationships. Note that
 * we must be able to generate two instances from this one object.
 *
 * @author Bruce Parrello
 *
 */
public class TextRelationBuilder extends RelationBuilder {

    // FIELDS
    /** line template for forward direction */
    private final LineTemplate forwardTemplate;
    /** line template for reverse direction */
    private final LineTemplate reverseTemplate;

    /**
     * Create a relation builder for a specified relationship type.
     *
     * @param relType	relationship type of interest
     * @param inStream	input stream containing records from which the relationship is built
     */
    public TextRelationBuilder(TextRelationshipType relType, FieldInputStream inStream) throws IOException, ParseFailureException {
        super(relType, inStream);
        // Compile the two templates.
        this.forwardTemplate = new LineTemplate(inStream, relType.getForwardString(), null);
        this.reverseTemplate = new LineTemplate(inStream, relType.getReverseString(), null);
    }

    @Override
    protected RelationshipInstance getForwardInstance(DbInstance db, FieldInputStream.Record record, EntityInstance sourceInstance,
            EntityInstance targetInstance) {
        return this.buildInstance(db, this.forwardTemplate, record, targetInstance);
    }

    @Override
    protected RelationshipInstance getReverseInstance(DbInstance db, FieldInputStream.Record record, EntityInstance sourceInstance,
            EntityInstance targetInstance) {
        return this.buildInstance(db, this.reverseTemplate, record, sourceInstance);
    }

    /**
     * Build a single relationship instance from the specified template that targets the specified instance.
     *
     * @param template		template to instantiate for the crossing sentence
     * @param record		source record for the relationship
     * @param target		target entity instance
     *
     * @return the relationship instance built
     */
    private RelationshipInstance buildInstance(DbInstance db, LineTemplate template, FieldInputStream.Record record,
            EntityInstance target) {
        String crossingText = template.apply(record);
        TextDbInstance textDb = (TextDbInstance) db;
        textDb.countTokens(crossingText);
        return new TextRelationshipInstance(crossingText, target);
    }

}
