/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.DbDefinition;
import org.theseed.memdb.walker.TextDbDefinition;
import org.theseed.memdb.walker.TextDbInstance;

/**
 * This command will use an entity-relationship model to guide a random walk of a JSON database dump.  The model
 * uses templates to create text about the database.  This is described in the "DbDefinition" class.
 *
 * The positional parameters are the name of the definition text file, and the name of the input dump directory.
 *
 * The text will be written to the standard output.
 *
 * The command-line options are as follows:
 *
 * -h	display commmand-line usage
 * -v	display more frequent log messages
 * -o	output file for the text (if not STDOUT)
 * -R	if specified, the input directory is considered a master directory, and all subdirectories will be processed
 *
 * @author Bruce Parrello
 *
 */
public class RandomWalkProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(RandomWalkProcessor.class);
    /** list of dump directories to process */
    private File[] inDirs;
    /** file filter for subdirectories */
    private FileFilter SUB_DIR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };


    // COMMAND-LINE OPTIONS

    /** if specified, then subdirectories of the input directory are processed instead of the directory itself */
    @Option(name = "--recursive", aliases = { "-R" }, usage = "if specified, process the subdirectories of the input directory instead of the input directory itself")
    private boolean recursive;

    /** name of the database definition file */
    @Argument(index = 0, metaVar = "definition.txt", usage = "database definition file", required = true)
    private File dbdFile;

    /** name of the input directory */
    @Argument(index = 1, metaVar = "inDir", usage = "input dump directory", required = true)
    private File inDir;

    @Override
    protected void setReporterDefaults() {
        this.recursive = false;
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Validate the input directory.
        if (! this.inDir.isDirectory())
            throw new FileNotFoundException("Invalid input directory " + this.inDir + ".");
        if (! this.recursive) {
            // Here we have a single input directory.
            this.inDirs = new File[] { this.inDir };
            log.info("Input directory for data is {}.", this.inDir);
        } else {
            // Here we are checking all the subdirectories.
            this.inDirs = this.inDir.listFiles(SUB_DIR_FILTER);
            log.info("{} subdirectories found in {}.", this.inDirs.length, this.inDir);
        }
        // Now we process the database definition.
        if (! this.dbdFile.canRead())
            throw new FileNotFoundException("Database definition file " + this.dbdFile + " is not found or unreadable.");
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // Create the database definition.
        long start = System.currentTimeMillis();
        DbDefinition dbd = new TextDbDefinition(this.dbdFile);
        Duration d = Duration.ofMillis(System.currentTimeMillis() - start);
        log.info("{} to compile database definition.", d);
        // Read in the data.
        log.info("Reading data from {}.", this.inDir);
        start = System.currentTimeMillis();
        TextDbInstance db = (TextDbInstance) dbd.readDatabase(this.inDirs);
        d = Duration.ofMillis(System.currentTimeMillis() - start);
        log.info("{} to read in database.", d);
        // Now perform the random walk.
        log.info("Writing output.");
        start = System.currentTimeMillis();
        db.generateWalk(writer);
        d = Duration.ofMillis(System.currentTimeMillis() - start);
        log.info("{} to generate random walk with {} tokens.", d, db.getTokenTotal());
    }

}
