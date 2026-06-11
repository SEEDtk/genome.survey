package org.theseed.memdb.words;

import java.util.Collection;
import java.util.List;

import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;

public class WordDbInstance extends DbInstance {

    /**
     * Create a new word database instance
     *
     * @param types		list of entity type names
     */
    public WordDbInstance(List<String> types) {
        super(types);
    }

    @Override
    protected void preProcess() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'preProcess'");
    }

    @Override
    protected EntityInstance createEntity(EntityType entityType, String entityId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createEntity'");
    }

    @Override
    protected void postProcessEntities(Collection<EntityType> entityTypes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'postProcessEntities'");
    }

}
