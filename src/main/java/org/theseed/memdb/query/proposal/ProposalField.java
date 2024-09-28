/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.query.QueryEntityInstance;

/**
 * This object represents a field of interest in a query proposal. We need to know the
 * entity type name and the attribute name. The subclass determines how the proposal is
 * interpreted (greater than, less than, boolean, equal, or exact.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ProposalField {

    // FIELDS
    /** entity type name */
    private String entityType;
    /** attribute name */
    private String attributeName;
    /** empty list, sometimes used as a return value */
    protected static final List<String> EMPTY_LIST = Collections.emptyList();

    /**
     * Construct a proposal for a specified field's use as a parameter.
     *
     * @param fieldSpec		field specification
     *
     * @throws ParseFailureException
     */
    public ProposalField(String fieldSpec) throws ParseFailureException {
        String[] pieces = StringUtils.split(fieldSpec, ".");
        if (pieces.length != 2)
            throw new ParseFailureException("Invalid field specification \"" + fieldSpec + "\".");
        if (StringUtils.contains(pieces[1], ":"))
            throw new ParseFailureException("Invalid use of colon in \"" + fieldSpec + "\".");
        this.entityType = pieces[0];
        this.attributeName = pieces[1];
    }

    /**
     * @return the entity type name for this proposal field
     */
    public String getEntityType() {
        return this.entityType;
    }

    /**
     * @return the name of the desired attribute
     */
    public String getName() {
        return this.attributeName;
    }

    /**
     * Extract the appropriate value for this field proposal from the specified entity instance.
     *
     * @param instance	entity instance of interest
     *
     * @return the string representation of the value, or nothing if the instance fails the criteria
     */
    protected abstract List<String> getValue(QueryEntityInstance instance);

}
