package org.theseed.memdb.words;

import java.util.ArrayList;
import java.util.List;

import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;

/**
 * The entity instance for a word database contains the attributes in the form of strings built using templates
 * in the entity type. The attributes are stored in a list, and the order is not significant. Eventually, the
 * attributes are shuffled and popped off the list in order.
 */
public class WordEntityInstance extends EntityInstance {

    // FIELDS
    /** list of attributes for this instance */
    private final List<String> attributes;

    public WordEntityInstance(WordEntityType entityType, String id) {
        super((EntityType)entityType, id);
        this.attributes = new ArrayList<>(5);
    }

    public void addAttribute(String attribute) {
        this.attributes.add(attribute);
    }
    
}
