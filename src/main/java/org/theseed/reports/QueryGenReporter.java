/**
 *
 */
package org.theseed.reports;

import java.io.PrintWriter;
import java.util.Collection;

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

    /**
     * This enumeration selects the different report types.
     */
    public static enum Type {
        TEXT {
            @Override
            public QueryGenReporter create(IParms processor) {
                // TODO code for create
                return null;
            }
        }, JSON {
            @Override
            public QueryGenReporter create(IParms processor) {
                // TODO code for create
                return null;
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

    }

    /**
     * Construct a query-generation reporter for a specific command processor.
     *
     * @param processor		controlling command processor
     */
    public QueryGenReporter(IParms processor) {
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
     * Write a question with multiple correct answers.
     *
     * @param questionText		text of the question
     * @param answers			list of correct answers
     */
    public abstract void writeQuestion(String questionText, Collection<String> answers);

    /**
     * Write a question with a single correct answer.
     *
     * @param questionText		text of the question
     * @param answer			correct answer
     */
    public abstract void writeQuestion(String questionText, String answer);

    /**
     * Write a multiple-choice question.
     *
     * @param questionText		text of the question
     * @param answer			correct answer
     * @param distractors		incorrect answers
     */
    public abstract void writeQuestion(String questionText, String answer, Collection<String> distractors);

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

}
