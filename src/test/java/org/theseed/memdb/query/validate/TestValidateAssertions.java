package org.theseed.memdb.query.validate;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;


public class TestValidateAssertions {

    @Test
    public void testValidationAssertions() throws IOException, JsonException, ParseFailureException {
        // Get the parameter object for the first test.
        File jsonFile = new File("data", "genome_parms1.json");
        JsonObject parms = (JsonObject) Jsoner.deserialize(new FileReader(jsonFile));
        // Open the input file.
        File inFile = new File("data", "genome_test1.tbl");
        try (TabbedLineReader inStream = new TabbedLineReader(inFile)) {
            // Create the assertions. Each input line has a different genome name. "Actibacterium" will match the
            // EQ assertion and fail the others. Only "Pseudomonas" will match the LT assertion, and it will
            // also be the only one to fail the GT assertion.
            ValidationAssertion eqAssert = ValidationAssertion.Type.EQ.create(inStream, "genome.genus", "Genome.1");
            ValidationAssertion ltAssert = ValidationAssertion.Type.LT.create(inStream, "genome.hypothetical_cds", "Genome.2");
            ValidationAssertion gtAssert = ValidationAssertion.Type.GT.create(inStream, "genome.hypothetical_cds", "Genome.2");
            // Now we process the input file.
            for (var line : inStream) {
                String name = line.get(0);
                if (name.startsWith("Actibacterium")) {
                    assertThat(name, eqAssert.validate(line, parms), is(true));
                } else {
                    assertThat(name, eqAssert.validate(line, parms), is(false));
                }
                if (name.startsWith("Pseudomonas")) {
                    assertThat(name, ltAssert.validate(line, parms), is(true));
                    assertThat(name, gtAssert.validate(line, parms), is(false));
                } else {
                    assertThat(name, ltAssert.validate(line, parms), is(false));
                    assertThat(name, gtAssert.validate(line, parms), is(true));
                }
            }
        }
        // For the second test, there are three lines and two assertions. The first line should pass both assertions,
        // the second should fail both, and the third should pass the first (which is where we test lists) and fail the
        // second.
        jsonFile = new File("data", "genome_parms2.json");
        parms = (JsonObject) Jsoner.deserialize(new FileReader(jsonFile));
        inFile = new File("data", "genome_test2.tbl");
        try (TabbedLineReader inStream = new TabbedLineReader(inFile)) {
            ValidationAssertion domainAssert = ValidationAssertion.Type.EQ.create(inStream, "genome.superkingdom", "Genome.1");
            ValidationAssertion geneAssert = ValidationAssertion.Type.EQ.create(inStream, "feature.gene", "Feature.1");
            TabbedLineReader.Line line = inStream.next();
            assertThat(domainAssert.validate(line, parms), is(true));
            assertThat(geneAssert.validate(line, parms), is(true));
            line = inStream.next();
            assertThat(domainAssert.validate(line, parms), is(false));
            assertThat(geneAssert.validate(line, parms), is(false));
            line = inStream.next();
            assertThat(domainAssert.validate(line, parms), is(true));
            assertThat(geneAssert.validate(line, parms), is(false));
        }

    }

}
