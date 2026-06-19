/**
 *
 */
package org.theseed.memdb.text;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationshipInstance;
import org.theseed.memdb.walk.WalkDbInstance;
import org.theseed.memdb.walk.WalkEntityInstance;

/**
 * The entity instance for a text-walk database contains the attributes in the form of
 * expanded template strings. It also contains a flag to indicate that the entity has
 * been deleted from the walk.
 *
 * @author Bruce Parrello
 *
 */
public class TextEntityInstance extends WalkEntityInstance {

    // FIELDS
    /** list of attribute sentences */
    private final List<String> attributes;

    /**
     * Create a new text entity instance.
     *
     * @param type	entity type
     * @param id	ID of the instance
     * @param db    parent database instance
     */
    public TextEntityInstance(EntityType type, String id, TextDbInstance db) {
        super(type, id, db);
        // Start with no attributes.
        this.attributes = new ArrayList<>();
    }

    /**
     * Reorder the attributes and relationships in this entity instance.
     */
    @Override
    public void shuffleAll() {
        Collections.shuffle(this.attributes);
        List<RelationshipInstance> rels = this.getRelationships();
        Collections.shuffle(rels);
    }

 
    /**
     * Add an attribute to this instance.
     *
     * @param attribute		attribute string to add
     */
    @Override
    public void addAttribute(String attribute) {
        this.attributes.add(attribute);
    }

    /**
     * Emit an attribute for this entity and remove it from the attribute list.
     *
     * @param writer	output writer to contain the text
     *
     * @return TRUE if an attribute was written, else FALSE
     */
    @Override
    public boolean popAttribute(PrintWriter writer) {
        boolean retVal = false;
        final int lastN = this.attributes.size() - 1;
        if (lastN >= 0) {
            // Here we have an attribute to print.
            writer.println(this.attributes.get(lastN));
            this.attributes.remove(lastN);
            retVal = true;
        }
        return retVal;
    }

    /**
     * Emit a relationship for this entity and return the target entity instance.
     *
     * @param writer	output text writer
     * @param db		controlling database instance
     *
     * @return the target entity instance, or NULL if there is none available
     */
    @Override
    public WalkEntityInstance popRelationship(PrintWriter writer, WalkDbInstance db) {
        WalkEntityInstance retVal = null;
        // Get the list of relationship instances.
        var connections = this.getRelationships();
        final int lastN = connections.size() - 1;
        if (lastN >= 0) {
            // Here we have a relationship instance to traverse.  First, write the
            // relationship sentence.
            TextRelationshipInstance rel = (TextRelationshipInstance) connections.get(lastN);
            writer.println(rel.getSentence());
            // Get the target entity instance.  This could be NULL if the entity
            // is already exhausted.
            retVal = (WalkEntityInstance) rel.getTarget(db);
            // Delete the relationship from the entity instance.
            connections.remove(lastN);
        }
        return retVal;
    }

}
