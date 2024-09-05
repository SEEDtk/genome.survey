/**
 *
 */
package org.theseed.memdb.walker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationshipInstance;

/**
 * The entity instance for a text-walk database contains the attributes in the form of
 * expanded template strings. It also contains a flag to indicate that the entity has
 * been deleted from the walk.
 *
 * @author Bruce Parrello
 *
 */
public class TextEntityInstance extends EntityInstance {

    // FIELDS
    /** list of attribute sentences */
    private List<String> attributes;
    /** TRUE if this instance has been deleted */
    private boolean deleted;

    /**
     * Create a new text entity instance.
     *
     * @param type	entity type
     * @param id	ID of the instance
     */
    public TextEntityInstance(EntityType type, String id) {
        super(type, id);
        // Start with no attributes and not deleted.
        this.attributes = new ArrayList<String>();
        this.deleted = false;
    }

    /**
     * Reorder the attributes and relationships in this entity instance.
     */
    public void shuffleAll() {
        Collections.shuffle(this.attributes);
        List<RelationshipInstance> rels = this.getRelationships();
        Collections.shuffle(rels);
    }

    /**
     * @return TRUE if this entity instance is deleted
     */
    public boolean isDeleted() {
        return this.deleted;
    }

    /**
     * Denote that this entity instance is deleted.
     */
    public void setDeleted() {
        this.deleted = true;
    }

    /**
     * Add an attribute to this instance.
     *
     * @param attribute		attribute string to add
     */
    public void addAttribute(String attribute) {
        this.attributes.add(attribute);
    }

}
