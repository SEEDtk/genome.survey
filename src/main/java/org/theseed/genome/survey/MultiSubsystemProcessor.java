package org.theseed.genome.survey;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;
import org.theseed.genome.iterator.BaseGenomeProcessor;

/**
 * This is a simple command that counts how many features are in multiple subsystems. For each count, we return the number
 * of features in that number of subsystems. The input in a genome source-- usually a directory of GTOs with subsystem
 * information included (this is not common, since subsystems must be projected after GTO creation).
 * 
 * The output is to the standard output in two columns: a subsystem count and the number of features with that count. Generally
 * the output will be very small, since most features are in zero or one subsystem.
 * 
 * The first positional parameter is the genome source file or directory.
 *
 * The command-line option are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	genome source type (default DIR)
 * -o   output file for report, if not STDOUT
 * 
 * 
 * @author Bruce Parrello
 */
public class MultiSubsystemProcessor extends BaseGenomeProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(MultiSubsystemProcessor.class);
    /** counter for subsystem  membership counts */
    private CountMap<Integer> subCountCounts;

    // COMMAND-LINE OPTIONS

    @Option(name = "--output", aliases = { "-o" }, usage = "output file for report (if not the standard output)")
    private File outputFile;

    @Override
    protected void setSourceDefaults() {
        this.outputFile = null;
    }

    @Override
    protected void validateSourceParms() throws IOException, ParseFailureException {
        // Verify that we can write to the output file.
        if (this.outputFile != null)
            try (var test = new PrintWriter(this.outputFile)) {
                test.println("count\t# features");
            }
    }

    @Override
    protected void runCommand() throws Exception {
        // Create the feature map.
        this.subCountCounts = new CountMap<>();
        // Get some counts for progress messages.
        int genomeCount = 0;
        int featureCount = 0;
        int totalGenomes = this.getGenomeIds().size();
        // Loop through the genomes, building a map of feature IDs to subsystem name sets.
        var genomeIDs = this.getGenomeIds();
        for (String genomeID : genomeIDs) {
            var genome = this.getGenome(genomeID);
            genomeCount++;
            log.info("Processing genome {} of {}: {}.", genomeCount, totalGenomes, genome);
            // Loop through the features, counting their subsystem memberships.
            for (var feature : genome.getFeatures()) {
                featureCount++;
                int subCount = feature.getSubsystems().size();
                this.subCountCounts.count(subCount);
            }
        }
        log.info("{} features processed from {} genomes.", featureCount, genomeCount);
        // Write the output.
        try (PrintWriter writer = this.openWriter(outputFile)) {
            log.info("Writing output.");
            writer.println("count\t# features");
            for (var entry : this.subCountCounts.sortedCounts())
                writer.format("%d\t%d%n", entry.getKey(), entry.getCount());
        }
    }
 
}
