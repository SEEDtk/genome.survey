/**
 *
 */
package org.theseed.memdb.query;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.memdb.AttributeBuilder;
import org.theseed.memdb.EntityType;

/**
 * The entity type for a query database will contain a list of attribute names. These
 * are also the column names used to read the attribute values.
 *
 * @author Bruce Parrello
 *
 */
public class QueryEntityType extends EntityType {

    // FIELDS
    /** list of attribute names */
    private Set<String> attributeNames;
    /** pattern for matching up to the first whitespace */
    private static final Pattern ATTR_NAME = Pattern.compile("(\\S+)\\s.+");

    /**
     * Construct a new entity type for a query-generation database.
     *
     * @param name	entity type name
     */
    public QueryEntityType(String name) {
        super(name);
        this.attributeNames = new TreeSet<String>();
    }

    @Override
    protected void addAttribute(String line) {
        // All we need to know about the attribute is its name, which is everything up to the first white
        // character. If the line begins with whitespace, it is ignored.
        Matcher m = ATTR_NAME.matcher(line);
        if (m.matches()) {
            String attrName = m.group(1);
            this.attributeNames.add(attrName);
        }
    }

    @Override
    protected Collection<? extends AttributeBuilder> getAttributeBuilders(FieldInputStream instanceStream)
            throws IOException, ParseFailureException {
        List<QueryAttributeBuilder> retVal = this.attributeNames.stream().map(x -> new QueryAttributeBuilder(instanceStream, x))
                .collect(Collectors.toList());
        return retVal;
    }

}
