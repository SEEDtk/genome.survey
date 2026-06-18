package org.theseed.memdb.words;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.theseed.memdb.EntityType;
import org.theseed.memdb.walk.WalkDbInstance;
import org.theseed.memdb.walk.WalkEntityInstance;

/**
 * The entity instance for a word database contains the attributes in the form of strings built using templates
 * in the entity type. The attributes are stored in a list, and the order is not significant. Eventually, the
 * attributes are shuffled and popped off the list in order.
 */
public class WordEntityInstance extends WalkEntityInstance {

    // FIELDS
    /** list of attributes for this instance */
    private final List<String> attributes;

    public WordEntityInstance(EntityType entityType, String id) {
        super(entityType, id);
        this.attributes = new ArrayList<>(5);
    }

    @Override
    public void addAttribute(String attribute) {
        this.attributes.add(attribute);
    }

    @Override
    public void shuffleAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'shuffleAll'");
    }

    @Override
    public boolean popAttribute(PrintWriter writer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'popAttribute'");
    }

    @Override
    public WalkEntityInstance popRelationship(PrintWriter writer, WalkDbInstance db) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'popRelationship'");
    }
    
}
