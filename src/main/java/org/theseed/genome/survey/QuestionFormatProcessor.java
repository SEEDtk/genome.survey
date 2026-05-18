package org.theseed.genome.survey;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.kohsuke.args4j.Argument;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.json.JsonListIterator;

import com.github.cliftonlabs.json_simple.JsonKey;

/**
 * This command looks at a question response file produced by the LLM testing facility and extracts the responses in a more readable format.
 * Each section of the output is headed by the question index and text, followed by the response text with escapes converted back to normal
 * characters. This is followed by three dashes and a blank line. This makes the result more readable, and the file can be scrolled in parallel
 * with the original JSON while evaluating the responses.
 * 
 * The single positional parameter is the name of the input JSON file. The output is written to standard output.
 * 
 * The input file is expected to be a JSON array of objects, each of which has the following fields:
 * 
 * index        the question index
 * question     the question text
 * response     the response text, with escapes
 * 
 * The command-line options are as follows.
 * 
 * -h   display command-line usage
 * -v   display more frequent log messages
 * -o   name of output file (if not STDOUT)
 * 
 */
public class QuestionFormatProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuestionFormatProcessor.class);

    // COMMAND-LINE OPTIONS

    /** input JSON file containing the question results */
    @Argument(index = 0, metaVar = "input.json", usage = "input JSON file containing the question results", required = true)
    private File jsonFile;

    /**
     * This enum defines the keys used and their default values.
     */
    public static enum SpecialKeys implements JsonKey {
        INDEX(0),
        QUESTION("Unknown question"),
        RESPONSE_TEXT("No response");

        private final Object m_value;

        SpecialKeys(final Object value) {
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

    }

    @Override
    protected void setReporterDefaults() {
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        if (! this.jsonFile.canRead())
            throw new IOException("Input file " + this.jsonFile + " is not found or unreadable.");
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        log.info("Opening input file {}.", this.jsonFile);
        try (JsonListIterator iter = new JsonListIterator(this.jsonFile)) {
            int count = 0;
            while (iter.hasNext()) {
                var item = iter.next();
                int index = item.getIntegerOrDefault(SpecialKeys.INDEX);
                String question = item.getStringOrDefault(SpecialKeys.QUESTION);
                String response = item.getStringOrDefault(SpecialKeys.RESPONSE_TEXT);
                response = response.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                writer.println("Question " + index + ": " + question);
                writer.println(response);
                writer.println("---\n");
                count++;
            }
            log.info("Processed {} questions.", count);
        }
    }

}
