package org.theseed.memdb.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.EntityType;

public class JsonEntityType extends EntityType {

    // FIELDS
    /** list of attribute builders for this entity type */
    private final List<JsonAttributeBuilder> attributes;

    public JsonEntityType(String name) {
        super(name);
        this.attributes = new ArrayList<>();
    }

    @Override
    protected void addAttribute(String line) {
        // FUTURE convert an attribute description line into an attribute builder
        throw new UnsupportedOperationException("Unimplemented method 'addAttribute'");
    }

    @Override
    protected Collection<? extends AttributeBuilder> getAttributeBuilders(FieldInputStream instanceStream)
            throws IOException, ParseFailureException {
        return this.attributes;
    }

}
