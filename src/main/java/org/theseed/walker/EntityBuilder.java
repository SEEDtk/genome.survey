/**
 *
 */
package org.theseed.walker;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;

/**
 * This is a helper class for building an entity instance from an entity type.
 */
public class EntityBuilder {

    /**
     *
     */
    private final EntityType entityType;
    /** ID column index, or -1 for generated */
    private int idColIdx;
    /** relationship builders */
    private Collection<RelationBuilder> relationBuilders;
    /** attribute builders */
    private Collection<AttributeBuilder> attributeBuilders;

    /**
     * Create an instance builder for this entity type on a given input stream.
     *
     * @param entityType		type of entity whose instances are being built
     * @param instanceStream	input stream for entity instances
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public EntityBuilder(EntityType entityType, FieldInputStream instanceStream) throws IOException, ParseFailureException {
        this.entityType = entityType;
        this.attributeBuilders = this.entityType.getAttributeBuilders(instanceStream);
        this.relationBuilders = this.entityType.getRelationshipBuilders(instanceStream);
        final String entityIdCol = this.entityType.getIdColName();
        if (entityIdCol == null)
            this.idColIdx = -1;
        else
            this.idColIdx = instanceStream.findField(entityIdCol);
    }

    /**
     * Compute the ID of the current entity instance.
     *
     * @param record	instance input record
     */
    protected String computeId(FieldInputStream.Record record) {
         String retVal;
         if (this.idColIdx < 0) {
             // Here we have a connector record with no ID.
             retVal = EntityType.NULL_ID;
         } else {
             // Here there is a natural ID in the input record.
             retVal = record.get(this.idColIdx);
         }
         return retVal;
    }

    /**
     * Build an entity instance from an input record.
     *
     * @param record	input record to use
     * @param db		database instance containing the entities
     *
     * @return the new entity instance, or NULL if there is none in this record
     */
    public EntityInstance build(FieldInputStream.Record record, DbInstance db) {
        EntityInstance retVal = null;
        // Get the entity ID.
        String entityId = this.computeId(record);
        // Only proceed if we have an ID for this entity.
        if (! StringUtils.isBlank(entityId)) {
            // If this is a real entity, set up the attributes and create an instance.
            if (! entityId.contentEquals(EntityType.NULL_ID)) {
                // Find or create the entity instance.
                retVal = db.findEntity(this.entityType, entityId);
                // Loop through the attribute builders, creating the attributes.
                for (AttributeBuilder template : this.attributeBuilders)
                    template.processAttribute(db, record, retVal);
            }
            // Loop through the relationship builders, creating the relationship instances
            // in each direction.
            for (RelationBuilder builder : this.relationBuilders) {
                EntityInstance targetInstance = builder.getTarget(record, db);
                EntityInstance sourceInstance = builder.getSource(record, db);
                RelationshipType relType = builder.getRelType();
                // Only proceed if there is a source and a target.
                if (sourceInstance != null && targetInstance != null) {
                    RelationshipInstance forward = relType.getForwardInstance(sourceInstance, targetInstance);
                    RelationshipInstance reverse = relType.getReverseInstance(sourceInstance, targetInstance);
                    forward.addConnection(relType, db, record, sourceInstance, targetInstance);
                    reverse.addConnection(relType, db, record, targetInstance, sourceInstance);
                }
            }
        }
        return retVal;
    }

}
