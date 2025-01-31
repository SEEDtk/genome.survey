/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Genome;
import org.theseed.genome.SubsystemRow;
import org.theseed.genome.iterator.BaseGenomeProcessor;
import org.theseed.genome.iterator.GenomeSource;
import org.theseed.subsystems.VariantCodeComparator;

/**
 * This subcommand outputs a report on the subsystem differences between pairs of genomes. It specifies two genome sources, and
 * both must have the same genomes in it. Each first-source genome's subsystems will be compared to the subsystems in the
 * corresponding second-source genome and the differing subsystems will be listed.
 *
 * The positional parameters are the two genome source files or directories. Every genome in the first source should also be
 * in the second source.
 *
 * The command-line option are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	first genome source type (default DIR)
 * -o	output file for report (if not STDOUT)
 *
 * --type2		second genome source type (default DIR)
 * --vType		type of variant code comparison (default STRICT)
 *
 * @author Bruce Parrello
 *
 */
public class SubsystemCompareProcessor extends BaseGenomeProcessor implements VariantCodeComparator.IParms {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(SubsystemCompareProcessor.class);
    /** second genome source */
    private GenomeSource genomes2;
    /** output print writer for report */
    private PrintWriter outStream;
    /** variant code comparator */
    private VariantCodeComparator varCodeMatcher;
    /** dummy subsystem map entry to use as merge trailer */
    private static final Map.Entry<String, SubsystemRow> SUBS_TRAILER =
            new AbstractMap.SimpleEntry<String, SubsystemRow>("\uFFFF\uFFFF\uFFFF", null);

    // COMMAND-LINE OPTIONS

    /** output file (if not STDOUT) */
    @Option(name = "--output", aliases = { "-o" }, metaVar = "report.tbl", usage = "output report file (if not STDOUT)")
    private File outFile;

    /** type of second genome source */
    @Option(name = "--type2", usage = "type of second genome source")
    private GenomeSource.Type source2Type;

    /** type of variant-code comparison to use */
    @Option(name = "--vType", usage = "type of variant-code comparison to use")
    private VariantCodeComparator.Type varType;

    /** second genome source file or directory */
    @Argument(index = 1, metaVar = "inDir2", usage = "file or directory name of second genome source", required = true)
    private File inDir2;

    @Override
    protected void setSourceDefaults() {
        this.source2Type = GenomeSource.Type.DIR;
        this.outFile = null;
        this.varType = VariantCodeComparator.Type.STRICT;
    }

    @Override
    protected void validateSourceParms() throws IOException, ParseFailureException {
        // Validate the second genome source.
        if (! this.inDir2.exists())
            throw new FileNotFoundException("Second genome source " + this.inDir2 + " is not found.");
        // Load the second genome source.
        log.info("Connecting to secondary genomes from {}.", this.inDir2);
        this.genomes2 = this.source2Type.create(this.inDir2);
        log.info("{} genomes found in second source.", this.genomes2.size());
        // Create the variant code comparator.
        this.varCodeMatcher = this.varType.create(this);
        log.info("Variant-code comparison algorithm is {}.", this.varType);
        // Finally, open the output report writer.
        if (this.outFile == null) {
            log.info("Report will be written to the standard output.");
            this.outStream = new PrintWriter(System.out);
        } else {
            log.info("Report will be written to {}.", this.outFile);
            this.outStream = new PrintWriter(this.outFile);
        }
    }

    @Override
    protected void runCommand() throws Exception {
        try {
            // Write the output header.
            this.outStream.println("genome_id\tgenome_name\tsubsystem_name\tvariant1\tvariant2\troles1\troles2\terror_type");
            // Get the IDs for both genome sources.
            Set<String> genomeIds1 = this.getGenomeIds();
            Set<String> genomeIds2 = this.genomes2.getIDs();
            // Loop through them.
            int gCount = 0;
            final int gTotal = genomeIds1.size();
            int notFoundCount = 0;
            int diffCount = 0;
            for (String genomeId1 : genomeIds1) {
                gCount++;
                // Get the first genome.
                Genome genome1 = this.getGenome(genomeId1);
                log.info("Processing genome {} of {}: {}.", gCount, gTotal, genome1);
                // Check for the second genome.
                if (! genomeIds2.contains(genomeId1)) {
                    log.warn("{} not found in second source {}.", genome1, this.inDir2);
                    notFoundCount++;
                } else {
                    // Get the second genome.
                    Genome genome2 = genomes2.getGenome(genomeId1);
                    // Compare the genomes.
                    diffCount += this.compare(genome1, genome2);
                }
            }
            log.info("{} genomes processed. {} not present in second source. {} total subsystem differences.", gCount,
                    notFoundCount, diffCount);
            // Flush the output stream.
            this.outStream.flush();
        } finally {
            // Insure the output file is closed.
            this.outStream.close();
        }
    }

    /**
     * Compare the subsystems in two genomes and output the differences.
     *
     * @param genome1	first genome
     * @param genome2	second genome
     *
     * @return the number of differing subsystems
     */
    private int compare(Genome genome1, Genome genome2) {
        int retVal = 0;
        // Get the list of subsystems for each genome. We put them in a sorted map so we can do a
        // merge compare.
        SortedMap<String, SubsystemRow> subs1Map = this.getSubsystems(genome1);
        SortedMap<String, SubsystemRow> subs2Map = this.getSubsystems(genome2);
        // Set up iterators through the maps.
        var iter1 = subs1Map.entrySet().iterator();
        var iter2 = subs2Map.entrySet().iterator();
        // Prime with the first subsystem in both maps.
        var entry1 = this.nextEntry(iter1);
        var entry2 = this.nextEntry(iter2);
        while (entry1 != SUBS_TRAILER && entry2 != SUBS_TRAILER) {
            // Get the two names and the two subsystem rows.
            String name1 = this.normalizeName(entry1.getKey());
            String name2 = this.normalizeName(entry2.getKey());
            SubsystemRow sub1 = entry1.getValue();
            SubsystemRow sub2 = entry2.getValue();
            int cmp = name1.compareTo(name2);
            if (cmp < 0) {
                // The subsystem is in genome 1 but not genome 2.
                this.writeDifference(genome1, name1, sub1.getVariantCode(), "", sub1.getRoleCount(), 0, "missing");
                retVal++;
                entry1 = this.nextEntry(iter1);
            } else if (cmp == 0) {
                // Here the subsystem is in both genomes. Insure it is the same variant with the same roles.
                String v1 = sub1.getVariantCode();
                String v2 = sub2.getVariantCode();
                if (! this.varCodeMatcher.matches(v1, v2)) {
                    // Here the variant codes are different.
                    this.writeDifference(genome1, name1, v1, v2, sub1.getRoleCount(), sub2.getRoleCount(), "variant mismatch");
                    retVal++;
                } else {
                    // Here we must compare the roles.
                    Set<SubsystemRow.Role> roles1 = sub1.getActiveRoles();
                    Set<SubsystemRow.Role> roles2 = sub2.getActiveRoles();
                    if (! roles1.equals(roles2)) {
                        // Here the roles are indeed different.
                        this.writeDifference(genome1, name1, v1, v2, roles1.size(), roles2.size(), "role mismatch");
                        retVal++;
                    }
                }
                entry1 = this.nextEntry(iter1);
                entry2 = this.nextEntry(iter2);
            } else {
                // The subsystem is in genome2 but not genome1.

                this.writeDifference(genome1, name1, "", sub2.getVariantCode(), 0, sub2.getRoleCount(), "missing");
                retVal++;
                entry2 = this.nextEntry(iter2);
            }
        }
        return retVal;
    }

    /**
     * This method fixes the stupid problem with subsystem names that sometimes end in spaces and
     * somtimes in underscores.
     *
     * @param key	subsystem name to fix
     *
     * @return the subsystem name with a trailing underscore converted to a space
     */
    private String normalizeName(String key) {
        String retVal = key;
        if (key.endsWith("_"))
            retVal = key.substring(0, key.length() - 1) + " ";
        return retVal;
    }

    /**
     * Write a report line describing the different between the occurrences of a subsystem in the two versions of a
     * genome.
     *
     * @param genome			sample genome (used to get ID and name)
     * @param sub_name			subsystem name
     * @param variantCode1		variant code in genome 1
     * @param variantCode2		variant code in genome 2
     * @param roleCount1		role count in genome 1
     * @param roleCount2		role count in genome 2
     * @param type				type of difference
     */
    private void writeDifference(Genome genome, String sub_name, String variantCode1, String variantCode2, int roleCount1,
            int roleCount2, String type) {
        this.outStream.println(genome.getId() + "\t" + genome.getName() + "\t" + sub_name + "\t" + variantCode1
                + "\t" + variantCode2 + "\t" + roleCount1 + "\t" + roleCount2 + "\t" + type);
    }

    /**
     * @return the next entry in the subsystem map, or the trailer if there is none.
     *
     * @param iter	iterator through the subsystem map
     */
    private Map.Entry<String, SubsystemRow> nextEntry(Iterator<Entry<String, SubsystemRow>> iter) {
        Map.Entry<String, SubsystemRow> retVal;
        if (iter.hasNext())
            retVal = iter.next();
        else
            retVal = SUBS_TRAILER;
        return retVal;
    }

    /**
     * Get a sorted subsystem map for the specified genome.
     *
     * @param genome	genome of interest
     *
     * @return a sorted map from subsystem names to subsystem rows
     */
    private SortedMap<String, SubsystemRow> getSubsystems(Genome genome) {
        // Get all the subsystem rows.
        Collection<SubsystemRow> subRows = genome.getSubsystems();
        // Create a sorted map to hold them.
        TreeMap<String, SubsystemRow> retVal = new TreeMap<String, SubsystemRow>();
        // Fill the map.
        for (SubsystemRow subRow : subRows)
            retVal.put(subRow.getName(), subRow);
        return retVal;
    }

}
