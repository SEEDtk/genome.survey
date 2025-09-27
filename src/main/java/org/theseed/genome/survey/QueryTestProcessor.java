package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.LineReader;
import org.theseed.io.TabbedLineReader;
import org.theseed.memdb.query.validate.QueryValidator;
import org.theseed.p3.query.QueryListProcessor;
import org.theseed.utils.BaseMultiReportProcessor;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This command looks at the JSON output from QueryGenProcessor and makes database calls to verify that the answers are correct
 * and the distractors are not. It makes use of test instructions embedded in the question template file as comments.
 * The test instructions always beging with "#Test" and end with a comment line that is just "#". The instruction lines themselves
 * all begin with a space after the comment mark, a command name, and then parameters.
 * 
 * The first instruction line identifies the answer field, and consists of the word "answers" and then a column label to assign to
 * the answer column. This is used to build a one-column input file. The next several lines should have the "list" command, and
 * contain parameters to be passed to QueryListProcessor. Each list command takes the previous output file as input and produces a
 * new one. The final output file is then validated against relational assertions. At least one output line should have the correct
 * answer in the first column and should satisfy all the assertions. No line with a distractor in the first column should satisfy
 * all the assertions. The maximum number of assertions satisfied by a line with a given value in the first column is output, and
 * the correct answer is identified. We want the distractors to have at least one, but less than the total, all though a maximum
 * of zero for a distractor is acceptable, since it is sometimes impossible to find a distractor that is close to the answer.
 * 
 * To prevent the results of a query from being over-large, you can put filters in the parameters of a list command. If this is
 * done, you can enclose part of the filter parameter in double curly braces and put int a parameterization spec. The parameterization
 * spec consists of an entity name, a period, and the 1-based position of the desired parameter in the value list. So, for example,
 * 
 *      --eq genome_name,{{Genome.2}}
 * 
 * would say that the genome name has to match the second parameter in the parameterization list for the Genome entity.
 * 
 * The positional parameters are the name of the query specification file and the name of the JSON file to be validated. Both files will
 * be read fully into memory, so they should not be too large. This is to avoid a major pain in reading the JSON file. That said, the
 * query specification is generally under 20K.
 * 
 * Key fields in the question JSON include:
 * 
 * template         the template string used to generate the question
 * parameters       the parameters used to characterize the question
 * question         the text of the question itself
 * correct_answer   the correct answer to the question
 * distractors      a list of incorrect answers to the question
 * 
 * The output reports will include a summary of each question (summary.tbl), a list of bad questions (badq.tbl), and a version of the
 * input JSON file with all the bad questions removed (same base name as input JSON filegoodq.json).
 * 
 * The command-line options are as follows:
 * 
 * -h	display command-line usage
 * -v	display more detailed log messages
 * -D   output directory for reports (default is "QueryTest" in the current directory)
 * 
 * --temp   directory for temporary files (default "Temp" in the current directory)
 * 
 */
public class QueryTestProcessor extends BaseMultiReportProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(QueryTestProcessor.class);
    /** query processor for accessing database */
    private QueryListProcessor queryEngine;
    /** map of template strings to validators */
    private Map<String, QueryValidator> validatorMap;
    /** maximum number of query steps in a test specification */
    private int maxQueries;
    /** set of templates without test specifications */
    private Set<String> badTemplates;
    /** pattern match for parameter substitution */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{\\{([^\\.]+)\\.([0-9]+)\\}\\}");
    
    // COMMAND-LINE OPTIONS

    /** temporary directory name */
    @Option(name = "--temp", metaVar = "Temp", usage = "name of a directory to hold temporary files")
    private File tempDir;

    /** query specification file */
    @Argument(index = 0, metaVar = "querySpecFile", usage = "query specification file", required = true)
    private File querySpecFile;

    /** JSON file to validate */
    @Argument(index = 1, metaVar = "jsonFile", usage = "query JSON file to validate", required = true)
    private File jsonFile;

    /**
     * List of useful JSON keys for the questions.
     */
    protected static enum QuestionKeys implements JsonKey {
        TEMPLATE("<missing>"),
        PARAMETERS(new JsonObject()),
        QUESTION("<missing>"),
        CORRECT_ANSWER("<missing>"),
        DISTRACTORS(new JsonArray());

        private final Object m_value;

        QuestionKeys(final Object value) {
            this.m_value = value;
        }

        /**
         * This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /**
         * This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    };


    @Override
    protected File setDefaultOutputDir(File curDir) {
        return new File(curDir, "QueryTest");
    }

    @Override
    protected void setMultiReportDefaults() {
        // Set up the default temporary directory.
        this.tempDir = new File(System.getProperty("user.dir"), "Temp");
    }

    @Override
    protected void validateMultiReportParms() throws IOException, ParseFailureException {
        // Insure the query spec file exists.
        if (! this.querySpecFile.canRead())
            throw new FileNotFoundException("Query specification file " + this.querySpecFile + " is not found or unreadable.");
        // Insure the JSON file exists.
        if (! this.jsonFile.canRead())
            throw new FileNotFoundException("JSON question file " + this.jsonFile + " is not found or unreadable.");
        // Insure we have a temporary file directory.
        if (! this.tempDir.exists()) {
            log.info("Creating temporary directory {}.", this.tempDir);
            FileUtils.forceMkdir(this.tempDir);
        } else if (! this.tempDir.isDirectory())
            throw new FileNotFoundException("Invalid temporary directory name " + this.tempDir);
        else
            log.info("Temporary files will be stored in directory {}.", this.tempDir);
        // Create the query engine.
        this.queryEngine = new QueryListProcessor();
    }

    @Override
    protected void runMultiReports() throws Exception {
        // Our first job is to read in the query specification file. This is used to build the validator map and
        // the bad-template list.
        this.validatorMap = new HashMap<>();
        this.badTemplates = new HashSet<>();
        log.info("Reading query specification file {}.", this.querySpecFile);
        this.maxQueries = 0;
        try (LineReader specStream = new LineReader(this.querySpecFile)) {
            // Insure the file is nonempty.
            if (! specStream.hasNext())
                throw new IOException("Query specification file is empty.");
            // We prime by reading the first line. Each time through the loop we process
            // the line just read.
            String line = specStream.next();
            while (line != null) {
                // Here we are expecting a template string. If we have a comment, we skip over it.
                if (line.startsWith("#"))
                    line = this.safeNext(specStream);
                else {
                    // Save the template string.
                    String template = line;
                    // Now we skip the next two lines. We will get an no-such-element error if we hit end-of-file.
                    specStream.next();
                    specStream.next();
                    // Now we read the next line, which may be null if we are at end-of-file, but it could be the
                    // next question spec or the beginning of a test specification.
                    line = this.safeNext(specStream);
                    if (line != null) {
                        // Check for a test specification.
                        if (! line.startsWith("#Test"))
                            this.badTemplates.add(template);
                        else {
                            // Here we can test this template. Create a validator and put it in the map.
                            QueryValidator validator = new QueryValidator(template);
                            // We now loop through the test specifications. There will be aa series of list lines and a series
                            // of assertion lines. At the end will be a blank comment. Note end-of-file is also a possibility.
                            line = specStream.next();
                            while (! line.equals("#")) {
                                if (line.startsWith("# list ")) {
                                    // Here we have a list command. We parse the parameters and add them to the validator.
                                    String[] args = line.substring(7).split(" ");
                                    if (args.length < 1)
                                        throw new IOException("Invalid list command in query specification file: " + line);
                                    validator.addQueryCommand(List.of(args));
                                } else {
                                    // Here we have an assertion line. We parse it and add it to the validator. It
                                    // has three parts: the relational operator, the column name, and the field specification.
                                    String[] parts = line.substring(2).split("\\s+");
                                    if (parts.length != 3)
                                        throw new IOException("Invalid assertion in query specification file: " + line);
                                    validator.addAssertion(parts[0], parts[1], parts[2]);
                                }
                                // Get the next line.
                                line = specStream.next();
                            }
                            // Here we have finished reading the test specification. We save the validator.
                            this.validatorMap.put(template, validator);
                            // Update the maximum number of queries.
                            int qCount = validator.getQueryCount();
                            if (qCount > this.maxQueries)
                                this.maxQueries = qCount;
                            // Get the next line.
                            line = this.safeNext(specStream);
                        }
                    }
                }
            }
            log.info("{} query specifications can be validated. {} have no test specification.", this.validatorMap.size(),
                    this.badTemplates.size());
            log.info("Maximum of {} queries per test.", this.maxQueries);
        } catch (NoSuchElementException e) {
            throw new IOException("Premature end-of-file found in query specification file.");
        }
        // Now that we know the maximum number of queries, we can create the temporary file names.
        // The first file serves as the inital input, and contains the column of answers.
        File[] tempFiles = new File[this.maxQueries + 1];
        for (int i = 0; i <= this.maxQueries; i++)
            tempFiles[i] = File.createTempFile(String.format("qtest%02d", i), ".tbl", this.tempDir);
        // Open the output files.
        try (PrintWriter writer = this.openReport("summary.tbl");
            PrintWriter badQWriter = this.openReport("badq.tbl")){
            // Now we load the JSON object and process each question against the appropriate validator.
            JsonArray questions;
            log.info("Reading question file {}.", this.jsonFile);
            try (FileReader jsonReader = new FileReader(this.jsonFile)) {
                questions = (JsonArray) Jsoner.deserialize(jsonReader);
            }
            // Set up our counters.
            int badCounter = 0;
            int goodCounter = 0;
            int qCounter = 0;
            int skipCounter = 0;
            log.info("{} questions found in file.", questions.size());
            // Initialize the output file.
            writer.println("question\tstatus\tassertions\tanswers_good\tdistractors_good\tbest_bad\ttemplate");
            // Prepare to iterate through the questions.
            Iterator<Object> iter = questions.iterator();
            while (iter.hasNext()) {
                JsonObject question = (JsonObject) iter.next();
                qCounter++;
                // Get the template string for this question.
                String template = question.getString(QuestionKeys.TEMPLATE);
                // Find the appropriate validator.
                QueryValidator validator = this.validatorMap.get(template);
                if (validator == null) {
                    skipCounter++;
                    if (! this.badTemplates.contains(template)) {
                        log.warn("No query definition found for question template \"{}\".", template);
                        // Make sure we don't get another warning for the same template.
                        this.badTemplates.add(template);
                    }
                } else {
                    // Get the correct answer and then the distractors into the first temp file.
                    String answerString = this.buildAnswerFile(tempFiles[0], question);
                    // Get the parameterization for this question. We'll need it to do parameter substitution.
                    JsonObject parameterizations = question.getMapOrDefault(QuestionKeys.PARAMETERS);
                    // Now we run through the queries. This builds us our final output file.
                    Iterator<List<String>> parmIter = validator.getParmIterator();
                    int i = 1;
                    while (parmIter.hasNext()) {
                        // Now we build the output parameter list.
                        List<String> args = new ArrayList<>();
                        // Process any substitions.
                        List<String> rawParms = parmIter.next();
                        this.copyParameters(rawParms, parameterizations, args);
                        // Add the input and output file names.
                        args.add("-i");
                        args.add(tempFiles[i-1].getAbsolutePath());
                        args.add("-o");
                        args.add(tempFiles[i].getAbsolutePath());
                        this.queryEngine.parseCommandLine(args.toArray(String[]::new));
                        this.queryEngine.run();
                        i++;
                    }
                    // We need the question string, the parameterization, and the number of assertions.
                    String qString = question.getStringOrDefault(QuestionKeys.QUESTION);
                    int assertCount = validator.getAssertionCount();
                    // The output file to use as input to the validation is now in tempFiles[i-1].
                    try (TabbedLineReader outFileinStream = new TabbedLineReader(tempFiles[i-1])) {
                        // We need to count the number of distractors with a full match, the number of answers
                        // with a full match, and the maximum number of matches for a distractor.
                        int answersOk = 0;
                        int distractorsOk = 0;
                        int maxDistractorMatch = 0;
                        // Fix the validator's column indices.
                        validator.fix(outFileinStream);
                        // Now we process each line of the output file.
                        while (outFileinStream.hasNext()) {
                            TabbedLineReader.Line line = outFileinStream.next();
                            // Find out if this is an answer or a distractor.
                            boolean isAnswer = line.get(0).equals(answerString);
                            // Validate this line.
                            int matchCount = validator.checkLine(line, parameterizations);
                            if (isAnswer) {
                                // Here we have an answer line.
                                if (matchCount == assertCount)
                                    answersOk++;
                            } else {
                                // Here we have a distractor. We need to count a full match, and we need
                                // to update the maximum match if it's not full.
                                if (matchCount == assertCount) {
                                    distractorsOk++;
                                    badQWriter.println(qString + "\t" + line.toString());
                                }
                                else if (maxDistractorMatch < matchCount)
                                    maxDistractorMatch = matchCount;
                            }
                        }
                        // Write the results for this question.
                        String status;
                        if (answersOk > 0 && distractorsOk <= 0) {
                            status = "ok";
                            goodCounter++;
                        } else {
                            status = "INVALID";
                            badCounter++;
                            // Remove this question from the JSON array.
                            iter.remove();
                        }
                        writer.println(qString + "\t" + status + "\t" + assertCount + "\t" + answersOk + "\t" + distractorsOk
                            + "\t" + maxDistractorMatch + "\t" + template);
                        writer.flush();
                        badQWriter.flush();
                    }
                }
            }
            log.info("Processed {} total questions. {} bad, {} good, {} skipped.", qCounter, badCounter, goodCounter, skipCounter);
            // Now write out the remaining good questions.
            if (goodCounter <= 0) {
                log.warn("No good questions found. No good question file created.");
            } else {
                File goodFile = this.getOutFile(this.jsonFile.getName());
                try (PrintWriter goodQWriter = new PrintWriter(goodFile)) {
                    Jsoner.serialize(questions, goodQWriter);
                }
                log.info("{} good questions written to {}.", goodCounter, goodFile);
            }
        } finally {
            // Here we delete the temporary files.
            for (File tempFile : tempFiles) {
                if (tempFile.exists() && ! tempFile.delete())
                    log.warn("Could not delete temporary file {}.", tempFile);
            }
        }
    }

    /**
     * Process a parameter list, doing any necessary substitutions, and copy the results to the output list.
     * 
     * @param rawParms          list of raw parameters, possibly requiring substitutions
     * @param parameterizations parameterization object for this question
     * @param args              output parameter list
     */
    private void copyParameters(List<String> rawParms, JsonObject parameterizations, List<String> args) {
        StringBuilder buf = new StringBuilder();
        for (String raw : rawParms) {
            // Clear the output buffer.
            buf.setLength(0);
            // Loop through the parameter, looking for substitution specifications.
            Matcher matcher = PARAM_PATTERN.matcher(raw);
            while (matcher.find()) {
                String entityName = matcher.group(1);
                String paramIndex = matcher.group(2);
                // Use the entity name and index to get the replacement string.
                JsonArray parmArray = (JsonArray) parameterizations.get(entityName);
                String replacement = null;
                if (parmArray != null) {
                    int index = Integer.parseInt(paramIndex) - 1;
                    if (index >= 0 && index < parmArray.size())
                        replacement = (String) parmArray.get(index);
                }
                // Abort if the specification is invalid. Otherwise, put it in the output buffer.
                if (replacement == null)
                    throw new IllegalArgumentException(String.format("Invalid parameterization %s.%s in parameter %s.",
                            entityName, paramIndex, raw));
                else {
                    // This appends the portion of the string before the match, followed by the replacement for the match.
                    matcher.appendReplacement(buf, replacement);
                }
            }
            // Append the unmatched portion of the string.
            matcher.appendTail(buf);
            // Save the completed parameter.
            args.add(buf.toString());
        }
    }

    /**
     * Create a temporary file containing the answer followed by the distractors for a particular question.
     * 
     * @param outFile   temporary file to fill
     * @param question  question containing answers and distractor
     * 
     * @return the correct answer
     * 
     * @throws IOException 
     */
    private String buildAnswerFile(File outFile, JsonObject question) throws IOException {
        String retVal;
        try (PrintWriter outWriter = new PrintWriter(outFile)) {
            // We need a column header.
            outWriter.println("answers");
            // Write out the correct answer first.
            retVal = question.getString(QuestionKeys.CORRECT_ANSWER);
            outWriter.println(retVal);
            // Now write the distractors.
            JsonArray distractors = question.getCollectionOrDefault(QuestionKeys.DISTRACTORS);
            for (Object distractorObj : distractors) {
                String distractor = (String) distractorObj;
                outWriter.println(distractor);
            }
        }
        return retVal;
    }

    /**
     * Get the next line, returning NULL if we are at end-of-file.
     * 
     * @param specStream    input line reader
     * 
     * @return the next line, or NULL if there is none
     */
    private String safeNext(LineReader specStream) {
        String retVal;
        if (! specStream.hasNext())
            retVal = null;
        else
            retVal = specStream.next();
        return retVal;
    }

}
