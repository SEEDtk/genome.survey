package org.theseed.memdb.json;

import org.theseed.io.FieldInputStream.Record;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;

public class JsonAttributeBuilder extends AttributeBuilder {

    @Override
    protected void processAttribute(DbInstance db, Record record, EntityInstance instance) {
        // TODO Store the attribute data from the record in the entity instance
        throw new UnsupportedOperationException("Unimplemented method 'processAttribute'");
    }

}
