/**
 *
 */
package org.theseed.memdb.query.proposal;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;

/**
 * This field proposal is satisfied when the specified field has a value numerically equal
 * to a parameter.
 *
 * @author Bruce Parrello
 *
 */
public class EqualProposalField extends BinaryProposalField {

    // FIELDS
    /** target numeric value */
    private double target;

    /**
     * Construct a numeric-equality field proposal
     *
     * @param fieldSpec		field specification (entity.name)
     *
     * @throws ParseFailureException
     */
    public EqualProposalField(String fieldSpec) throws ParseFailureException {
        super(StringUtils.substringBefore(fieldSpec, ":"));
        try {
            this.target = Double.valueOf(StringUtils.substringAfter(fieldSpec, ":"));
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Invalid target on field specification \"" + fieldSpec + "\".");
        }
    }

}
