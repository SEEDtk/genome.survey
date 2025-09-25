package org.theseed.memdb.query.validate;

import java.util.List;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * A query validator contains a set of questions from the JSON output of QueryGenProcessor along with the validation instructions.
 * The validation instructions are used to query the database and then verify that the answers and distractors for each question are correct.
 * The validators are keyed by template string.
 * 
 * Key fields in the question JSON include:
 * 
 * template         the template string used to generate the question
 * parameters       the parameters used to characterize the question
 * question         the text of the question itself
 * correct_answer   the correct answer to the question
 * distractors      a list of incorrect answers to the question
 * 
 * The validation instructions consist of a name to give to the answer column of the output file, a list of QueryListProcessor parameters
 * (excluding the input and output file names), and a list of relational assertions to be applied to the final output file.
 * 
 * Each relational assertion consists of a relational operator (currently "eq", "lt", or "gt"), a column label from the output file, and
 * a parameter specification. The parameter specification consists of an entity name, a period, and a 1-based position in the parameterization
 * list for the entity. The parameter specification is replaced by the actual parameter value from the question being validated. The column
 * label is replaced by the column value from the current output file line. Note that "eq" is a case-insensitive string equality test, while "lt" and "gt"
 * are numeric comparisons.
 * 
 */
public class QueryValidator implements Comparable<QueryValidator> {

    // FIELDS
    /** template string */
    private String template;
    /** list of questions for this template */
    private List<JsonObject> questions;
    /** query parameter lists */
    private List<List<String>> queryParms;
    /** list of validation assertions */
    private List<ValidationAssertion> assertions;
    /** delimiter used to flatten lists in the query output */
    public static final String DELIM = "::";

    // TODO constructor and methods

    @Override
    public int compareTo(QueryValidator o) {
        return this.template.compareTo(o.template);
    }

}
