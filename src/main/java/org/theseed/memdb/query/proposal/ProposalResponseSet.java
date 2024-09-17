/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.ArrayList;
import java.util.List;

/**
 * This object contains a set of proposal responses. The responses are associated with specific
 * parameter values for all the proposal fields.
 *
 * @author Bruce Parrello
 *
 */
public class ProposalResponseSet {

    // FIELDS
    /** parameterization of this response set */
    private Parameterization parameters;
    /** proposal responses for these parameters */
    private List<ProposalResponse> responses;

    /**
     * Create a new, empty proposal response set for a query proposal.
     */
    public ProposalResponseSet(Parameterization parms) {
        this.parameters = parms;
        this.responses = new ArrayList<ProposalResponse>();
    }

    /**
     * Add a new response to this set
     *
     * @param response	new response to add
     */
    public void addResponse(ProposalResponse response) {
        this.responses.add(response);
    }

}
