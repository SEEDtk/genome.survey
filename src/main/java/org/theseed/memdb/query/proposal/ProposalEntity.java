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

}
