/**
 *
 */
package org.theseed.memdb.query.proposal;

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
import org.theseed.reports.QueryGenReporter;

import com.github.cliftonlabs.json_simple.JsonArray;

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
    private static final Logger log = LoggerFactory.getLogger(ProposalQuery.class);
    /** list of entity proposals along the query path */
    private List<ProposalEntity> path;
    /** template string */
    private String questionString;
    /** cutoff limit for response set sizes */
    private final int maxResponseLimit;
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
        this.path = new ArrayList<>(pathNames.length);
        for (String entity : pathNames)
            this.path.add(new ProposalEntity(entity));
        // Now get the proposal fields.
        Matcher m = FIELD_PATTERN.matcher(templateString);
        while (m.find()) {
            // Here we have a proposal field. The type is determined by the first matched group and the field string
            // is the second group.
            String typeChar = m.group(1);
            String fieldSpec = m.group(2);
            String parm = m.group(3);
            ProposalField proposalField;
            if (typeChar == null) {
                proposalField = new ExactProposalField(fieldSpec);
            } else switch (typeChar) {
            case "<" -> proposalField = new LessThanProposalField(fieldSpec, parm);
            case "=" -> proposalField = new EqualProposalField(fieldSpec, parm);
            case ">" -> proposalField = new GreaterThanProposalField(fieldSpec, parm);
            default -> throw new ParseFailureException("Invalid field specification character \"" + typeChar + "\".");
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
        Map<Parameterization, ProposalResponseSet> currentMap = new HashMap<>();
        // Compute the response entity type.
        ProposalEntity responseEntity = this.getResponseEntity();
        // We start with the first entity in the query and process all its records into response sets.
        ProposalEntity originEntity = this.path.get(0);
        // Get the instances for that entity.
        Collection<EntityInstance> originInstances = db.getSomeEntities(originEntity.getName(), this.maxResponseLimit);
        log.info("Processing entity {} (1 of {}). {} instances.", originEntity.getName(), this.path.size(), originInstances.size());
        for (var originInstance : originInstances) {
            // Get this instance as a query entity instance.
            QueryEntityInstance queryInstance = (QueryEntityInstance) originInstance;
            // Compute all its parameterizations.
            Parameterization instanceParms = new Parameterization();
            Set<Parameterization> allInstanceParms = instanceParms.addInstance(queryInstance, originEntity);
            if (log.isDebugEnabled() && originEntity.equals(responseEntity)) {
                String action = (allInstanceParms.isEmpty() ? "Rejecting" : "Accepting");
                log.debug("{} instance {}.", action, queryInstance);
            }
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
            Map<Parameterization, ProposalResponseSet> newMap = new HashMap<>();
            ProposalEntity currEntity = this.path.get(i);
            log.info("Processing entity {} ({} of {}). {} sets in map.", currEntity.getName(), i + 1, this.path.size(), currentMap.size());
            // We need to process each response. Note that a response is a sequence of entity instances along the
            // path. For each response, we cross the relationship of interest and parameterize the entity instances
            // found there. We'll count the number of new responses created and the number rejected.
            int newResponseCount = 0;
            int rejectCount = 0;
            int tooBigCount = 0;
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
                        if (newParms.isEmpty()) {
                            rejectCount++;
                            log.debug("Rejecting response {} with new instance {}.", response, queryInstance);
                        } else {
                            log.debug("Accepting response {} with {} parameterizations and new instance {}.", 
                            response, newParms.size(), queryInstance);
                        }
                        // Put them in the map.
                        for (Parameterization parms : newParms) {
                            ProposalResponseSet responses = newMap.computeIfAbsent(parms, x -> new ProposalResponseSet(x));
                            // If this response set is acceptable, add the new response.
                            if (responses.isActive()) {
                                if (responses.checkStatus(this.maxResponseLimit)) {
                                    responses.addResponse(new ProposalResponse(response, queryInstance));
                                    newResponseCount++;
                                } else {
                                    tooBigCount++;
                                    responses.countResponse();
                                }
                            } else
                                responses.countResponse();
                        }
                    }
                }
            }
            log.info("Created {} new responses, {} rejected, {} sets too big.", newResponseCount, rejectCount, tooBigCount);
            if (log.isInfoEnabled()) {
                // Here we want to determine the number of inactive response sets and their average size.
                int inactiveCount = 0;
                int totalInactiveSize = 0;
                for (var responses : newMap.values()) {
                    if (!responses.isActive()) {
                        inactiveCount++;
                        totalInactiveSize += responses.getResponseCount();
                    }
                }
                if (inactiveCount > 0) {
                    double avgSize = (double) totalInactiveSize / (double) inactiveCount;
                    log.info("{} inactive sets with an average size of {}.", inactiveCount, String.format("%.1f", avgSize));
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
     * @return the proposal for the entity relevant to the response
     */
    protected abstract ProposalEntity getResponseEntity();

    /**
     * Write a response for this proposal to the output.
     *
     * @param response	response set containing answers
     * @param reporter	output report writer
     * @param others	full list of response sets for this query
     */
    public abstract void writeResponseDetails(ProposalResponseSet response, QueryGenReporter reporter, List<ProposalResponseSet> others);

    /**
     * Formulate the question statement for this proposal.
     *
     * @param response	response set containing answers
     */
    protected String computeQuestion(ProposalResponseSet response) {
        // Get the parameterization.
        Parameterization parms = response.getParameters();
        // We will build the question text in here.
        StringBuilder retVal = new StringBuilder(this.questionString.length());
        // We need to loop through the question string, putting in the parameters.
        Matcher m = FIELD_PATTERN.matcher(this.questionString);
        // Denote we're starting at the beginning of the string.
        int processed = 0;
        // Find the next parameter mark.
        while (m.find()) {
            // Get the field specification.
            String fieldSpec = m.group(2);
            // Copy the clear text.
            retVal.append(this.questionString.subSequence(processed, m.start()));
            // Get the parameter value.
            String value = parms.getValue(this, fieldSpec);
            retVal.append(value);
            // Set up for the next search.
            processed = m.end();
        }
        // Copy the residual.
        retVal.append(this.questionString.substring(processed));
        // Return the question text.
        return retVal.toString();
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

    /**
     * @return the last proposal entity on the query path
     */
    public ProposalEntity getEndOfPath() {
        return this.path.get(this.path.size() - 1);
    }

    @Override
    public String toString() {
        return String.format("ProposalQuery [%s]", this.questionString);
    }

    /**
     * This is the main entry point for the response output. It saves the query template and then calls through to write
     * the output.
     *
     * @param correctResponse	correct response to use
     * @param reporter			output report writer
     * @param responses			alternative responses to scan
     */
    public void writeResponse(ProposalResponseSet correctResponse, QueryGenReporter reporter, List<ProposalResponseSet> responses) {
        reporter.saveTemplate(this);
        this.writeResponseDetails(correctResponse, reporter, responses);
    }

    /**
     * @return the question template string
     */
    public String getRawQuestion() {
        return this.questionString;
    }

    /**
     * @return the path through the database for this question
     */
    public JsonArray getPath() {
        JsonArray retVal = new JsonArray();
        this.path.stream().forEach(x -> retVal.add(x.getName()));
        return retVal;
    }

    /**
     * @return a string description of this query's desired result
     */
    public abstract String getResult();

}
