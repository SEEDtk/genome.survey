package org.theseed.memdb.words;

import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.RelationshipInstance;

public class WordRelationshipInstance extends RelationshipInstance {

    // FIELDS
    /** source ID string */
    private final String sourceId;
    /** relationship name string */
    private final String name;
    /** target ID string */
    private final String targetId;

    public WordRelationshipInstance(String sourceId, String name, String targetId, EntityInstance target) {
        super(target);
        this.sourceId = sourceId;
        this.name = name;
        this.targetId = targetId;
    }

    /**
     * @return the output string for the source ID
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @return the output string for the relationship name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the output string for the target ID
     */
    public String getTargetId() {
        return targetId;
    }




}
