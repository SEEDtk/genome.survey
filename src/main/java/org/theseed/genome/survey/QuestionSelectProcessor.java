package org.theseed.genome.survey;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This sub-command reads one or more JSON question files and outputs one randomly-chosen question-answer pair for each template found. Note that
 * the templates are only gathered on a file-by-file basis, so if you have multiple files with the same template, you will get one question from 
 * each file's use of the template, not one question total for the template.
 * 
 * The positional parameters are the names of the question files to read. Each file must be a JSON array of question objects. The question objects
 * have a "template" field that contains the template text, a "question" field that contains the question text, and a "correct_answer" field that
 * contains the correct answer. All other fields are ignored.
 * 
 * The output is a tab-delimite file with two columns: the question text and the correct answer. The first line is a header line.
 * 
 * The command-line options are as follows.
 * 
 * -h   display command-line usage
 * -v   display more detailed log messages
 * -o   output file for report (if not STDOUT)
 * -n   number of questions to output per template (default 1)
 */

public class QuestionSelectProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuestionSelectProcessor.class);
    /** map of template strings to possible output lines for the current file */
    private Map<String, List<String>> templateMap;
    /** random number generator */
    private java.util.Random random;
    /** default initial capacity for template map arrays */
    private static final int DEFAULT_TEMPLATE_LINE_LIST_SIZE = 100;

    // COMMAND-LINE OPTIONS

    /** number of questions to output per template */
    @Option(name = "--num", aliases = { "-n" }, metaVar = "5", usage = "number of questions to output per template")
    private int numPerTemplate;

    /** input files containing questions */
    @Argument(index = 0, metaVar = "questionFile", usage = "input file(s) containing questions in JSON format", required = true)
    private List<File> questionFiles;

    /**
     * List of useful JSON keys for the questions.
     */
    protected static enum QuestionKeys implements JsonKey {
        TEMPLATE("<missing>"),
        QUESTION("<missing>"),
        CORRECT_ANSWER("<missing>");

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
    protected void setReporterDefaults() {
        this.questionFiles = new ArrayList<>();
        this.random = new java.util.Random();
        this.numPerTemplate = 1;
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Verify that the input files exist and are readable.
        for (File questionFile : this.questionFiles) {
            if (!questionFile.canRead())
                throw new IOException("Question file " + questionFile + " is not found or unreadable.");
        }
        // Create the template map.
        this.templateMap = new java.util.HashMap<>(50);
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // Write the output file header line.
        writer.println("question\tcorrect_answer");
        // Create some counters for the log.
        int fileCount = 0;
        int templateCount = 0;
        int outCount = 0;
        int inCount = 0;
        // Loop through the input files.
        for (File questionFile : this.questionFiles) {
            fileCount++;
            // Read the questions from the file.
            log.info("Processing question file {}: {}.", fileCount, questionFile);
            JsonArray questions;
            try (FileReader jsonReader = new FileReader(questionFile)) {
                questions = (JsonArray) Jsoner.deserialize(jsonReader);
            }
            log.info("{} questions read from {}.", questions.size(), questionFile);
            // Loop through the questions, gathering them by template.
            for (Object questionObj : questions) {
                JsonObject question = (JsonObject) questionObj;
                inCount++;
                // Get the template for this question and form the output line.
                String template = question.getStringOrDefault(QuestionKeys.TEMPLATE);
                String questionText = question.getStringOrDefault(QuestionKeys.QUESTION) + "\t" + question.getStringOrDefault(QuestionKeys.CORRECT_ANSWER);
                // Add the output line to the template map.
                List<String> lineList = this.templateMap.computeIfAbsent(template, k -> new ArrayList<>(DEFAULT_TEMPLATE_LINE_LIST_SIZE));
                lineList.add(questionText);
            }
            log.info("{} templates found in {}.", this.templateMap.size(), questionFile);
            // Now loop through the templates, picking one question from each and writing it out.
            for (Map.Entry<String, List<String>> templateEntry : this.templateMap.entrySet()) {
                templateCount++;
                List<String> lineList = templateEntry.getValue();
                // Shuffle the desired questions to the front.
                for (int i = 0; i < this.numPerTemplate && i < lineList.size() - 1; i++) {
                    int index = this.random.nextInt(lineList.size() - i);
                    Collections.swap(lineList, i, i + index);
                }
                for (int i = 0; i < this.numPerTemplate && i < lineList.size(); i++) {
                    writer.println(lineList.get(i));
                    outCount++;
                }
            }
            // Clear the template map for the next file.
            this.templateMap.clear();
        }
        log.info("{} questions read for {} templates in {} files, {} questions written.", inCount, templateCount, fileCount, outCount);
    }

}
