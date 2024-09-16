/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.theseed.basic.ParseFailureException;

/**
 * This field proposal is satisfied when the value of the field contains the parameter value,
 * when the field is interpreted as a list.
 *
 * @author Bruce Parrello
 *
 */
public class ExactProposalField extends ProposalField {

    /**
     * Construct a new string-match proposal field.
     *
     * @param fieldSpec		field specification (entity.name)
     * @throws ParseFailureException
     */
    public ExactProposalField(String fieldSpec) throws ParseFailureException {
        super(fieldSpec);
    }

}
