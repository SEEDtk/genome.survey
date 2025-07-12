/**
 *
 */
package org.theseed.reports;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import com.github.cliftonlabs.json_simple.JsonArray;

/**
 * This is a utility object used to count field occurrences.
 *
 * @author Bruce Parrello
 *
 */
public class FieldCounter {

    /** number of nonblank values */
    private int fillCount;
    /** number of blank values */
    private int blankCount;
    /** number of list values */
    private int listCount;
    /** number of string values */
    private int stringCount;
    /** number of boolean values */
    private int boolCount;
    /** number of integer values */
    private int integerCount;
    /** number of unknown-type values */
    private int otherCount;

    /**
     * Create a new, blank field counter.
     */
    public FieldCounter() {
        this.fillCount = 0;
        this.blankCount = 0;
        this.listCount = 0;
        this.stringCount = 0;
        this.boolCount = 0;
        this.integerCount = 0;
        this.otherCount = 0;
    }

    /**
     * Count a field from a JSON object.
     *
     * @param field		field to count
     */
    public void count(Object field) {
        if (field == null)
            this.blankCount++;
        else if (field instanceof String) {
            // For a string, we need to separate empty and blank.
            String fieldString = (String) field;
            if (StringUtils.isBlank(fieldString))
                this.blankCount++;
            else {
                this.stringCount++;
                this.fillCount++;
            }
        } else if (field instanceof JsonArray) {
            // For a list, we need to separate empty.
            JsonArray fieldList = (JsonArray) field;
            if (fieldList.isEmpty())
                this.blankCount++;
            else {
                this.fillCount++;
                this.listCount++;
            }
        } else {
            // Here we don't have a null case: the field is a boxed primitive.
            this.fillCount++;
            if (field instanceof BigDecimal) {
                // Here we have a number. We need to know if it's an integer.
                BigDecimal numField = (BigDecimal) field;
                if (numField.scale() <= 0)
                    this.integerCount++;
                else
                    this.otherCount++;
            } else if (field instanceof Boolean)
                this.boolCount++;
            else
                this.otherCount++;
        }
    }

    /**
     * @return the proposed headers for a report involving a field counter
     */
    public static String header() {
        return String.format("%10s %10s %10s %10s %10s %10s %10s", "present", "blank", "list", "string", "boolean", "int", "other");
    }

    /**
     * @return the data string of these counts for a report
     */
    public String getResults() {
        return String.format("%10d %10d %10d %10d %10d %10d %10d", this.fillCount, this.blankCount, this.listCount,
                this.stringCount, this.boolCount, this.integerCount, this.otherCount);
    }

    /**
     * @return the number of filled fields
     */
    public int getFillCount() {
        return this.fillCount;
    }

    /**
     * @return the number of blank/empty fields
     */
    public int getBlankCount() {
        return this.blankCount;
    }

    /**
     * @return the number of list fields
     */
    public int getListCount() {
        return this.listCount;
    }

    /**
     * @return the number of string fields
     */
    public int getStringCount() {
        return this.stringCount;
    }

    /**
     * @return the the number of boolean fields
     */
    public int getBoolCount() {
        return this.boolCount;
    }

    /**
     * @return the number of integer fields
     */
    public int getIntegerCount() {
        return this.integerCount;
    }

    /**
     * @return the number of unknown-type fields
     */
    public int getOtherCount() {
        return this.otherCount;
    }


}
