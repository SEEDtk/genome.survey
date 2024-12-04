/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.List;
import java.util.Set;
import org.theseed.basic.ParseFailureException;
import org.theseed.reports.QueryGenReporter;

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
     * @param maxLimit				maximum acceptable response limit (for performance)
     * @param fieldSpec				result field specification
     *
     * @throws ParseFailureException
     */
    public ListProposalQuery(String templateString, String entityPath, int maxLimit, String fieldSpec) throws ParseFailureException {
        super(templateString, entityPath, maxLimit);
        this.outputField = new ExactProposalField(fieldSpec);
    }

    @Override
    public void writeResponseDetails(ProposalResponseSet responses, QueryGenReporter reporter, List<ProposalResponseSet> otherResponses) {
        // Get the question string.
        String questionText = this.computeQuestion(responses);
        // Now we need to get the list of valid answers.
        Set<String> answers = responses.getOutputValues(this.outputField.getEntityType(), this.outputField.getName());
        // Write all the answers.
        reporter.writeQuestion(questionText, answers);
    }

    @Override
    public int getResponseSize(ProposalResponseSet responseSet) {
        // The number of distinct output values is the size of a list proposal response.
        Set<String> retVal = responseSet.getOutputValues(this.outputField.getEntityType(), this.outputField.getName());
        return retVal.size();
    }

    @Override
    protected ProposalEntity getResponseEntity() {
        // Here the response entity is obvious: it's the one containing the output field.
        String responseType = this.outputField.getEntityType();
        return this.getEntity(responseType);
    }

    @Override
    public String getResult() {
        return "list " + outputField.getFieldName();
    }

}
