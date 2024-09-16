/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.io.PrintWriter;

import org.theseed.basic.ParseFailureException;

/**
 * This is a query proposal where the result is a list of field values from the response set.
 *
 * @author Bruce Parrello
 *
 */
public class ListProposalQuery extends ProposalQuery {

    // FIELDS
    /** output field specification */
    private ProposalField outputField;

    /**
     * Construct a list query proposal.
     *
     * @param templateString		question template string
     * @param entityPath			path through the entities
     * @param fieldSpec				result field specification
     *
     * @throws ParseFailureException
     */
    public ListProposalQuery(String templateString, String entityPath, String fieldSpec) throws ParseFailureException {
        super(templateString, entityPath);
        this.outputField = new ExactProposalField(fieldSpec);
    }

    @Override
    public void writeResponse(ProposalResponseSet response, PrintWriter writer) {
        // TODO code for writeResponse List
    }

}
