/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.Attribute;
import org.theseed.memdb.query.QueryEntityInstance;

/**
 * This field proposal is satisfied when the specified field has a value equal
 * to a parameter. Case-insensitive string equality is used. If a field value is
 * a list, any list element that matches satisfies the proposal.
 *
 * @author Bruce Parrello
 *
 */
public class EqualProposalField extends ProposalField {

    // FIELDS
    /** string to compare */
    private final String comparand;
    /** value to return */
    private final List<String> valueList;

    /**
     * Construct a numeric-equality field proposal
     *
     * @param fieldSpec		field specification (entity.name)
     * @param value			number parameter
     *
     * @throws ParseFailureException
     */
    public EqualProposalField(String fieldSpec, String value) throws ParseFailureException {
        super(fieldSpec);
        this.comparand = value;
        this.valueList = List.of(value);
    }

    @Override
    protected List<String> getValue(QueryEntityInstance instance) {
        List<String> retVal;
        Attribute instanceVal = instance.getAttribute(this.getName());
        List<String> actualVal = instanceVal.getList();
        boolean found = actualVal.stream().anyMatch(x -> StringUtils.equalsIgnoreCase(x, comparand));
        if (found)
            retVal = this.valueList;
        else
            retVal = ProposalField.EMPTY_LIST;
        return retVal;
    }

}
