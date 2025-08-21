/**
 *
 */
package org.theseed.reports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * @author Bruce Parrello
 *
 */
class TestFieldCounter {

    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(TestFieldCounter.class);

    @Test
    void testFieldCounter() throws JsonException, IOException {
        Map<String, FieldCounter> countMap = new TreeMap<String, FieldCounter>();
        File jsonFile = new File("data", "test_fields.json");
        FileReader fileReader = new FileReader(jsonFile);
        JsonArray json = (JsonArray) Jsoner.deserialize(fileReader);
        for (var recordObj : json) {
            JsonObject record = (JsonObject) recordObj;
            for (var fieldEntry : record.entrySet()) {
                String fieldName = fieldEntry.getKey();
                Object field = fieldEntry.getValue();
                FieldCounter counter = countMap.computeIfAbsent(fieldName, x -> new FieldCounter());
                counter.count(field);
            }
        }
        log.info(StringUtils.rightPad("name", 20) + " " + FieldCounter.header());
        for (var countEntry : countMap.entrySet()) {
            log.info(StringUtils.rightPad(countEntry.getKey(), 20) + " " + countEntry.getValue().getResults());
        }
        FieldCounter test = countMap.get("id");
        assertThat(test.getBlankCount(), equalTo(3));
        assertThat(test.getIntegerCount(), equalTo(1));
        assertThat(test.getOtherCount(), equalTo(2));
        assertThat(test.getFillCount(), equalTo(7));
        test = countMap.get("reactants");
        assertThat(test.getListCount(), equalTo(10));
        test = countMap.get("products");
        assertThat(test.getBlankCount(), equalTo(1));
        assertThat(test.getListCount(), equalTo(10));
        test = countMap.get("gene_rule");
        assertThat(test.getFillCount(), equalTo(9));
        assertThat(test.getStringCount(), equalTo(9));
    }

}
