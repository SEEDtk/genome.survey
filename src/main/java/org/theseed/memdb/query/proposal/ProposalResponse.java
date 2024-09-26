/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.theseed.io.Attribute;
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
    /** empty string for failure case */
    private static List<String> EMPTY_LIST = Collections.emptyList();

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

    /**
     * @return the value of the named entity attribute as a list
     *
     * @param entityName	name of the entity of interest
     * @param attrName		name of the attribute of interest
     */
    public List<String> getValue(String entityName, String attrName) {
        List<String> retVal;
        QueryEntityInstance entityInstance = null;
        for (QueryEntityInstance qInstance : this.instancePath) {
            if (entityName.equals(qInstance.getType()))
                entityInstance = qInstance;
        }
        if (entityInstance == null)
            retVal = EMPTY_LIST;
        else {
            Attribute attr = entityInstance.getAttribute(attrName);
            retVal = attr.getList();
        }
        return retVal;
    }

    @Override
    public String toString() {
        String retVal;
        if (this.instancePath.isEmpty())
            retVal = "<null>";
        else
            retVal = this.instancePath.stream().map(x -> x.toString()).collect(Collectors.joining(",", "Response:", ""));
        return retVal;
    }

}
