package org.theseed.reports.dbdbuilder;

import org.theseed.reports.FieldCounter;

/**
 * This class contains field information for a single file for use in a prototype DBD. The field is associated
 * with a single parent file, and we need to know the field name and whether or not it needs to be quoted.
 */
public class FieldAccumulator {

    // FIELDS
    /** field name */
    private String fieldName;
    /** TRUE if the field needs to be quoted */
    private boolean quoted;

    /**
     * Construct a field accumulator from a field counter.
     *
     * @param name      the name of the field
     * @param counter   field counter for this field
     */
    public FieldAccumulator(String name, FieldCounter counter) {
        this.fieldName = name;
        this.quoted = counter.getStringCount() > 0 || counter.getListCount() > 0;
    }

    /**
     * @return a string representing a declaration of this field in a DBD prototype file
     */
    public String toDeclaration() {
        String retVal = this.fieldName + "\t" + (this.quoted ? "quoted" : "unquoted") + "\t{{" + this.fieldName + "}}";
        return retVal;
    }

}
