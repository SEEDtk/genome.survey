/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;
import org.theseed.io.MasterGenomeDir;
import org.theseed.json.JsonFileDir;
import org.theseed.json.JsonListIterator;
import org.theseed.reports.BaseJsonScanReporter;
import org.theseed.reports.FieldCounter;

import com.github.cliftonlabs.json_simple.JsonObject;

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
 * --format     output report format (default TEXT)
 * --dbd        DBD source file to use for file and relationship information in the DBD report
 *
 * @author Bruce Parrello
 *
 */
public class JsonScanProcessor extends BaseReportProcessor implements  BaseJsonScanReporter.IParms {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(JsonScanProcessor.class);
    /** master genome directory */
    private MasterGenomeDir genomeDirs;
    /** map of file names to field names to field counters */
    private Map<String, Map<String, FieldCounter>> countMap;
    /** file occurrence counters */
    private CountMap<String> fileCounts;
    /** record counters */
    private CountMap<String> recordCounts;
    /** output report writer */
    private BaseJsonScanReporter jscanWriter;

    // COMMAND-LINE OPTIONS

    /** output report format */
    @Option(name = "--format", usage = "output report format (default TEXT)", metaVar = "format")
    private BaseJsonScanReporter.Type reportType;

    /** DBD source file */
    @Option(name = "--dbd", usage = "DBD source file to use for file and relationship information in the DBD report", metaVar = "dbdFile")
    private File dbdFile;

    /** genome master directory */
    @Argument(index = 0, metaVar = "genomeDumpDir", usage = "master directory containing JSON genome dumps", required = true)
    private File genomeDirsIn;

    @Override
    protected void setReporterDefaults() {
        this.reportType = BaseJsonScanReporter.Type.TEXT;
        this.dbdFile = null;
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Verify the DBD file.
        if (this.dbdFile != null && ! this.dbdFile.canRead())
            throw new FileNotFoundException("DBD source file " + this.dbdFile + " is not found or unreadable.");
        // Get the genome master directory.
        if (! this.genomeDirsIn.isDirectory())
            throw new FileNotFoundException("Master genome dump directory " + this.genomeDirsIn + " is not found or invalid.");
        this.genomeDirs = new MasterGenomeDir(this.genomeDirsIn);
        log.info("{} genomes found in {}.", this.genomeDirs.size(), this.genomeDirsIn);
        // Initialize the maps.
        this.countMap = new TreeMap<>();
        this.fileCounts = new CountMap<>();
        this.recordCounts = new CountMap<>();
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // Create the report writer.
        this.jscanWriter = this.reportType.createReporter(writer, this);
        // Loop through the genomes.
        int gCount = 0;
        final int gTotal = this.genomeDirs.size();
        for (File genomeDir : this.genomeDirs) {
            gCount++;
            String genomeId = genomeDir.getName();
            log.info("Processing genome {} of {}: {}.", gCount, gTotal, genomeId);
            long lastMsg = System.currentTimeMillis();
            JsonFileDir subFiles = new JsonFileDir(genomeDir);
            for (File subFile : subFiles) {
                log.debug("Reading file {}.", subFile);
                // Get the JSON records for this JSON file.
                try (JsonListIterator jsonIter = new JsonListIterator(subFile)) {
                    // If we have records, count this file.
                    if (jsonIter.hasNext()) {
                        String name = subFile.getName();
                        Map<String, FieldCounter> fileMap = this.countMap.computeIfAbsent(name, x -> new TreeMap<String, FieldCounter>());
                        this.fileCounts.count(name);
                        int recordCount = 0;
                        // Loop through the records, counting the fields.
                        while (jsonIter.hasNext()) {
                            this.recordCounts.count(name);
                            // Now loop through the fields.
                            JsonObject record = jsonIter.next();
                            recordCount++;
                            for (var fieldEntry : record.entrySet()) {
                                String fieldName = fieldEntry.getKey();
                                FieldCounter counter = fileMap.computeIfAbsent(fieldName, x -> new FieldCounter());
                                counter.count(fieldEntry.getValue());
                            }
                            long now = System.currentTimeMillis();
                            if (now - lastMsg >= 5000) {
                                log.info("{} records read from {}.", recordCount, subFile);
                                lastMsg = now;
                            }
                        }
                    }
                }
            }
        }
        // Now we produce the output.
        log.info("Writing report on {} files.", this.fileCounts.size());
        jscanWriter.startReport();
        // Loop through the file names in the count map.
        for (var fileEntry : this.countMap.entrySet()) {
            // Create the heading for this file
            String fileName = fileEntry.getKey();
            jscanWriter.startFile(fileName, this.fileCounts.getCount(fileName), this.recordCounts.getCount(fileName));
            // Write the field data.
            Map<String, FieldCounter> fieldMap = fileEntry.getValue();
            for (var fieldEntry : fieldMap.entrySet())
                jscanWriter.writeField(fieldEntry.getKey(), fieldEntry.getValue());
            // End the file section.
            jscanWriter.endFile(fileName);
        }
        // Terminate the report.
        jscanWriter.endReport();
    }

    @Override
    public File getDbdFile() {
        return this.dbdFile;
    }


}
