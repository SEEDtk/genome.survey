/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.ArrayList;
import java.util.List;

/**
 * This object represents a single entity along the query path for a query proposal. It contains
 * the entity name and a list of the attribute proposals.
 *
 * @author Bruce Parrello
 *
 */
public class ProposalEntity {

    // FIELDS
    /** entity type name */
    private String entityName;
    /** list of attribute proposals */
    private List<ProposalField> fields;

    /**
     * Construct a new, blank proposal entity.
     */
    public ProposalEntity(String name) {
        this.entityName = name;
        this.fields = new ArrayList<ProposalField>(5);
    }

    /**
     * Add a new attribute proposal.
     */
    public void addAttribute(ProposalField field) {
        this.fields.add(field);
    }

    /**
     * @return the name of this proposal's entity
     */
    public String getName() {
        return this.entityName;
    }

    /**
     * @return the list of attribute proposals
     */
    protected List<ProposalField> getProposals() {
        return this.fields;
    }

    /**
     * @return the index of a field's proposal, or -1 if the proposal does not exist
     *
     * @param name		field name
     */
    public int getFieldIdx(String name) {
        int retVal = this.fields.size() - 1;
        while (retVal >= 0 && ! name.equals(this.fields.get(retVal).getName())) retVal--;
        return retVal;
    }

    /**
     * @return the number of proposal fields in this entity proposal
     */
    public int size() {
        return this.fields.size();
    }

}
