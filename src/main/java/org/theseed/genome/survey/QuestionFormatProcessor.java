package org.theseed.genome.survey;

import java.io.IOException;
import java.io.PrintWriter;

import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;

/**
 * This command looks at a question response file produced by the LLM testing facility and extracts the responses in a more readable format.
 * Each section of the output is headed by the question index and text, followed by the response text with escapes converted back to normal
 * characters. This is followed by three dashes and a blank line. This makes the result more readable, and the file can be scrolled in parallel
 * with the original JSON while evaluating the responses.
 * 
 * The single positional parameter is the name of the input JSON file. The output is written to standard output.
 * 
 * The input file is expected to be a JSON array of objects, each of which has the following fields:
 * 
 * index        the question index
 * question     the question text
 * response     the response text, with escapes
 * 
 * The command-line options are as follows:
 */
public class QuestionFormatProcessor extends BaseReportProcessor {

    @Override
    protected void setReporterDefaults() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setReporterDefaults'");
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'validateReporterParms'");
    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'runReporter'");
    }

}
