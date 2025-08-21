/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.io.TabbedLineReader;
import org.theseed.reports.NaturalSort;

/**
 * This command looks for differences between two lists of feature IDs and outputs the IDs and roles of the
 * differing features.
 *
 * The positional parameters are the name of the GTO file for the genome in question, the name of the master file,
 * and the name of the alternate file.  The two files must have feature IDs in the first column.  The features
 * output will be the ones in the master file not found in the alternate file.
 *
 * The command-line options are as follows.
 *
 * -h	display command-line usage
 * -v	display more detailed log messages
 *
 * @author Bruce Parrello
 *
 */
public class CompareProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(CompareProcessor.class);
    /** reference genome */
    private Genome genome;

    // COMMAND-LINE OPTIONS

    /** file containing the reference genome */
    @Argument(index = 0, metaVar = "refGenome.gto", usage = "reference genome file", required = true)
    private File gtoFile;

    /** file containing the master feature list */
    @Argument(index = 1, metaVar = "fid.master.tbl", usage = "master feature list", required = true)
    private File masterFile;

    /** file containing the alternate feature list */
    @Argument(index = 2, metaVar = "fid.alt.tbl", usage = "alternate feature list", required = true)
    private File altFile;

    @Override
    protected void setDefaults() {
    }

    @Override
    protected void validateParms() throws IOException, ParseFailureException {
        // Verify the two input files.
        if (! this.masterFile.canRead())
            throw new FileNotFoundException("Master feature file " + this.masterFile + " is not found or unreadable.");
        if (! this.altFile.canRead())
            throw new FileNotFoundException("Alternate feature file " + this.altFile + " is not found or unreadable.");
        // Load the genome.
        if (! this.gtoFile.canRead())
            throw new FileNotFoundException("Genome file " + this.gtoFile + " is not found or unreadable.");
        this.genome = new Genome(this.gtoFile);
    }

    @Override
    protected void runCommand() throws Exception {
        // Get the master feature list.
        SortedSet<String> master = new TreeSet<>(new NaturalSort());
        master.addAll(TabbedLineReader.readSet(this.masterFile, "1"));
        log.info("{} features in master list.", master.size());
        // Get the alternate feature list.
        Set<String> alt = TabbedLineReader.readSet(this.altFile, "1");
        log.info("{} features in alternate list.", alt.size());
        // Compute the features of interest.
        master.removeAll(alt);
        log.info("{} features of interest.", master.size());
        // Loop through the features, writing the output.
        try (PrintWriter writer = new PrintWriter(System.out)) {
            writer.println("feature_id\tfunction\tsub_count");
            for (String fid : master) {
                Feature feat = genome.getFeature(fid);
                writer.format("%s\t%s\t%d%n", fid, feat.getFunction(), feat.getSubsystems().size());
            }
        }
    }

}
