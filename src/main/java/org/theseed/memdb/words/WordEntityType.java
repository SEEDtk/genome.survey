package org.theseed.memdb.words;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.walk.WalkEntityType;

public class WordEntityType extends WalkEntityType {

    // FIELDS
    /** list of attribute templates */
    private final List<String> attributeTemplates;

    public WordEntityType(String name) {
        super(name);
        this.attributeTemplates = new ArrayList<>(5);
    }

    @Override
    protected void addAttribute(String line) {
        this.attributeTemplates.add(line);
    }

    @Override
    protected Collection<? extends AttributeBuilder> getAttributeBuilders(FieldInputStream instanceStream)
            throws IOException, ParseFailureException {
                List<WordAttributeBuilder> retVal = new ArrayList<>(this.attributeTemplates.size());
        for (String attributeString : this.attributeTemplates) {
            WordAttributeBuilder attributeBuilder = new WordAttributeBuilder(this, attributeString, instanceStream);
            retVal.add(attributeBuilder);
        }
        return retVal;
    }

}
