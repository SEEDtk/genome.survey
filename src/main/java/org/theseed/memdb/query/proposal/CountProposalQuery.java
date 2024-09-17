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
     *
     * @throws ParseFailureException
     */
    public CountProposalQuery(String templateString, String entityPath) throws ParseFailureException {
        super(templateString, entityPath);
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

}
