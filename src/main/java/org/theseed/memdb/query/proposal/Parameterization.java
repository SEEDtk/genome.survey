/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.theseed.memdb.query.QueryEntityInstance;

/**
 * A parameterization is a list of strings containing the values for the variable parameters in
 * a proposal set. For each entity type, we have the proposal field values in order.
 *
 * @author Bruce Parrello
 *
 */
public class Parameterization {

    // FIELDS
    /** map from entity type names to value lists */
    private Map<String, List<String>> valueMap;

    /**
     * Construct a blank parameterization.
     */
    public Parameterization() {
        this.valueMap = new TreeMap<String, List<String>>();
    }

    /**
     * Create a clone of an existing parameterization.
     *
     * @param source		original parameterization to copy
     */
    public Parameterization(Parameterization source) {
        this.valueMap = new TreeMap<String, List<String>>();
        for (var valueEntry : source.valueMap.entrySet())
            this.valueMap.put(valueEntry.getKey(), valueEntry.getValue());
    }

    /**
     * Add the parameterization of an entity instance to this one. This could result in multiple
     * output parameterizations, since a list value will cause replication. Conversely, an unsatisfied
     * binary proposal could cause all the parameterizations to disappear.
     *
     * @param instance	entity instance of interest
     * @param proposal	query proposal relevant to the entity instance
     *
     * @return the resulting set of parameterizations, none of which will be this one
     */
    public Set<Parameterization> addInstance(QueryEntityInstance instance, ProposalEntity proposal) {
        // Start off returning just this parameterization. This is the most common case.
        Set<Parameterization> retVal = new TreeSet<Parameterization>();
        retVal.add(new Parameterization(this));
        // Loop through the field proposals.
        Iterator<ProposalField> iter = proposal.getProposals().iterator();
        while (iter.hasNext() && retVal != null) {
            // Get the value for this field.
            ProposalField fieldProposal = iter.next();
            List<String> value = fieldProposal.getValue(instance);
            if (value.isEmpty()) {
                // We got no values back, so the entity instance is invalid.
                retVal = null;
            } else if (value.size() == 1) {
                // We got one value back. Add it to all our return parameterizations.
                final String vString = value.get(0);
                retVal.stream().forEach(x -> x.addValue(proposal, vString));
            } else {
                // We have multiple values back. We need to explode each parameterization for
                // each value.  Loop through the return parameterizations. We'll put the new
                // parameterizations in here.
                List<Parameterization> newParms = new ArrayList<Parameterization>(value.size() * retVal.size());
                for (Parameterization retParm : retVal) {
                    // Explode this parameterization, adding all values but the first.
                    for (int i = 1; i < value.size(); i++) {
                        Parameterization newParm = new Parameterization(retParm);
                        newParm.addValue(proposal, value.get(i));
                        newParms.add(newParm);
                    }
                    // Add the first value to the original parameterization.
                    retParm.addValue(proposal, value.get(0));
                }
                // Add the exploded parameterizations to the output set.
                retVal.addAll(newParms);
            }
        }
        // Convert a failure return to an empty set.
        if (retVal == null)
            retVal = Collections.emptySet();
        return retVal;
    }

       /**
        * Add a new value to the parameter list for the specified proposal's entity type.
        *
     * @param proposal		query proposal for the target entity
     * @param vString		value to add to its list
     */
    private void addValue(ProposalEntity proposal, String vString) {
        List<String> vList = this.valueMap.computeIfAbsent(proposal.getName(),
                x -> new ArrayList<String>(proposal.size()));
        vList.add(vString);
    }

    /**
     * Compute the value to substitute in for this parameterization given an entity and attribute name.
     *
     * @param query				governing proposal query
     * @param fieldSpec			field specification (entity.name)
     *
     * @return the field value to output
     */
    public String getValue(ProposalQuery query, String fieldSpec) {
        // Split the field spec into an entity name and an attribute name.
        String[] parts = StringUtils.split(fieldSpec, ".");
        // Get the entity proposal.
        ProposalEntity entity = query.getEntity(parts[0]);
        if (entity == null)
            throw new IllegalArgumentException("Cannot find entity for field specification \"" + fieldSpec + "\".");
        // Find the parameter value index.
        int idx = entity.getFieldIdx(parts[1]);
        String retVal;
        if (idx < 0) {
            // Invalid attribute name, so just plug in the field spec.
            retVal = fieldSpec;
        } else {
            // Get the value list for this entity.
            List<String> parms = this.valueMap.get(parts[0]);
            // Extract the listed parameter.
            retVal = parms.get(idx);
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        int retVal = 1;
        for (var thisEntry : this.valueMap.entrySet()) {
            retVal = retVal * 31 + thisEntry.getKey().hashCode();
            List<String> thisList = thisEntry.getValue();
            for (String value : thisList)
                retVal = retVal * 29 + value.hashCode();
        }
        return retVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Parameterization other = (Parameterization) obj;
        for (var thisEntry : this.valueMap.entrySet()) {
            List<String> otherValue = other.valueMap.get(thisEntry.getKey());
            List<String> thisValue = thisEntry.getValue();
            if (otherValue == null || otherValue.size() != thisValue.size())
                return false;
            for (int i = 0; i < otherValue.size(); i++) {
                if (! thisValue.get(i).equals(otherValue.get(i)))
                    return false;
            }
        }
        return true;
    }

}
