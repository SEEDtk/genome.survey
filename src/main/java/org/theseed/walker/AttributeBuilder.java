/**
 *
 */
package org.theseed.walker;

import org.theseed.io.FieldInputStream.Record;

/**
 * This is a helper class for processing attributes. Each builder represents a single attribute for the entity type.
 *
 * @author Bruce Parrello
 *
 */
public abstract class AttributeBuilder {

    /**
     * Build an attribute for a specified entity instance
     *
     * @param db		relevant database instance
     * @param record	input record for the entity instance
     * @param instance	parent entity instance into which attribute will be stored
     */
    protected abstract void processAttribute(DbInstance db, Record record, EntityInstance instance);

}
