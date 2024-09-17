/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.ArrayList;
import java.util.List;

import org.theseed.memdb.query.QueryEntityInstance;

/**
 * This is a list of entity instances representing a possible answer to a query proposal.
 * Proposal responses are organized into response sets. Our goal is to get a response set
 * smaller than the target size.
 *
 * @author Bruce Parrello
 *
 */
public class ProposalResponse {

    // FIELDS
    /** sequence of related entity instances for this response */
    private List<QueryEntityInstance> instancePath;

    /**
     * Construct a proposal response with a single entity instance.
     *
     * @param instance	first entity instance in the path
     */
    public ProposalResponse(QueryEntityInstance instance) {
        this.instancePath = List.of(instance);
    }

    /**
     * Construct a proposal response with an additional entity instance.
     *
     * @param oldResponse	previous response
     * @param instance		instance to add at the end of the path
     */
    public ProposalResponse(ProposalResponse oldResponse, QueryEntityInstance instance) {
        this.instancePath = new ArrayList<QueryEntityInstance>(oldResponse.size() + 1);
        this.instancePath.addAll(oldResponse.instancePath);
        this.instancePath.add(instance);
    }

    /**
     * @return the number of entity instances in the path
     */
    public int size() {
        return this.instancePath.size();
    }

    /**
     * @return the instance of the last entity in the path.
     */
    public QueryEntityInstance getLastEntity() {
        final int n = this.instancePath.size() - 1;
        return this.instancePath.get(n);
    }

}
