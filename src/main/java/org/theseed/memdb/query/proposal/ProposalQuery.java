/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.query.QueryDbInstance;

/**
 * This object represents an actual proposal. The proposals come in two types-- list and count.
 * Each proposal consists of a text template and a proposal path.  The subclass contains the
 * specification used to produce the answers.
 *
 * @author Bruce Parrello
 *
 */
public abstract class ProposalQuery {

    // FIELDS
    /** list of entity proposals along the query path */
    private List<ProposalEntity> path;
    /** template string */
    private String questionString;
    /** pattern for finding attribute substitution elements */
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\{\\{([=<>?])?(\\w+\\.\\w+)(?::([^{}]+))?\\}\\}");

    /**
     * Initialize the proposal query.
     *
     * @param templateString	question template string
     * @param entityPath		path through the entities for the proposed query
     *
     * @throws ParseFailureException
     */
    public ProposalQuery(String templateString, String entityPath) throws ParseFailureException {
        this.questionString = templateString;
        // Set up the path through the database.
        String[] pathNames = StringUtils.split(entityPath);
        this.path = new ArrayList<ProposalEntity>(pathNames.length);
        for (String entity : pathNames)
            this.path.add(new ProposalEntity(entity));
        // Now get the proposal fields.
        Matcher m = FIELD_PATTERN.matcher(templateString);
        while (m.find()) {
            // Here we have a proposal field. The type is determined by the first group and the field string
            // is the second group.
            String typeChar = m.group(1);
            String fieldSpec = m.group(2);
            ProposalField proposalField;
            if (typeChar == null) {
                proposalField = new ExactProposalField(fieldSpec);
            } else switch (typeChar) {
            case "<" :
                proposalField = new LessThanProposalField(fieldSpec);
                break;
            case "=" :
                proposalField = new EqualProposalField(fieldSpec);
                break;
            case ">" :
                proposalField = new GreaterThanProposalField(fieldSpec);
                break;
            default :
                throw new ParseFailureException("Invalid field specification character \"" + typeChar + "\".");
            }
            // Find the entity for this attribute.
            final String entityTypeName = proposalField.getEntityType();
            Optional<ProposalEntity> targetEntity =
                    this.path.stream().filter(x -> x.getName().equals(entityTypeName)).findFirst();
            if (targetEntity.isEmpty())
                throw new ParseFailureException("Entity \"" + entityTypeName + "\" is not found in the proposed query path.");
            // Add the attribute to the entity.
            targetEntity.get().addAttribute(proposalField);
        }

    }

    /**
     * @return the size of the entity path
     */
    public int getPathSize() {
        return this.path.size();
    }

    /**
     * Compute good response sets for this proposal against the specified database instance.
     *
     * @param db	target database instance
     *
     * @return a list of proposal response sets containing valid answers
     */
    public List<ProposalResponseSet> computeSets(QueryDbInstance db) {
        // TODO code for computeSets
        return null;
    }

    /**
     * Write a response for this proposal to the output.
     *
     * @param response	response set containing answers
     * @param writer	output print writer
     */
    public abstract void writeResponse(ProposalResponseSet response, PrintWriter writer);

}