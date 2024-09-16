/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.theseed.basic.ParseFailureException;

/**
 * This field proposal is satisfied when the requested field evaluates to TRUE as a boolean.
 *
 * @author Bruce Parrello
 *
 */
public class BooleanProposalField extends ProposalField {

    /**
     * Construct a boolean proposal field.
     *
     * @param fieldSpec		field specification (entity.name)
     *
     * @throws ParseFailureException
     */
    public BooleanProposalField(String fieldSpec) throws ParseFailureException {
        super(fieldSpec);
    }

}
