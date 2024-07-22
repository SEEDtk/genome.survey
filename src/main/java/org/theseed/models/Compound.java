/**
 *
 */
package org.theseed.models;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This object describes a compound from the models.
 *
 * @author Bruce Parrello
 *
 */
public class Compound {

    // FIELDS
    /** compound ID */
    private String cpdId;
    /** compartment name */
    private String compartment;
    /** compound formula */
    private String formula;
    /** compound name */
    private String name;
    /** pattern for matching a compartment specification */
    private static final Pattern COMPARTMENT_SPEC = Pattern.compile("\\[(\\w+)\\]$");

    /** This enum defines the keys used and their default values.
     */
    public static enum CompoundKeys implements JsonKey {
        ID("missing"),
        NAME(null),
        COMPARTMENT(""),
        FORMULA("unknown");

        private final Object m_value;

        CompoundKeys(final Object value) {
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
     * Construct a compound from a JSON descriptor.
     *
     * @param json		json object describing the compound, from a model dump
     * @param cMap		map of compartment IDs to name
     *
     * @throws IOException
     */
    public Compound(JsonObject json, Map<String, String> cMap) throws IOException {
        this.cpdId = json.getStringOrDefault(CompoundKeys.ID);
        this.formula = json.getStringOrDefault(CompoundKeys.FORMULA);
        // Get the compartment.  We need to translate it to a name.
        String compartmentId = json.getStringOrDefault(CompoundKeys.COMPARTMENT);
        this.compartment = cMap.get(compartmentId);
        if (this.compartment == null)
            throw new IOException("Invalid compartment ID \"" + compartmentId + "\" in compound definition for " + this.cpdId + ".");
        // Get the compound name.
        this.name = json.getStringOrDefault(CompoundKeys.NAME);
        if (this.name == null)
            this.name = this.cpdId;
        else {
            // Here we have a real compound name, so we must translate the compartment ID.
            this.name = Compound.fixCompartment(this.name, cMap);
        }
    }

    /**
     * @return the compound ID
     */
    public String getId() {
        return this.cpdId;
    }

    /**
     * @return the compartment
     */
    public String getCompartment() {
        return this.compartment;
    }

    /**
     * @return the compound formula
     */
    public String getFormula() {
        return this.formula;
    }

    /**
     * @return the compound name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Replace a compartment ID indicator with the compartment name.  The compartment ID indicator
     * is always last in the string and is enclosed in square brackets.
     *
     * @param nameString	name string containing the compartment ID indicator
     * @param cMap			map from compartment IDs to compartment names
     */
    public static String fixCompartment(String nameString, Map<String, String> cMap) {
        Matcher m = COMPARTMENT_SPEC.matcher(nameString);
        String retVal = nameString;
        if (m.find()) {
            String compartmentId = m.group(1);
            String cName = cMap.getOrDefault(compartmentId, compartmentId);
            retVal = StringUtils.substring(nameString, 0, m.start()) + "[" + cName + "]";
        }
        return retVal;
    }
}
