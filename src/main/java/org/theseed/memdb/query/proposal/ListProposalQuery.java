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
    protected ProposalField outputField;

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
        // Set the field spec for the result field.
        this.outputField = new ExactProposalField(fieldSpec);
    }

    @Override
    public void writeResponseDetails(ProposalResponseSet responses, QueryGenReporter reporter, List<ProposalResponseSet> otherResponses) {
        // Get the question string.
        String questionText = this.computeQuestion(responses);
        // Now we need to get the list of valid answers.
        Set<String> answers = responses.getOutputValues(getOutputEntityType(), getOutputAttrName());
        // Write all the answers.
        reporter.writeQuestion(responses.getParameters(), questionText, answers);
    }

    @Override
    public int getResponseSize(ProposalResponseSet responseSet) {
        // The number of distinct output values is the size of a list proposal response.
        Set<String> retVal = responseSet.getOutputValues(this.getOutputEntityType(), this.getOutputAttrName());
        return retVal.size();
    }

    /**
     * @return the attribute name of the output field
     */
    public String getOutputAttrName() {
        return this.outputField.getName();
    }

    /**
     * @return the entity type of the output field
     */
    public String getOutputEntityType() {
        return this.outputField.getEntityType();
    }

    @Override
    protected ProposalEntity getResponseEntity() {
        // Here the response entity is obvious: it's the one containing the output field.
        String responseType = this.getOutputEntityType();
        return this.getEntity(responseType);
    }

    @Override
    public String getResult() {
        return "list " + this.getOutputFieldName();
    }

    /**
     * @return the name of the output field
     */
    protected String getOutputFieldName() {
        return this.outputField.getFieldName();
    }

}
