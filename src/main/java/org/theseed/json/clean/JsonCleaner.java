/**
 *
 */
package org.theseed.json.clean;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This object is the subclass for operations to clean up records in JSON dumps. The framework
 * takes JSON objects and the subclasses modify them in place.
 *
 * @author Bruce Parrello
 *
 */
public abstract class JsonCleaner {


    /**
     * This enumeration lists the different types of cleaners.
     */
    public static enum Type {
        /** Convert hyphens to empty strings. */
        HYPHENS {
            @Override
            public JsonCleaner create(IParms processor) {
                return new JsonHyphenCleaner(processor);
            }
        };

        /**
         * @return a JSON cleaner of this type
         *
         * @param processor		controlling command processor
         */
        public abstract JsonCleaner create(IParms processor);

    }

    /**
     * This interface describes the methods a controlling command processor must support.
     */
    public interface IParms {

    }

    /**
     * Construct a new JSON cleaner.
     *
     * @processor	controlling command processor
     */
    public JsonCleaner(IParms processor) {
    }

    /**
     * Clean a single JSON record. This process MUST be thread-safe.
     *
     * @param json	JSON record to clean
     */
    public abstract void process(JsonObject json);

    /**
     * Log the statistics for this cleaner.
     */
    public abstract void logStats();

}
