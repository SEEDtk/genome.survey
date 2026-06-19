/**
 *
 */
package org.theseed.memdb.text;

import java.util.List;

import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.walk.WalkDbInstance;

/**
 * This is the instance for a text-walk database. Most of the code is in the parent class, but
 * it needs to know that the entity instances created are for text-walk entities.
 *
 * @author Bruce Parrello
 *
 */
public class TextDbInstance extends WalkDbInstance {

    /**
     * Create a new text-walk database instance
     *
     * @param types		list of entity type names
     */
    public TextDbInstance(List<String> types) {
        super(types);
    }
    
    @Override
    protected EntityInstance createEntity(EntityType entityType, String entityId) {
        // Create a text entity instance with the specified ID and type.
        return new TextEntityInstance(entityType, entityId, this);
    }

}
