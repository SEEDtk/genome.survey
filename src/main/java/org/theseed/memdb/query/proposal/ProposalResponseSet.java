/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This object contains a set of proposal responses. The responses are associated with specific
 * parameter values for all the proposal fields. A response set can be marked as "inactive", which\
 * means we've decided to give up on it and will not add any more responses to it.
 *
 * @author Bruce Parrello
 *
 */
public class ProposalResponseSet implements Iterable<ProposalResponse> {

    // FIELDS
    /** parameterization of this response set */
    private Parameterization parameters;
    /** proposal responses for these parameters */
    private List<ProposalResponse> responses;
    /** TRUE if this response set is still active */
    private boolean activeFlag;
    /** cache for desired set of output values */
    private Set<String> outputValues;

    /**
     * Create a new, empty proposal response set for a query proposal.
     */
    public ProposalResponseSet(Parameterization parms) {
        this.parameters = parms;
        this.responses = new ArrayList<ProposalResponse>();
        this.activeFlag = true;
        this.outputValues = null;
    }

    /**
     * Add a new response to this set
     *
     * @param response	new response to add
     */
    public void addResponse(ProposalResponse response) {
        if (this.activeFlag)
            this.responses.add(response);
    }

    /**
     * @return the number of responses in this set
     */
    public int size() {
        return this.responses.size();
    }


    /**
     * @return the parameters
     */
    public Parameterization getParameters() {
        return this.parameters;
    }

    /**
     * @return the responses
     */
    public List<ProposalResponse> getResponses() {
        return this.responses;
    }

    /**
     * Get the set of response values for the specified entity type and attribute name. It is very possible
     * this could be larger or smaller than the number of elements in the response set.
     *
     * NOTE that for performance reasons we only compute the output set once. If you try again with different
     * a different entity/attribute you will get a bogus result.
     *
     * @param entityTypeName	name of the desired entity type containing the attribute
     * @param attrName			name of the desired attribute
     *
     * @return the set of response values
     */
    public Set<String> getOutputValues(String entityTypeName, String attrName) {
        // If we have not cached the output, compute it now.
        if (this.outputValues == null) {
            this.outputValues = new TreeSet<String>();
            for (ProposalResponse response : this.responses) {
                List<String> values = response.getValue(entityTypeName, attrName);
                this.outputValues.addAll(values);
            }
        }
        // Return the computed output value set.
        return this.outputValues;
    }

    /**
     * Mark this response set as inactive.
     */
    public void setInactive() {
        this.activeFlag = false;
        // Delete all the responses to save memory.
        this.responses.clear();
    }

    /**
     * @return TRUE if this response set is active, else FALSE
     */
    public boolean isActive() {
        return this.activeFlag;
    }

    /**
     * Determine whether or not it is safe to add a new response. It is unsafe if the response set is too big
     * or it is inactive.  If the response set is too big, it is made inactive here.
     *
     * @param maxResponseLimit	maximum permissible intermediate response set size
     *
     * @return TRUE if there is room for more responses, FALSE if this set is now inactive
     */
    public boolean checkStatus(int maxResponseLimit) {
        if (this.activeFlag) {
            if (this.responses.size() >= maxResponseLimit)
                this.setInactive();
        }
        return this.activeFlag;
    }

    @Override
    public String toString() {
        return String.format("ProposalResponseSet (size=%d) [parameters=%s]", this.responses.size(), this.parameters);
    }

    @Override
    public Iterator<ProposalResponse> iterator() {
        return this.responses.iterator();
    }

}
