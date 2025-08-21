/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.BaseGenomeProcessor;
import org.theseed.p3api.KeyBuffer;
import org.theseed.p3api.P3Connection;
import org.theseed.p3api.P3Connection.Table;
import org.theseed.proteins.FamilyType;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This command will read a genome source and create a protein family definition file. This file contains an MD5, family ID, and
 * family product for each protein found in the source. This requires reading all the genomes in the source as well as reading
 * data directly from the protein-family table in PATRIC.
 *
 * The positional parameters should be the genome source file or directory and the name for the output family definition file.
 *
 * The command-line option are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	genome source type (default DIR)
 *
 * --fam	protein family type (default PLFAM)
 *
 * @author Bruce Parrello
 *
 */
public class FamilyFileProcessor extends BaseGenomeProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(FamilyFileProcessor.class);
    /** protein MD5 to family ID map */
    private Map<String, String> md5Map;
    /** family ID to produce map */
    private Map<String, String> familyMap;
    /** connection to PATRIC */
    private P3Connection p3;

    // COMMAND-LINE OPTIONS

    /** type of protein family to extract */
    @Option(name = "--fam", usage = "type of protein family to extract")
    private FamilyType famType;

    /** name of the output file for the family definitions */
    @Argument(index = 1, metaVar = "outFile.tbl", usage = "output file for family definition data", required = true)
    private File outFile;

    @Override
    protected void setSourceDefaults() {
        this.famType = FamilyType.PLFAM;
    }

    @Override
    protected void validateSourceParms() throws IOException, ParseFailureException {
        // Verify that we can write to the output file.
        try (PrintWriter outStream = new PrintWriter(this.outFile)) {
            outStream.println("fam_id\tproduct\tmd5");
        }
        // Set up the maps.
        this.md5Map = new HashMap<>();
        this.familyMap = new HashMap<>();
        // Connect to PATRIC.
        this.p3 = new P3Connection();
    }

    @Override
    protected void runCommand() throws Exception {
        // Loop through the genomes, extracting the family data.
        var genomeIds = this.getGenomeIds();
        final int total = genomeIds.size();
        int gCount = 0;
        for (String genomeId : genomeIds) {
            gCount++;
            Genome genome = this.getGenome(genomeId);
            log.info("Processing genome {} of {}: {}.", gCount, total ,genome);
            // Loop through the features.
            int famCount = 0;
            int skipCount = 0;
            int errCount = 0;
            for (Feature feat : genome.getFeatures()) {
                String famId = this.famType.getFamily(feat);
                // Only proceed if we have a family membership for this family type.
                if (famId == null)
                    skipCount++;
                else {
                    // Get the MD5. It is an error if we don't have one.
                    String md5 = feat.getMD5();
                    if (md5 == null)
                        errCount++;
                    else {
                        this.md5Map.put(md5, famId);
                        // Save the product in the family ID.
                        this.familyMap.put(famId, feat.getPegFunction());
                        famCount++;
                    }
                }
            }
            log.info("{} proteins stored, {} features skipped, {} errors.", famCount, skipCount, errCount);
        }
        log.info("{} total proteins, {} families.", this.md5Map.size(), this.familyMap.size());
        // Now we want to run through the family IDs and get the correct products from PATRIC.
        log.info("Retrieving protein family products.");
        var familyRecords = this.p3.getRecords(Table.FAMILY, this.familyMap.keySet(), "family_id,family_product");
        log.info("{} products found out of {} families.", familyRecords.size(), this.familyMap.size());
        int fCount = 0;
        long lastMsg = System.currentTimeMillis();
        for (JsonObject familyRecord : familyRecords.values()) {
            String familyId = KeyBuffer.getString(familyRecord, "family_id");
            String product = KeyBuffer.getString(familyRecord, "family_product");
            this.familyMap.put(familyId, product);
            fCount++;
            long now = System.currentTimeMillis();
            if (now - lastMsg >= 10000) {
                log.info("{} family products stored.", fCount);
                lastMsg = now;
            }
        }
        log.info("{} total family products stored.", fCount);
        // Now we write the output.
        log.info("Writing results to {}.", this.outFile);
        lastMsg = System.currentTimeMillis();
        int wCount = 0;
        try (PrintWriter writer = new PrintWriter(this.outFile)) {
            writer.println("fam_id\tproduct\tmd5");
            for (var md5Entry : this.md5Map.entrySet()) {
                String familyId = md5Entry.getValue();
                writer.println(familyId + "\t" + this.familyMap.getOrDefault(familyId, "<unknown>") + "\t" + md5Entry.getKey());
                wCount++;
                long now = System.currentTimeMillis();
                if (now - lastMsg >= 5000) {
                    log.info("{} of {} lines written.", wCount, fCount);
                    lastMsg = now;
                }
            }
        }
        log.info("{} total lines written.", wCount);
    }
}
