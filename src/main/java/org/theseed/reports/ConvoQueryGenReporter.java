/**
 *
 */
package org.theseed.reports;

import java.util.Collection;
import java.util.List;

import org.theseed.io.template.LineTemplate;
import org.theseed.memdb.query.proposal.Parameterization;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This report generates JSON text in a conversational format. Each conversation is coded as a list of hashes, each hash
 * having a "role" and "content" member. The question has a role of "user" and the expected response has a role of "assistant".
 * Currently, each output question will be a single conversation with one user and one assistant entry.
 *
 * @author Bruce Parrello
 *
 */
public class ConvoQueryGenReporter extends QueryGenReporter {

    // FIELDS
    /** output json list */
    private JsonArray jsonList;

    public ConvoQueryGenReporter(IParms processor) {
        super(processor);
    }

    @Override
    protected void startReport() {
        this.jsonList = new JsonArray();
    }

    @Override
    public void writeQuestion(Parameterization parms, String questionText, Collection<String> answers) {
        // Create a list of answers.
        JsonArray answerJson = new JsonArray();
        answerJson.addAll(answers);
        // Output the question with the answer list.
        this.outputQuestion(questionText, answerJson);

    }

    @Override
    public void writeQuestion(Parameterization parms, String questionText, int answer) {
        // Box the answer.
        Integer answerObj = answer;
        // Output the question with the single answer.
        this.outputQuestion(questionText, answerObj);
    }

    @Override
    public void writeQuestion(Parameterization parms, String questionText, String answer, Collection<String> distractors) {
        // Put the choices in the question list.
        List<String> choices = this.randomizeChoices(answer, distractors);
        String fullQuestion = questionText + " " + LineTemplate.conjunct("or", choices);
        // Output the question and answer.
        this.outputQuestion(fullQuestion, answer);
    }

    /**
     * Output a conversational question.
     *
     * @param questionText	text of the question itself
     * @param answerJson	object representing the answer
     */
    public void outputQuestion(String questionText, Object answerJson) {
        JsonArray convoJson = new JsonArray();
        convoJson.add(this.questionObject(questionText));
        // Format the answer description.
        JsonObject answerObject = new JsonObject();
        answerObject.put("role", "assistant");
        answerObject.put("content", answerJson);
        // Add it to the conversation.
        convoJson.add(answerObject);
        // Save the conversation.
        this.jsonList.add(convoJson);
    }

    /**
     * Create a question object using the specified question text.
     *
     * @param questionText	question to put in the object
     */
    public JsonObject questionObject(String questionText) {
        JsonObject retVal = new JsonObject();
        retVal.put("role", "user");
        retVal.put("content", questionText);
        return retVal;
    }


    @Override
    public void finishReport() {
        // This is the point where we write out all the JSON.
        this.outputAllJson(this.jsonList);
    }

}
