package org.theseed.reports;

import java.io.File;
import java.io.PrintWriter;

/**
 * This is the base class for output from a JSON scan report. The reports include information about both
 * files and fields, and are not necessarily standard tab-delimited files.
 */
public abstract class BaseJsonScanReporter {

    // FIELDS
    /** output report writer */
    private PrintWriter writer;

    /** 
     * This interface must be supported by the controlling command processor for the report.
     */
    public static interface IParms {

        /**
         * @return the old DBD source file for the DBD report, or null if not applicable
         */
        public File getDbdFile();
    }

    /** This enum indicates the types of reports */
    public static enum Type {
        DBD {
            @Override
            public BaseJsonScanReporter createReporter(PrintWriter writer, IParms controller) {
                // Replace with actual DBD reporter implementation
                return new DbdJsonScanReporter(writer, controller);
            }
        },
        TEXT {
            @Override
            public BaseJsonScanReporter createReporter(PrintWriter writer, IParms controller) {
                // Replace with actual TEXT reporter implementation
                return new TextJsonScanReporter(writer, controller);
            }
        };

        /** 
         * Create a new reporter of this type
         * 
         * @param writer        the PrintWriter to use for output
         * @param controller    the command processor controlling the report
         * 
         * @return a new reporter instance of the appropriate type
         */
        public abstract BaseJsonScanReporter createReporter(PrintWriter writer, IParms controller);
    }

    /** 
     * Construct a new report writer
     * 
     * @param writer    the PrintWriter to use for output
     */
    public BaseJsonScanReporter(PrintWriter writer) {
        this.writer = writer;
    }

    /** 
     * Start the report and output the header 
     */
    public abstract void startReport();

    /**
     * Start the section for a file.
     * 
     * @param fileName   the name of the file being reported
     */
    public abstract void startFile(String fileName, int fileCount, int recordCount);

    /**
     * Write the information for a field.
     * 
     * @param fieldName  the name of the field being reported
     * @param fieldData  the data for the field
     */
    public abstract void writeField(String fieldName, FieldCounter fieldData);

    /**
     * Finish the section for a file.
     * 
     * @param fileName   the name of the file being reported
     */
    public abstract void endFile(String fileName);

    /**
     * Finish the report.
     */
    public abstract void endReport();

    /**
     * Write a line to the report.
     * 
     * @param fields    the fields to write, separated by tabs (specify one for a raw line, none for a blank line)
     */
    public void writeLine(String... fields) {
        if (fields.length > 0) {
            String line = String.join("\t", fields);
            writer.println(line);
        } else {
            writer.println();
        }
    }
}
