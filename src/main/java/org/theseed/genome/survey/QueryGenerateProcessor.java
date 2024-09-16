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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.LineReader;
import org.theseed.memdb.query.QueryDbDefinition;
import org.theseed.memdb.query.QueryDbInstance;
import org.theseed.memdb.query.proposal.CountProposalQuery;
import org.theseed.memdb.query.proposal.ListProposalQuery;
import org.theseed.memdb.query.proposal.ProposalQuery;
import org.theseed.memdb.query.proposal.ProposalResponseSet;
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
 * The queries are described in the standard input with three lines of information per query.  The
 * first line contains a text question with embedded field names. The second line contains a path
 * through the entity-relationship model. The third line describes the field containing the answer.
 * This last can be "count" if the answer is a count of the applicable records.
 *
 * Embedded field names in the questions are enclosed in double curly braces. All fields are expressed
 * as an entity name followed by a period and the field name. If a prefix of ">", "<", or "=" is specified for
 * a field name, then the field is interpreted as numeric, and it is expected we are asking for a relation
 * of greater than, less than, or equal, respectively. The field name must be followed by a colon and a
 * value. In this case, the value is fixed, so the field is effectively treated as a boolean. If a prefix of
 * "?" is specified, then the field is interpreted as a real boolean, and only satisfied if it is true.
 *
 * The command-line options are
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -i	input file containing query specifications (if not STDIN)
 * -o	output file for test questions (if not STDOUT)
 * -R	if specified, the input data directory contains multiple sub-directories with identical
 * 		file structures
 *
 * --target		maximum number of desirable results for a query (default 10)
 *
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

    /** target result set size */
    @Option(name = "--target", metaVar = "20", usage = "maximum desired target set size")
    private int targetSize;

    /** database definition file */
    @Argument(index = 0, metaVar = "dbdFile.txt", usage = "database definition file", required = true)
    private File dbdFile;

    /** input data directory */
    @Argument(index = 1, metaVar = "dataDir", usage = "input data directory", required = true)
    private File dataDir;

    @Override
    protected void setTextDefaults() {
        this.recursive = false;
        this.targetSize = 10;
    }

    @Override
    protected void validateTextParms() throws IOException, ParseFailureException {
        // Verify the input files.
        if (! this.dbdFile.canRead())
            throw new FileNotFoundException("Database definition file " + this.dbdFile + " is not found or unreadable.");
        if (! this.dataDir.isDirectory())
            throw new FileNotFoundException("Data directory " + this.dataDir + " is not found or invalid.");
        // Validate the target size.
        if (this.targetSize < 1)
            throw new ParseFailureException("Invalid target size: must be positive.");
        // Assemble the list of input directories.
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
        // Loop through the input file, reading query specifications.
        Iterator<String> inputIter = inputStream.iterator();
        while (inputIter.hasNext()) {
            // Get the three query lines.
            String qString = inputIter.next();
            String pathString = this.safeGet(inputIter);
            String resultString = this.safeGet(inputIter);
            // Create the query proposal.
            ProposalQuery proposal;
            if (StringUtils.compareIgnoreCase(resultString, "count") == 0)
                proposal = new CountProposalQuery(qString, pathString);
            else
                proposal = new ListProposalQuery(qString, pathString, resultString);
            log.info("Computing responses for query: {}", qString);
            List<ProposalResponseSet> responses = proposal.computeSets(this.db);
            log.info("{} response sets found.", responses.size());
            // Loop through the response sets, writing the questions.
            for (ProposalResponseSet response : responses)
                proposal.writeResponse(response, writer);
        }

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

    /**
     * This gets the next record and throws an IO error if we are at end-of-file.
     *
     * @param inputIter		iterator for the input file
     *
     * @return the next record in the input file
     *
     * @throws IOException
     */
    private String safeGet(Iterator<String> inputIter) throws IOException {
        if (! inputIter.hasNext())
            throw new IOException("Early end-of-file on input.");
        return inputIter.next();
    }


}
