/**
 *
 */
package org.theseed.reports;

import org.theseed.genome.Feature;

/**
 * This report simply lists the bidirectional best hits in a tab-delimited output file.
 *
 * @author Bruce Parrello
 *
 */
public class TableBbhReporter extends BbhReporter {

    /**
     * Construct a tabular BBH reporter.
     *
     * @param processor		controlling command processor
     */
    public TableBbhReporter(IParms processor) {
    }

    @Override
    protected void writeHeader() {
        this.println("source_fid\tfunction\ttarget_fid\tfunction\tdistance");
    }

    @Override
    public void reportHit(Feature sourceFeat, Feature targetFeat, double distance) {
        String targetFid;
        String targetFunction;
        if (targetFeat == null) {
            targetFid = "";
            targetFunction = "";
        } else {
            targetFid = targetFeat.getId();
            targetFunction = targetFeat.getFunction();
            if (targetFunction == null)
                targetFunction = "";
        }
        String sourceFunction = sourceFeat.getFunction();
        if (sourceFunction == null)
            sourceFunction = "";
        this.formatln("%s\t%s\t%s\t%s\t%6.4f", sourceFeat.getId(), sourceFunction, targetFid,
                targetFunction, distance);
    }

    @Override
    public void closeReport() {
    }

}
