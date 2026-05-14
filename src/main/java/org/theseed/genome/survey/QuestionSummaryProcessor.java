package org.theseed.genome.survey;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kohsuke.args4j.Argument;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;
import org.theseed.json.JsonListIterator;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This command will read a JSON file containing the results of testing a large language model on a set of questions and 
 * produce a summary report. For each of several key values, the report will contain counts for the different values of
 * that key. This is a very simple task, but it provides a useful summary when the number of questions is large.
 * 
 * The positional parameters are the name of the input file followed by the names of the keys to tally. The report will
 * be written to the standard output. The input file is expected to be a JSON list of objects, each of which contains the
 * specified keys.
 * 
 * The command-line options are as follows.
 * 
 * -h   display command-line usage
 * -v   display more frequent log messages
 * -o   name of output file (if not STDOUT)
 * 
 */
public class QuestionSummaryProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuestionSummaryProcessor.class);
    /** map of input key names to value count maps */
    private Map<String, CountMap<String>> keyMaps;

    // COMMAND-LINE OPTIONS

    /** input JSON file containing the question results */
    @Argument(index = 0, metaVar = "input.json", usage = "input JSON file containing the question results", required = true)
    private File jsonFile;

    /** list of keys to tally */
    @Argument(index = 1, metaVar = "key1 key2 ...", usage = "list of keys to tally")
    private List<String> keys;

    @Override
    protected void setReporterDefaults() {
        this.keys = new ArrayList<>();
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        if (! this.jsonFile.canRead())
            throw new IOException("Input file " + this.jsonFile + " is not found or unreadable.");
        if (this.keys.isEmpty())
            throw new ParseFailureException("No keys specified for tallying.");
        // Create the count maps.
        this.keyMaps = this.keys.stream().collect(Collectors.toMap(k -> k, k -> new CountMap<String>()));
        log.info("Processing {} questions for keys {}.", this.jsonFile, String.join(", ", this.keys));
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // Set up a counter.
        int inCount = 0;
        // Create the JSON list iterator.
        try (JsonListIterator iter = new JsonListIterator(this.jsonFile)) {
            // Loop through the records.
            while (iter.hasNext()) {
                inCount++;
                JsonObject record = iter.next();
                // Loop through the keys.
                for (String key : this.keys) {
                    // Get the value for this key.
                    Object value = record.get(key);
                    String stringValue;
                    if (value == null)
                        stringValue = "(null)";
                    else if (value instanceof String string)
                        stringValue = string;
                    else
                        stringValue = value.toString();
                    // Update the count map for this key.
                    this.keyMaps.get(key).count(stringValue);
                }
            }
            log.info("Processed {} questions.", inCount);
        }
        // Write the report.
        for (String key : this.keys) {
            writer.println("Key: " + key);
            CountMap<String> counts = this.keyMaps.get(key);
            for (var entry : counts.sortedCounts())
                writer.println(String.format("    %-20s\t%6d", entry.getKey(), entry.getCount()));
            writer.println();
        }
    }


}
