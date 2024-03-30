/**
 *
 */
package org.theseed.walker;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.LineTemplate;

/**
 * A relationship builder contains the templates we need to build relationship instances.  Each builder will
 * produce two instances-- one for each direction.
 *
 * @author Bruce Parrello
 *
 */
public class RelationBuilder {

    // FIELDS
    /** template for the forward sentence */
    private LineTemplate forwardTemplate;
    /** template for the converse sentence */
    private LineTemplate converseTemplate;
    /** column index for target entity ID */
    private int targetIdColIdx;
    /** target entity type name */
    private EntityType targetType;

    /**
     * Construct a relation builder for a specified relationship type from a given entity type.
     *
     * @param entityType	source entity type
     * @param relType		type of relationship to build
     * @param inStream		entity instance input stream
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    public RelationBuilder(EntityType entityType, RelationshipType relType, FieldInputStream inStream) throws IOException, ParseFailureException {
        this.forwardTemplate = new LineTemplate(inStream, relType.getForwardSentence(), null);
        this.converseTemplate = new LineTemplate(inStream, relType.getConverseSentence(), null);
        this.targetIdColIdx = inStream.findField(relType.getTargetColName());
        this.targetType = relType.getTargetType();
    }

    /**
     * Compute the connection sentence for the relationship instance in the forward direction.
     *
     * @param record	entity instance input record
     *
     * @return the appropriate forward-direction sentence
     */
    public String getForwardString(Record record) {
        return this.forwardTemplate.apply(record);
    }

    /**
     * Compute the target entity instance for the forward direction of a record's relationship
     * instance.
     *
     * @param record	input record for the source entity instance
     * @param db		database instance containing the entities
     *
     * @return the target entity instance, or NULL if there is none
     */
    public EntityInstance getTarget(Record record, DbInstance db) {
        EntityInstance retVal;
        // Get the target entity ID.
        String targetId = record.get(this.targetIdColIdx);
        if (StringUtils.isBlank(targetId)) {
            // Here there is no target entity.
            retVal = null;
        } else {
            // Get an entity instance from the database.
            retVal = db.findEntity(this.targetType, targetId);
        }
        return retVal;
    }

    /**
     * Compute the connection sentence for the relationship instance in the converse direction.
     *
     * @param record	entity instance input record
     *
     * @return the appropriate forward-direction sentence
     */
    public String getConverseString(Record record) {
        return this.converseTemplate.apply(record);
    }

}
