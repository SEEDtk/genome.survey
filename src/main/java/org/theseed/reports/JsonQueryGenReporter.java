/**
 *
 */
package org.theseed.reports;

import java.util.Collection;

import org.theseed.memdb.query.proposal.Parameterization;
import org.theseed.memdb.query.proposal.ProposalQuery;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This generates standard JSON output for test questions and answers. We build the question objects in memory and
 * then write them at the end.
 *
 * There are certain constant fields required in every question object. These are provided by the controlling
 * command processor and copied to each question's JSON object.
 *
 * @author Bruce Parrello
 *
 */
public class JsonQueryGenReporter extends QueryGenReporter {

    // FIELDS
    /** map of additional properties */
    private final JsonObject constantJson;
    /** number of JSON objects written */
    private int outCount;

    /**
     * Initialize this report.
     *
     * @param processor		controlling command processor
     */
    public JsonQueryGenReporter(IParms processor) {
        super(processor);
        // Store the constants required for every output question.
        this.constantJson = processor.getConstantJson();
        // Denote we have not written any objects yet.
        this.outCount = 0;
    }

    @Override
    protected void startReport() {
        // Start the output json list.
        this.write("[");        
    }

    /**
     * @return a new JSON object with the constants and the question text filled in
     *
     * @param questionText	text for the question
     * @param parms		    parameterization for the question
     */
    private JsonObject getNewJson(String questionText, Parameterization parms) {
        JsonObject retVal = new JsonObject();
        retVal.putAll(this.constantJson);
        retVal.put("question", questionText);
        // Add the template data.
        ProposalQuery query = this.getQuery();
        retVal.put("template", query.getRawQuestion());
        retVal.put("path", query.getPath());
        retVal.put("result", query.getResult());
        // Save the parameterization. All this helps us with verification later.
        JsonObject parmJson = parms.toJson();
        retVal.put("parameters", parmJson);
        return retVal;
    }

    @Override
    public void writeQuestion(Parameterization parms, String questionText, Collection<String> answers) {
        JsonObject outputJson = this.getNewJson(questionText, parms);
        JsonArray answerList = new JsonArray();
        answerList.addAll(answers);
        outputJson.put("correct_answers", answerList);
        this.writeJson(outputJson);
    }

    @Override
    public void writeQuestion(Parameterization parms, String questionText, int answer) {
        JsonObject outputJson = this.getNewJson(questionText, parms);
        outputJson.put("correct_answer", answer);
        this.writeJson(outputJson);
    }

    @Override
    public void writeQuestion(Parameterization parms, String questionText, String answer, Collection<String> distractors) {
        JsonObject outputJson = this.getNewJson(questionText, parms);
        outputJson.put("correct_answer", answer);
        JsonArray wrongList = new JsonArray();
        wrongList.addAll(distractors);
        outputJson.put("distractors", wrongList);
        this.writeJson(outputJson);
    }

    /**
     * Write a JSON object to the output.
     * 
     * @param outputJson    JSON object to write
     */
    private void writeJson(JsonObject outputJson) {
        // If this is not the first object, we need a comma and a new-line first.
        if (this.outCount > 0)
            this.write(",");
        String jsonString = outputJson.toJson();
        this.writeNoNL("    " + jsonString);
        this.outCount++;
    }

    @Override
    public void finishReport() {
        // If we wrote something, we need a new line before the closing bracket.
        if (this.outCount > 0)
            this.write("");
        // Close the JSON list.
        this.write("]");
    }

}
