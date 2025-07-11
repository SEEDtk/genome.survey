package org.theseed.reports;

import java.io.PrintWriter;

/**
 * This the text version of a JSON scan report. It tells us useful information about each file and
 * field in the genome dump directory.
 */
import org.apache.commons.lang3.StringUtils;

public class TextJsonScanReporter extends BaseJsonScanReporter {

    public TextJsonScanReporter(PrintWriter writer, BaseJsonScanReporter.IParms controller) {
        super(writer);
    }

    @Override
    public void startReport() {
    }

    @Override
    public void startFile(String fileName, int fileCount, int recordCount) {
            String fileHeading = "FILE " + fileName + ": " + fileCount + " instances, "
                    + recordCount + " records.";
            this.writeLine(fileHeading);
            this.writeLine(StringUtils.repeat("-", fileHeading.length()));
            this.writeLine(StringUtils.rightPad("name", 20) + " " + FieldCounter.header());
    }

    @Override
    public void writeField(String fieldName, FieldCounter fieldData) {
        this.writeLine(StringUtils.rightPad(fieldName, 20) + " " + fieldData.getResults());
    }

    @Override
    public void endFile(String fileName) {
        // We finish the file section by writing a spacer line.
        this.writeLine();
    }

    @Override
    public void endReport() {
    }

}