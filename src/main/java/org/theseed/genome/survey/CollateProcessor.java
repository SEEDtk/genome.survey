/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package org.theseed.genome.survey;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.MasterGenomeDir;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This command takes randomly-selected CDS features from a genome JSON dump and outputs them in a single JSON file. This is used to
 * obtain feature data for various testing purposes.
 * 
 * The positional parameter should be the name of the input genome JSON dump directory.
 * 
 * The command-line options are as follows.
 * 
 * -h   display command-line usage information
 * -v   display more detailed log messages
 * -o   output file name (if not STDOUT)
 * -n   number of features to select (default 100) 
 *
 * @author Bruce Parrello
 */

public class CollateProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(CollateProcessor.class);
    /** genome subdirectories */
    private MasterGenomeDir genomeDirs;

    // COMMAND-LINE OPTIONS

    /** number of features to select */
    @Option(name = "--number", aliases = { "-n" }, usage = "number of features to select per genome")
    private int num;

    @Argument(index = 0, metaVar = "genomeDir", usage = "input genome JSON dump directory", required = true)
    private File genomeDir;

    @Override
    protected void setReporterDefaults() {
        this.num = 100;
    }

    /** 
     * This enum defines the important fields in a feature JSON object.
     */
    public static enum FeatureKeys implements JsonKey {
        TYPE("CDS"),
        PATRIC_ID("");

        private final Object m_value;

        FeatureKeys(final Object value) {
            this.m_value = value;
        }

        /** 
         * This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /** 
         * This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Validate the input directory.
        if (! this.genomeDir.isDirectory())
            throw new IOException("Genome JSON dump directory " + this.genomeDir + " is not found or invalid.");
        // Validate the feature count.
        if (this.num < 1)
            throw new ParseFailureException("Number of features to select must be at least 1.");
        // Get access to the genome subdirectories.
        this.genomeDirs = new MasterGenomeDir(this.genomeDir);
        log.info("Processing {} genomes from {}.", this.genomeDirs.size(), this.genomeDir);
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // This array will hold the collected features.
        JsonArray featureArray = new JsonArray();
        // Loop through the genomes.
        int gCount = 0;
        int gSkipped = 0;
        int featOut = 0;
        for (File genomeSubDir : this.genomeDirs) {
            // Get the genome feature JSON file.
            File genomeFile = new File(genomeSubDir, "genome_feature.json");
            if (! genomeFile.canRead()) {
                log.warn("Genome feature file {} is not found or unreadable--skipping genome.", genomeFile);
                gSkipped++;
            } else {
                gCount++;
                log.info("Processing genome {}.", genomeSubDir.getName());
                // Select features from this genome.
                JsonArray genomeFeatures = this.readFeatures(genomeFile);
                // Add the features to our output array.
                featureArray.addAll(genomeFeatures);
                featOut += genomeFeatures.size();
                log.info("Selected {} features from genome {}. {} total", genomeFeatures.size(), genomeSubDir.getName(), featOut);
            }
        }
        // Write the output.
        log.info("Writing {} features from {} genomes ({} skipped).", featOut, gCount, gSkipped);
        Jsoner.serialize(featureArray, writer);
        log.info("Output written.");
    }

    /**
     * Select the specified number of CDS features from the named genome_feature dump.
     * 
     * @param genomeFile    the genome_feature.json file
     * 
     * @return the selected features in a JSON array
     * 
     * @throws IOException, JsonException
     */
    private JsonArray readFeatures(File genomeFile) throws IOException, JsonException {
        JsonArray retVal = new JsonArray();
        // Read in all the features.
        try (FileReader featReader = new FileReader(genomeFile)) {
            JsonArray features = (JsonArray) Jsoner.deserialize(featReader);
            log.info("Found {} features in file {}.", features.size(), genomeFile);
            // Remove the non-CDS features. This includes features without PATRIC IDs.
            Iterator<Object> iter = features.iterator();
            while (iter.hasNext()) {
                JsonObject feat = (JsonObject) iter.next();
                String type = feat.getStringOrDefault(FeatureKeys.TYPE);
                String fid = feat.getStringOrDefault(FeatureKeys.PATRIC_ID);
                if (! type.equals("CDS") || fid.isBlank())
                    iter.remove();
            }
            log.info("Selecting from {} PATRIC CDS features.", features.size());
            // Select the desired number of features.
            Collections.shuffle(features);
            final int n = Math.min(this.num, features.size());
            for (int i = 0; i < n; i++)
                retVal.add(features.get(i));
        }
        return retVal;
    }

}
