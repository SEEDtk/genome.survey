/**
 *
 */
package org.theseed.reports;

import org.theseed.genome.Feature;

/**
 * This is the basic report that creates a file for mapping peg IDs to role IDs.
 *
 * @author Bruce Parrello
 *
 */
public class TextRolePegReporter extends RolePegReporter {

    public TextRolePegReporter(IParms processor) {
    }

    @Override
    protected void startReport() {
        this.println("peg_id\trole_id");
    }

    @Override
    public void recordPeg(Feature feat, String role) {
        this.println(feat.getId() + "\t" + role);
    }

    @Override
    public void closeReport() {
    }

}
