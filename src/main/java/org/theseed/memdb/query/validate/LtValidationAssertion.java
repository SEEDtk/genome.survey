package org.theseed.memdb.query.validate;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.TabbedLineReader;

public class LtValidationAssertion extends ValidationAssertion {

    public LtValidationAssertion(TabbedLineReader outStream, String column, String parameter) throws ParseFailureException, IOException {
        super(outStream, column, parameter);
    }

    @Override
    protected boolean compareValues(String colVal, String paramVal) {
        double colNum = toDouble(colVal);
        double paramNum = toDouble(paramVal);
        return (colNum < paramNum);
    }

}
