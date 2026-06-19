package org.theseed.memdb.words;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.theseed.memdb.EntityType;
import org.theseed.memdb.RelationshipInstance;
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

    public WordEntityInstance(EntityType entityType, String id, WordDbInstance db) {
        super(entityType, id, db);
        this.attributes = new ArrayList<>(5);
    }

    @Override
    public void addAttribute(String attribute) {
        this.attributes.add(attribute);
    }

    @Override
    public void shuffleAll() {
        Collections.shuffle(this.attributes);
        List<RelationshipInstance> rels = this.getRelationships();
        Collections.shuffle(rels);
    }

    @Override
    public boolean popAttribute(PrintWriter writer) {
        boolean retVal = false;
        final int lastN = this.attributes.size() - 1;
        if (lastN >= 0) {
            // Here we have an attribute to print. We put our ID first, then the attribute, to insure that the
            // ID is considered related to the attribute. Most of the time, the ID will be suppressed, since the entity ID will have
            // been emitted as part of the relationship sentence.
            WordDbInstance db = (WordDbInstance) this.getParentDb();
            db.emitPhrase(writer, this.getId());
            db.emitPhrase(writer, this.attributes.get(lastN));
            this.attributes.remove(lastN);
            retVal = true;
        }
        return retVal;
    }

    @Override
    public WalkEntityInstance popRelationship(PrintWriter writer, WalkDbInstance db) {
        WalkEntityInstance retVal = null;
        // Get the list of relationship instances.
        var connections = this.getRelationships();
        final int lastN = connections.size() - 1;
        if (lastN >= 0) {
            // Here we have a relationship instance to traverse.  First, write the
            // relationship sentence.
            WordRelationshipInstance rel = (WordRelationshipInstance) connections.get(lastN);
            WordDbInstance wdb = (WordDbInstance) this.getParentDb();
            wdb.emitPhrase(writer, rel.getSourceId());
            wdb.emitPhrase(writer, rel.getName());
            wdb.emitPhrase(writer, rel.getTargetId());
            // Get the target entity instance.  This could be NULL if the entity
            // is already exhausted.
            retVal = (WalkEntityInstance) rel.getTarget(db);
            // Delete the relationship from the entity instance.
            connections.remove(lastN);
        }
        return retVal;
    }
    
}
