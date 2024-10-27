/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.MasterGenomeDir;
import org.theseed.stats.Shuffler;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This command will scan BV-BRC genome dumps and add taxonomic relatives to the genome.json files.  The entire
 * set of genome JSON objects will be loaded into memory and organized by the various taxonomic groupings.
 * Then for each genome, we will extract a random selection of neighbors in each group, add them to the
 * json object, and write it back out to the genome.json file.
 *
 * The positional parameter is the name of the genome dump directory.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line options
 * -v	display more frequent log messages
 * -n	number of neighbors to display in each grouping (default 10)
 *
 * @author Bruce Parrello
 *
 */
public class TaxonomyScanProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TaxonomyScanProcessor.class);
    /** map of genome IDs to JSON objects */
    private Map<String, JsonObject> jsonMap;
    /** map of genome IDs to file names */
    private Map<String, File> fileMap;
    /** list of genome files to process */
    private List<File> genomeFiles;
    /** array of taxonomic groupings of interest */
    private TaxonKey[] groupings;
    /** list of taxonomic grouping names */
    private static final String[] GROUPINGS = new String[] { "phylum", "genus", "family", "class", "order" };
    /** empty json array */
    private static final JsonArray EMPTY_ARRAY = new JsonArray();

    // COMMAND-LINE OPTIONS

    /** number of choices to select for each grouping */
    @Option(name = "--nChoices", aliases = { "-n" }, metaVar = "5", usage = "number of genomes to display for each grouping")
    private int nChoices;

    /** master genome dump directory */
    @Argument(index = 0, metaVar = "inDir", usage = "master genome JSON dump directory", required = true)
    private File inDir;

    @Override
    protected void setDefaults() {
        this.nChoices = 10;
    }

    /**
     * Enumeration for special keys used in the genome record.
     */
    protected static enum SpecialKey implements JsonKey {
        GENOME_ID(null), GENOME_WORD(null);

        /** default value for this key */
        private String defaultValue;

        private SpecialKey(String defaultVal) {
            this.defaultValue = defaultVal;
        }

        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        @Override
        public Object getValue() {
            return this.defaultValue;
        }

    }

    /**
     * Utility class for taxonomic field keys.  This object also contains the hash
     * of group names to genome word lists.
     */
    protected static class TaxonKey implements JsonKey {

        /** name of the key */
        private String keyName;
        /** map of group names to genome word lists */
        private Map<String, Shuffler<String>> groupMap;

        public TaxonKey(String keyname) {
            this.keyName = keyname;
            this.groupMap = new HashMap<String, Shuffler<String>>();
        }

        @Override
        public String getKey() {
            return this.keyName;
        }

        @Override
        public Object getValue() {
            return null;
        }

        /**
         * Add a genome word to a group.
         *
         * @param groupName		taxonomic grouping name
         * @param genomeWord	magic-word identifier of a genome in the group
         */
        public void addGenome(String groupName, String genomeWord) {
            Shuffler<String> groupList = this.groupMap.computeIfAbsent(groupName, x -> new Shuffler<String>(10));
            groupList.add(genomeWord);
        }

        /**
         * @return a random selection of genome words from a group
         *
         * @param groupName		taxonomic grouping name
         * @param genomeWord	magic-word identifier of a genome in the group to not return
         * @param nChoices		maximum number of words to return
         */
        public JsonArray getNeighbors(String groupName, String genomeWord, int nChoices) {
            JsonArray retVal = new JsonArray();
            Shuffler<String> groupList = this.groupMap.get(groupName);
            // We don't need to bother if the group is a singleton, since this means only the
            // genome we're skipping is in it.
            if (groupList != null && groupList.size() > 1) {
                groupList.shuffle(nChoices + 1);
                // Fill the result array
                Iterator<String> iter = groupList.iterator();
                while (iter.hasNext() && retVal.size() < nChoices) {
                    String word = iter.next();
                    if (! word.contentEquals(genomeWord))
                        retVal.add(word);
                }
            }
            return retVal;
        }

        @Override
        public int hashCode() {
            int retVal = ((this.keyName == null) ? 0 : this.keyName.hashCode());
            return retVal;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TaxonKey)) {
                return false;
            }
            TaxonKey other = (TaxonKey) obj;
            if (this.keyName == null) {
                if (other.keyName != null) {
                    return false;
                }
            } else if (!this.keyName.equals(other.keyName)) {
                return false;
            }
            return true;
        }

    }

    @Override
    protected boolean validateParms() throws IOException, ParseFailureException {
        // Validate the choice count.
        if (this.nChoices < 1)
            throw new ParseFailureException("Number of genomes to display per group must be positive.");
        // Validate the input directory.
        if (! this.inDir.isDirectory())
            throw new FileNotFoundException("Input directory " + this.inDir + " is not found or invalid.");
        // Now find the genome files.
        MasterGenomeDir gDirs = new MasterGenomeDir(this.inDir);
        if (gDirs.size() <= 0)
            throw new IOException("Input directory " + this.inDir + " has no genome subdirectories.");
        this.genomeFiles = gDirs.stream().map(x -> new File(x, "genome.json")).collect(Collectors.toList());
        final int gTotal = this.genomeFiles.size();
        log.info("{} genome files found in {}.", gTotal, this.inDir);
        // Create the groupings array.
        this.groupings = Arrays.stream(GROUPINGS).map(x -> new TaxonKey(x)).toArray(TaxonKey[]::new);
        // Set up the main data structures.
        int hashSize = gTotal * 4 / 3 + 1;
        this.fileMap = new HashMap<String, File>(hashSize);
        this.jsonMap = new HashMap<String, JsonObject>(hashSize);
        return true;
    }

    @Override
    protected void runCommand() throws Exception {
        // Loop through the genomes, loading the json objects and scanning the taxonomic groupings.
        final int nGenomes = this.genomeFiles.size();
        int gCount = 0;
        int taxCount = 0;
        int errorCount = 0;
        for (File gFile : this.genomeFiles) {
            gCount++;
            log.info("Loading genome {} of {} from {}.", gCount, nGenomes, gFile);
            String jsonString = FileUtils.readFileToString(gFile, Charset.defaultCharset());
            JsonArray jsonList = Jsoner.deserialize(jsonString, EMPTY_ARRAY);
            if (jsonList.size() < 1) {
                log.error("No data found in {}.", gFile);
                errorCount++;
            } else {
                // Get the genome's JSON object and extract the ID and the magic-word identifier.
                JsonObject genomeJson = (JsonObject) jsonList.get(0);
                String genomeId = genomeJson.getStringOrDefault(SpecialKey.GENOME_ID);
                String genomeWord = genomeJson.getStringOrDefault(SpecialKey.GENOME_WORD);
                if (genomeWord == null) {
                    log.error("No genome identifier word found for {}.", genomeId);
                    errorCount++;
                } else {
                    log.info("Processing genome {} with identifier {}.", genomeId, genomeWord);
                    // Store the file and JSON associations.
                    this.fileMap.put(genomeId, gFile);
                    this.jsonMap.put(genomeId, genomeJson);
                    // Now process each taxonomic grouping.
                    for (TaxonKey grouping : this.groupings) {
                        String groupName = genomeJson.getString(grouping);
                        if (groupName != null) {
                            // Here there is a grouping at this level.
                            grouping.addGenome(groupName, genomeWord);
                            taxCount++;
                        }
                    }
                    log.info("{} genomes processed, {} taxonomic groupings stored, {} errors.", gCount, taxCount, errorCount);
                }
            }
        }
        // Now we update the genomes with the taxonomic information.
        gCount = 0;
        taxCount = 0;
        for (var fileEntry : this.fileMap.entrySet()) {
            String genomeId = fileEntry.getKey();
            File gFile = fileEntry.getValue();
            log.info("Writing genome {} to file {}.", genomeId, gFile);
            JsonObject genomeJson = this.jsonMap.get(genomeId);
            String genomeWord = genomeJson.getStringOrDefault(SpecialKey.GENOME_WORD);
            gCount++;
            boolean changes = false;
            for (TaxonKey taxKey : this.groupings) {
                String groupName = genomeJson.getStringOrDefault(taxKey);
                if (groupName != null) {
                    JsonArray neighbors = taxKey.getNeighbors(groupName, genomeWord, this.nChoices);
                    if (! neighbors.isEmpty()) {
                        genomeJson.put(taxKey.getKey() + "_neighbors", neighbors);
                        taxCount++;
                        changes = true;
                    }
                }
            }
            if (changes) {
                // Here we need to update the json.
                JsonArray outJson = new JsonArray();
                outJson.add(genomeJson);
                String jsonString = Jsoner.prettyPrint(Jsoner.serialize(outJson));
                FileUtils.writeStringToFile(gFile, jsonString, Charset.defaultCharset());
            }
            log.info("{} genomes processed, {} neighborhoods output.", gCount, taxCount);
        }
    }

}
