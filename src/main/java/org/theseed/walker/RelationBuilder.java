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
    /** column index for source entity ID */
    private int sourceIdColIdx;
    /** source entity type */
    private EntityType sourceType;
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
    public RelationBuilder(RelationshipType relType, FieldInputStream inStream) throws IOException, ParseFailureException {
        this.forwardTemplate = new LineTemplate(inStream, relType.getForwardSentence(), null);
        this.converseTemplate = new LineTemplate(inStream, relType.getConverseSentence(), null);
        this.targetIdColIdx = inStream.findField(relType.getTargetColName());
        this.targetType = relType.getTargetType();
        this.sourceIdColIdx = inStream.findField(relType.getSourceColName());
        this.sourceType = relType.getSourceType();
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
     * @param record	input record for the current table
     * @param db		database instance containing the entities
     *
     * @return the target entity instance, or NULL if there is none
     */
    public EntityInstance getTarget(Record record, DbInstance db) {
        return getInstance(record, db, this.targetType, this.targetIdColIdx);
    }

    /**
     * Compute the source entity instance for the forward direction of a record's relationship
     * instance.
     *
     * @param record	input record for the current table
     * @param db		database instance containing the entities
     *
     * @return the target entity instance, or NULL if there is none
     */
    public EntityInstance getSource(Record record, DbInstance db) {
        return getInstance(record, db, this.sourceType, this.sourceIdColIdx);
    }

    /**
     * Find the desired entity instance.
     *
     * @param record	input record
     * @param db		current database instance
     * @param type		entity type
     * @param idColIdx	entity ID column index
     *
     * @return the desired instance, or NULL if there is none
     */
    private static EntityInstance getInstance(Record record, DbInstance db, EntityType type, int idColIdx) {
        EntityInstance retVal;
        // Get the entity ID.
        String id = record.get(idColIdx);
        if (StringUtils.isBlank(id)) {
            // Here there is no entity instance.
            retVal = null;
        } else {
            // Get an entity instance from the database.
            retVal = db.findEntity(type, id);
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
