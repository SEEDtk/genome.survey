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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.LineReader;
import org.theseed.io.LineReaderCommentSkipper;
import org.theseed.memdb.query.QueryDbDefinition;
import org.theseed.memdb.query.QueryDbInstance;
import org.theseed.memdb.query.proposal.ChoiceProposalQuery;
import org.theseed.memdb.query.proposal.CountProposalQuery;
import org.theseed.memdb.query.proposal.ListProposalQuery;
import org.theseed.memdb.query.proposal.ProposalQuery;
import org.theseed.memdb.query.proposal.ProposalResponseSet;
import org.theseed.reports.QueryGenReporter;
import org.theseed.utils.BaseTextProcessor;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

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
 * This last can be "count" if the answer is a count of the applicable records, or it can be
 * "choice" followed by a space and a field specification if a multiple-choice query is desired.
 * 
 * Input lines beginning with '#' are treated as comments and ignored.
 *
 * Embedded field names in the questions are enclosed in double curly braces. All fields are expressed
 * as an entity name followed by a period and the field name. If a prefix of ">" or "<" is specified for
 * a field name, then the field is interpreted as numeric, and it is expected we are asking for a relation
 * of greater than or less than, respectively. The field name must be followed by a colon and a value. In
 * this case, the value is fixed, so the field is effectively treated as a boolean.
 *
 * To prevent memory from being overwhelmed, a cutoff limit is specified for the size of an intermediate result
 * set during the search. As the algorithm travels down the path, it builds a set of all the instance sets with
 * identical parameters. If one of these sets gets too big, we discard it before proceeding. If the initial
 * entity instance set is too big, we pare it down by selecting random elements. Both of these measures may cause us to
 * lose acceptable results, but it is a concession to what we can do with limited memory.
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
 * --limit		maximum intermediate result set size (default 20000)
 * --max		maximum number of examples to output per query template (default 100)
 * --format		output report format (default JSON)
 * --domain		question domains to list in JSON output (may occur multiple times)
 * --support	support string to include in JSON output
 * --choices    number of multiple-choice responses to generate (default 4)
 *
 * @author Bruce Parrello
 *
 */
public class QueryGenerateProcessor extends BaseTextProcessor implements QueryGenReporter.IParms {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(QueryGenerateProcessor.class);
    /** database instance */
    QueryDbInstance db;
    /** array of input data diretories to scan */
    private File[] dataDirs;
    /** output report writer */
    private QueryGenReporter reporter;
    /** list of failed templates */
    private List<String> failedTemplates;
    /** filter for data subdirectories */
    private static final FileFilter SUB_DIR_FILTER = (File pathname) -> pathname.isDirectory();

    // COMMAND-LINE OPTIONS

    /** if specified, the input directory is a master directory with multiple sub-directories containing data */
    @Option(name = "--recursive", aliases = { "-R" }, usage = "if specified, data is in subdirectories of the input data dir")
    private boolean recursive;

    /** target result set size */
    @Option(name = "--target", metaVar = "20", usage = "maximum desired target set size")
    private int targetSize;

    /** maximum intermediate result set size */
    @Option(name = "--limit", metaVar = "10000", usage = "maximum intermediate result set size")
    private int maxLimit;

    /** maximum number of output questions per query template */
    @Option(name = "--max", metaVar = "10", usage = "maximum number of output questions per query template")
    private int maxOutput;

    /** output report format */
    @Option(name = "--format", usage = "output report format")
    private QueryGenReporter.Type reportType;

    /** domain for output questions (JSON report only) */
    @Option(name = "--domain", metaVar = "chemistry", usage = "for JSON output, domain to specify for each question")
    private List<String> domains;

    /** support string for output questions (JSON report only) */
    @Option(name = "--support", metaVar = "U_Chicago", usage = "for JSON output, support organization to specify for each question")
    private String support;

    /** number of multiple-choice responses to generate */
    @Option(name = "--choices", metaVar = "6", usage = "number of multiple-choice responses to generate")
    private int numChoices;

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
        this.maxLimit = 5000;
        this.maxOutput = 100;
        this.reportType = QueryGenReporter.Type.JSON;
        this.domains = new ArrayList<>();
        this.support = "";
        this.numChoices = 4;
    }

    @Override
    protected void validateTextParms() throws IOException, ParseFailureException {
        // Verify the input files.
        if (! this.dbdFile.canRead())
            throw new FileNotFoundException("Database definition file " + this.dbdFile + " is not found or unreadable.");
        if (! this.dataDir.isDirectory())
            throw new FileNotFoundException("Data directory " + this.dataDir + " is not found or invalid.");
        // Validate the limit parameters.
        if (this.targetSize < 1)
            throw new ParseFailureException("Invalid target size: must be positive.");
        if (this.maxLimit < 1)
            throw new ParseFailureException("Invalid intermediate-set limit: must be positive.");
        if (this.maxOutput < 1)
            throw new ParseFailureException("Invalid output limit: must be positive.");
        if (this.numChoices < 2)
            throw new ParseFailureException("Invalid number of choices: must be at least 2.");
        // Set the number of choices in the choice query class.
        ChoiceProposalQuery.setNumResponses(this.numChoices);
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
        // Create the output report writer.
        log.info("Initializing report type {}.", this.reportType);
        this.reporter = this.reportType.create(this);
        // Initialize the failed-template list.
        this.failedTemplates = new ArrayList<>();
    }

    @Override
    protected void runPipeline(LineReader inputStream, PrintWriter writer) throws Exception {
        // Load the database.
        this.loadDatabase();
        // Start the output report.
        this.reporter.open(writer);
        // Count the number of queries written.
        int totalCount = 0;
        // Loop through the input file, reading query specifications.
        Iterator<String> inputIter = new LineReaderCommentSkipper(inputStream);
        while (inputIter.hasNext()) {
            // Get the three query lines.
            String qString = inputIter.next();
            String pathString = this.safeGet(inputIter);
            String resultString = this.safeGet(inputIter);
            // Create the query proposal.
            ProposalQuery proposal;
            if (StringUtils.compareIgnoreCase(resultString, "count") == 0)
                proposal = new CountProposalQuery(qString, pathString, this.maxLimit);
            else if (resultString.startsWith("choice"))
                proposal = new ChoiceProposalQuery(qString, pathString, this.maxLimit, resultString, this.db);
            else if (resultString.startsWith("group"))
                proposal = new GroupProposalQuery(qString, pathString, 0, resultString);
            else
                proposal = new ListProposalQuery(qString, pathString, this.maxLimit, resultString);
            log.info("Computing responses for query: {}", qString);
            List<ProposalResponseSet> responses = proposal.computeSets(this.db);
            log.info("{} response sets found.", responses.size());
            // Loop through the response sets, removing bad sets.
            int skipCount = 0;
            int outCount = 0;
            Iterator<ProposalResponseSet> iter = responses.iterator();
            while (iter.hasNext()) {
                ProposalResponseSet response = iter.next();
                if (proposal.getResponseSize(response) <= this.targetSize)
                    outCount++;
                else {
                    iter.remove();
                    skipCount++;
                }
            }
            // Shuffle the response sets. We'll only output some of them, but we want to randomly select the ones
            // chosen. Some of the response sets may end up being rejected even now, so we shuffle the entire set
            // and write them in order until we reach our output limit.
            Collections.shuffle(responses);
            // Finally, write the responses.
            int writeCount = 0;
            Iterator<ProposalResponseSet> outIter = responses.iterator();
            while (writeCount < this.maxOutput && outIter.hasNext()) {
                ProposalResponseSet response = outIter.next();
                writeCount += proposal.writeResponse(response, this.reporter, responses);
            }
            log.info("{} responses kept, {} skipped, {} written.", outCount, skipCount, writeCount);
            totalCount += writeCount;
            // If we wrote nothing, remember this template as a failure.
            if (writeCount == 0)
                this.failedTemplates.add(qString);
            // Flush the output.
            this.reporter.flush();
        }
        // Log any failed templates.
        if (! this.failedTemplates.isEmpty()) {
            log.warn("{} templates failed to generate any output.", this.failedTemplates.size());
            for (String template : this.failedTemplates)
                log.warn("    {}", template);
        }
        log.info("{} total questions generated.", totalCount);
        // Insure our output is complete.
        this.reporter.close();
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

    @Override
    public JsonObject getConstantJson() {
        JsonObject retVal = new JsonObject();
        if (! this.domains.isEmpty()) {
            // Here we have one or more domains to include.
            JsonArray domainList = new JsonArray();
            domainList.addAll(this.domains);
            retVal.put("domains", domainList);
        }
        if (! this.support.isBlank()) {
            // Here we have a support organization to include.
            retVal.put("support", this.support);
        }
        return retVal;
    }


}
