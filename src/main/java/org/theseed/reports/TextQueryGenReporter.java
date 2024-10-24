/**
 *
 */
package org.theseed.reports;

import java.util.Collection;
import java.util.List;

/**
 * This reporter produces a simple line-by-line text file of the questions, designed to be human-readable.
 *
 * @author Bruce Parrello
 *
 */
public class TextQueryGenReporter extends QueryGenReporter {

    // FIELDS
    /** labels for possible choices */
    private static String[] LABELS = new String[] { "A", "B", "C", "D" };

    public TextQueryGenReporter(IParms processor) {
        super(processor);
    }

    @Override
    protected void startReport() {
    }

    @Override
    public void writeQuestion(String questionText, Collection<String> answers) {
        // Here we have a question with multiple correct answers.
        this.write(questionText);
        this.write("* Correct answers:");
        for (String answer : answers)
            this.write("    " + answer);
    }

    @Override
    public void writeQuestion(String questionText, int answer) {
        // Here we have a question with a numeric correct answer.
        this.write(questionText);
        this.write("* Correct answer: " + answer);
    }

    @Override
    public void writeQuestion(String questionText, String answer, Collection<String> distractors) {
        // Here we have a multiple-choice question, the most complicated one.
        this.write(questionText);
        // Put the answer in with the distractors and shuffle the result.
        List<String> choices = this.randomizeChoices(answer, distractors);
        // Write the choices.
        String correctChoice = "";
        for (int i = 0; i < choices.size(); i++) {
            String choice = choices.get(i);
            this.write("   " + LABELS[i] + ") " + choice);
            if (answer.contentEquals(choice))
                correctChoice = LABELS[i];
        }
        this.write("* Correct answer: " + correctChoice);
    }

    @Override
    public void finishReport() {
    }

}
