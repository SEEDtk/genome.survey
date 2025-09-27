package org.theseed.memdb.query.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;
import org.theseed.io.TabbedLineReader.Line;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * A query validator contains a set of questions from the JSON output of QueryGenProcessor along with the validation instructions.
 * The validation instructions are used to query the database and then verify that the answers and distractors for each question are correct.
 * The validators are keyed by template string.
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
    private final String template;
    /** query parameter lists */
    private final List<List<String>> queryParms;
    /** list of validation assertions */
    private final List<ValidationAssertion> assertions;
    /** delimiter used to flatten lists in the query output */
    public static final String DELIM = "::";

    /**
     * Construct the query validator for the specified template.
     * 
     * @param templateString    template string used to generate the questions
     */
    public QueryValidator(String templateString) {
        this.template = templateString;
        this.queryParms = new ArrayList<>(4);
        this.assertions = new ArrayList<>(4);
    }

    /**
     * Add a query parameter list.
     * 
     * @param parms     list of query parameters to add
     */
    public void addQueryCommand(List<String> parms) {
        this.queryParms.add(parms);
    }

    /**
     * Add a validation assertion.
     * 
     * @param relop         relational operator string
     * @param column        query result file column name
     * @param fieldSpec     parameter field specification (entityName.#)
     * 
     * @throws ParseFailureException 
     */
    public void addAssertion(String relop, String column, String fieldSpec) throws ParseFailureException {
        ValidationAssertion.Type relType = switch (relop) {
            case "eq" -> ValidationAssertion.Type.EQ;
            case "lt" -> ValidationAssertion.Type.LT;
            case "gt" -> ValidationAssertion.Type.GT;
            default -> null;
        };
        if (relType == null)
            throw new ParseFailureException("Invalid relational operator \"" + relop + "\".");
        ValidationAssertion assertion = relType.create(column, fieldSpec);
        this.assertions.add(assertion);
    }

    /**
     * Fix up the validation assertions for use.
     * 
     * @param inStream  query output file being used as input
     * 
     * @throws IOException 
     */
    public void fix(TabbedLineReader inStream) throws IOException {
        for (ValidationAssertion assertion : this.assertions) {
            assertion.fix(inStream);
        }
    }

    @Override
    public int compareTo(QueryValidator o) {
        return this.template.compareTo(o.template);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + Objects.hashCode(this.template);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QueryValidator other = (QueryValidator) obj;
        return Objects.equals(this.template, other.template);
    }

    /**
     * @return the number of queries we need to run for this validator
     */
    public int getQueryCount() {
        return this.queryParms.size();
    }

    /**
     * @return the number of assertions in this validator
     */
    public int getAssertionCount() {
        return this.assertions.size();
    }

    /**
     * @return an iterator through the query parameter lists
     */
    public Iterator<List<String>> getParmIterator() {
        return this.queryParms.iterator();
    }

    /**
     * Check an output line from the queries against the parameterization to see
     * how many assertions match.
     * 
     * @param line                  output line to check
     * @param parameterizations     parameterizations for the current question
     * 
     * @return the number of assertions that matched
     */
    public int checkLine(Line line, JsonObject parameterizations) {
        // Count the number of assertions that match.
        int retVal = 0;
        for (ValidationAssertion assertion : this.assertions) {
            boolean matched = assertion.validate(line, parameterizations);
            if (matched)
                retVal++;
        }
        return retVal;
    }

}
