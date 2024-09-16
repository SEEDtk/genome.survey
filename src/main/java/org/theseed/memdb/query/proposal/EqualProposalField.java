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
public class EqualProposalField extends ProposalField {

    /**
     * Construct a numeric-equality field proposal
     *
     * @param fieldSpec		field specification (entity.name)
     *
     * @throws ParseFailureException
     */
    public EqualProposalField(String fieldSpec) throws ParseFailureException {
        super(fieldSpec);
    }

}
