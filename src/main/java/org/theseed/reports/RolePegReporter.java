/**
 *
 */
package org.theseed.reports;

import java.io.PrintWriter;
import org.theseed.genome.Feature;

/**
 * This is the base class for role-peg reports.  The basic report unit is a peg in a role.
 *
 * @author Bruce Parrello
 *
 */
public abstract class RolePegReporter {

    // FIELDS
    /** output print writer */
    private PrintWriter writer;

    /**
     * This interface is used by a report subclass to interrogate the command processor for special
     * tuning parameters.
     */
    public interface IParms {

    }

    /**
     * Enumeration for the different report types.
     */
    public static enum Type {
        PEGS {
            @Override
            public RolePegReporter create(IParms processor) {
                return new TextRolePegReporter(processor);
            }
        };

        /**
         * @return a new reporter of this type.
         *
         * @param processor		controlling command processor
         */
        public abstract RolePegReporter create(IParms processor);
    }

    /**
     * Start the report.
     *
     * @param writer
     */
    public void openReport(PrintWriter writer) {
        this.writer = writer;
        this.startReport();
    }

    /**
     * Initialize the reporting and optionally write the header.
     */
    protected abstract void startReport();

    /**
     * Record a peg in a role.
     *
     * @param feat		feature for the peg to be recorded
     * @param role		ID of the associated role
     */
    public abstract void recordPeg(Feature feat, String role);

    /**
     * Finish the report.
     */
    public abstract void closeReport();

    /**
     * Write a formatted output line.
     */
    protected void print(String format, Object... args) {
        this.getWriter().format(format, args);
        this.getWriter().println();
    }

    /**
     * Write an unformatted output line.
     */
    protected void println(String line) {
        this.getWriter().println(line);
    }

    /**
     * Write a blank output line.
     */
    protected void println() {
        this.getWriter().println();
    }

    /**
     * @return the writer object
     */
    protected PrintWriter getWriter() {
        return this.writer;
    }


}
