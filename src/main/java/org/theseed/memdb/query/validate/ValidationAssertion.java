package org.theseed.memdb.query.validate;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This object represents an assertion used to validate a query output line. Each relational operator corresponds to a subclass.
 * The assertion is satisfied when the specified column value from the output line compares correctly to the identified parameter value.
 */
public abstract class ValidationAssertion {

    // FIELDS
    /** name of the column to check */
    private String columnName;
    /** index of the column to check */
    private int columnIndex;
    /** name of the entity containing the parameter to compare */
    private String entityName;
    /** position of the parameter in the entity's parameter list (0-based) */
    private int parameterIndex;
    
    public static enum Type {
        /** equality assertion */
        EQ {
            @Override
            public ValidationAssertion create(String column, String parameter) throws ParseFailureException {
                return new EqValidationAssertion(column, parameter);
            }
        }, 
        /** numeric less-than assertion */
        LT {
            @Override
            public ValidationAssertion create(String column, String parameter) throws ParseFailureException {
                return new LtValidationAssertion(column, parameter);
            }
        }, 
        /** numeric greater-than assertion */
        GT {
            @Override
            public ValidationAssertion create(String column, String parameter) throws ParseFailureException {
                return new GtValidationAssertion(column, parameter);
            }
        };

        /** Create a validation assertion of this type.
         * 
         * @param outStream     line reader for the output file (used to map column names to indices)
         * @param column	    name of the output file column to check
         * @param parameter	    parameter specification
         * 
         * @return a new validation assertion of the specified type
         * 
         * @throws ParseFailureException
         */
        public abstract ValidationAssertion create(String column, String parameter) throws ParseFailureException;
        
    }

    /**
     * Construct a validation assertion.
     *
     * @param column	    name of the output file column to check
     * @param parameter	    parameter specification
     * 
     * @throws ParseFailureException
     */
    public ValidationAssertion(String column, String parameter) throws ParseFailureException {
        this.columnName = column;
        int dotPos = parameter.indexOf('.');
        if (dotPos < 0)
            throw new ParseFailureException("Parameter specification " + parameter + " is invalid.");
        this.entityName = parameter.substring(0,  dotPos);
        try {
            this.parameterIndex = Integer.parseInt(parameter.substring(dotPos + 1)) - 1;
            if (this.parameterIndex < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Parameter specification " + parameter + " is invalid.");
        }
    }

    /**
     * Set the output-file column index for this assertion's column name.
     * 
     * @param inStream  input stream for the query output file
     * 
     * @throws IOException
     */
    public void fix(TabbedLineReader inStream) throws IOException {
        this.columnIndex = inStream.findField(this.columnName);
    }

    /**
     * Determine if this assertion is satisfied.
     * 
     * @param outputLine    output line of interest
     * @param qParms        parameter lists for the question being tested
     * 
     * @return TRUE if the assertion is satisfied, else FALSE
     */
    public boolean validate(TabbedLineReader.Line outputLine, JsonObject qParms) {
        boolean retVal = false;
        // Get the column value.
        String colVal = outputLine.get(this.columnIndex);
        // Get the parameter value.
        JsonArray entityParms = (JsonArray) qParms.get(this.entityName);
        if (entityParms != null) {
            if (this.parameterIndex < entityParms.size()) {
                String paramVal = entityParms.get(this.parameterIndex).toString();
                // We have both values, so do the comparison.
                retVal = this.compareValues(colVal, paramVal);
            }
        }
        return retVal;
    }

    protected abstract boolean compareValues(String colVal, String paramVal);

    /**
     * Convert a string to a double.
     * 
     * @param val   string to convert
     * 
     * @return the corresponding double, or NaN if the string is not a valid number
     */
    protected static double toDouble(String val) {
        double retVal;
        try {
            retVal = Double.parseDouble(val);
        } catch (NumberFormatException e) {
            retVal = Double.NaN;
        }
        return retVal;
    }    

}

