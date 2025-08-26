/**
 *
 */
package org.theseed.memdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
// import java.util.Map;
// import java.util.TreeMap;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;


/**
 * An entity type contains instructions for reading an entity.  To read an entity, we need to know the
 * file name, the type name, and the definitions of the relationships.  In the case of the relationship,
 * we will also need to know the ID and type of the destination entity.  This information is in the
 * relationship definition.
 *
 * Note that some entities don't have files and are only built as a result of a relationship crossing.
 *
 * An entity type is identified solely by name.  The types are sorted by descending priority (0 is lowest)
 * followed by name.
 *
 * If the ID column name is "null", then the entity is actually a table representing a many-to-many relationship,
 * and will not have any entity instances.
 *
 * @author Bruce Parrello
 *
 */
public abstract class EntityType implements Comparable<EntityType> {

    // FIELDS
    /** entity type name */
    private final String name;
    private final String name;
    /** entity input file name */
    private String fileName;
    /** ID column name */
    private String idColName;
    /** list of relationship definitions */
    private final List<RelationshipType> relationships;
//    /** map of file names to ID attribute names for additional files */
//    private final Map<String, String> adjunctFileMap;
    /** priority */
    private int priority;
    /** special ID for connector records */
    protected static final String NULL_ID = "<connector>";

    // TODO adjunct files

    /**
     * Create a new, blank entity type.
     *
     * @param name		entity type name
     */
    public EntityType(String name) {
        this.name = name;
        this.fileName = null;
        this.idColName = null;
        this.relationships = new ArrayList<>();
        this.relationships = new ArrayList<>();
        this.priority = 0;
//        this.adjunctFileMap = new TreeMap<>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EntityType)) {
            return false;
        }
        EntityType other = (EntityType) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(EntityType o) {
        int retVal = o.priority - this.priority;
        if (retVal == 0)
            retVal = this.name.compareTo(o.name);
        return retVal;
    }

    /**
     * Specify the file name.
     *
     * @param nameString	base name of the file containing entity instances
     */
    public void setFileName(String nameString) {
        this.fileName = nameString;
    }

    /**
     * Add a new attribute template string to this entity.
     *
     * @param line	line containing the template string
     */
    protected abstract void addAttribute(String line);

    /**
     * Specify the name of this entity type's ID column
     *
     * @param idCol		the id column name
     */
    public void setIdColName(String idCol) {
        if (idCol.contentEquals("null"))
            this.idColName = null;
        else
            this.idColName = idCol;
    }

    /**
     * Specify the priority of this entity.  In the random walk, lower highest priority numbers
     * will go first.
     *
     * @param prio	new priority number
     */
    public void setPriority(int prio) {
        this.priority = prio;
    }

    /**
     * @return the name of this entity type
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the name of the file containing this entity's instances
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * @return the name of the ID column in the entity's instance file
     */
    public String getIdColName() {
        return this.idColName;
    }

    /**
     * Create a set of relationship builders for each relationship connected to this entity.
     *
     * @param inStream	input stream for entity instances
     *
     * @return a collection of relationship builders
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    public Collection<RelationBuilder> getRelationBuilders(FieldInputStream inStream)
            throws IOException, ParseFailureException {
        List<RelationBuilder> retVal = new ArrayList<>(this.relationships.size());
        List<RelationBuilder> retVal = new ArrayList<>(this.relationships.size());
        for (RelationshipType relType : this.relationships) {
            RelationBuilder builder = relType.createRelationBuilder(inStream);
            retVal.add(builder);
        }
        return retVal;
    }

    /**
     * Add a new relationship type to this entity type.
     *
     * @param rel	new relationship type
     */
    public void addRelationship(RelationshipType rel) {
        this.relationships.add(rel);
    }

    /**
     * @return a set of attribute builders for this entity type
     *
     * @param instanceStream	input stream containing the records from which the attributes will be built
     *
     * @throws ParseFailureException
     * @throws IOException
     */
    protected abstract Collection<? extends AttributeBuilder> getAttributeBuilders(FieldInputStream instanceStream) throws IOException, ParseFailureException;


}
