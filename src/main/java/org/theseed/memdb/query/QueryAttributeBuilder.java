/**
 *
 */
package org.theseed.memdb.query;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.theseed.io.Attribute;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;

/**
 * The query attribute builder memorizes the column number and name of an attribute.
 *
 * @author Bruce Parrello
 *
 */
public class QueryAttributeBuilder extends AttributeBuilder {

    // FIELD
    /** attribute name */
    private String attrName;
    /** attribute column index */
    private int attrColIdx;

    /**
     * Create a new query attribute builder.
     *
     * @param stream	input stream containing the entity instance records
     * @param colName	name of the attribute (which is also the column name)
     */
    public QueryAttributeBuilder(FieldInputStream stream, String colName) {
        this.attrName = colName;
        try {
            this.attrColIdx = stream.findField(colName);
        } catch (IOException e) {
            // Rethrow field-not-found as an unchecked exception.
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected void processAttribute(DbInstance db, FieldInputStream.Record record, EntityInstance instance) {
        QueryEntityInstance qInstance = (QueryEntityInstance) instance;
        // Create the attribute holder.
        Attribute attr = new Attribute(record, attrColIdx);
        // Store it in the entity instance.
        qInstance.addAttribute(this.attrName, attr);
        // Record the values in the counts.
        QueryDbInstance qDb = (QueryDbInstance) db;
        qDb.recordAttribute(instance.getType(), this.attrName, attr.getList());
    }

}
