/**
 *
 */
package org.theseed.subsystems;

/**
 * This is the same as a strict variant code comparison, except that a missing subsystem is OK if matched with
 * a fully inactive (variant code =
 * @author Bruce Parrello
 *
 */
public class WeakVariantCodeComparator extends StrictVariantCodeComparator {

    public WeakVariantCodeComparator(IParms processor) {
        super(processor);
    }


    @Override
    public boolean okMissing(String v1) {
        return (VariantId.computeActiveLevel(v1).contentEquals("inactive"));
    }

}
