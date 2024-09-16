/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;

/**
 * This field proposal is satisfied if the field value, interpreted numerically, is less than the parameter
 * value.
 *
 * @author Bruce Parrello
 *
 */
public class LessThanProposalField extends BinaryProposalField {

    // FIELDS
    /** upper bound for values that satisfy this proposal */
    private double bound;

    /**
     * Construct a numerically-less field proposal.
     *
     * @param fieldSpec		field specification (entity.name)
     *
     * @throws ParseFailureException
     */
    public LessThanProposalField(String fieldSpec) throws ParseFailureException {
        super(StringUtils.substringBefore(fieldSpec, ":"));
        try {
            this.bound = Double.valueOf(StringUtils.substringAfter(fieldSpec, ":"));
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Invalid bound on field specification \"" + fieldSpec + "\".");
        }
    }

}
