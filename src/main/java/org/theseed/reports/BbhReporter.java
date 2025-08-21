/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;

/**
 * This is the base class for all Bidirectional Best Hit reports.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BbhReporter extends BaseReporterReporter {

    /**
     * This interface is used to retrieve information from the controlling command processor.
     */
    public interface IParms {

        /**
         * @return the file containing the target genome's GTO
         */
        File getTargetFile();

        /**
         * @return TRUE if the command is in dry-run mode (suppressing updates)
         */
        boolean isDryRun();

        /**
         * @return the subsystem projector file (or NULL if there is none)
         */
        File getSubFile();

    }

    /**
     * This enum lists the report types.
     */
    public static enum Type {
        TABULAR {
            @Override
            public BbhReporter create(IParms processor) throws IOException, ParseFailureException {
                return new TableBbhReporter(processor);
            }
        },
        PROJECTION {

            @Override
            public BbhReporter create(IParms processor) throws IOException, ParseFailureException {
                return new ProjectionBbhReporter(processor);
            }

        };

        /**
         * @return a reporter of this type
         *
         * @param processor		controlling command processor
         */
        public abstract BbhReporter create(IParms processor) throws IOException, ParseFailureException;
    }

    /**
     * Report on a bidirectional best hit.
     *
     * @param sourceFeat	source genome feature that was hit
     * @param targetFeat	target genome feature that hits back
     * @param distance		distance between the features
     */
    public abstract void reportHit(Feature sourceFeat, Feature targetFeat, double distance);

    /**
     * Finish processing the report.
     */
    public abstract void closeReport();

}
