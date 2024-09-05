/**
 *
 */
package org.theseed.memdb;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;

/**
 * A relationship type describes a connection between two entity types.  It only exists
 * for the many-to-one direction.  It consists of the source and target entity types, and
 * the column name for the IDs of the source and target entities.  In most case the source
 * entity ID will be the ID of the table used to compute the relationship instances. The
 * exception is when a many-to-many relationship is stored in a table of its own.
 * When we process an entity instance, we will use the entity's relationship types to compute
 * the crossings to the target entities, and then attach the converse crossing to those targets.
 *
 * @author Bruce Parrello
 *
 */
public abstract class RelationshipType {

    // FIELDS
    /** source entity type */
    private EntityType source;
    /** target entity type */
    private EntityType target;
    /** source entity column name */
    private String sourceColName;
    /** target entity column name */
    private String targetColName;

    /**
     * Construct a relationship type for a specified relationship.
     *
     * @param sourceType	source entity type
     * @param sourceCol		column name of source entity ID in record
     * @param targetType	target entity type
     * @param targetCol		column name of target entity ID in record
     */
    public RelationshipType(EntityType sourceType, String sourceCol, EntityType targetType, String targetCol) {
        this.source = sourceType;
        this.sourceColName = sourceCol;
        this.target = targetType;
        this.targetColName = targetCol;
    }

    /**
     * @return the target entity ID column name
     */
    public String getTargetColName() {
        return this.targetColName;
    }

    /**
     * @return the target entity type
     */
    public EntityType getTargetType() {
        return this.target;
    }

    /**
     * @return the column name containing the source entity ID
     */
    public String getSourceColName() {
        return this.sourceColName;
    }

    /**
     * @return the source entity type
     */
    public EntityType getSourceType() {
        return this.source;
    }

    /**
     * Creater a relation builder that can build instances of this relationship in both directions.
     *
     * @param inStream	input file containing source records for the relationship
     *
     * @return the relation builder needed
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    protected abstract RelationBuilder createRelationBuilder(FieldInputStream inStream) throws IOException, ParseFailureException;

}
