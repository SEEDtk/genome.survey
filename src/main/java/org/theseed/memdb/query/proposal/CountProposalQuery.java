/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.reports.QueryGenReporter;

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
    public int writeResponseDetails(ProposalResponseSet response, QueryGenReporter reporter, List<ProposalResponseSet> responses) {
        // Get the question string.
        String questionText = this.computeQuestion(response);
        // Get the answer.
        int count = response.size();
        // Write it out.
        reporter.writeQuestion(response.getParameters(), questionText, count);
        return 1;
    }

    @Override
    public int getResponseSize(ProposalResponseSet responseSet) {
        // The number of records in the response set is the output of a count proposal.
        return responseSet.size();
    }

    @Override
    protected ProposalEntity getResponseEntity() {
        // The count is based on entity instances at the end of the path, since the entire path
        // is counted.
        return this.getEndOfPath();
    }

    @Override
    public String getResult() {
        return "count";
    }

}
