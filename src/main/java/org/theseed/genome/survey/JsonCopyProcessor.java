/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.MarkerFile;
import org.theseed.io.MasterGenomeDir;
import org.theseed.json.JsonFileDir;
import org.theseed.json.clean.JsonCleaner;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This command will copy JSON dumps to a new directory, optionally performing cleanup tasks.
 *
 * The positional parameters are the input and output directories.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 *
 * --action		type of cleanup action to perform (may occur multiple times)
 * --clear		if specified, the output directory will be erased before processing
 *
 * @author Bruce Parrello
 *
 */
public class JsonCopyProcessor extends BaseProcessor implements JsonCleaner.IParms {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(JsonCopyProcessor.class);
    /** list of JSON cleaners to use */
    private List<JsonCleaner> cleaners;
    /** input JSON dump directory */
    private MasterGenomeDir jsonDirs;
    /** number of directories processed */
    private int dirCounter;
    /** number of files processed */
    private int fileCounter;
    /** number of files skipped */
    private int skipCounter;
    /** number of empty files */
    private int emptyCounter;

    // COMMAND-LINE OPTIONS

    /** list of cleaners to use */
    @Option(name = "--action", usage = "type of cleaner to use to modify records")
    private List<JsonCleaner.Type> cleanerTypes;

    /** if specified, the output directory will be erased before processing */
    @Option(name = "--clear", usage = "erase the output directory before processing")
    private boolean clearFlag;

    /** if specified, files that already exist in the output directory will not be overwritten */
    @Option(name = "--missing", usage = "if specified, files already in the output will not be overwritten")
    private boolean missingFlag;

    /** input JSON dump directory */
    @Argument(index = 0, metaVar = "inDir", usage = "input JSON dump directory", required = true)
    private File inDir;

    /** output directory for modified JSON dumps */
    @Argument(index = 1, metaVar = "outDir", usage = "output directory for modified dumps", required = true)
    private File outDir;


    @Override
    protected void setDefaults() {
        this.cleanerTypes = new ArrayList<JsonCleaner.Type>();
        this.clearFlag = false;
        this.missingFlag = false;
    }

    @Override
    protected boolean validateParms() throws IOException, ParseFailureException {
        // Validate the input directory.
        if (! this.inDir.isDirectory())
            throw new FileNotFoundException("Input directory " + this.inDir + " is not found or invalid.");
        this.jsonDirs = new MasterGenomeDir(this.inDir);
        log.info("{} genomes found in {}.", this.jsonDirs.size(), this.inDir);
        // Set up the output directory.
        if (! this.outDir.isDirectory()) {
            log.info("Creating output directory {}.", this.outDir);
            FileUtils.forceMkdir(this.outDir);
        } else if (this.clearFlag) {
            log.info("Erasing output directory {}.", this.outDir);
            FileUtils.cleanDirectory(this.outDir);
        } else
            log.info("Using output directory {}.", this.outDir);
        // Set up the cleaners.
        log.info("Initializing {} cleaners.", this.cleanerTypes.size());
        this.cleaners = new ArrayList<JsonCleaner>(this.cleanerTypes.size());
        for (JsonCleaner.Type type : this.cleanerTypes)
            this.cleaners.add(type.create(this));
        return true;
    }

    @Override
    protected void runCommand() throws Exception {
        // Initialize the counters.
        this.dirCounter = 0;
        this.fileCounter = 0;
        this.skipCounter = 0;
        this.emptyCounter = 0;
        // We process the genome directories in parallel.
        this.jsonDirs.parallelStream().forEach(x -> this.processDirectory(x));
        // Now output the counters.
        log.info("{} directories processed. {} files, {} skipped, {} empty.", this.dirCounter, this.fileCounter, this.skipCounter,
                this.emptyCounter);
        // Allow the cleaners to output their own stats.
        for (JsonCleaner cleaner : this.cleaners)
            cleaner.logStats();
    }

    /**
     * Process a single genome dump directory. Note that we have to quiesce checked exceptions to
     * permit usage in stream expressions.
     *
     * @param gInDir	genome dump directory to process
     *
     */
    private void processDirectory(File gInDir) {
        // This will hold the name of the current file for error message purposes.
        File gInFile = null;
        try {
            synchronized (this) {
                this.dirCounter++;
                log.info("Processing input directory {} of {}.", this.dirCounter, this.jsonDirs.size());
            }
            // Get the genome name and construct the output directory.
            String genomeId = gInDir.getName();
            File gOutDir = new File(this.outDir, genomeId);
            if (! gOutDir.isDirectory())
                FileUtils.forceMkdir(gOutDir);
            // Count the files processed.
            int fileCount = 0;
            int skipCount = 0;
            int emptyCount = 0;
            // Count the records processed.
            int recordCount = 0;
            // Now loop through the input files.
            JsonFileDir gFiles = new JsonFileDir(gInDir);
            var gIter = gFiles.iterator();
            while (gIter.hasNext()) {
                gInFile = gIter.next();
                fileCount++;
                File gOutFile = new File(gOutDir, gInFile.getName());
                if (this.missingFlag && gOutFile.exists()) {
                    // Here the output file already exists and we don't want to overwrite.
                    skipCount++;
                } else {
                    // Here we can overwrite the output file.
                    JsonArray jsonList = JsonFileDir.getJson(gInFile);
                    if (jsonList.isEmpty()) {
                        // Here the input file is empty.
                        emptyCount++;
                        // Write an empty output JSON file.
                        MarkerFile.write(gOutFile, "[]");
                    } else {
                        // Now we loop through the records, cleaning them.
                        for (Object recordObj : jsonList) {
                            JsonObject record = (JsonObject) recordObj;
                            for (JsonCleaner cleaner : cleaners) {
                                recordCount++;
                                cleaner.process(record);
                            }
                        }
                        // Write the updated JSON to the output file.
                        try (PrintWriter writer = new PrintWriter(gOutFile)) {
                            JsonFileDir.writeJson(jsonList, writer);
                        }
                    }
                }
            }
            log.info("{} files processed in {}. {} records output. {} empty files, {} skipped.", fileCount,
                    gInDir, recordCount, emptyCount, skipCount);
            // Roll up the counters.
            synchronized (this) {
                this.fileCounter += fileCount;
                this.skipCounter += skipCount;
                this.emptyCounter += emptyCount;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JsonException e) {
            String fileName = (gInFile == null ? "<unknown>" : gInFile.toString());
            throw new RuntimeException("JSON error in " + fileName + ": " + e.getMessage());
        }
    }

}
