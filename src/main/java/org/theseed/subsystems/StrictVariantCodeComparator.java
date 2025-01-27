/**
 *
 */
package org.theseed.subsystems;

/**
 * This is a strict variant code comparison. The two codes match if they are the same.
 *
 * @author Bruce Parrello
 *
 */
public class StrictVariantCodeComparator extends VariantCodeComparator {

    public StrictVariantCodeComparator(IParms processor) {
        super(processor);
    }

    @Override
    public boolean matches(String v1, String v2) {
        return v1.equals(v2);
    }

    @Override
    public boolean okMissing(String v1) {
        return false;
    }

}
