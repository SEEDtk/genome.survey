/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.theseed.basic.ParseFailureException;

/**
 * This field specification is satisfied when the field value, interpreted numerically, is greater than
 * the parameter.
 *
 * @author Bruce Parrello
 *
 */
public class GreaterThanProposalField extends BinaryProposalField {

    /**
     * Construct a numerically-greater field proposal.
     *
     * @param fieldSpec		field specification (entity.name)
     * @param value			number parameter
     *
     * @throws ParseFailureException
     */
    public GreaterThanProposalField(String fieldSpec, String value) throws ParseFailureException {
        super(fieldSpec, value);
    }

    @Override
    protected boolean isSatisfied(double actualVal, double targetVal) {
        return (actualVal > targetVal);
    }

}
