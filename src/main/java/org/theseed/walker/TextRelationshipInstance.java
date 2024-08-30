/**
 *
 */
package org.theseed.walker;

/**
 * A relationship describes a unidirectional crossing between two entity instances.
 * The text-template version contains a sentence describing the crossing in addition to
 * data about the target entity instance and is stored in the source entity instance.
 *
 * @author Bruce Parrello
 *
 */
public class TextRelationshipInstance extends RelationshipInstance {

    // FIELDS
    /** relationship crossing sentence */
    private String crossingSentence;


    /**
     * Create a new relationship instance.
     *
     * @param sentence		crossing sentence
     * @param destType		target entity type name
     * @param id			target instance ID
     */
    public TextRelationshipInstance(String sentence, String destType, String id) {
        super(destType, id);
        this.crossingSentence = sentence;
    }

    /**
     * Convstruct a relationship instance with the specified text string and target entity.
     *
     * @param connectString		relationship description string
     * @param targetInstance	target entity instance
     */
    public TextRelationshipInstance(String connectString, EntityInstance targetInstance) {
        super(targetInstance);
        this.crossingSentence = connectString;
    }

    /**
     * @return the crossing sentence
     */
    public String getSentence() {
        return this.crossingSentence;
    }

}
