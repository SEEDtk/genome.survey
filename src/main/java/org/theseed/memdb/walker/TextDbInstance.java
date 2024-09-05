/**
 *
 */
package org.theseed.memdb.walker;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

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
     * Generate a random walk and output it to the specified output stream.
     *
     * @param writer	print writer for the walk output
     */
    public void generateWalk(PrintWriter writer) {
        // TODO code for generateWalk

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
