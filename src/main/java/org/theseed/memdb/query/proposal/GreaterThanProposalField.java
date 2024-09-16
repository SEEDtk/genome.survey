/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;

/**
 * This field specification is satisfied when the field value, interpreted numerically, is greater than
 * the parameter.
 *
 * @author Bruce Parrello
 *
 */
public class GreaterThanProposalField extends BinaryProposalField {

    /** lower bound for values that satisfy this proposal */
    private double bound;

    /**
     * Construct a numerically-greater field proposal.
     *
     * @param fieldSpec		field specification
     *
     * @throws ParseFailureException
     */
    public GreaterThanProposalField(String fieldSpec) throws ParseFailureException {
        super(StringUtils.substringBefore(fieldSpec, ":"));
        try {
            this.bound = Double.valueOf(StringUtils.substringAfter(fieldSpec, ":"));
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Invalid bound on field specification \"" + fieldSpec + "\".");
        }
    }

}
