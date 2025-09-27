package org.theseed.memdb.query.validate;

import org.theseed.basic.ParseFailureException;

public class GtValidationAssertion extends ValidationAssertion {

    public GtValidationAssertion(String column, String parameter) throws ParseFailureException {
        super(column, parameter);
    }

    @Override
    protected boolean compareValues(String colVal, String paramVal) {
        double colNum = toDouble(colVal);
        double paramNum = toDouble(paramVal);
        return (colNum > paramNum);
    }

}
