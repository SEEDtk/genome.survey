/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;
import org.theseed.io.LineReader;
import org.theseed.io.Shuffler;
import org.theseed.utils.BaseProcessor;
import org.theseed.utils.ParseFailureException;

/**
 * This simple command creates a directory of the well-behaved Core genomes.  These are Bacteria or Archaea that are not considered
 * questionable.  The questionable genomes are listed in Eval.New/questionables.tbl.  The full set of GTOs is in GTOcouple.
 *
 * The positional parameters are the name of the CoreSEED directory and the name of the output directory.  If the "split" option is
 * greater than zero, the third parameter is the name of the target directory for the split.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	show more detailed log messages.
 *
 * --clear	erase the output directory or directories before starting
 * --split	number of genomes to put into split directory (default 0)
 *
 * @author Bruce Parrello
 *
 */
public class GoodCoreProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(GoodCoreProcessor.class);
    /** list of genome IDs */
    private Shuffler<String> genomeIDs;
    /** set of questionable genomes */
    private Set<String> questionables;
    /** genome input directory */
    private File gtoDir;

    // COMMAND-LINE OPTIONS

    /** TRUE to erase the output directory before starting */
    @Option(name = "--clear", usage = "if specified, files in the output directory will be erased before processing")
    private boolean clearFlag;

    /** number of genomes to put into split directory */
    @Option(name = "--split", usage = "number of genomes to put into split directory")
    private int splitCount;

    /** name of the CoreSEED directory */
    @Argument(index = 0, metaVar = "CoreSEED", usage = "name of CoreSEED directory", required = true)
    private File coreDir;

    /** name of the output directory */
    @Argument(index = 1, metaVar = "outDir", usage = "name of output directory", required = true)
    private File outDir;

    /** name of the split directory */
    @Argument(index = 2, metaVar = "splitDir", usage = "alternate output directory (optional)")
    private File splitDir;

    @Override
    protected void setDefaults() {
        this.clearFlag = false;
        this.splitDir = null;
        this.splitCount = 0;
    }

    @Override
    protected boolean validateParms() throws IOException, ParseFailureException {
        if (! this.coreDir.isDirectory())
            throw new FileNotFoundException("Cannot find input CoreSEED directory " + this.coreDir + ".");
        else {
            // Verify that we have the GTO directory.
            this.gtoDir = new File(this.coreDir, "GTOcouple");
            if (! this.gtoDir.isDirectory())
                throw new FileNotFoundException("GTOcouple not found in CoreSEED directory " + this.coreDir + ".");
            File qFile = new File(this.coreDir, "Eval.New/questionables.tbl");
            if (! qFile.exists())
                throw new FileNotFoundException("Questionables file in Eval.New not found in CoreSEED directory " + this.coreDir + ".");
            // Load the genomes.
            GenomeDirectory genomes = new GenomeDirectory(gtoDir);
            log.info("{} genomes found.", genomes.size());
            this.genomeIDs = new Shuffler<String>(genomes.getGenomeIDs());
            // Load the questionables.
            this.questionables = LineReader.readSet(qFile);
            log.info("{} questionable genomes identified.", this.questionables.size());
        }
        if (this.splitCount > 0 && this.splitDir == null)
            throw new ParseFailureException("The name of the split directory is required if the split count is greater than 0.");
        prepareOutputDirectory(this.outDir);
        if (this.splitDir != null)
            prepareOutputDirectory(this.splitDir);
        return true;
    }

    /**
     * Prepare a directory for output.
     *
     * @param testDir	directory to prepare
     *
     * @throws IOException
     */
    protected void prepareOutputDirectory(File testDir) throws IOException {
        if (! testDir.isDirectory()) {
            log.info("Creating output directory {}.", testDir);
            FileUtils.forceMkdir(testDir);
        } else if (this.clearFlag) {
            log.info("Erasing output directory {}.", testDir);
            FileUtils.cleanDirectory(testDir);
        } else {
            log.info("Output directory is {}.", testDir);
        }
    }

    @Override
    protected void runCommand() throws Exception {
        int qFound = 0;
        int saved = 0;
        // Get the genome IDs in random order.
        genomeIDs.shuffle(genomeIDs.size());
        // Loop through the genomes, saving the good ones in the appropriate output directory.
        for (String genomeID : this.genomeIDs) {
             if (this.questionables.contains(genomeID))
                qFound++;
            else {
                String gName = genomeID + ".gto";
                File gFile = new File(this.gtoDir, gName);
                Genome genome = new Genome(gFile);
                String d = genome.getDomain();
                if (d.contentEquals("Bacteria") || d.contentEquals("Archaea")) {
                    saved++;
                    File outFile;
                    if (saved <= this.splitCount)
                        outFile = new File(this.splitDir, gName);
                    else
                        outFile = new File(this.outDir, gName);
                    log.info("Saving genome #{}: {} to {}.", saved, genome, outFile);
                    genome.update(outFile);
                }
            }
        }
        log.info("{} genomes saved. {} questionable found.", saved, qFound);
    }

}
