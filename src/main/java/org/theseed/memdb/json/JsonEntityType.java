package org.theseed.memdb.json;

import java.io.IOException;
import java.util.Collection;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.EntityType;

public class JsonEntityType extends EntityType {

    // FIELDS
    /** list of attribute builders for this entity type */
    // private List<JsonAttributeBuilder> attributes;

    public JsonEntityType(String name) {
        super(name);
        // this.attributes = new ArrayList<>();
    }

    @Override
    protected void addAttribute(String line) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addAttribute'");
    }

    @Override
    protected Collection<? extends AttributeBuilder> getAttributeBuilders(FieldInputStream instanceStream)
            throws IOException, ParseFailureException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttributeBuilders'");
    }

}
