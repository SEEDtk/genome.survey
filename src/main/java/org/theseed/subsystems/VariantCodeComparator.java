/**
 *
 */
package org.theseed.subsystems;

/**
 * This is the base class for all variant-code comparison algorithms used by the subsystem
 * comparison processor.
 *
 * @author Bruce Parrello
 *
 */
public abstract class VariantCodeComparator {

    /**
     * Type of comparison algorithm
     */
    public static enum Type {
        /** strict lexical equality */
        STRICT {
            @Override
            public VariantCodeComparator create(IParms processor) {
                return new StrictVariantCodeComparator(processor);
            }
        },
        WEAK_MISSING {
            public VariantCodeComparator create(IParms processor) {
                return new WeakVariantCodeComparator(processor);
            }
        },
        /** matches if both are active or both are inactive */
        ACTIVE {
            @Override
            public VariantCodeComparator create(IParms processor) {
                return new ActiveVariantCodeComparaotr(processor);
            }
        };

        /**
         * Construct a variant code comparator of this type.
         *
         * @param processor		controlling command processor
         *
         * @return the desired comparator
         */
        public abstract VariantCodeComparator create(IParms processor);
    }

    /**
     * This interface must be supported by the controlling command processors.
     */
    public interface IParms {

    }

    /**
     * Construct a variant code comparator.
     *
     * @param processor		controlling command processor
     */
    public VariantCodeComparator(IParms processor) {

    }

    /**
     * Compare two variant codes.
     *
     * @param v1	first variant code
     * @param v2	second variant code
     *
     * @returns TRUE if the codes match, else FALSE
     */
    public abstract boolean matches(String v1, String v2);

    /**
     * Compare a variant code to a missing subsystem.
     *
     * @param v1	variant code to compare
     */
    public abstract boolean okMissing(String v1);

}
