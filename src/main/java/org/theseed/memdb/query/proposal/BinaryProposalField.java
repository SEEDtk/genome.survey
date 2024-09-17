/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.Attribute;
import org.theseed.memdb.query.QueryEntityInstance;

/**
 * This is a proposal field type that is either satisfied or violated. It is satisfied if it is
 * in the proper relation to a numeric value.  That is fundamentally different from proposal
 * fields where we are searching for a useful parameter value.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BinaryProposalField extends ProposalField {

    // FIELDS
    /** target numeric value */
    private double target;
    /** value return */
    private List<String> valueReturn;
    /** empty list */
    private static final List<String> EMPTY_LIST = Collections.emptyList();

    /**
     * Construct a binary proposal field.
     *
     * @param fieldSpec
     * @throws ParseFailureException
     */
    public BinaryProposalField(String fieldSpec) throws ParseFailureException {
        super(StringUtils.substringBefore(fieldSpec, ":"));
        try {
            String value = StringUtils.substringAfter(fieldSpec, ":");
            this.valueReturn = List.of(value);
            this.target = Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Invalid target on field specification \"" + fieldSpec + "\".");
        }
    }

    @Override
    protected List<String> getValue(QueryEntityInstance instance) {
        List<String> retVal;
        Attribute instanceVal = instance.getAtttribute(this.getName());
        double actualVal = instanceVal.getDouble();
        if (this.isSatisfied(actualVal, this.target))
            retVal = valueReturn;
        else
            retVal = EMPTY_LIST;
        return retVal;
    }

    /**
     * @return TRUE if the current value satisfies the proposal condition
     *
     * @param actualVal		current value in the entity instance
     * @param targetVal		relevant target value
     */
    protected abstract boolean isSatisfied(double actualVal, double targetVal);


}
