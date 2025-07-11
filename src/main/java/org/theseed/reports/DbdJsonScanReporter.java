package org.theseed.reports;

import java.io.PrintWriter;

public class DbdJsonScanReporter extends BaseJsonScanReporter {



    public DbdJsonScanReporter(PrintWriter writer, BaseJsonScanReporter.IParms controller) {
        super(writer);
    }

    @Override
    public void startReport() {
        // TODO Implementation for DBD report start
    }

    @Override
    public void startFile(String fileName, int fileCount, int recordCount) {
        // TODO Implementation for DBD file start
    }

    @Override
    public void writeField(String fieldName, FieldCounter fieldData) {
        // TODO Implementation for DBD field write
    }

    @Override
    public void endFile(String fileName) {
        // TODO Implementation for DBD file end
    }

    @Override
    public void endReport() {
        // TODO Implementation for DBD report end
    }

}