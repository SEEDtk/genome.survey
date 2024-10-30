/**
 *
 */
package org.theseed.json.clean;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This JSON cleaner removes values that are single hyphens from the data records.
 *
 * @author Bruce Parrello
 *
 */
public class JsonHyphenCleaner extends JsonCleaner {

    // FIELDS
    /** counter of fields updated */
    private int updateCounter;

    public JsonHyphenCleaner(IParms processor) {
        super(processor);
    }

    @Override
    public void process(JsonObject json) {
        int updateCount = 0;
        var fieldIter = json.entrySet().iterator();
        while (fieldIter.hasNext()) {
            Object fieldValue = fieldIter.next().getValue();
            if (fieldValue instanceof String) {
                String stringValue = (String) fieldValue;
                if (stringValue.contentEquals("-")) {
                    fieldIter.remove();
                    updateCount++;
                }
            }
        }
        // Roll up our one counter.
        synchronized (this) {
            this.updateCounter += updateCount;
        }
    }

    @Override
    public void logStats() {
        log.info("{} hyphens were cleaned.", this.updateCounter);
    }

}
