/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.theseed.basic.ParseFailureException;

/**
 * This field proposal is satisfied if the field value, interpreted numerically, is less than the parameter
 * value.
 *
 * @author Bruce Parrello
 *
 */
public class LessThanProposalField extends BinaryProposalField {

    /**
     * Construct a numerically-less field proposal.
     *
     * @param fieldSpec		field specification (entity.name)
     * @param value			number parameter
     *
     * @throws ParseFailureException
     */
    public LessThanProposalField(String fieldSpec, String value) throws ParseFailureException {
        super(fieldSpec, value);
    }

    @Override
    protected boolean isSatisfied(double actualVal, double targetVal) {
        return (actualVal < targetVal);
    }

}
