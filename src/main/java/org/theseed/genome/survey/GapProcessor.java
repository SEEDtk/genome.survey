/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.locations.Location;

/**
 * This command takes as input a single GTO, and outputs an analysis of the gap distance between adjacent genes, categorized
 * in multiples of 100.
 *
 * All the pegs will be sorted using the StrandComparator.  Each will be compared to the following feature.  If they are
 * on the same strand, the appropriate length bucket (int(gap/100)) will be incremented.
 *
 * The positional parameter is the name of the GTO file.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 *
 * @author Bruce Parrello
 *
 */
public class GapProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(GapProcessor.class);
    /** genome of interest */
    private Genome genome;

    // COMMAND-LINE OPTIONS

    /** file containing genome of interest */
    @Argument(index = 0, metaVar = "inFile.gto", usage = "name of file containing genome to examine")
    private File inFile;


    @Override
    protected void setDefaults() {
    }

    @Override
    protected void validateParms() throws IOException, ParseFailureException {
        // Read in the genome.
        if (! this.inFile.canRead())
            throw new FileNotFoundException("Genome file " + this.inFile + " is not found or unreadable.");
        log.info("Reading genome from {}.", this.inFile);
        this.genome = new Genome(this.inFile);
    }

    @Override
    protected void runCommand() throws Exception {
        log.info("Processing genome {}.", this.genome);
        // We will create a list of features sorted by strandcomparator.
        SortedSet<Feature> sorted = new TreeSet<Feature>(new Feature.StrandComparator());
        sorted.addAll(this.genome.getPegs());
        // Now set up the output buckets.  These are for values from 0 to 499.
        int[] buckets = new int[5];
        Arrays.fill(buckets, 0);
        // This is for values of 500 or more (including different strand or contig).
        int bigBucket = 0;
        // Loop through the features, filling the buckets.
        log.info("Comparing {} pegs.", sorted.size());
        Iterator<Feature> iter = sorted.iterator();
        Location prev = iter.next().getLocation();
        while (iter.hasNext()) {
            Location curr = iter.next().getLocation();
            if (! curr.isSameStrand(prev))
                bigBucket++;
            else {
                // Here the locations are comparable.
                int gap = curr.distance(prev);
                if (gap >= 500)
                    bigBucket++;
                else if (gap < 0)
                    buckets[0]++;
                else
                    buckets[gap / 100]++;
            }
            prev = curr;
        }
        // Write the results.
        System.out.println("Range\tCount");
        System.out.format("< 100\t%8d%n", buckets[0]);
        for (int i = 1; i < 5; i++)
            System.out.format("%d to < %d\t%8d%n", i*100, (i+1)*100, buckets[i]);
        System.out.format(">= 500\t%8d%n", bigBucket);
        log.info("All done.");
    }
}
