/**
 *
 */
package org.theseed.reports;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.theseed.json.JsonFileDir;
import org.theseed.memdb.query.proposal.Parameterization;
import org.theseed.memdb.query.proposal.ProposalQuery;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This is the report writer for query generation. Query reports are generally designed to be read by
 * other software, so they are not as structured as a normal report.
 *
 * @author Bruce Parrello
 *
 */
public abstract class QueryGenReporter {

    // FIELDS
    /** output print writer */
    private PrintWriter writer;
    /** random number generator */
    private final Random rand;
    /** saved proposal query */
    private ProposalQuery query;

    /**
     * This enumeration selects the different report types.
     */
    public static enum Type {
        /** line-by-line text display of questions and answers */
        TEXT {
            @Override
            public QueryGenReporter create(IParms processor) {
                return new TextQueryGenReporter(processor);
            }
        },
        /** standard json-format questions and answers */
        JSON {
            @Override
            public QueryGenReporter create(IParms processor) {
                return new JsonQueryGenReporter(processor);
            }
        },
        /** conversational json-format questions and answers */
        CONVO {
            @Override
            public QueryGenReporter create(IParms processor) {
                return new ConvoQueryGenReporter(processor);
            }
        };

        /**
         * @return a query generation report writer of this type
         *
         * @param processor		controlling command processor
         */
        public abstract QueryGenReporter create(IParms processor);

    }

    /**
     * Command processors that use this reporter must support this interface.
     */
    public interface IParms {

        /**
         * @return a JSON object containing the constant fields for each output question
         */
        JsonObject getConstantJson();

    }

    /**
     * Construct a query-generation reporter for a specific command processor.
     *
     * @param processor		controlling command processor
     */
    public QueryGenReporter(IParms processor) {
        this.rand = new Random();
    }

    /**
     * Initialize this report.
     *
     * @param printWriter	output print writer
     */
    public void open(PrintWriter printWriter) {
        this.writer = printWriter;
        this.startReport();
    }

    /**
     * Start the report output.
     */
    protected abstract void startReport();

    /**
     * Save the question template.
     *
     * @param template		question template
     */
    public void saveTemplate(ProposalQuery template) {
        this.query = template;
    }

    /**
     * Write a question with multiple correct answers.
     *
     * @param parms             parameterization for the question
     * @param questionText		text of the question
     * @param answers			list of correct answers
     */
    public abstract void writeQuestion(Parameterization parms, String questionText, Collection<String> answers);

    /**
     * Write a question with a single numeric answer.
     *
     * @param parms             parameterization for the question
     * @param questionText		text of the question
     * @param answer			correct answer
     */
    public abstract void writeQuestion(Parameterization parms, String questionText, int answer);

    /**
     * Write a multiple-choice question.
     *
     * @param parms             parameterization for the question
     * @param questionText		text of the question
     * @param answer			correct answer
     * @param distractors		incorrect answers
     */
    public abstract void writeQuestion(Parameterization parms, String questionText, String answer, Collection<String> distractors);

    /**
     * Finish the report.
     */
    public void close() {
        this.finishReport();
        this.writer.flush();
    }

    /**
     * Finish the report output.
     */
    public abstract void finishReport();

    /**
     * Write a line of output.
     *
     * @param line	text to write
     */
    public void write(String line) {
        this.writer.println(line);
    }

    /**
     * Write a line of output without a newline.
     *
     * @param line	text to write
     */
    public void writeNoNL(String line) {
        this.writer.print(line);
    }

    /**
     * Flush the output.
     */
    public void flush() {
        this.writer.flush();
    }

    /**
     * @return the output print writer
     */
    protected PrintWriter getWriter() {
        return this.writer;
    }

    /**
     * This is a utility method that randomizes the correct answer and the distractors
     * in a multiple-choice question.
     *
     * @param answer		correct answers
     * @param distractors	alternate answers
     *
     * @return a shuffled list of all the answers
     */
    public List<String> randomizeChoices(String answer, Collection<String> distractors) {
        ArrayList<String> choices = new ArrayList<>(distractors.size() + 1);
        choices.add(answer);
        choices.addAll(distractors);
        Collections.shuffle(choices, this.rand);
        return choices;
    }

    /**
     * This is a utility method that outputs a JSON array.
     *
     * @param outJson	JSON array to output
     */
    protected void outputAllJson(JsonArray outJson) {
        JsonFileDir.writeJson(outJson, this.writer);
    }

    /**
     * @return the query that generated the current response
     */
    public ProposalQuery getQuery() {
        return this.query;
    }
}
