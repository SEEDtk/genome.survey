package org.theseed.reports.dbdbuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * This object contains information about an entity that can be used by a JSON scan report to build a JSON-walk DBD
 * prototype. It will contain a list of relationships and adjunct files, and it will be used to populate the
 * fields in the DBD prototype file and output the appropriate headers.
 */
public class EntityAccumulator {

    // FIELDS
    /** entity header line */
    private String headerString;
    /** entity file name */
    private String fileName;
    /** list of relationships for this entity */
    private List<String> relationshipList;
    /** map of file names to the accumulators for that file */
    private Map<String, FileAccumulator> fileMap;

    /**
     * Construct a new entity accumulator from the entity definition section of a DBD prototype file.
     * 
     * @param entitySection     the entity definition section of the DBD prototype file
     * 
     * @throws IOException 
     * 
     */
    public EntityAccumulator(List<String> entitySection) throws IOException {
        this.relationshipList = new ArrayList<String>(entitySection.size());
        this.fileMap = new HashMap<String, FileAccumulator>(entitySection.size() * 4 / 3 + 1);
        // We need to check for a source file for this entity. If there is one, it will be the fifth
        // field of the first line, the fields being space-delimited.
        if (entitySection.size() <= 0)
            throw new IllegalArgumentException("Entity section is empty.");
        this.headerString = entitySection.get(0);
        String[] fields = StringUtils.split(this.headerString, ' ');
        // Check for a source file. Note that some entities have none.
        if (fields.length > 4) {
            String sourceFile = fields[4];
            this.fileMap.put(sourceFile, new FileAccumulator(this.headerString));
            this.fileName = sourceFile;
        } else
            this.fileName = null;
        // Now we have file and relationship lines to process.
        for (int i = 1; i < entitySection.size(); i++) {
            String line = entitySection.get(i);
            if (StringUtils.startsWith(line, "#Relationship"))
                this.relationshipList.add(line);
            else if (StringUtils.startsWith(line, "#File")) {
                // Here we have a file line. We need to extract the file name and create an accumulator
                // for it. The file name is in the second field, and the fields are space-delimited.
                String[] fileFields = StringUtils.split(line, ' ');
                if (fileFields.length < 3)
                    throw new IOException("Invalid file line in entity section: " + line);
                else {
                    String fileName = fileFields[1];
                    this.fileMap.put(fileName, new FileAccumulator(line));
                }
            }
        }
    }

    /**
     * Get the header string for this entity.
     * 
     * @return the original header string for this entity from the DBD prototype file
     */
    public String getHeaderString() {
        return headerString;
    }

    /**
     * Get the list of relationships for this entity.
     * 
     * @return the list of relationship definition strings from the DBD prototype file
     */
    public List<String> getRelationshipList() {
        return relationshipList;
    }

    /**
     * Get the map of file names to file accumulators for this entity.
     * 
     * @return the map of file names to file accumulators
     */
    public Map<String, FileAccumulator> getFileMap() {
        return fileMap;
    }

    /**
     * Get the file accumulator for the entity's main file.
     * 
     * @return a file accumulator corresponding to the entity's main file, or NULL if there is none
     */
    public FileAccumulator getMainFileAccumulator() {
        FileAccumulator retVal;
        if (this.fileName == null)
            retVal = null;
        else
            retVal = this.fileMap.get(this.fileName);
        return retVal;
    }

    /**
     * Get the file accumulators for the entity's adjunct files.
     * 
     * @return a list of the file accumulators for the adjunct files
     */
    public Collection<FileAccumulator> getAdjunctFileAccumulators() {
        // We return all the files but the main one.
        List<FileAccumulator> retVal = new ArrayList<FileAccumulator>(this.fileMap.size() - 1);
        for (var mapEntry : this.fileMap.entrySet()) {
            String adjunctName = mapEntry.getKey();
            if (! adjunctName.equals(this.fileName))
                retVal.add(mapEntry.getValue());
        }
        return retVal;
    }

}
