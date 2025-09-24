/**
 *
 */
package org.theseed.genome.survey;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;
import org.theseed.memdb.query.proposal.ExactProposalField;
import org.theseed.memdb.query.proposal.ListProposalQuery;
import org.theseed.memdb.query.proposal.ProposalResponse;
import org.theseed.memdb.query.proposal.ProposalResponseSet;
import org.theseed.reports.QueryGenReporter;

import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * The group proposal is very similar to the list proposal; however, the answer is a CSV table of values and counts. The
 * value is taken from a specified result field and the count is the number of output records with that value. A count
 * map is used to accumulate the values and counts. Group queries are performance-intensive because they have to return
 * everything, unlike normal queries.
 *
 * @author Bruce Parrello
 *
 */
public class GroupProposalQuery extends ListProposalQuery {

    /**
     * Construct a group query proposal.
     *
     * @param templateString		question template string
     * @param entityPath			path through the entities
     * @param maxLimit				maximum acceptable response limit (for performance)
     * @param fieldSpec				result field specification
     *
     * @throws ParseFailureException
     */
    public GroupProposalQuery(String templateString, String entityPath, int maxLimit, String resultString) throws ParseFailureException {
        super(templateString, entityPath, maxLimit, resultString);
        // Fix the field spec for the result field. This is messy, but we can't do overrides in constructors
        // any more.
        String[] pieces = StringUtils.split(resultString);
        if (pieces.length < 2)
            throw new ParseFailureException("Invalid field specification on group query proposal.");
        this.outputField = new ExactProposalField(pieces[1]);
    }

    @Override
    public void writeResponseDetails(ProposalResponseSet responses, QueryGenReporter reporter,
            List<ProposalResponseSet> others) {
        // Get the question string.
        String questionText = this.computeQuestion(responses);
        // Get the values and counts.
        CountMap<String> countMap = this.getCountMap(responses);
        // Form them into a CSV string sorted by key.
        StringBuilder outString = new StringBuilder(countMap.size() * 30);
        for (String key : countMap.keys()) {
            int value = countMap.getCount(key);
            outString.append('"').append(Jsoner.escape(key)).append('"');
            outString.append(',').append(value).append("\n");
        }
        // Make the CSV a singleton answer set.
        Set<String> answers = Set.of(outString.toString());
        // Write the question and response.
        reporter.writeQuestion(responses.getParameters(), questionText, answers);
    }

    @Override
    public int getResponseSize(ProposalResponseSet responseSet) {
        CountMap<String> countMap = this.getCountMap(responseSet);
        return countMap.size();
    }

    /**
     * Build a table of counts for each field value from a response set.
     *
     * @param responseSet	response set to be converted to a table of counts
     *
     * @return a table of field values and counts for the response set
     */
    private CountMap<String> getCountMap(ProposalResponseSet responseSet) {
        CountMap<String> retVal = new CountMap<>();
        // Get the key attribute name and entity type.
        String entityType = this.getOutputEntityType();
        String attrName = this.getOutputAttrName();
        // Loop through the responses in the response set.
        for (ProposalResponse response : responseSet) {
            List<String> outputValue = response.getValue(entityType, attrName);
            outputValue.forEach(x -> retVal.count(x));
        }
        return retVal;
    }

    @Override
    public String getResult() {
        return "table " + this.getOutputFieldName();
    }

}
