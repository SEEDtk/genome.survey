/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.Attribute;
import org.theseed.memdb.query.QueryEntityInstance;

/**
 * This field proposal is satisfied when the value of the field contains the parameter value,
 * when the field is interpreted as a list, each list element generates its own output question.
 *
 * @author Bruce Parrello
 *
 */
public class ExactProposalField extends ProposalField {

    /**
     * Construct a new string-match proposal field.
     *
     * @param fieldSpec		field specification (entity.name)
     * 
     * @throws ParseFailureException
     */
    public ExactProposalField(String fieldSpec) throws ParseFailureException {
        super(fieldSpec);
    }

    @Override
    protected List<String> getValue(QueryEntityInstance instance) {
        Attribute instanceVal = instance.getAttribute(this.getName());
        return instanceVal.getList();
    }

}
