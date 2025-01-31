/**
 *
 */
package org.theseed.subsystems;

/**
 * This variant-code comparator returns TRUE if both variants are active or if both are inactive.
 *
 * @author Bruce Parrello
 *
 */
public class ActiveVariantCodeComparaotr extends VariantCodeComparator {

    public ActiveVariantCodeComparaotr(IParms processor) {
        super(processor);
    }

    @Override
    public boolean matches(String v1, String v2) {
        return VariantId.isActive(v1) == VariantId.isActive(v2);
    }

    @Override
    public boolean okMissing(String v1) {
        return ! VariantId.isActive(v1);
    }

}
