/**
 *
 */
package org.theseed.memdb.walker;

import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.RelationshipInstance;

/**
 * The text-walk relationship instance contains the text string that describes the crossing of the relationship
 * in a single direction.
 *
 * @author Bruce Parrello
 *
 */
public class TextRelationshipInstance extends RelationshipInstance {

    // FIELDS
    /** text for the relationship crossing */
    private String crossingText;

    /**
     * Construct a relationship instance for a text-walk crossing.
     *
     * @param templateString	template string for the crossing in this direction
     * @param destType			type name for the target entity
     * @param id				ID of the target entity
     */
    public TextRelationshipInstance(String templateString, String destType, String id) {
        super(destType, id);
        this.crossingText = templateString;
    }

    /**
     * Construct a relationship instance for a text-walk crossing.
     *
     * @param templateString	template string for the crossing in this direction
     * @param targetInstance	target entity instance
     */
    public TextRelationshipInstance(String templateString, EntityInstance targetInstance) {
        super(targetInstance);
        this.crossingText = templateString;
    }

    /**
     * @return the crossing sentence for this relationship instance
     */
    public String getSentence() {
        return this.crossingText;
    }

}
