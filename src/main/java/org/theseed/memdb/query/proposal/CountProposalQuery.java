/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.io.PrintWriter;
import org.theseed.basic.ParseFailureException;

/**
 * This is a proposal query that outputs the number of responses that match the criteria.
 *
 * @author Bruce Parrello
 *
 */
public class CountProposalQuery extends ProposalQuery {

    /**
     * Construct a count proposal query.
     *
     * @param templateString	question template string
     * @param entityPath		path through the entities
     * @param maxLimit			maximum acceptable response limit (for performance)
     *
     * @throws ParseFailureException
     */
    public CountProposalQuery(String templateString, String entityPath, int maxLimit) throws ParseFailureException {
        super(templateString, entityPath, maxLimit);
    }

    @Override
    public void writeResponse(ProposalResponseSet response, PrintWriter writer) {
        // Write the question string.
        this.writeQuestion(response, writer);
        // Get the answer.
        int count = response.size();
        // Write it out.
        writer.println("* Correct answer: " + count);
    }

    @Override
    public int getResponseSize(ProposalResponseSet responseSet) {
        // The number of records in the response set is the output of a count proposal.
        return responseSet.size();
    }

}
