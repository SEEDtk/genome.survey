/**
 *
 */
package org.theseed.walker;

import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * An entity instance describes a single entity occurrence.  It contains all of the relationship
 * instances that describe the connections and all the attribute sentences that indicate the entity
 * instance's properties.  These are removed as they are emitted, so we can always pick a random
 * unused one.
 *
 * @author Bruce Parrello
 *
 */
public class EntityInstance {

    // FIELDS
    /** ID of this entity */
    private String entityId;
    /** type name of this entity */
    private String entityType;
    /** list of attribute sentences */
    private List<String> attributes;
    /** list of relationship instances */
    private List<RelationshipInstance> connections;

    /**
     * Create a new, empty entity instance.
     *
     * @param type		type of the new entity
     * @param id		ID of the instance
     */
    public EntityInstance(EntityType type, String id) {
        this.entityId = id;
        this.entityType = type.getName();
        this.attributes = new ArrayList<String>(type.getAttributeCount());
        this.connections = new ArrayList<RelationshipInstance>();
    }

    /**
     * Emit an attribute for this entity and remove it from the attribute list.
     *
     * @param writer	output writer to contain the text
     *
     * @return TRUE if an attribute was written, else FALSE
     */
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
    public EntityInstance popRelationship(PrintWriter writer, DbInstance db) {
        EntityInstance retVal = null;
        final int lastN = this.connections.size() - 1;
        if (lastN >= 0) {
            // Here we have a relationship instance to traverse.  First, write the
            // relationship sentence.
            RelationshipInstance rel = this.connections.get(lastN);
            this.connections.remove(lastN);
            // Get the target entity instance.  This could be NULL if the entity
            // is already exhausted.
            retVal = rel.getTarget(db);
            // Emit the relationship sentence.
            writer.println(rel.getSentence());
        }
        return retVal;
    }

    /**
     * @return the type name of this entity instance
     */
    public String getType() {
        return this.entityType;
    }

    /**
     * @return the ID of this entity instance
     */
    public String getId() {
        return this.entityId;
    }

    /**
     * Add an attribute string to this entity instance.
     *
     * @param attributeString	attribute string to add
     */
    public void addAttribute(String attributeString) {
        this.attributes.add(attributeString);
    }

    /**
     * Add a relationship connection to this entity instance.
     *
     * @param connection	new connection to add
     */
    public void addConnection(RelationshipInstance rel) {
        this.connections.add(rel);
    }

    /**
     * Shuffle the attribute and relationship lists.
     */
    public void shuffleAll() {
        Collections.shuffle(this.attributes);
        Collections.shuffle(this.connections);
    }

}
