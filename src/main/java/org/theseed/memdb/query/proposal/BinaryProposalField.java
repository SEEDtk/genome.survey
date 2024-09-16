/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.theseed.basic.ParseFailureException;

/**
 * This is a proposal field type that is either satisfied or violated.  That is fundamentally
 * different from proposal fields where we are searching for a useful parameter value.
 *
 * @author Bruce Parrello
 *
 */
public class BinaryProposalField extends ProposalField {

    /**
     * Construct a binary proposal field.
     *
     * @param fieldSpec
     * @throws ParseFailureException
     */
    public BinaryProposalField(String fieldSpec) throws ParseFailureException {
        super(fieldSpec);
    }

}
