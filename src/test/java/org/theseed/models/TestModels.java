/**
 *
 */
package org.theseed.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * @author Bruce Parrello
 *
 */
class TestModels {

    @Test
    void testSmallModel() throws JsonException, IOException {
        File modelFile = new File("data", "1266996.3.json");
        JsonObject modelJson;
        try (FileReader modelReader = new FileReader(modelFile)) {
            modelJson = (JsonObject) Jsoner.deserialize(modelReader);
        }
        Model myModel = new Model(modelJson);
        assertThat(myModel.getBadCompoundCount(), equalTo(0));
        assertThat(myModel.getBadReactionCount(), equalTo(0));
        Map<String, Compound> compounds = myModel.getCompounds();
        assertThat(compounds.size(), equalTo(10));
        Compound c = compounds.get("cpd00008_c0");
        assertThat(c, not(nullValue()));
        assertThat(c.getName(), equalTo("ADP [Cytosol]"));
        assertThat(c.getFormula(), equalTo("C10H13N5O10P2"));
        c = compounds.get("cpd00096_c0");
        assertThat(c, not(nullValue()));
        assertThat(c.getName(), equalTo("CDP [Cytosol]"));
        assertThat(c.getFormula(), equalTo("C9H13N3O11P2"));
        c = compounds.get("cpd00067_e0");
        assertThat(c, not(nullValue()));
        assertThat(c.getName(), equalTo("Phosphoenolpyruvate [Extracellular]"));
        assertThat(c.getFormula(), equalTo("C3H2O6P"));
        Map<String, Reaction> reactions = myModel.getReactions();
        assertThat(reactions.size(), equalTo(2));
        Reaction r = reactions.get("rxn00836_c0");
        assertThat(r, not(nullValue()));
        assertThat(r.getName(), equalTo("IMP:diphosphate phospho-D-ribosyltransferase [Cytosol]"));
        assertThat(r.getGeneRule(), equalTo("fig|1266996.3.peg.441"));
        assertThat(r.getFormula(), equalTo("2*HO7P2 + H + C10H11N4O8P --> C5H9O14P3 + C5H4N4O"));
        assertThat(r.getReactants(), containsInAnyOrder("PPi [Cytosol]", "H+ [Cytosol]",
                "IMP [Cytosol]"));
        assertThat(r.getProducts(), containsInAnyOrder("PRPP [Cytosol]", "HYXN [Cytosol]"));
        JsonObject rJson = r.toJson();
        assertThat(rJson.get("id"), equalTo("rxn00836_c0"));
        assertThat(rJson.get("name"), equalTo("IMP:diphosphate phospho-D-ribosyltransferase [Cytosol]"));
        assertThat(rJson.get("gene_rule"), equalTo("fig|1266996.3.peg.441"));
        assertThat(rJson.get("formula"), equalTo("2*HO7P2 + H + C10H11N4O8P --> C5H9O14P3 + C5H4N4O"));
        JsonArray reactants = (JsonArray) rJson.get("reactants");
        assertThat(reactants, containsInAnyOrder("PPi [Cytosol]", "H+ [Cytosol]",
                "IMP [Cytosol]"));
        JsonArray products = (JsonArray) rJson.get("products");
        assertThat(products, containsInAnyOrder("PRPP [Cytosol]", "HYXN [Cytosol]"));

    }

}
