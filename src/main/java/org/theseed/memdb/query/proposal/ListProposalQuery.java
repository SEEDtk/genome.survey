/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
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
    public void writeResponse(ProposalResponseSet responses, PrintWriter writer) {
        // Write the question string.
        this.writeQuestion(responses, writer);
        // Now we need to get the list of valid answers.
        Set<String> answers = new TreeSet<String>();
        for (ProposalResponse response : responses.getResponses()) {
            List<String> values = response.getValue(outputField.getEntityType(), outputField.getName());
            answers.addAll(values);
        }
        // Write all the answers.
        writer.println("* Correct answers: " + StringUtils.join(answers, ", "));
    }

}
