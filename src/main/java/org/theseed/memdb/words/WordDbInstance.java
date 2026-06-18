package org.theseed.memdb.words;

import java.util.List;

import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.walk.WalkDbInstance;


/**
 * This is the instance for a word-walk database. Most of the code is in the parent class, but
 * it needs to know that the entity instances created are for word-walk entities.
 */
public class WordDbInstance extends WalkDbInstance {

    /**
     * Create a new word database instance
     *
     * @param types		list of entity type names
     */
    public WordDbInstance(List<String> types) {
        super(types);
    }

    @Override
    protected EntityInstance createEntity(EntityType entityType, String entityId) {
        // Create a word entity instance with the specified ID and type.
        return new WordEntityInstance(entityType, entityId);
    }

}
