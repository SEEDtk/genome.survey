package org.theseed.genome.survey;

import java.io.IOException;
import java.io.PrintWriter;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;
import org.theseed.utils.BasePipeProcessor;

public class QuestionAnalysisProcessor extends BasePipeProcessor {

    @Override
    protected void setPipeDefaults() {
        // TODO question analysis defaults
        throw new UnsupportedOperationException("Unimplemented method 'setPipeDefaults'");
    }

    @Override
    protected void validatePipeInput(TabbedLineReader inputStream) throws IOException {
        // TODO we want "question" and "query" columns
        throw new UnsupportedOperationException("Unimplemented method 'validatePipeInput'");
    }

    @Override
    protected void validatePipeParms() throws IOException, ParseFailureException {
        // TODO validate any parameters needed for question analysis
        throw new UnsupportedOperationException("Unimplemented method 'validatePipeParms'");
    }

    @Override
    protected void runPipeline(TabbedLineReader inputStream, PrintWriter writer) throws Exception {
        // TODO build JSON output from the input questions and the query results
        throw new UnsupportedOperationException("Unimplemented method 'runPipeline'");
    }

}
