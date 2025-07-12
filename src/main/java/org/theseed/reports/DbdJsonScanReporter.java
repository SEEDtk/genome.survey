package org.theseed.reports;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.LineReader;
import org.theseed.reports.dbdbuilder.EntityAccumulator;
import org.theseed.reports.dbdbuilder.FieldAccumulator;
import org.theseed.reports.dbdbuilder.FileAccumulator;

/**
 * This class is a specialized reporter for JSON scan reports that takes as input a JSON-walk DBD source file prototype and
 * fills in the fields. The result can be easily adapted to a fully functional JSON-walk DBD. For each entity, it will
 * keep a list of relationships and adjunct files. The JSON scan report data will then be used to populate the field
 * information in each section. Finally, it will all be written when the report is finished.
 */
public class DbdJsonScanReporter extends BaseJsonScanReporter {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(DbdJsonScanReporter.class);
    /** list of entity descriptors */
    private List<EntityAccumulator> entityList;
    /** map of file names to file accumulators */
    private Map<String, FileAccumulator> fileMap;
    /** current file accumulator */
    private FileAccumulator currentAccumulator;

    public DbdJsonScanReporter(PrintWriter writer, BaseJsonScanReporter.IParms controller) throws IOException {
        super(writer);
        // Clear the entity list, current-accumulator pointer, and file map.
        this.entityList = new ArrayList<EntityAccumulator>();
        this.fileMap = new HashMap<String, FileAccumulator>();
        this.currentAccumulator = null;
        // Get the DBD file name from the controller.
        File dbdFile = controller.getDbdFile();
        log.info("Reading DBD file prototype {}.", dbdFile);
        // Open the DBD prototype file and read it in to build the entity accumulators.
        try (LineReader reader = new LineReader(dbdFile)) {
            // Loop through the lines in the file, pulling out the entity sections.
            int lineCount = 0;
            List<String> entityLines = new ArrayList<>();
            for (String line : reader) {
                lineCount++;
                if (line.startsWith("#Entity")) {
                    // Here we have a new entity section. Build an accumulator for the old one, if any.
                    if (!entityLines.isEmpty()) {
                        this.buildEntityAccumulator(entityLines);
                        entityLines.clear();
                    }
                }
                // Add this line to the current entity section.
                entityLines.add(line);
            }
            // If we have any lines left, build the last entity accumulator.
            if (!entityLines.isEmpty()) {
                this.buildEntityAccumulator(entityLines);
            }
            log.info("{} lines read from DBD file prototype. {} entities found. {} files identified", 
                    lineCount, this.entityList.size(), this.fileMap.size());
        }
    }

    /**
     * Build an entity accumulator from the provided entity-section lines. We can assume that
     * the entity section is non-empty. We build the accumulator and then unroll its file map
     * to our own file map.
     * 
     * @param entityLines   the lines of the entity section
     * 
     * @throws IOException
     */
    private void buildEntityAccumulator(List<String> entityLines) throws IOException {
        // Create the entity accumulator.
        EntityAccumulator entity = new EntityAccumulator(entityLines);
        this.entityList.add(entity);
        // Now add the entity's file map to our file map.
        this.fileMap.putAll(entity.getFileMap());
    }

    @Override
    public void startReport() {
    }

    @Override
    public void startFile(String fileName, int fileCount, int recordCount) {
        // Here we have a file in the JSON dump. We find and save its file accumulator.
        this.currentAccumulator = this.fileMap.get(fileName);
        // A file that is not in the DBD is expected, but the user will still want to know.
        if (this.currentAccumulator == null)
                log.warn("File {} not found in the DBD.", fileName);
    }

    @Override
    public void writeField(String fieldName, FieldCounter fieldData) {
        // Insure this file is in the DBD and is of interest to us.
        if (this.currentAccumulator != null) {
            // Here we want to record the field for future output.
            this.currentAccumulator.addField(fieldName, fieldData);
        }
    }

    @Override
    public void endFile(String fileName) {
    }

    @Override
    public void endReport() {
        // Here is where all the output happens. We loop through the entities, writing the fields, relationship, and file sections.
        for (EntityAccumulator entity : this.entityList) {
            // Write out the entity header.
            String line = entity.getHeaderString();
            this.writeLine(line);
            // Now we write out the fields for the entity itself.
            FileAccumulator fileAccumulator = entity.getMainFileAccumulator();
            if (fileAccumulator != null)
                this.writeFileFields(fileAccumulator);
            // Next, the adjunct file sections.
            Collection<FileAccumulator> fileAccumulators = entity.getAdjunctFileAccumulators();
            for (var accumulator : fileAccumulators) {
                this.writeLine(accumulator.getHeader());
                this.writeFileFields(accumulator);
            }
        }
    }

    /** 
     * write the definition lines for the fields in a file accumulator.
     * 
     * @param accumulator       file accumulator whose fields should be written
     */
    private void writeFileFields(FileAccumulator accumulator) {
        Collection<FieldAccumulator> fieldDescriptors = accumulator.getFieldList();
        for (var descriptor : fieldDescriptors)
            this.writeLine(descriptor.toDeclaration());
    }

}