/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.locations.Location;

/**
 * This command will examine the BV-BRC genome dumps in a directory and create a file that describes the adjacent
 * roles for each incoming role.  The output file for a genome will be placed in the genome dump directory, and
 * each record will contain a protein feature ID, a genome ID, a genome name, a role, and the preceding and
 * subsequent roles on the same strand.  The file will be called "neighbors.tbl" and be tab-delimited.
 *
 * The positional parameter is the name of the input directory, which should contain one or more genome-dump
 * sub-directories.  The command-line options are as follows.
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 *
 * --gap	the maximum allowable gap between adjacent protein features (default 2000)
 *
 * @author Bruce Parrello
 *
 */
public class RoleAdjacencyProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(RoleAdjacencyProcessor.class);
    /** list of genome dump subdirectories */
    private File[] genomeDirs;
    /** file filter for genome subdirectories */
    private FileFilter GENOME_SUBDIR_FILTER = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            boolean retVal = pathname.isDirectory();
            if (retVal) {
                File featureFile = new File(pathname, "genome_feature.json");
                retVal = featureFile.canRead();
            }
            return retVal;
        }

    };

    // COMMAND-LINE OPTION

    /** maximum gap between adjacent proteins */
    @Option(name = "--gap", metaVar = "5000", usage = "maximum gap allowed between neighbors")
    private int maxGap;

    /** input master directory */
    @Argument(index = 0, metaVar = "inDir", usage = "master directory for genome dumps", required = true)
    private File inDir;

    /**
     * Feature descriptor, containing role and location
     */
    public static class FeatureData implements Comparable<FeatureData> {

        /** feature ID */
        private String fid;
        /** functional assignment */
        private String product;
        /** location */
        private Location loc;

        /**
         * Construct this feature from the input data.
         *
         * @param id		feature ID
         * @param function	functional assignment
         * @param contig	contig ID
         * @param strand	protein strand
         * @param start		protein left position
         * @param end		protein right position
         */
        public FeatureData(String id, String function, String contig, String strand, int start, int end) {
            this.fid = id;
            this.product = function;
            this.loc = Location.create(contig, strand, start, end);
        }

        @Override
        public int compareTo(FeatureData o) {
            // Compare locations.
            int retVal = this.loc.compareTo(o.loc);
            if (retVal == 0) {
                // Locations are equal, so compare the feature IDs.
                retVal = this.fid.compareTo(o.fid);
            } else if (this.loc.getStrand().equals("-")) {
                // Locations are unequal.  On the minus strand, we reverse the order.
                retVal = -retVal;
            }
            return retVal;
        }

        /**
         * Compute the gap between two locations. If the strand or contig is different,
         * the gap will be over 2 billion.
         *
         * @param other		other feature to check against this one (may be null)
         *
         * @return the gap between this location and the other
         */
        public int getGap(FeatureData other) {
            int retVal;
            if (other == null)
                retVal = Integer.MAX_VALUE;
            else
                retVal = this.loc.strandDistance(other.loc);
            return retVal;
        }

        /**
         * @return the function of this feature
         */
        public String getProduct() {
            return this.product;
        }

        /**
         * @return the strand of this feature
         */
        public String getStrand() {
            return this.loc.getContigId() + this.loc.getStrand();
        }

        /**
         * @return the feature ID
         */
        public String getFid() {
            return this.fid;
        }

    }

    @Override
    protected void setDefaults() {
        this.maxGap = 2000;
    }

    @Override
    protected boolean validateParms() throws IOException, ParseFailureException {
        // Validate the gap.
        if (this.maxGap <= 0)
            throw new ParseFailureException("Gap must be a positive number.");
        // Validate the input directory.
        if (! this.inDir.isDirectory())
            throw new FileNotFoundException("Input directory " + this.inDir + " is not found or invalid.");
        this.genomeDirs = this.inDir.listFiles(GENOME_SUBDIR_FILTER);
        if (this.genomeDirs.length <= 0)
            throw new FileNotFoundException("No genome feature files found in " + this.inDir + ".");
        log.info("{} genomes found in {}.", this.genomeDirs.length, this.inDir);
        return true;
    }

    @Override
    protected void runCommand() throws Exception {
        // We process each genome separately. All the features are loaded and then sorted to produce the neighbor data.
        Arrays.stream(this.genomeDirs).parallel().forEach(x -> this.processGenome(x));
    }

    /**
     * Process a single genome.  The genome-feature JSON file is read and the necessary fields extracted to create a feature
     * descriptor.  The descriptors are sorted and from this we can produce the neighbor file.  Note that to allow easy use
     * in streams, we de-check any IO exceptions.
     *
     * @param gDir	name of the genome's directory
     *
     */
    private void processGenome(File gDir) {
        // Compute the name of the input file.
        File inFile = new File(gDir, "genome_feature.json");
        // Allocate variables for the genome ID and name.
        String genomeId = null;
        String genomeName = null;
        // Compute the name of the output file.
        File outFile = new File(gDir, "neighbors.tbl");
        // Create a hash to map strand IDs to feature data lists.
        Map<String, List<FeatureData>> featMap = new HashMap<String, List<FeatureData>>();
        // Get access to the feature records.
        log.info("Connecting to genome features in {}.", inFile);
        try (FieldInputStream inStream = FieldInputStream.create(inFile)) {
            // We need the column indices for the fields of interest.
            int idColIdx = inStream.findField("patric_id");
            int prodColIdx = inStream.findField("product");
            int contigColIdx = inStream.findField("accession");
            int strandColIdx = inStream.findField("strand");
            int startColIdx = inStream.findField("start");
            int endColIdx = inStream.findField("end");
            int typeColIdx = inStream.findField("feature_type");
            // We need the genome ID and name from the first record.
            int gIdColIdx = inStream.findField("genome_id");
            int gNameColIdx = inStream.findField("genome_name");
            // Now loop through the records.
            int inCount = 0;
            int protCount = 0;
            long lastMsg = System.currentTimeMillis();
            for (var record : inStream) {
                inCount++;
                // First check for the genome information.
                if (genomeId == null) {
                    genomeId = record.get(gIdColIdx);
                    genomeName = record.get(gNameColIdx);
                    log.info("Processing genome {}: {}.", genomeId, genomeName);
                }
                // Now insure we have a protein.
                String type = record.get(typeColIdx);
                if (type.equals("CDS")) {
                    protCount++;
                    // Get the feature data for this protein.
                    String fid = record.get(idColIdx);
                    // Only proceed if we have a PATRIC ID.
                    if (! StringUtils.isBlank(fid)) {
                        String product = record.get(prodColIdx);
                        String contig = record.get(contigColIdx);
                        String strand = record.get(strandColIdx);
                        int start = record.getInt(startColIdx);
                        int end = record.getInt(endColIdx);
                        FeatureData feat = new FeatureData(fid, product, contig, strand, start, end);
                        // Get the strand's hash entryn and add us to the list.
                        String strandId = feat.getStrand();
                        List<FeatureData> flist = featMap.computeIfAbsent(strandId, x -> new ArrayList<FeatureData>());
                        flist.add(feat);
                    }
                }
                if (System.currentTimeMillis() - lastMsg >= 5000) {
                    lastMsg = System.currentTimeMillis();
                    log.info("{} records read for genome {}.  {} proteins, {} strands.", inCount, genomeId, protCount, featMap.size());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // Now we have our hash.  For each strand, we generate output.
        log.info("Writing data for genome {} ({}) to {}.", genomeId, genomeName, outFile);
        try (PrintWriter outStream = new PrintWriter(outFile)) {
            // Start with the header line.
            outStream.println("genome_id\tgenome_name\tpatric_id\ttype\tproduct\tprevious\tnext");
            // Now loop through the strands.
            for (var strandEntry : featMap.entrySet()) {
                List<FeatureData> flist = strandEntry.getValue();
                log.info("{} proteins found on strand {} of {}.", flist.size(), strandEntry.getKey(), genomeId);
                // Sort the features in order by location.
                Collections.sort(flist);
                // Now we go through each feature and check the ones next to it to form our output list.  Note that
                // there must be at least one protein, or the entry would not exist in the hash.
                FeatureData prev = null;
                Iterator<FeatureData> iter = flist.iterator();
                FeatureData curr = iter.next();
                while (curr != null) {
                    // Check for a next feature.
                    FeatureData next = (iter.hasNext() ? iter.next() : null);
                    // Get the current feature's role.
                    String currRole = curr.getProduct();
                    // Skip if it is hypothetical.
                    if (! isHypothetical(currRole)) {
                        // Get the feature ID.
                        String fid = curr.getFid();
                        // Check the adjacent features.
                        String prevRole = this.checkAdjacentRole(curr, prev);
                        String nextRole = this.checkAdjacentRole(curr, next);
                        if (! nextRole.isBlank() || ! prevRole.isBlank()) {
                            // Here there is at least one good neighbor.
                            outStream.println(genomeId + "\t" + genomeName + "\t" + fid + "\tCDS\t" + curr.getProduct() +"\t"
                                    + prevRole + "\t" + nextRole);
                        }
                    }
                    // Slide to the next protein..
                    prev = curr;
                    curr = next;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return TRUE if this is a hypothetical protein function, else FALSE
     *
     * @param function		functional assignment of interest
     */
    private boolean isHypothetical(String function) {
        return function.equals("hypothetical protein");
    }

    /**
     * Check the current protein against an adjacent protein and return the role if it's useful.
     *
     * @param curr		current protein's feature data (cannot be NULL)
     * @param other		adjacent protein's feature data (can be NULL)
     *
     * @return the other protein's function if it is close enough and not hypothetical, else an empty string
     */
    private String checkAdjacentRole(FeatureData curr, FeatureData other) {
        String retVal = "";
        if (curr.getGap(other) <= this.maxGap) {
            retVal = other.getProduct();
            // Insure we ignore hypothetical proteins.
            if (isHypothetical(retVal))
                retVal = "";
        }
        return retVal;
    }

}
