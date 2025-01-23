/**
 *
 */
package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.subsystems.core.SubsystemRuleProjector;

/**
 * This report updates the target genome with the annotations from bidirectional best hits in the
 * source genome.
 *
 * @author Bruce Parrello
 *
 */
public class ProjectionBbhReporter extends BbhReporter {

    // FIELDS
    /** target genome */
    private Genome target;
    /** target genome file name */
    private File targetFile;
    /** TRUE if this is a dry run */
    private boolean dryRunMode;
    /** subsystem projector (if any) */
    private SubsystemRuleProjector projector;

    /**
     * Construct a projection report for the specified command processor.
     *
     * @param processor		controlling command processor
     *
     * @throws IOException
     */
    public ProjectionBbhReporter(IParms processor) throws IOException {
        this.targetFile = processor.getTargetFile();
        this.dryRunMode = processor.isDryRun();
        File subFile = processor.getSubFile();
        this.projector = SubsystemRuleProjector.load(subFile);
    }

    @Override
    protected void writeHeader() {
        this.println("target_fid\told_function\tnew_function\tsource_fid\tdistance");
    }

    @Override
    public void reportHit(Feature sourceFeat, Feature targetFeat, double distance) {
        if (targetFeat != null) {
            String sourceFun = sourceFeat.getFunction();
            if (sourceFun == null)
                sourceFun = "";
            String targetFun = targetFeat.getFunction();
            if (targetFun == null)
                targetFun = "";
            if (! sourceFun.contentEquals(targetFun)) {
                // Here the source function is different from the target function, so we make
                // an update.
                targetFeat.setFunction(sourceFun);
                this.formatln("%s\t%s\t%s\t%s\t%6.4f", targetFeat.getId(), targetFun,
                        sourceFun, sourceFeat.getId(), distance);
                // Save the target genome.
                this.target = targetFeat.getParent();
            }
        }
    }

    @Override
    public void closeReport() {
        if (this.target == null)
            log.info("No updates made to target genome.");
        else if (! this.dryRunMode) {
            // Check for a subsystem update.
            if (this.projector != null) {
                log.info("Fixing subsystems in {}.", this.target);
                this.projector.project(this.target, true);
            } else {
                // Here we can only clear the existing subsystems.
                log.info("Clearing subsystems in {}.", this.target);
                this.target.clearSubsystems();
            }
            log.info("Writing {} to file {}.", this.target, this.targetFile);
            try {
                this.target.save(this.targetFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
