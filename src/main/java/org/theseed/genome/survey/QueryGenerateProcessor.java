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
import org.theseed.basic.ParseFailureException;
import org.theseed.io.LineReader;
import org.theseed.memdb.query.QueryDbDefinition;
import org.theseed.memdb.query.QueryDbInstance;
import org.theseed.utils.BaseTextProcessor;

/**
 * This command will accept as input a list of instructions for generating queries and output
 * multiple-choice test questions for an LLM training database.  The training database is
 * defined using an entity-relationship QueryDbDefinition model, and will be loaded into
 * a QueryDbInstance. Once this is complete, we will read the query-generation commands
 * from the standard input and write the test questions on the standard output.
 *
 * The positional parameters are the name of the database definition file and the name of
 * the input directory containing the data files. The input directory can potentially be
 * a master directory with multiple sub-directories containing more-or-less identical file
 * sets.
 *
 * The command-line options are
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -i	input file containing query specifications (if not STDIN)
 * -o	output file for test questions (if not STDOUT)
 * -R	if specified, the input data directory contains multiple sub-directories with identical
 * 		file structures
 *
 * @author Bruce Parrello
 *
 */
public class QueryGenerateProcessor extends BaseTextProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(QueryGenerateProcessor.class);
    /** database instance */
    QueryDbInstance db;
    /** array of input data diretories to scan */
    private File[] dataDirs;
    /** filter for data subdirectories */
    private static final FileFilter SUB_DIR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    // COMMAND-LINE OPTIONS

    /** if specified, the input directory is a master directory with multiple sub-directories containing data */
    @Option(name = "--recursive", aliases = { "-R" }, usage = "if specified, data is in subdirectories of the input data dir")
    private boolean recursive;

    /** database definition file */
    @Argument(index = 0, metaVar = "dbdFile.txt", usage = "database definition file", required = true)
    private File dbdFile;

    /** input data directory */
    @Argument(index = 1, metaVar = "dataDir", usage = "input data directory", required = true)
    private File dataDir;

    @Override
    protected void setTextDefaults() {
        this.recursive = false;
    }

    @Override
    protected void validateTextParms() throws IOException, ParseFailureException {
        if (! this.dbdFile.canRead())
            throw new FileNotFoundException("Database definition file " + this.dbdFile + " is not found or unreadable.");
        if (! this.dataDir.isDirectory())
            throw new FileNotFoundException("Data directory " + this.dataDir + " is not found or invalid.");
        if (! this.recursive) {
            // Here we are just reading data from one directory.
            this.dataDirs = new File[] { this.dataDir };
            log.info("Data will be loaded from {}.", this.dataDir);
        } else {
            this.dataDirs = this.dataDir.listFiles(SUB_DIR_FILTER);
            if (this.dataDirs.length == 0)
                throw new FileNotFoundException("No subdirectories found for " + this.dataDir + ".");
            log.info("{} data directories found in {}.", this.dataDirs.length, this.dataDir);
        }
    }

    @Override
    protected void runPipeline(LineReader inputStream, PrintWriter writer) throws Exception {
        // Load the database.
        this.loadDatabase();
        // TODO generate the queries
    }

    /**
     * Load the database into memory.
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    private void loadDatabase() throws IOException, ParseFailureException {
        long start = System.currentTimeMillis();
        // Load the definition.
        log.info("Loading database definition from {}.", this.dbdFile);
        QueryDbDefinition dbd = new QueryDbDefinition(this.dbdFile);
        // Use the definition to load the data.
        log.info("Loading data directories.");
        this.db = (QueryDbInstance) dbd.readDatabase(this.dataDirs);
        log.info("{} to load database.", Duration.ofMillis(System.currentTimeMillis() - start));
    }

}
