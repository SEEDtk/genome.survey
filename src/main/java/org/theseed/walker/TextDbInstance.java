/**
 *
 */
package org.theseed.walker;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A database instance contains the data described by a DbDefinition.  The data is stored in a
 * massive two-level hash keyed on entity type name, each consisting of a hash of entity instances
 * keyed by ID. This version contains all the code necessary for the random template-generating
 * text walk.
 *
 * @author Bruce Parrello
 *
 */
public class TextDbInstance extends DbInstance {

    // FIELDS
    /** number of attributes emitted */
    private long attrCount;
    /** number of relationship crossings */
    private long crossCount;
    /** number of tokens generated */
    private long tokenTotal;

    /**
     * Create a blank, empty database instance.
     *
     * @param size	number of entity types in this database
     */
    public TextDbInstance(List<String> types) {
        super(types);
        this.tokenTotal = 0;
    }

    /**
     * Write out a random walk through the database.  This is a destructive operation, as entities, relationships,
     * and attributes are deleted from the database instance as they are used.
     *
     * @param writer	output writer for the generated text
     *
     */
    public void generateWalk(PrintWriter writer) {
        this.attrCount = 0;
        this.crossCount = 0;
        long walkCount = 0;
        // Link all the entity instances into a master list.  We process the types in priority order.
        List<EntityInstance> masterList = new ArrayList<EntityInstance>();
        for (String typeName : this.getTypeNames()) {
            var entityMap = this.getEntityMap(typeName);
            // Only proceed if this is a real entity.
            if (entityMap != null)
                masterList.addAll(entityMap.values());
        }
        int passCount = 0;
        while (! masterList.isEmpty()) {
            passCount++;
            log.info("{} entity instances in master list for pass {}.", masterList.size(), passCount);
            long lastMsg = System.currentTimeMillis();
            // Loop through the entity instances, processing the undeleted ones.
            for (EntityInstance currObject : masterList) {
                TextEntityInstance curr = (TextEntityInstance) currObject;
                if (! curr.isDeleted()) {
                    this.processEntity(writer, curr);
                    walkCount++;
                    long now = System.currentTimeMillis();
                    if (now - lastMsg >= 5000) {
                        log.info("{} walks completed. {} attributes written, {} crossings.", walkCount,
                                this.attrCount, this.crossCount);
                        lastMsg = now;
                    }
                }
            }
            // Clean deleted entities from the list.
            log.info("Cleaning the list.");
            List<EntityInstance> newList = masterList.stream().filter(x -> ! x.isDeleted()).collect(Collectors.toList());
            masterList = newList;
        }
        log.info("{} total walks completed in {} passes. {} attributes written, {} crossings.",
                walkCount, passCount, this.attrCount, this.crossCount);
    }

    /**
     * Produce the longest possible walk from the specified entity instance.
     *
     * @param writer	current output text writer
     * @param first		entity instance from which to start the walk.
     */
    private void processEntity(PrintWriter writer, TextEntityInstance first) {
        TextEntityInstance nextEntity = first;
        while (nextEntity != null) {
            // Check for an attribute to write.
            boolean found = nextEntity.popAttribute(writer);
            // Check for a relationship to write.
            TextEntityInstance target = nextEntity.popRelationship(writer, this);
            if (target == null && ! found) {
                // Here we have no more data on this entity, so we need to delete it.
                this.removeFromMap(nextEntity);
            } else {
                if (target != null)
                    this.crossCount++;
                if (found)
                    this.attrCount++;
            }
            // Keep walking.  If the target was NULL, we will stop.
            nextEntity = target;
        }
    }

    @Override
    protected EntityInstance createEntity(EntityType entityType, String entityId) {
        return new TextEntityInstance(entityType, entityId);
    }

    /**
     * @return the total tokens generated
     */
    public long getTokenTotal() {
        return this.tokenTotal;
    }

    /**
     * Store the total tokens generated.
     *
     * @param tokenTotal 	the total number of tokens generated
     */
    public void setTokenTotal(long tokenTotal) {
        this.tokenTotal = tokenTotal;
    }

}
