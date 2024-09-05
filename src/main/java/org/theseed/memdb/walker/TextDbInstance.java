/**
 *
 */
package org.theseed.memdb.walker;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;

/**
 * This is the instance for a text-walk database, and it contains the code for doing the
 * random walk.
 *
 * @author Bruce Parrello
 *
 */
public class TextDbInstance extends DbInstance {

    // FIELDS
    /** number of relation crossings */
    private int crossCount;
    /** number of attyributes emitted */
    private int attrCount;
    /** total number of tokens generated */
    private long tokenTotal;
    /** encoder for counting tokens */
    private Encoding encoder;

    /**
     * Create a new text-walk database instance
     *
     * @param types		list of entity type names
     */
    public TextDbInstance(List<String> types) {
        super(types);
        // Clear everything.
        this.attrCount = 0;
        this.crossCount = 0;
        this.tokenTotal = 0L;
        this.encoder = null;
    }

    @Override
    protected void preProcess() {
        // Create the encoder for tracking the tokens.
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoder = registry.getEncoding(EncodingType.CL100K_BASE);
        // Clear the counters.
        this.attrCount = 0;
        this.crossCount = 0;
        this.tokenTotal = 0L;
    }

    @Override
    protected EntityInstance createEntity(EntityType entityType, String entityId) {
        // Create a text entity instance with the specified ID and type.
        return new TextEntityInstance(entityType, entityId);
    }

    /**
     * Generate a random walk and output it to the specified output stream.  Note that the
     * random walk destroys the database instance as it goes.
     *
     * @param writer	print writer for the walk output
     */
    public void generateWalk(PrintWriter writer) {
        this.attrCount = 0;
        this.crossCount = 0;
        long walkCount = 0;
        // Link all the entity instances into a master list.  We process the types in priority order.
        List<TextEntityInstance> masterList = new ArrayList<TextEntityInstance>();
        for (String typeName : this.getTypeNames()) {
            var entityMap = this.getEntityMap(typeName);
            // Only proceed if this is a real entity. We get all of its instances.
            if (entityMap != null) {
                for (EntityInstance x : entityMap.values())
                    masterList.add((TextEntityInstance) x);
            }
        }
        int passCount = 0;
        while (! masterList.isEmpty()) {
            passCount++;
            log.info("{} entity instances in master list for pass {}.", masterList.size(), passCount);
            long lastMsg = System.currentTimeMillis();
            // Loop through the entities, processing the undeleted ones.
            for (TextEntityInstance curr : masterList) {
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
            List<TextEntityInstance> newList = masterList.stream().filter(x -> ! x.isDeleted()).collect(Collectors.toList());
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

    /**
     * Remove the specified entity instance from the master map.
     *
     * @param curr		entity instance to remove
     */
    protected void removeFromMap(TextEntityInstance curr) {
        String type = curr.getType();
        var entityMap = this.getEntityMap(type);
        if (entityMap != null) {
            String id = curr.getId();
            entityMap.remove(id);
            // Denote this entity instance is gone.
            curr.setDeleted();
        }
    }


    /**
     * @return the number of tokens generated during the walk
     */
    public long getTokenTotal() {
        return this.tokenTotal;
    }

    /**
     * Count the tokens in a text string. This also updates the global token count.
     *
     * @param string	text string to process
     *
     * @return the number of tokens found
     */
    public long countTokens(String string) {
        long retVal = this.encoder.countTokens(string);
        this.tokenTotal += retVal;
        return retVal;
    }

    @Override
    protected void postProcessEntities(Collection<EntityType> entityTypes) {
        // Loop through the entity instances.  For each one, we shuffle the attribute and relationship lists to
        // get them in random order, and then we output the total token and instance counts for each entity type.
        for (EntityType typeObject : entityTypes) {
            TextEntityType type = (TextEntityType) typeObject;
            String typeName = type.getName();
            long typeTokens = type.getTokenCount();
            log.info("Entity type {} has {} instances and generated {} tokens.", typeName, this.getTypeCount(typeName), typeTokens);
            for (EntityInstance instanceObject : this.getAllEntities(typeName)) {
                TextEntityInstance instance = (TextEntityInstance) instanceObject;
                instance.shuffleAll();
            }
        }
        log.info("{} total tokens generated in database.", this.tokenTotal);
        // Release the memory for the encoder.
        this.encoder = null;
    }

}
