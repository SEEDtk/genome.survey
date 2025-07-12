package org.theseed.reports.dbdbuilder;

import java.util.ArrayList;
import java.util.List;

import org.theseed.reports.FieldCounter;

public class FileAccumulator {

    // FIELDS
    /** file header string */
    private String header;
    /** list of field accumulators for this file */
    private List<FieldAccumulator> fieldList;

    /**
     * Create a new, blank accumulator for a specific file.
     *
     * @param header        header taken from the DBD prototype
     */
    public FileAccumulator(String header) {
        this.header = header;
        this.fieldList = new ArrayList<FieldAccumulator>();
    }

    /**
     * Get the file header.
     *
     * @return the file header
     */
    public String getHeader() {
        return header;
    }

    /** Get the list of field accumulators for this file.
     *
     * @return the list of field accumulators
     */
    public List<FieldAccumulator> getFieldList() {
        return fieldList;
    }

    public void addField(String fieldName, FieldCounter fieldData) {
        // Create a field accumulator and add it to this file's field list.
        FieldAccumulator fieldDescriptor = new FieldAccumulator(fieldName, fieldData);
        this.fieldList.add(fieldDescriptor);
    }

}
