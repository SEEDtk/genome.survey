/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.reports.BbhReporter;

import ord.theseed.genome.KmerFeatureData;

/**
 * This command computes the bidirectional best hits between two genomes, the source
 * and the target.
 *
 * Note that the PROJECTION report will actually update the annotations in the target
 * genome and save it back.
 *
 * The positional parameters are the file names for the two genome GTO files.
 *
 * The command-line options are as follows.
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -o	output file for report (if not STDOUT)
 *
 * --format		format of report (default TABULAR)
 * --maxDist	maximum distance allowed for report output (this affects updates in PROJECTION)
 * --dryRun		if specified, the updates from the PROJECTION report will not be written to disk
 * --subs		subsystem projector file for updating the target GTO subsystems (for PROJECTION)
 *
 * @author Bruce Parrello
 *
 */
public class BbhProcessor extends BaseReportProcessor implements BbhReporter.IParms {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(BbhProcessor.class);
    /** source genome */
    private Genome source;
    /** target genome */
    private Genome target;
    /** reporter object */
    private BbhReporter reporter;
    /** best target hits for each source feature, mapped by ID */
    private Map<String, Set<KmerFeatureData.Hit>> sourceHitMap;
    /** best source hits for each target feature, mapped by ID */
    private Map<String, Set<KmerFeatureData.Hit>> targetHitMap;

    // COMMAND-LINE OPTIONS

    /** report format */
    @Option(name = "--format", usage = "format for the output report")
    private BbhReporter.Type reportType;

    /** maximum permissible distance for report output */
    @Option(name = "--maxDist", metaVar = "1.0", usage = "maximum possible distance to write to report")
    private double maxDist;

    /** if specified, a PROJECTION update will be suppressed */
    @Option(name = "--dryRun", usage = "if specified, the PROJECTION report will not update the target genome")
    private boolean dryRunMode;

    /** if specified, a subsystem projector file for updating the target subsystems */
    @Option(name = "--subs", metaVar = "variants.tbl", usage = "subsystem projector file for updating target")
    private File subFile;

    /** source genome file */
    @Argument(index = 0, metaVar = "sourceGto", usage = "name of the source GTO file", required = true)
    private File sourceFile;

    /** target genome file */
    @Argument(index = 1, metaVar = "targetGto", usage = "name of the target GTO file", required = true)
    private File targetFile;

    @Override
    protected void setReporterDefaults() {
        this.reportType = BbhReporter.Type.TABULAR;
        this.maxDist = 0.60;
        this.dryRunMode = false;
        this.subFile = null;
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Validate the two genome files.
        if (! this.sourceFile.canRead())
            throw new FileNotFoundException("Source GTO file " + this.sourceFile + " is not found or unreadable.");
        if (! this.targetFile.canRead())
            throw new FileNotFoundException("Target GTO file " + this.targetFile + " is not found or unreadable.");
        // Create the genomes.
        this.source = new Genome(this.sourceFile);
        this.target = new Genome(this.targetFile);
        // Create the reporter.
        this.reporter = this.reportType.create(this);
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        log.info("Finding best hits.");
        this.computeBestHits();
        // Start the report.
        this.reporter.openReport(writer);
        // Now for each genome, we have a map from every feature to its best hits in the
        // other genome.  We need to refine this to bidirectional best hits.  A hit from
        // X to Y in X's best-hit set is bidirectional if X is in Y's best-hit set.
        log.info("Processing source genome features.");
        int sourceCount = 0;
        int foundCount = 0;
        int skipCount = 0;
        int zeroCount = 0;
        double distTotal = 0.0;
        for (Map.Entry<String, Set<KmerFeatureData.Hit>> hitEntry : this.sourceHitMap.entrySet()) {
            // Get the source feature and its set of targets.
            Feature sourceFeat = this.source.getFeature(hitEntry.getKey());
            sourceCount++;
            Set<KmerFeatureData.Hit> hitSet = hitEntry.getValue();
            var testHit = new KmerFeatureData.Hit(sourceFeat);
            // Find a target feature that maps back to the source feature.
            Iterator<KmerFeatureData.Hit> iter = hitSet.iterator();
            Feature targetFeat = null;
            double distance = 1.0;
            while (iter.hasNext() && targetFeat == null) {
                var hit = iter.next();
                // Get the hits back from the above hit's target features.
                Feature testFeat = hit.getFeature();
                var hitsBack = this.targetHitMap.get(testFeat.getId());
                // If the hits back include the source feature, stop the loop.
                if (hitsBack != null && hitsBack.contains(testHit)) {
                    targetFeat = testFeat;
                    distance = hit.getDistance();
                    foundCount++;
                    if (distance == 0.0)
                        zeroCount++;
                    else
                        distTotal += distance;
                }
            }
            // Report on this bidirectional best hit.
            if (distance <= this.maxDist)
                this.reporter.reportHit(sourceFeat, targetFeat, distance);
            else
                skipCount++;
        }
        log.info("{} source features found, {} had bidirectional best hits.", sourceCount, foundCount);
        if (foundCount > 0)
            log.info("Mean distance is {}.", distTotal / foundCount);
        log.info("{} features suppressed due to distance.  {} were zero-distance.", skipCount, zeroCount);
        // Close off the report.
        this.reporter.closeReport();
    }

    /**
     * Compute the best-hit maps for both genomes.
     */
    protected void computeBestHits() {
        // We start by sorting each genome's features by type.
        Map<String, List<KmerFeatureData>> sourceTypeMap = createTypeMap(this.source);
        Map<String, List<KmerFeatureData>> targetTypeMap = createTypeMap(this.target);
        // Now get the best hits in each direction.
        log.info("Computing best hits from {} to {}.", this.source, this.target);
        this.sourceHitMap = buildHitMap(sourceTypeMap, targetTypeMap);
        log.info("Computing best hits from {} to {}.", this.target, this.source);
        this.targetHitMap = buildHitMap(targetTypeMap, sourceTypeMap);
    }

    /**
     * Compute the best hits in the second genome for each feature in the first genome.
     *
     * @param g1TypeMap		map of feature types to kmer objects for the first genome
     * @param g2TypeMap		map of feature types to kmer objects for the second genome
     *
     * @return a map from each feature ID in the first genome to its best hits in the second genome
     */
    private Map<String, Set<KmerFeatureData.Hit>> buildHitMap(Map<String, List<KmerFeatureData>> g1TypeMap,
            Map<String, List<KmerFeatureData>> g2TypeMap) {
        // Create the return map.
        var retVal = new HashMap<String, Set<KmerFeatureData.Hit>>(4000);
        // Loop through the feature type lists.
        for (Map.Entry<String, List<KmerFeatureData>> typeEntry : g1TypeMap.entrySet()) {
            String type = typeEntry.getKey();
            log.info("Processing features of type {}.", type);
            List<KmerFeatureData> g1List = typeEntry.getValue();
            List<KmerFeatureData> g2List = g2TypeMap.get(type);
            // If there are no features of this type in genome 2, we give each one an empty set.
            if (g2List == null) {
                Set<KmerFeatureData.Hit> empty = Collections.emptySet();
                g1List.stream().map(x -> x.getFid()).forEach(x -> retVal.put(x, empty));
            } else {
                // Here we have features to process.
                int count = 0;
                for (KmerFeatureData g1 : g1List) {
                    var hitSet = g1.getBest(g2List);
                    retVal.put(g1.getFid(), hitSet);
                    count++;
                    if (count % 100 == 0)
                        log.info("{} of {} completed.", count, g1List.size());
                }
            }
        }
        return retVal;
    }

    /**
     * Create a map of each feature type to a collection of kmer data objects for all
     * features of that type.
     *
     * @param genome	genome whose features are to be collated by type
     *
     * @return a map from type names to kmer feature data object lists
     */
    private static Map<String, List<KmerFeatureData>> createTypeMap(Genome genome) {
        log.info("Collating features for {}.", genome);
        // Get the kmer feature data objects for all the features.
        var kmerObjects = genome.getFeatures().parallelStream()
                .map(x -> new KmerFeatureData(x)).collect(Collectors.toList());
        // Collate them into the return map.
        var retVal = new TreeMap<String, List<KmerFeatureData>>();
        // Most features are pegs.  For these, we pre-allocate the list.
        retVal.put("CDS", new ArrayList<KmerFeatureData>(kmerObjects.size()));
        for (KmerFeatureData kmerObject : kmerObjects) {
            String type = kmerObject.getType();
            List<KmerFeatureData> list =
                    retVal.computeIfAbsent(type, x -> new ArrayList<KmerFeatureData>(50));
            list.add(kmerObject);
        }
        return retVal;
    }

    @Override
    public File getTargetFile() {
        return this.targetFile;
    }

    @Override
    public boolean isDryRun() {
        return this.dryRunMode;
    }

    @Override
    public File getSubFile() {
        return this.subFile;
    }

}
