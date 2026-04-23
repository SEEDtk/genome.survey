package org.theseed.genome.survey;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;


public class TestCommandLineParse {

    @Test
    void cmdLineParseTest() {
        String line = "get subsystem_item subsystem_name --eq \"genome_name,Lactobacillus hilgardii ATCC 8290\" --eq gene,glnA2";
        List<String> parms = QuestionAnalysisProcessor.parseQueryLine(line);
        assertThat(parms.size(), equalTo(6));
        assertThat(parms.get(0), equalTo("subsystem_item"));
        assertThat(parms.get(1), equalTo("subsystem_name"));
        assertThat(parms.get(2), equalTo("--eq"));
        assertThat(parms.get(3), equalTo("genome_name,Lactobacillus hilgardii ATCC 8290"));
        assertThat(parms.get(4), equalTo("--eq"));
        assertThat(parms.get(5), equalTo("gene,glnA2"));
        line = "pipe genome genome_id --eq genus,Polycyclovorans == subsystem_item genome_id genome_name --eq \"subsystem_name,Glutathione: \\\"Redox\\\" cycle\"";
        parms = QuestionAnalysisProcessor.parseQueryLine(line);
        assertThat(parms.size(), equalTo(10));
        assertThat(parms.get(0), equalTo("genome"));
        assertThat(parms.get(1), equalTo("genome_id"));
        assertThat(parms.get(2), equalTo("--eq"));
        assertThat(parms.get(3), equalTo("genus,Polycyclovorans"));
        assertThat(parms.get(4), equalTo("=="));
        assertThat(parms.get(5), equalTo("subsystem_item"));
        assertThat(parms.get(6), equalTo("genome_id"));
        assertThat(parms.get(7), equalTo("genome_name"));
        assertThat(parms.get(8), equalTo("--eq"));
        assertThat(parms.get(9), equalTo("subsystem_name,Glutathione: \"Redox\" cycle"));
    }
}
