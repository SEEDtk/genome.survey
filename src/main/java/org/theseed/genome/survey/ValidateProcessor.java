/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;

/**
 * This program reads all the genomes in a genome directory to make sure they are valid.  If
 * a genome fails to load, it will write its ID to the output file.  It is presumed the file
 * name for each genome is the genome ID with the suffix ".gto".
 *
 * The positional parameter is the name of the genome directory.
 *
 * The command-line options are as follows.
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -o	name of the output file (if not STDIN)
 *
 * @author Bruce Parrello
 *
 */
public class ValidateProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(ValidateProcessor.class);
    /** input genome directory */
    private GenomeDirectory genomes;

    // COMMAND-LINE OPTIONS

    /** input genome directory name */
    @Argument(index = 0, metaVar = "genomeDir", usage = "input genome directory")
    private File genomeDir;

    @Override
    protected void setReporterDefaults() {
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Insure the genome directory is valid.
        if (! this.genomeDir.isDirectory())
            throw new FileNotFoundException("Input directory " + this.genomeDir + " is not found or not a directory.");
        this.genomes = new GenomeDirectory(this.genomeDir);
        log.info("{} genome files found in {}.", this.genomes.size(), this.genomeDir);
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        int badGenomes = 0;
        var genomeList = this.genomes.getGenomeIDs();
        // Loop through the genome IDs.
        for (String genomeId : genomeList) {
            log.info("Checking genome ID {}.", genomeId);
            try {
                Genome genome = this.genomes.getGenome(genomeId);
                log.info("Genome {} validated.", genome);
            } catch (Exception e) {
                if (log.isInfoEnabled()) {
                    // Here we want to output the error.  Insure we have the original cause.
                    Throwable original = e;
                    while (original.getCause() != null)
                        original = original.getCause();
                    log.info("Error in genome {}: {}", genomeId, original.toString());
                }
                // Now we must output the bad genome ID.  If it's the first one, we also output the header.
                if (badGenomes == 0)
                    writer.println("genome_id");
                writer.println(genomeId);
                badGenomes++;
            }
        }
        log.info("{} bad genomes found.", badGenomes);
    }

}
