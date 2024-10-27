/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;
import org.theseed.io.MasterGenomeDir;
import org.theseed.reports.FieldCounter;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This scans a genome dump directory and reports on the contents of the files. For each file name found, we
 * count the number of occurrences of each field, the number of occurrences of the files themselves, and the
 * number of records. We also indicate how many field values are lists, strings, and integers, booleans,
 * and floating-point numbers.
 *
 * The positional parameter is the name of the genome JSON dump directory.  The report will be produced on
 * the standard output.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messags
 * -o	output file for report (if not STDOUT)
 *
 * @author Bruce Parrello
 *
 */
public class JsonScanProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(JsonScanProcessor.class);
    /** master genome directory */
    private MasterGenomeDir genomeDirs;
    /** map of file names to field names to field counters */
    private Map<String, Map<String, FieldCounter>> countMap;
    /** file occurrence counters */
    private CountMap<String> fileCounts;
    /** record counters */
    private CountMap<String> recordCounts;
    /** JSON file filter */
    private FileFilter JSON_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            boolean retVal = pathname.isFile();
            if (retVal) {
                String fileName = pathname.getName();
                retVal = fileName.endsWith(".json");
            }
            return retVal;
        }
    };

    /** genome master directory */
    @Argument(index = 0, metaVar = "genomeDumpDir", usage = "master directory containing JSON genome dumps")
    private File genomeDirsIn;

    @Override
    protected void setReporterDefaults() {
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Get the genome master directory.
        if (! this.genomeDirsIn.isDirectory())
            throw new FileNotFoundException("Master genome dump directory " + this.genomeDirsIn + " is not found or invalid.");
        this.genomeDirs = new MasterGenomeDir(this.genomeDirsIn);
        log.info("{} genomes found in {}.", this.genomeDirs.size(), this.genomeDirsIn);
        // Initialize the maps.
        this.countMap = new TreeMap<String, Map<String, FieldCounter>>();
        this.fileCounts = new CountMap<String>();
        this.recordCounts = new CountMap<String>();
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // Loop through the genomes.
        int gCount = 0;
        final int gTotal = this.genomeDirs.size();
        for (File genomeDir : this.genomeDirs) {
            gCount++;
            String genomeId = genomeDir.getName();
            log.info("Processing genome {} of {}: {}.", gCount, gTotal, genomeId);
            for (File subFile : genomeDir.listFiles(JSON_FILE_FILTER)) {
                // Get a reader for this JSON file.
                JsonArray subJson;
                try (FileReader reader = new FileReader(subFile)) {
                    subJson = (JsonArray) Jsoner.deserialize(reader);
                }
                // If we have records, count this file.
                if (! subJson.isEmpty()) {
                    String name = subFile.getName();
                    Map<String, FieldCounter> fileMap = this.countMap.computeIfAbsent(name, x -> new TreeMap<String, FieldCounter>());
                    this.fileCounts.count(name);
                    // Loop through the records, counting the fields.
                    for (var recordObj : subJson) {
                        this.recordCounts.count(name);
                        // Now loop through the fields.
                        JsonObject record = (JsonObject) recordObj;
                        for (var fieldEntry : record.entrySet()) {
                            String fieldName = fieldEntry.getKey();
                            FieldCounter counter = fileMap.computeIfAbsent(fieldName, x -> new FieldCounter());
                            counter.count(fieldEntry.getValue());
                        }
                    }
                }
            }
        }
        // Now we produce the output.
        log.info("Writing report on {} files.", this.fileCounts.size());
        for (var fileEntry : this.countMap.entrySet()) {
            // Create the heading for this file.
            String fileName = fileEntry.getKey();
            String fileHeading = "FILE " + fileName + ": " + this.fileCounts.getCount(fileName) + " instances, "
                    + this.recordCounts.getCount(fileName) + " records.";
            writer.println(fileHeading);
            writer.println(StringUtils.repeat("-", fileHeading.length()));
            writer.println(StringUtils.rightPad("name", 20) + " " + FieldCounter.header());
            // Write the field data.
            Map<String, FieldCounter> fieldMap = fileEntry.getValue();
            for (var fieldEntry : fieldMap.entrySet())
                writer.println(StringUtils.rightPad(fieldEntry.getKey(), 20) + " " + fieldEntry.getValue().getResults());
            // Write the spacer at the end.
            writer.println();
        }
    }

}
