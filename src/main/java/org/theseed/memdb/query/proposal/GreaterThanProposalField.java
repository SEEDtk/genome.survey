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
public class GreaterThanProposalField extends ProposalField {

    /**
     * Construct a numerically-greater field proposal.
     *
     * @param fieldSpec		field specification
     *
     * @throws ParseFailureException
     */
    public GreaterThanProposalField(String fieldSpec) throws ParseFailureException {
        super(fieldSpec);
    }

}
