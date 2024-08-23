/**
 *
 */
package org.theseed.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;



/**
 * This object represents a single reaction.  It converts the incoming reaction JSON to a form that can be
 * used to generate template output.  This involves translating compounds and creating formulas.
 *
 * @author Bruce Parrello
 *
 */
public class Reaction {

    // FIELDS
    /** genome ID */
    private String genomeId;
    /** reaction ID */
    private String reactId;
    /** reaction name */
    private String name;
    /** list of reactant names */
    private List<String> reactants;
    /** list of product names */
    private List<String> products;
    /** set of triggering feature ids */
    private Set<String> features;
    /** chemical formula */
    private String formula;
    /** triggering rule */
    private String geneRule;
    /** empty JSON hash */
    private static final JsonObject EMPTY_MAP = new JsonObject();
    /** pattern for finding FIG IDs */
    private static final Pattern FID_PATTERN = Pattern.compile("fig\\|\\d+\\.\\d+\\.\\w+\\.\\d+");

    /** This enum defines the keys used and their default values.
     */
    public static enum ReactionKeys implements JsonKey {
        ID("missing"),
        NAME(null),
        GENE_REACTION_RULE(""),
        METABOLITES(EMPTY_MAP);

        private final Object m_value;

        ReactionKeys(final Object value) {
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

    protected static class IntegerKey implements JsonKey {

        /** key name */
        String keyName;

        /**
         * Specify a new key name.
         *
         * @param key	new key name
         */
        public void setKey(String key) {
            this.keyName = key;
        }

        @Override
        public String getKey() {
            return this.keyName;
        }

        @Override
        public Object getValue() {
            return (Integer) 0;
        }

    }

    /**
     * Create a new reaction descriptor from the JSON input.
     *
     * @param json			JSON object containing the reaction data
     * @param compartMap	map of compartment IDs to names
     * @param compMap		map of compound IDs to compound descriptors
     * @param genome		ID of the genome in which the reaction occurs
     *
     * @throws IOException
     */
    public Reaction(JsonObject json, Map<String, String> compartMap, Map<String, Compound> compMap, String genome) throws IOException {
        // Get the ID.
        this.reactId = json.getStringOrDefault(ReactionKeys.ID);
        // Get the name.
        this.name = json.getStringOrDefault(ReactionKeys.NAME);
        if (this.name == null)
            this.name = this.reactId;
        else {
            // Here we have a real reaction name.  Fix the compartment ID.
            this.name = Compound.fixCompartment(this.name, compartMap);
        }
        // Get the gene rule and parse out the feature IDs.
        this.geneRule = json.getStringOrDefault(ReactionKeys.GENE_REACTION_RULE);
        this.features = new TreeSet<String>();
        Matcher m = FID_PATTERN.matcher(this.geneRule);
        while (m.find())
            this.features.add(m.group());
        // Now get the metabolites.
        JsonObject metabolites = json.getMapOrDefault(ReactionKeys.METABOLITES);
        if (metabolites.isEmpty())
            throw new IOException("No metabolites found in reaction " + this.reactId + ".");
        else {
            // We need to separate out the reactants and the products.
            this.reactants = new ArrayList<String>(metabolites.size());
            this.products = new ArrayList<String>(metabolites.size());
            // We will build the formula in here.
            StringBuilder reactantBuffer = new StringBuilder(metabolites.size() * 10);
            StringBuilder productBuffer = new StringBuilder(metabolites.size() * 10);
            // This will be used to access metabolites.
            IntegerKey metaboliteKey = new IntegerKey();
            // Now loop through the metabolites.  The metabolite maps compound IDs to stoichiometry numbers.
            // A negative number is a reactant and a positive number is a product.
            List<String> compounds = metabolites.keySet().stream().sorted().collect(Collectors.toList());
            for (String compoundId : compounds) {
                // Get the compound object.
                Compound compound = compMap.get(compoundId);
                if (compound == null)
                    throw new IOException("Invalid compound \"" + compoundId + "\" found in reaction " + this.reactId + ".");
                // Get the stoichiometric number.
                metaboliteKey.setKey(compoundId);
                int num = metabolites.getIntegerOrDefault(metaboliteKey);
                if (num < 0) {
                    this.storeCompound(reactantBuffer, -num, compound);
                    this.reactants.add(compound.getName());
                } else if (num > 0) {
                    this.storeCompound(productBuffer, num, compound);
                    this.products.add(compound.getName());
                }
            }
            // Now, assemble the formula.
            this.formula = reactantBuffer.toString() + " --> " + productBuffer.toString();
        }
        // Finally, store the genome ID.
        this.genomeId = genome;
    }

    /**
     * Store a compound in one of the formula buffers.
     *
     * @param buffer		output buffer for compound
     * @param num			stoichiometry number
     * @param compound		compound descriptor
     */
    private void storeCompound(StringBuilder buffer, int num, Compound compound) {
        if (buffer.length() > 0)
            buffer.append(" + ");
        if (num > 1) {
            buffer.append(num);
            buffer.append("*");
        }
        buffer.append(compound.getFormula());
    }

    /**
     * @return the reaction ID
     */
    public String getId() {
        return this.reactId;
    }

    /**
     * @return the reaction name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the reactant list
     */
    public List<String> getReactants() {
        return this.reactants;
    }

    /**
     * @return the product list
     */
    public List<String> getProducts() {
        return this.products;
    }

    /**
     * @return the formula
     */
    public String getFormula() {
        return this.formula;
    }

    /**
     * @return the geneRule
     */
    public String getGeneRule() {
        return this.geneRule;
    }

    /**
     * @return a JSON representation of this reaction
     */
    public JsonObject toJson() {
        JsonObject retVal = new JsonObject();
        // Store the two compound lists.
        this.storeList(retVal, "reactants", this.getReactants());
        this.storeList(retVal, "products", this.getProducts());
        // Store the string values.
        retVal.put("id", this.getId());
        retVal.put("formula", this.getFormula());
        retVal.put("gene_rule", this.getGeneRule());
        retVal.put("name", this.getName());
        retVal.put("genome_id", this.genomeId);
        return retVal;
    }

    /**
     * Store a list of compound names in the output JsonObject.
     *
     * @param json			json object in which to store the compound name list
     * @param key			key to give to the list
     * @param compoundList	list to store
     */
    private void storeList(JsonObject json, String key, List<String> compoundList) {
        JsonArray compoundJson = new JsonArray();
        compoundJson.addAll(compoundList);
        json.put(key, compoundJson);
    }

    /**
     * @return the list of triggering features
     */
    public Collection<String> getFeatures() {
        return this.features;
    }

}
