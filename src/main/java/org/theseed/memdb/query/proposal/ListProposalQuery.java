/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
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
    public void writeResponse(ProposalResponseSet responses, PrintWriter writer, List<ProposalResponseSet> otherResponses) {
        // Write the question string.
        this.writeQuestion(responses, writer);
        // Now we need to get the list of valid answers.
        Set<String> answers = responses.getOutputValues(this.outputField.getEntityType(), this.outputField.getName());
        // Write all the answers.
        writer.println("* Correct answers:");
        for (String answer : answers)
            writer.println("\t" + answer);
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

}
