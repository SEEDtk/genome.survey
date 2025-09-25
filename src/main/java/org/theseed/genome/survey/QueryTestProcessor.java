package org.theseed.genome.survey;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.BaseReportProcessor;
import org.theseed.basic.ParseFailureException;

/**
 * This command looks at the JSON output from QueryGenProcessor and makes database calls to verify that the answers are correct
 * and the distractors are not. It makes use of test instructions embedded in the question template file as comments.
 * The test instructions always beging with "#Test" and end with a comment line that is just "#". The instruction lines themselves
 * all begin with a space after the comment mark, a command name, and then parameters.
 * 
 * The first instruction line identifies the answer field, and consists of the word "answers" and then a column label to assign to
 * the answer column. This is used to build a one-column input file. The next several lines should have the "list" command, and
 * contain parameters to be passed to QueryListProcessor. Each list command takes the previous output file as input and produces a
 * new one. The final output file is then validated against relational assertions. At least one output line should have the correct
 * answer in the first column and should satisfy all the assertions. No line with a distractor in the first column should satisfy
 * all the assertions. The maximum number of assertions satisfied by a line with a given value in the first column is output, and
 * the correct answer is identified. We want the distractors to have at least one, but less than the total, all though a maximum
 * of zero for a distractor is acceptable, since it is sometimes impossible to find a distractor that is close to the answer.
 * 
 * The positional parameters are the name of the query specification file and the name of the JSON file to be validated. Both files will
 * be read fully into memory, so they should not be too large. This is because the ordering of the two files is unpredictable, and
 * this process is slow enough without having to do multiple passes through the JSON file. That said, the query specification is generally
 * under 20K.
 * 
 * The command-line options are as follows:
 * 
 * -h	display command-line usage
 * -v	display more detailed log messages
 * -o   output file for report (if not STDOUT)
 * 
 */
public class QueryTestProcessor extends BaseReportProcessor {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(QueryTestProcessor.class);
    /** query processor for accessing database */
    // COMMAND-LINE OPTIONS

    /** query specification file */
    @Argument(index = 0, metaVar = "querySpecFile", usage = "query specification file", required = true)
    private File querySpecFile;

    /** JSON file to validate */
    @Argument(index = 1, metaVar = "jsonFile", usage = "query JSON file to validate", required = true)
    private File jsonFile;

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
