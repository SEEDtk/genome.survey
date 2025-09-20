/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseProcessor;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.MarkerFile;
import org.theseed.io.MasterGenomeDir;
import org.theseed.models.Model;
import org.theseed.models.Reaction;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;


/**
 * This subcommand processes a raw model dump and translates it into input files suitable for use in templates.
 * A json-format reaction data file will be produced for each genome.
 *
 * The positional parameters are the name of the model-dump input directory and the name of the genome template input directory, 
 * where the reaction files will be placed.
 *
 * The processing will be done from the genome template input directory (which is an output directory for us).
 * This insures that if a model file does not exist for a genome, a blank reaction file will still be created.
 *
 * The command-line options are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 *
 * --para	maximum number of parallel processes, or 0 for all available processors; default is maximum available
 *
 * @author Bruce Parrello
 *
 */
public class ModelDumpFixProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(ModelDumpFixProcessor.class);
    /** genome template input directory master */
    private MasterGenomeDir genomeDirs;
    /** number of genomes for which there is no model file */
    private int missingModelCount;
    /** number of reactions output */
    private int reactionCount;
    /** number of reactions in error */
    private int badReactionCount;
    /** number of compounds in error */
    private int badCompoundCount;
    /** number of models successfully processed */
    private int goodModelCount;
    /** custom thread pool for parallel processing */
    private ForkJoinPool threadPool;
    /** array of two booleans */
    private static final boolean[] BOOLS = new boolean[] { true, false };

    // COMMAND-LINE OPTIONS

    /** number of threads to use in parallel processing */
    @Option(name = "--para", metaVar = "60", usage = "maximum number of threads to run in parallel")
    private int maxThreads;

    /** input directory containing model files */
    @Argument(index = 0, metaVar = "modelDir", usage = "name of directory containing chemistry json files", required = true)
    private File modelDir;

    /** genome template directory, used to store reaction files */
    @Argument(index = 1, metaVar = "genomeDir", usage = "name of genome dump directory to contain reaction files", required = true)
    private File genomeDir;



    @Override
    protected void setDefaults() {
        this.maxThreads = Runtime.getRuntime().availableProcessors();
    }

    @Override
    protected void validateParms() throws IOException, ParseFailureException {
        // Verify the model directory.
        if (! this.modelDir.isDirectory())
            throw new IOException("Model directory " + this.modelDir + " is not found or invalid.");
        log.info("Model files will be retrieved from {}.", this.modelDir);
        // Verify the genome template directory.
        if (! this.genomeDir.isDirectory())
            throw new IOException("Genome directory " + this.genomeDir + " is not found or invalid.");
        // Get all the genome sub-directories.
        this.genomeDirs = new MasterGenomeDir(this.genomeDir);
        log.info("{} genome subdirectories found in {}.", this.genomeDirs.size(), this.genomeDir);
        // Validate the core count.
        if (this.maxThreads < 1)
            throw new ParseFailureException("Maximum number of threads must be positive.");
        int maxCores = Runtime.getRuntime().availableProcessors();
        if (this.maxThreads > 1 && this.maxThreads > maxCores) {
            log.warn("Too many threads specified:  reducing from {} to {}.", this.maxThreads, maxCores);
            this.maxThreads = maxCores;
        }
        // Create the custom thread pool.
        if (this.maxThreads == 1)
            this.threadPool = null;
        else {
            this.threadPool = new ForkJoinPool(this.maxThreads);
            log.info("Parallel processing selected with {} threads.", this.maxThreads);
        }
    }

    @Override
    protected void runCommand() throws Exception {
        // Clear the counters.
        this.missingModelCount = 0;
        this.badCompoundCount = 0;
        this.badReactionCount = 0;
        this.reactionCount = 0;
        this.goodModelCount = 0;
        // Are we parallelizing?
        if (this.threadPool == null) {
            // No, use a normal stream.
            this.genomeDirs.stream().forEach(x -> this.processModel(x));
        } else try {
            // Yes, use a parallel stream.
            this.threadPool.submit(() -> this.genomeDirs.parallelStream().forEach(x -> this.processModel(x))).get();
        } finally {
            this.threadPool.shutdown();
        }
        log.info("Processing complete.  {} missing models, {} bad compounds, {} bad reactions, {} reactions output for {} genomes.",
                this.missingModelCount, this.badCompoundCount, this.badReactionCount, this.reactionCount, this.genomeDirs.size());
   }

    /**
     * Process a single model file.
     *
     * @param gDir		genome directory to process
     *
     */
    private void processModel(File gDir) {
        String genomeId = gDir.getName();
        File modelFile = new File(this.modelDir, genomeId + ".json");
        // We write out the reactions, the triggers (reaction to feature) and the linkages (reaction to compound).
        File reactionFile = new File(gDir, "reactions.json");
        File triggerFile = new File(gDir, "triggers.json");
        File linkageFile = new File(gDir, "linkages.json");
        if (! modelFile.canRead()) {
            // No model file, so create an empty reaction list.
            log.info("No model file found for genome {}.", genomeId);
            MarkerFile.write(reactionFile, "[]");
            MarkerFile.write(triggerFile, "[]");
            synchronized (this) {
                this.missingModelCount++;
            }
        } else {
            log.info("Processing model file {}.", modelFile);
            // Read in the model Json.
            JsonObject modelJson;
            try (FileReader modelReader = new FileReader(modelFile)) {
                modelJson = (JsonObject) Jsoner.deserialize(modelReader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (JsonException e) {
                throw new RuntimeException(e);
            }
            // Construct the model object.
            Model model = new Model(modelJson);
            // Get the reactions and convert them to JSON. We will also create the trigger list
            // and the linkage list.
            JsonArray reactionJson = new JsonArray();
            JsonArray triggerJson = new JsonArray();
            JsonArray linkageJson = new JsonArray();
            Collection<Reaction> reactions = model.getReactions().values();
            for (Reaction reaction : reactions) {
                // Get the list of triggering features.
                Collection<String> fidList = reaction.getFeatures();
                // Set the simple flag.
                boolean simple = (fidList.size() <= 1);
                // Here we add the full reaction JSON to the output list.
                JsonObject reactionObject = reaction.toJson();
                reactionJson.add(reactionObject);
                // Now we connect the reaction to its triggering features.
                String reactionId = reaction.getId();
                String reactionName = reaction.getName();
                for (String fid : fidList) {
                    JsonObject trigger = new JsonObject();
                    trigger.put("reaction_id", reactionId);
                    trigger.put("patric_id", fid);
                    trigger.put("name", reactionName);
                    trigger.put("genome_id", genomeId);
                    trigger.put("gene_rule", reaction.getGeneRule());
                    trigger.put("simple", simple);
                    triggerJson.add(trigger);
                }
                // Finally we connect the reaction to its compounds. We have a kludgy
                // method here for getting both products and reactants.
                for (boolean isProduct : BOOLS) {
                    List<String> compounds = (isProduct ? reaction.getProducts() : reaction.getReactants());
                    for (String compound : compounds) {
                        JsonObject linkage = new JsonObject();
                        linkage.put("genome_id", genomeId);
                        linkage.put("reaction_id", reactionId);
                        linkage.put("reaction_name", reactionName);
                        linkage.put("product", isProduct);
                        linkage.put("cname", compound);
                        linkageJson.add(linkage);
                    }
                }
            }
            // Write the reactions to the output.
            try (PrintWriter writer = new PrintWriter(reactionFile)) {
                Jsoner.serialize(reactionJson, writer);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // Now we write out the trigger file, which connects features to reactions.
            try (PrintWriter writer = new PrintWriter(triggerFile)) {
                Jsoner.serialize(triggerJson, writer);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // Finally we write the linkage file, which links reactions to compounds.
            try (PrintWriter writer = new PrintWriter(linkageFile)) {
                Jsoner.serialize(linkageJson, writer);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // Update the counters.
            synchronized (this) {
                this.goodModelCount++;
                this.reactionCount += reactions.size();
                this.badCompoundCount += model.getBadCompoundCount();
                this.badReactionCount += model.getBadReactionCount();
                log.info("{} models complete.  {} reactions, {} bad reactions, {} bad compounds.", this.goodModelCount,
                        this.reactionCount, this.badCompoundCount, this.badReactionCount);
            }
        }
    }

}
