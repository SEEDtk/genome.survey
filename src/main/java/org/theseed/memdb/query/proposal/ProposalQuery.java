/**
 *
 */
package org.theseed.memdb.query.proposal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.query.QueryDbInstance;
import org.theseed.memdb.query.QueryEntityInstance;

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
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ProposalQuery.class);
    /** list of entity proposals along the query path */
    private List<ProposalEntity> path;
    /** template string */
    private String questionString;
    /** cutoff limit for response set sizes */
    private int maxResponseLimit;
    /** pattern for finding attribute substitution elements */
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\{\\{([=<>?])?(\\w+\\.\\w+)(?::([^{}]+))?\\}\\}");

    /**
     * Initialize the proposal query.
     *
     * @param templateString	question template string
     * @param entityPath		path through the entities for the proposed query
     * @param maxLimit			maximum acceptable response limit (for performance)
     *
     * @throws ParseFailureException
     */
    public ProposalQuery(String templateString, String entityPath, int maxLimit) throws ParseFailureException {
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
            String parm = m.group(3);
            ProposalField proposalField;
            if (typeChar == null) {
                proposalField = new ExactProposalField(fieldSpec);
            } else switch (typeChar) {
            case "<" :
                proposalField = new LessThanProposalField(fieldSpec, parm);
                break;
            case "=" :
                proposalField = new EqualProposalField(fieldSpec, parm);
                break;
            case ">" :
                proposalField = new GreaterThanProposalField(fieldSpec, parm);
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
        // Save the response set size cutoff.
        this.maxResponseLimit = maxLimit;

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
        // Create a map of parameterizations to response sets.
        Map<Parameterization, ProposalResponseSet> currentMap = new HashMap<Parameterization, ProposalResponseSet>();
        // We start with the first entity in the query and process all its records into response sets.
        ProposalEntity originEntity = this.path.get(0);
        // Get the instances for that entity.
        Collection<EntityInstance> originInstances = db.getAllEntities(originEntity.getName());
        for (var originInstance : originInstances) {
            // Get this instance as a query entity instance.
            QueryEntityInstance queryInstance = (QueryEntityInstance) originInstance;
            // Compute all its parameterizations.
            Parameterization instanceParms = new Parameterization();
            Set<Parameterization> allInstanceParms = instanceParms.addInstance(queryInstance, originEntity);
            // Now we have all the parameterizations for which this entity instance should be included in a
            // proposal response set. It's possible this could be zero. It is usually one. We need to add it
            // to any pre-existing response set with the same parameters, or create a new response set if there
            // is none.
            for (Parameterization parms : allInstanceParms) {
                ProposalResponseSet responses = currentMap.computeIfAbsent(parms, x -> new ProposalResponseSet(x));
                // If this response set is acceptable, add the new response.
                if (responses.checkStatus(this.maxResponseLimit))
                    responses.addResponse(new ProposalResponse(queryInstance));
            }
        }
        // Now we need to add records for the remaining entities of the path. We will build these in this
        // new map.
        for (int i = 1; i < this.path.size(); i++) {
            Map<Parameterization, ProposalResponseSet> newMap = new HashMap<Parameterization, ProposalResponseSet>();
            ProposalEntity currEntity = this.path.get(i);
            // We need to process each response. Note that a response is a sequence of entity instances along the
            // path. For each response, we cross the relationship of interest and parameterize the entity instances
            // found there.
            for (ProposalResponseSet responseSet : currentMap.values()) {
                // Get this response set's parameterization.
                Parameterization mainParms = responseSet.getParameters();
                for (ProposalResponse response : responseSet.getResponses()) {
                    // Here we have a response containing instances of all the previous entities on
                    // the path. Get all the new entity instances further down the path.
                    QueryEntityInstance endInstance = response.getLastEntity();
                    Collection<EntityInstance> newInstances = endInstance.getTargetsOfType(db, currEntity.getName());
                    for (var newInstance : newInstances) {
                        // Create a response that has the new entity instance in it.
                        QueryEntityInstance queryInstance = (QueryEntityInstance) newInstance;
                        // Get the parameterizations for this new response.
                        Set<Parameterization> newParms = mainParms.addInstance(queryInstance, currEntity);
                        // Put them in the map.
                        for (Parameterization parms : newParms) {
                            ProposalResponseSet responses = newMap.computeIfAbsent(parms, x -> new ProposalResponseSet(x));
                            // If this response set is acceptable, add the new response.
                            if (responses.checkStatus(this.maxResponseLimit))
                                responses.addResponse(new ProposalResponse(response, queryInstance));
                        }
                    }
                }
            }
            // Discard the old map and use the new one next time.
            currentMap = newMap;
        }
        // Return a list of the response sets found.
        List<ProposalResponseSet> retVal = currentMap.values().stream().filter(x -> x.isActive()).collect(Collectors.toList());
        return retVal;
    }

    /**
     * Write a response for this proposal to the output.
     *
     * @param response	response set containing answers
     * @param writer	output print writer
     */
    public abstract void writeResponse(ProposalResponseSet response, PrintWriter writer);

    /**
     * Write the question statement for this proposal to the output.
     *
     * @param response	response set containing answers
     * @param writer	output print writer
     */
    protected void writeQuestion(ProposalResponseSet response, PrintWriter writer) {
        // Get the parameterization.
        Parameterization parms = response.getParameters();
        // We will build the output line in here.
        StringBuilder outputLine = new StringBuilder(this.questionString.length());
        // We need to loop through the question string, putting in the parameters.
        Matcher m = FIELD_PATTERN.matcher(this.questionString);
        // Denote we're starting at the beginning of the string.
        int processed = 0;
        // Find the next parameter mark.
        while (m.find()) {
            // Get the field specification.
            String fieldSpec = m.group(2);
            // Copy the clear text.
            outputLine.append(this.questionString.subSequence(processed, m.start()));
            // Get the parameter value.
            String value = parms.getValue(this, fieldSpec);
            outputLine.append(value);
            // Set up for the next search.
            processed = m.end();
        }
        // Copy the residual.
        outputLine.append(this.questionString.substring(processed));
        // Write this output line as a question.
        writer.println(outputLine.toString());
    }

    /**
     * Find the proposal for the specified entity name.
     *
     * @param name	target entity name
     *
     * @return the proposal for the entity with that name
     */
    public ProposalEntity getEntity(String name) {
        ProposalEntity retVal = null;
        for (ProposalEntity entity : this.path) {
            if (name.equals(entity.getName()))
                retVal = entity;
        }
        return retVal;
    }

    /**
     * @return the size of the output for this proposal query given the specified response set
     *
     * @param responseSet	response set to check
     */
    public abstract int getResponseSize(ProposalResponseSet responseSet);

}
