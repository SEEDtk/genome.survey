/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.GenomeSource;
import org.theseed.io.TabbedLineReader;
import org.theseed.sequence.MD5Hex;
import org.theseed.utils.BaseMultiReportProcessor;

/**
 * This command generates a map of protein sequences to annotations for a set of genomes
 * in a genome source.  As usual, the source can be a GTO directory, a file of PATRIC genome
 * IDs, a CoreSEED organism directory, or a master genome directory.
 *
 * Each protein will be output along with its MD5 and its annotation.  If the same protein
 * sequence is encountered twice with different annotations, it will be written to a special
 * error report.
 *
 * The positional parameter is the name of the input source directory or file.
 *
 * The output reports are "roleMap.tbl" for the main report and "errors.tbl" for the bad
 * proteins.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -D	output directory name (default "Proteins" in the current directory)
 * -t	type of input genome source (default DIR)
 *
 * --clear	erase the output directory before processing
 * --filter	specifies a file with genome IDs in the first column; only those genomes will be processed
 *
 *
 * @author Bruce Parrello
 */
public class RoleMapProcessor extends BaseMultiReportProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(RoleMapProcessor.class);
    /** map of protein strings to annotations to feature IDs */
    private Map<String, NavigableMap<String, Set<String>>> annotationMap;
    /** input genome source */
    private GenomeSource genomes;
    /** IDs of genomes to process */
    private Set<String> filterSet;
    /** MD5 computer */
    private MD5Hex md5Computer;
    /** header line for output files */
    private static final String HEADER_LINE = "md5\tprotein\tannotation";

    // COMMAND-LINE OPTIONS

    /** type of input genome source */
    @Option(name = "--source", aliases = { "-t" }, usage = "type of input genome source")
    private GenomeSource.Type sourceType;

    /** optional filter file */
    @Option(name = "--filter", metaVar = "complete.tbl", usage = "optional list of genome IDs to use")
    private File filterFile;

    /** input genome source file or directory */
    @Argument(index = 0, metaVar = "inDir", usage = "input genome source file or directory", required = true)
    private File inDir;

    @Override
    protected File setDefaultOutputDir(File curDir) {
        return new File(curDir, "Proteins");
    }

    @Override
    protected void setMultiReportDefaults() {
        this.sourceType = GenomeSource.Type.DIR;
        this.filterFile = null;
    }

    @Override
    protected void validateMultiReportParms() throws IOException, ParseFailureException {
        // Insure the genome source exists.
        if (! this.inDir.exists())
            throw new FileNotFoundException("Input genome source " + this.inDir + " does not exist.");
        log.info("Connecting to {} genome source at {}.", this.sourceType, this.inDir);
        this.genomes = this.sourceType.create(this.inDir);
        log.info("{} genomes found in {}.", this.genomes.size(), this.inDir);
        if (this.filterFile != null) {
            log.info("Reading genomes to use from {}.", this.filterFile);
            this.filterSet = TabbedLineReader.readSet(this.filterFile, "1");
        } else {
            log.info("All genomes will be processed.");
            this.filterSet = this.genomes.getIDs();
        }
    }

    @Override
    protected void runMultiReports() throws Exception {
        this.md5Computer = new MD5Hex();
        // Set up the main map.
        final int nGenomes = this.filterSet.size();
        this.annotationMap = new HashMap<>(nGenomes * 6000);
        // Initialize some counters.
        int gCount = 0;
        int pegTotal = 0;
        int missingTotal = 0;
        int badTotal = 0;
        // Insanely, we read everything into memory; then we write it to the output.
        // This is because we can't know the error proteins until we're done.  The only
        // exception is the invalid proteins.
        try (PrintWriter badWriter = this.openReport("invalid.tbl")) {
            badWriter.println("fid\terror\tannotation");
            for (String genomeId : this.filterSet) {
                gCount++;
                Genome genome = this.genomes.getGenome(genomeId);
                int pegCount = 0;
                int badCount = 0;
                int missing = 0;
                log.info("Processing genome {} of {}: {}.", gCount, nGenomes, genome);
                // Loop through the pegs.
                for (Feature peg : genome.getPegs()) {
                    String protein = peg.getProteinTranslation();
                    String annotation = peg.getPegFunction();
                    // Only proceed if the protein is present.
                    if (StringUtils.isBlank(protein)) {
                        missing++;
                        badWriter.println(peg.getId() + "\tmissing\t" + annotation);
                    } else {
                        // Insure there are no internal stops.
                        if (protein.endsWith("*"))
                            protein = StringUtils.chop(protein);
                        if (protein.contains("*")) {
                            badCount++;
                            badWriter.println(peg.getId() + "\tinvalid\t" + annotation);
                        } else {
                            Map<String, Set<String>> annoFidMap = this.annotationMap.computeIfAbsent(protein, x -> new TreeMap<String, Set<String>>());
                            Set<String> fidSet = annoFidMap.computeIfAbsent(annotation, x -> new TreeSet<String>());
                            fidSet.add(peg.getId());
                            pegCount++;
                        }
                    }
                }
                log.info("{} proteins found, {} missing, {} bad in {}.", pegCount, missing, badCount, genome);
                pegTotal += pegCount;
                missingTotal += missing;
                badTotal += badCount;
            }
            log.info("{} proteins found in {} pegs. {} invalid pegs found.", this.annotationMap.size(),
                    pegTotal, missingTotal + badTotal);
        }
        // Open the output files.
        try (
                PrintWriter writer  = this.openReport("roleMap.tbl");
                PrintWriter errorWriter = this.openReport("errors.tbl")) {
            // Set up the counters.
            int goodCount = 0;
            int errCount = 0;
            // Write the headers.
            writer.println(HEADER_LINE);
            errorWriter.println(HEADER_LINE + "\tfids");
            // Loop through the map.
            long lastMsg = System.currentTimeMillis();
            log.info("Processing {} proteins in annotation map.", this.annotationMap.size());
            for (var annoEntry : this.annotationMap.entrySet()) {
                String protein = annoEntry.getKey();
                String md5 = this.md5Computer.sequenceMD5(protein);
                // This will have one entry per annotation, mapped to a set of feature IDs.
                NavigableMap<String, Set<String>> fidMap = annoEntry.getValue();
                if (fidMap.size() == 1) {
                    // Here we have a good protein.
                    writer.println(md5 + "\t" + protein + "\t" + fidMap.firstKey());
                    goodCount++;
                } else {
                    // Here we have an ambiguous protein.
                    var iter = fidMap.entrySet().iterator();
                    errorWriter.println(md5 + "\t" + protein + "\t" + this.annoData(iter.next()));
                    while (iter.hasNext())
                        errorWriter.println("\t\t" + this.annoData(iter.next()));
                    errCount++;
                }
                if (log.isInfoEnabled() && System.currentTimeMillis() - lastMsg >= 10000) {
                    log.info("{} good proteins and {} ambiguous proteins output.", goodCount, errCount);
                    lastMsg = System.currentTimeMillis();
                }
            }
            log.info("{} good proteins and {} ambiguous proteins found.", goodCount, errCount);
        }
    }

    /**
     * Display the annotation string and features for the specified annotation/fid map entry.
     *
     * @param entry		map entry of an annotation and the set of features using it
     *
     * @return the string to print in the last two report columns
     */
    private String annoData(Map.Entry<String, Set<String>> entry) {
        String anno = entry.getKey();
        String fids = StringUtils.join(entry.getValue(), ", ");
        return anno + "\t" + fids;
    }

}
