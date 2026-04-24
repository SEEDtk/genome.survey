package org.theseed.genome.survey;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Strings;
import org.kohsuke.args4j.Option;
import org.theseed.basic.ICommand;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;
import org.theseed.p3.query.QueryGetProcessor;
import org.theseed.p3.query.QueryPipeProcessor;
import org.theseed.utils.BasePipeProcessor;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This sub-command will read a flat file of questions and queries to produce a JSON report containing each question along with the
 * results from the queries. This JSON report can then be used to test the data MCP server/ML model.
 * 
 * The standard input is a tab-delimited file with columns "question" and "query". The standard output will contain the JSON report.
 * 
 * Each query in the input file is effectively the command-line parameter list for a p3.query command. The command can either be a "get"
 * or a "pipe". There is no provision for "list". 
 * 
 * The command-line options are as follows:
 * 
 * -h   display command-line usage
 * -v   display more frequent log messages
 * -i   input file containing the questions and queries (if not STDIN)
 * -o   output file for the JSON report (if not STDOUT)
 * 
 * --badFile    if specified, the name of a file to write the queries that produced no results. If not specified, these will be written to the log.
 * 
 */
public class QuestionAnalysisProcessor extends BasePipeProcessor {

    // COMMAND-LINE OPTIONS

    /** name of file to write queries with no results */
    @Option(name = "--badFile", metaVar = "badQuestions.tbl", usage = "file to write queries with no results")
    private File badFile;

    // FIELDS
    /** logging facility */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuestionAnalysisProcessor.class);
    /** pipe query processor */
    private QueryPipeProcessor pipeQueryProcessor;
    /** get query processor */
    private QueryGetProcessor getQueryProcessor;
    /** constant parameters for all queries to redirect output */
    private List<String> baseParams;
    /** name of the temporary file to use for query output */
    private File tempFileName;
    /** index of the question column in the input file */
    private int questionIndex;
    /** index of the query column in the input file */
    private int queryIndex;
    /** count of queries without results */
    private int noResultCount;
    /** output stream for bad queries */
    private PrintWriter badStream;

    @Override
    protected void setPipeDefaults() {
        this.badFile = null;
    }

    @Override
    protected void validatePipeInput(TabbedLineReader inputStream) throws IOException {
        // Locate the two key input columns.
        this.questionIndex = inputStream.findField("question");
        this.queryIndex = inputStream.findField("query");
    }

    @Override
    protected void validatePipeParms() throws IOException, ParseFailureException {
        if (this.badFile != null) {
            this.badStream = new PrintWriter(this.badFile);
            log.info("Bad queries will be written to {}.", this.badFile);
            this.badStream.println("question\tquery");
        } else
            this.badStream = null;
    }

    @Override
    protected void runPipeline(TabbedLineReader inputStream, PrintWriter writer) throws Exception {
        this.tempFileName = File.createTempFile("query_output", ".json");
        try {
            // Build the constant parameters.
            this.baseParams = new ArrayList<>(2);
            this.baseParams.add("-o");
            this.baseParams.add(this.tempFileName.getAbsolutePath());
            log.info("Temporary file for query output is {}.", this.tempFileName);
            // Create the query command processors.
            this.pipeQueryProcessor = new QueryPipeProcessor();
            this.getQueryProcessor = new QueryGetProcessor();
            // Now we need to loop through the input questions and run the queries.
            // We want to output the JSON objects produced from our analysis as we go. This means
            // we must first prime the JSON output by writing the opening bracket.
            writer.println("[");
            // Get some counters.
            int questionsOut = 0;
            int questionsIn = 0;
            int recordsOut = 0;
            this.noResultCount = 0;
            // Loop through the input lines.
            for (var line : inputStream) {
                String question = line.get(this.questionIndex);
                String query = line.get(this.queryIndex);
                questionsIn++;
                log.info("Processing question {}: {}", questionsIn, question);
                // Process the query to get the possible results.
                ICommand processor = determineProcessor(query);
                List<String> parms = parseQueryLine(query);
                JsonArray results = runQuery(processor, parms);
                recordsOut += results.size();
                if (results.isEmpty()) {
                    this.noResultCount++;
                    log.warn("No results found for query {}: {}", questionsIn, query);
                    if (this.badStream != null)
                        this.badStream.println(question + "\t" + query);
                }
                // Form a JSON object describing the question and its results.
                JsonObject jsonObject = new JsonObject();
                jsonObject.put("question", question);
                jsonObject.put("query", query);
                jsonObject.put("results", results);
                jsonObject.put("status", "untested");
                jsonObject.put("comment", "");
                // Write the JSON object to the output.
                if (questionsOut > 0)
                    writer.println(",");
                writer.print(jsonObject.toJson());
                questionsOut++;
            }
            writer.println();
            writer.println("]");
            log.info("Output {} questions with {} total records written.", questionsOut, recordsOut);
            if (this.noResultCount > 0)
                log.warn("No results found for {} queries.", this.noResultCount);
        } finally {
            log.info("Deleting temporary file {}.", this.tempFileName);
            FileUtils.forceDelete(this.tempFileName);
            if (this.badStream != null) {
                this.badStream.flush();
                this.badStream.close();
            }
        }
    }

    /**
     * This method will determine the correct processor to call for a query line. This is computed by looking at the first 
     * word of the query line ("get" or "pipe").
     * 
     * @param queryLine  line containing the query
     * 
     * @return the processor to use for the query
     */
    private ICommand determineProcessor(String queryLine) {
        ICommand retVal;
        if (Strings.CS.startsWith(queryLine, "get "))
            retVal = this.getQueryProcessor;
        else if (Strings.CS.startsWith(queryLine, "pipe "))
            retVal = this.pipeQueryProcessor;
        else
            throw new IllegalArgumentException("Invalid query line: " + queryLine);
        return retVal;
    }

    /**
     * This method will parse the query line (discarding the command word) and output a list of command-line options
     * to pass to the appropriate processor.
     * 
     * We start by skipping over the command word to the first space. We know one exists, or we would never have gotten this far.
     * We then parse out each substring one character at a time. We allow queoted strings so that spaces can be embedded. A backslash
     * can be resolved to an internal quote or another backslash. At this time we don't allow control characters or other exotics.
     * 
     * @param queryLine  line containing the query
     * 
     * @return a list of parameter strings to pass to the command processor
     */
    static protected List<String> parseQueryLine(String queryLine) {
        List<String> retVal = new ArrayList<>();
        // This enum represents the state of the parser.
        enum ParseState { NORMAL, IN_QUOTE, IN_SPACES }
        ParseState state = ParseState.IN_SPACES;
        // We will store the current parameter here.
        StringBuilder current = new StringBuilder();
        // Loop through the characters in the query line.
        final int n = queryLine.length();
        for (int i = queryLine.indexOf(' '); i < n; i++) {
            switch (state) {

                case IN_SPACES -> {
                    switch (queryLine.charAt(i)) {
                        case ' ' -> {
                            // More spaces, so keep skipping.
                            }
                        case '"' -> {
                            // Here we have a quoted parameter, so switch to the IN_QUOTE state.
                            state = ParseState.IN_QUOTE;
                        }
                        case '\\' -> {
                            // Skip past the backslash and treat the next character as a literal.
                            // We also are starting a normal parameter, so switch to the NORMAL state.
                            i++;
                            state = ParseState.NORMAL;
                            current.append(queryLine.charAt(i));
                        }
                        default -> {
                            // Here we have a normal parameter, so switch to the NORMAL state.
                            current.append(queryLine.charAt(i));
                            state = ParseState.NORMAL;
                        }
                    }
                }

                case NORMAL -> {
                    switch (queryLine.charAt(i)) {
                        case ' ' -> {
                            retVal.add(current.toString());
                            current.setLength(0);
                            state = ParseState.IN_SPACES;
                        }
                        case '\\' -> {
                            // Skip past the backslash and treat the next character as a literal.
                            i++;
                            current.append(queryLine.charAt(i));
                        }
                        default -> current.append(queryLine.charAt(i));
                    }
                }

                case IN_QUOTE -> {
                    switch (queryLine.charAt(i)) {
                        case '"' -> {
                            // Here we have reached the end of a quoted parameter, so switch to the NORMAL state.
                            // If the next character is a space (which it should be), we will store the parameter
                            // and switch to IN_SPACES.
                            state = ParseState.NORMAL;
                        }
                        case '\\' -> {
                            // Skip past the backslash and treat the next character as a literal.
                            i++;
                            current.append(queryLine.charAt(i));
                        }
                        default -> current.append(queryLine.charAt(i));
                    }
                }
            }
        }
        // Add the last parameter if there is one.
        if (current.length() > 0) {
            retVal.add(current.toString());
        }
        return retVal;
    }
    
    /**
     * This method calls the appropriate processor for a query line and returns the results. The tab-delimited output is written by the
     * query processor to the temporary file. We read it back in and remove duplicates, then return the results as a JsonArray.
     * 
     * @param processor     query processor to use
     * @param parms         list of parameters to pass to the processor
     * 
     * @return the unique results of the query as a JsonArray
     * 
     * @throws IOException if an error occurs reading the temporary file
     */
    private JsonArray runQuery(ICommand processor, List<String> parms) throws IOException {
        JsonArray retVal = new JsonArray();
        // Add the constant parameters to the parameter list.
        parms.addAll(this.baseParams);
        // Pass the parameters to the processor.
        processor.parseCommand(parms.toArray(String[]::new));
        // Run the processor. This will write the output to the temporary file.
        processor.run();
        // Now we need to read the output back in and remove duplicates. We will use a set for this.
        Set<List<String>> results = new HashSet<>();
        try (TabbedLineReader reader = new TabbedLineReader(this.tempFileName)) {
            // First, we get the field names, which are also the column headers.
            String[] fields = reader.getLabels();
            // Loop through the output lines and add the each line to the set.
            int linesIn = 0;
            for (var line : reader) {
                results.add(Arrays.asList(line.getFields()));            
                linesIn++;
            }
            log.info("{} lines read from query output, {} unique.", linesIn, results.size());
            if (linesIn <= 0)
                log.warn("No results found for query.");
            // Now we need to convert the unique results to JSON objects. We will use the field names 
            // as the keys and the field values as the values.
            for (List<String> result : results) {
                JsonObject obj = new JsonObject();
                for (int i = 0; i < fields.length; i++) {
                    obj.put(fields[i], result.get(i));
                }
                retVal.add(obj);
            }
        }
        return retVal;
    }

}