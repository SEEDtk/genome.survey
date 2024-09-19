/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.theseed.basic.ParseFailureException;

/**
 * This field proposal is satisfied when the specified field has a value numerically equal
 * to a parameter.
 *
 * @author Bruce Parrello
 *
 */
public class EqualProposalField extends BinaryProposalField {

    /**
     * Construct a numeric-equality field proposal
     *
     * @param fieldSpec		field specification (entity.name)
     * @param value			number parameter
     *
     * @throws ParseFailureException
     */
    public EqualProposalField(String fieldSpec, String value) throws ParseFailureException {
        super(fieldSpec, value);
    }

    @Override
    protected boolean isSatisfied(double actualVal, double targetVal) {
        return (actualVal == targetVal);
    }

}
