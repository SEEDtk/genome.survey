/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.List;

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

    /**
     * Construct a binary proposal field.
     *
     * @param fieldSpec		field specification (entity.name)
     * @param value			number parameter
     *
     * @throws ParseFailureException
     */
    public BinaryProposalField(String fieldSpec, String value) throws ParseFailureException {
        super(fieldSpec);
        try {
            this.valueReturn = List.of(value);
            this.target = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Invalid target on field specification \"" + fieldSpec + "\".");
        }
    }

    @Override
    protected List<String> getValue(QueryEntityInstance instance) {
        List<String> retVal;
        Attribute instanceVal = instance.getAttribute(this.getName());
        double actualVal = instanceVal.getDouble();
        if (this.isSatisfied(actualVal, this.target))
            retVal = valueReturn;
        else
            retVal = ProposalField.EMPTY_LIST;
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
