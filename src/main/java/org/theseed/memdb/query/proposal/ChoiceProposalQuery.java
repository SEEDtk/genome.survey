/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.regex.Matcher;

import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.query.QueryDbInstance;
import org.theseed.memdb.query.QueryEntityInstance;
import org.theseed.reports.QueryGenReporter;
import org.theseed.stats.Shuffler;

/**
 * This is a variant of the ListProposalQuery where the user wants to set up a multiple-choice response. In this
 * case, we choose one correct answer and three wrong answers to include in the question text.
 *
 * @author Bruce Parrello
 *
 */
public class ChoiceProposalQuery extends ProposalQuery {

    // FIELDS
    /** output field descriptor */
    private ExactProposalField outputField;
    /** randomizer for choosing correct answers */
    private Random rand;
    /** database instance for emergencies */
    private QueryDbInstance db;
    /** match pattern for field specification */
    private static final Pattern SPEC_PATTERN = Pattern.compile("choice\\s+(.+)");

    /**
     * Construct a list query proposal.
     *
     * @param templateString		question template string
     * @param entityPath			path through the entities
     * @param maxLimit				maximum acceptable response limit (for performance)
     * @param fieldSpec				result field specification
     * @param qDb					controlling database instance
     *
     * @throws ParseFailureException
     */
    public ChoiceProposalQuery(String templateString, String entityPath, int maxLimit, String resultString,
            QueryDbInstance qDb) throws ParseFailureException {
        super(templateString, entityPath, maxLimit);
        // Extract the output field spec from the result string.
        Matcher m = SPEC_PATTERN.matcher(resultString);
        if (! m.matches())
            throw new ParseFailureException("Invalid choice specification \"" + resultString + "\".");
        else
            this.outputField = new ExactProposalField(m.group(1));
        this.rand = new Random();
        this.db = qDb;
    }

    @Override
    protected ProposalEntity getResponseEntity() {
        // This is the same as for a ListProposalQuery: the response entity is the one containing the output field.
        String responseType = this.outputField.getEntityType();
        return this.getEntity(responseType);
    }

    @Override
    public void writeResponse(ProposalResponseSet response, QueryGenReporter reporter, List<ProposalResponseSet> others) {
        // Write the question string.
        String questionText = this.computeQuestion(response);
        // Get the output field specs.
        final String outEntityType = this.outputField.getEntityType();
        final String outEntityAttr = this.outputField.getName();
        // Get the possible correct responses.
        Set<String> correct = response.getOutputValues(outEntityType, outEntityAttr);
        if (correct.size() <= 0)
            log.warn("No correct responses found in set {}.", response);
        else {
            // Now get the possible incorrect responses. Note this only works if the reponse sets are for the this query,
            // and we filter out incorrect responses on the way.
            Set<String> incorrect = others.stream().flatMap(x -> x.getOutputValues(outEntityType, outEntityAttr).stream())
                    .filter(x -> ! correct.contains(x)).collect(Collectors.toSet());
            // Select the one correct answer we want to use.
            int desired = this.rand.nextInt(correct.size());
            String answer1 = Shuffler.selectItem(correct, desired);
            // Now we need the wrong answers.
            Collection<String> distractors = new ArrayList<String>(4);
            if (incorrect.size() > 3) {
                // The best case: we have enough answers.
                Collection<String> selected = Shuffler.selectPart(incorrect, 3);
                distractors.addAll(selected);
            } else if (incorrect.size() > 0) {
                // At least one incorrect answer, but not more than 3.
                distractors.addAll(incorrect);
            } else {
                // Here we have no incorrect answers, so we need to get some from the whole database.
                log.warn("Slow method for alternative answers required in {}.", this);
                incorrect = new HashSet<String>();
                for (var instance : this.db.getAllEntities(outEntityType)) {
                    QueryEntityInstance qInstance = (QueryEntityInstance) instance;
                    var values = qInstance.getAttribute(outEntityAttr).getList();
                    for (String value : values) {
                        if (! correct.contains(value))
                            incorrect.add(value);
                    }
                }
                distractors = Shuffler.selectPart(incorrect, 3);
            }
            // Write the question.
            reporter.writeQuestion(questionText, answer1, distractors);
        }
    }

    @Override
    public int getResponseSize(ProposalResponseSet responseSet) {
        // The number of distinct output values is always 1, the one we choose.
        return 1;
    }

}
