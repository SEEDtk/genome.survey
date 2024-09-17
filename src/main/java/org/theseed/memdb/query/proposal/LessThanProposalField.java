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
     *
     * @throws ParseFailureException
     */
    public LessThanProposalField(String fieldSpec) throws ParseFailureException {
        super(fieldSpec);
    }

    @Override
    protected boolean isSatisfied(double actualVal, double targetVal) {
        return (actualVal < targetVal);
    }

}
