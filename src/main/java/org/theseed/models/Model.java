/**
 *
 */
package org.theseed.models;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This object represents a model.  We isolate the compartments, the compounds, and the reactions.
 *
 * @author Bruce Parrello
 *
 */
public class Model {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(Model.class);
    /** number of bad compounds */
    private int badCompoundCount;
    /** number of bad reactions */
    private int badReactionCount;
    /** compartment map */
    private Map<String, String> compartments;
    /** compound map */
    private Map<String, Compound> compounds;
    /** reaction map */
    private Map<String, Reaction> reactions;
    /** genome ID */
    private String genomeId;
    /** empty JSON list */
    private static final JsonArray EMPTY_LIST = new JsonArray();
    /** empty JSON hash */
    private static final JsonObject EMPTY_MAP = new JsonObject();

    /** This enum defines the keys used and their default values.
     */
    public static enum ModelKeys implements JsonKey {
        METABOLITES(EMPTY_LIST),
        REACTIONS(EMPTY_LIST),
        COMPARTMENTS(EMPTY_MAP),
        ID("unknown");

        private final Object m_value;

        ModelKeys(final Object value) {
            this.m_value = value;
        }

        /** This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /** This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    }

    /**
     * Construct a new model from a JSON object.
     *
     * @param json		JSON object containing the model definition
     */
    public Model(JsonObject json) {
        // Save the genome ID.
        this.genomeId = json.getStringOrDefault(ModelKeys.ID);
        // Start with the compartments.
        this.getCompartments(json);
        // Since we have the compartments, we can read the compounds.
        this.getCompounds(json);
        // Finally, we can read the reactions.
        this.getReactions(json);
    }

    /**
     * Extract the compartment map from a model JSON.
     *
     * @param json	JSON object containing the model definition
     */
    public void getCompartments(JsonObject json) {
        this.compartments = new TreeMap<String, String>();
        JsonObject compartmentJson = json.getMap(ModelKeys.COMPARTMENTS);
        for (var jsonEntry : compartmentJson.entrySet()) {
            String key = jsonEntry.getKey();
            String value = jsonEntry.getValue().toString();
            this.compartments.put(key, value);
        }
    }

    /**
     * Extract the compounds from a model JSON.
     *
     * @param json	JSON object containing the model definition
     */
    public void getCompounds(JsonObject json) {
        // Get the compound array from the JSON object.
        JsonArray compoundList = json.getCollectionOrDefault(ModelKeys.METABOLITES);
        // Create the compound map.
        this.compounds = new HashMap<String, Compound>(compoundList.size() * 4 / 3 + 1);
        // Loop through the compounds, converting.
        for (var compoundEntry : compoundList) {
            JsonObject compoundJson = (JsonObject) compoundEntry;
            try {
                Compound compound = new Compound(compoundJson, this.compartments);
                String compoundId = compound.getId();
                this.compounds.put(compoundId, compound);
            } catch (IOException e) {
                log.error(e.toString());
                this.badCompoundCount++;
            }
        }
    }

    /**
     * Extract the reactions from a model JSON.
     *
     * @param json	JSON object containing the model definition
     */
    public void getReactions(JsonObject json) {
        // Get the reaction array from the JSON object.
        JsonArray reactionList = json.getCollectionOrDefault(ModelKeys.REACTIONS);
        // Create the reaction map.
        this.reactions = new HashMap<String, Reaction>(reactionList.size() * 4 / 3 + 1);
        // Loop through the reactions, converting.
        for (var reactionEntry : reactionList) {
            JsonObject reactionJson = (JsonObject) reactionEntry;
            try {
                Reaction reaction = new Reaction(reactionJson, this.compartments, this.compounds, this.genomeId);
                String reactionId = reaction.getId();
                this.reactions.put(reactionId, reaction);
            } catch (IOException e) {
                log.error(e.toString());
                this.badReactionCount++;
            }
        }
    }

    /**
     * @return the number of compound-construction errors
     */
    public int getBadCompoundCount() {
        return this.badCompoundCount;
    }

    /**
     * @return the number reaction-construction errors
     */
    public int getBadReactionCount() {
        return this.badReactionCount;
    }

    /**
     * @return the compound map
     */
    public Map<String, Compound> getCompounds() {
        return this.compounds;
    }

    /**
     * @return the reaction map
     */
    public Map<String, Reaction> getReactions() {
        return this.reactions;
    }


}
