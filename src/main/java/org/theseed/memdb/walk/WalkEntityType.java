package org.theseed.memdb.walk;

import org.theseed.memdb.EntityType;

/**
 * This is a common version of the entity type for databases that are walked. It is responsible
 * for doing token counts, which is common to all walk operations.
 * 
 * @author Bruce Parrello
 */
public abstract class WalkEntityType extends EntityType {

    // FIELDS
    /** token counter for all entity instances */
    private long tokenCount;

    public WalkEntityType(String name) {
        super(name);
        this.tokenCount = 0;
    }

    /**
     * @return the number of tokens generated for this entity type
     */
    public long getTokenCount() {
        return this.tokenCount;
    }

    /**
     * Update the token count for this entity type.
     *
     * @param count		number of tokens to add
     */
    public void countTokens(long count) {
        this.tokenCount += count;
    }

}
