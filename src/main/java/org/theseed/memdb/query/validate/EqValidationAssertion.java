package org.theseed.memdb.query.validate;

import java.util.Arrays;

import org.theseed.basic.ParseFailureException;

/**
 * This assertion returns TRUE if the specified column value is equal to the parameter value.
 * Case-insensitive string equality is used. The column value can be a list, but the parameter
 * is always a scalar. In this case, the assertion is TRUE if any list element matches the parameter.
 * Note that the column value is a string, so if it came out of the database as a list, it will
 * be delimited with the value of DELIM.
 */
public class EqValidationAssertion extends ValidationAssertion {

    public EqValidationAssertion(String column, String parameter) throws ParseFailureException {
        super(column, parameter);
    }

    @Override
    protected boolean compareValues(String colVal, String paramVal) {
        boolean retVal;
        // Do we have a list?
        if (colVal.contains(QueryValidator.DELIM)) {
            // Check for any match in the list.
            String[] parts = colVal.split(QueryValidator.DELIM);
            retVal = Arrays.stream(parts).anyMatch(x -> x.equalsIgnoreCase(paramVal));
        } else {
            // No, just do a simple comparison.
            retVal = colVal.equalsIgnoreCase(paramVal);
        }
        return retVal;
    }

}
