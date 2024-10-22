/**
 *
 */
package org.theseed.reports;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Collection;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

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
    /** accumulated JSON object */
    private JsonArray questionJson;
    /** map of additional properties */
    private JsonObject constantJson;

    /**
     * Initialize this report.
     *
     * @param processor		controlling command processor
     */
    public JsonQueryGenReporter(IParms processor) {
        super(processor);
        // Store the constants required for every output question.
        this.constantJson = processor.getConstantJson();
    }

    @Override
    protected void startReport() {
        // Create the output JSON list.
        this.questionJson = new JsonArray();
    }

    /**
     * @return a new JSON object with the constants and the question text filled in
     *
     * @param questionText	text for the question
     */
    private JsonObject getNewJson(String questionText) {
        JsonObject retVal = new JsonObject();
        retVal.putAll(this.constantJson);
        retVal.put("question", questionText);
        return retVal;
    }

    @Override
    public void writeQuestion(String questionText, Collection<String> answers) {
        JsonObject outputJson = this.getNewJson(questionText);
        JsonArray answerList = new JsonArray();
        answerList.addAll(answers);
        outputJson.put("correct_answers", answerList);
        this.questionJson.add(outputJson);
    }

    @Override
    public void writeQuestion(String questionText, int answer) {
        JsonObject outputJson = this.getNewJson(questionText);
        outputJson.put("correct_answer", answer);
        this.questionJson.add(outputJson);
    }

    @Override
    public void writeQuestion(String questionText, String answer, Collection<String> distractors) {
        JsonObject outputJson = this.getNewJson(questionText);
        outputJson.put("correct_answer", answer);
        JsonArray wrongList = new JsonArray();
        wrongList.addAll(distractors);
        outputJson.put("distractors", wrongList);
        this.questionJson.add(outputJson);
    }

    @Override
    public void finishReport() {
        // This is the point where we actually output the JSON.
        try {
            String jsonString = Jsoner.serialize(this.questionJson);
            StringReader jsonReader = new StringReader(jsonString);
            Jsoner.prettyPrint(jsonReader, this.getWriter(), "    ", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JsonException e) {
            throw new RuntimeException("JSON output error: " + e.getMessage());
        }
    }

}
