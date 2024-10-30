/**
 *
 */
package org.theseed.json.clean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * @author Bruce Parrello
 *
 */
class TestJsonCleaners implements JsonCleaner.IParms {

    static final String TESTJSON = "{\"aa_sequence_md5\": \"accd66cb0af2e06e8108425f268ba767\", \"date_inserted\": \"2021-07-27T13:04:21.337Z\", \"date_modified\": \"2021-07-27T13:04:21.337Z\", \"description\": \"consensus disorder prediction\", \"e_value\": \"-\", \"end\": 25, \"evidence\": \"InterProScan\", \"feature_id\": \"PATRIC.11191.93.NC_001552.CDS.6693.8420.fwd\", \"gene\": \"HN\", \"genome_id\": \"11191.93\", \"genome_name\": \"Murine respirovirus Ohita\", \"id\": \"080316b1-bbf1-4123-b868-1d4581f5e37c\", \"interpro_description\": \"-\", \"interpro_id\": \"-\", \"patric_id\": \"fig|11191.93.CDS.8\", \"product\": \"hemagglutinin-neuraminidase protein\", \"refseq_locus_tag\": \"SeVgp8\", \"source\": \"MobiDBLite\", \"source_id\": \"mobidb-lite\", \"start\": 1, \"taxon_id\": 11191, \"_version_\": 1809292188504293405}";

    @Test
    void testJsonRecord() throws JsonException {
        JsonCleaner hCleaner = JsonCleaner.Type.HYPHENS.create(this);
        JsonObject myJson = (JsonObject) Jsoner.deserialize(TESTJSON);
        int oldSize = myJson.size();
        hCleaner.process(myJson);
        // Assure the three fields we know are hyphens are gone.
        assertThat(myJson.size(), equalTo(oldSize - 3));
        assertThat(myJson.get("e_value"), nullValue());
        assertThat(myJson.get("interpro_description"), nullValue());
        assertThat(myJson.get("interpro_id"), nullValue());
        // Spot check other fields to make sure they aren't changed.
        assertThat(myJson.get("genome_id"), equalTo("11191.93"));
        assertThat(myJson.get("end"), equalTo(new BigDecimal(25)));
        assertThat(myJson.get("date_inserted"), equalTo("2021-07-27T13:04:21.337Z"));
    }

}
