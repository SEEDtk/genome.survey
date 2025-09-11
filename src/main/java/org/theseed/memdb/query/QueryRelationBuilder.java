/**
 *
 */
package org.theseed.memdb.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.Attribute;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.RelationBuilder;
import org.theseed.memdb.RelationshipInstance;
import org.theseed.memdb.RelationshipType;

/**
 * The only extra work required here is storing attributes in the target entity instance for many-to-many relationships.
 *
 * @author Bruce Parrello
 *
 */
public class QueryRelationBuilder extends RelationBuilder {

    // FIELDS
    /** map from input file values to target entity instance fields */
    private final List<Mapping> valueMap;

    /**
     * This object is a mapping from input record fields to target entity instance attribute names.
     */
    protected static class Mapping {

        /** input record field index */
        private final int colIdx;
        /** target instance attribute name */
        private final String attrName;

        /**
         * Construct a target-field mapping.
         *
         * @param inStream	field input stream containing the data
         * @param mapEntry	target value map entry
         *
         * @throws IOException
         */
        protected Mapping(FieldInputStream inStream, Map.Entry<String, String> mapEntry) throws IOException {
            this.colIdx = inStream.findField(mapEntry.getKey());
            this.attrName = mapEntry.getValue();
        }

        /**
         * Store this mapped field in the target entity instance.
         */
        protected void store(FieldInputStream.Record record, QueryEntityInstance target) {
            Attribute attr = new Attribute(record, this.colIdx);
            target.addAttribute(this.attrName, attr);
        }
    }

    /**
     * Construct a relation-builder for a query-generation database.
     *
     * @param relType	relationship type
     * @param inStream	input stream for source records
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public QueryRelationBuilder(RelationshipType relType, FieldInputStream inStream)
            throws IOException, ParseFailureException {
        super(relType, inStream);
        QueryRelationshipType qRelType = (QueryRelationshipType) relType;
        // Convert the target-field map to a list of instructions for copying the fields.
        var targetMap = qRelType.getValueMap();
        this.valueMap = new ArrayList<>(targetMap.size());
        for (var targetEntry : targetMap.entrySet()) {
            Mapping mapping = new Mapping(inStream, targetEntry);
            this.valueMap.add(mapping);
        }
    }

    @Override
    protected RelationshipInstance getForwardInstance(DbInstance db, Record record, EntityInstance sourceInstance,
            EntityInstance targetInstance) {
        QueryEntityInstance qTarget = (QueryEntityInstance) targetInstance;
        // If there are any target-field mappings, we fill them in here.
        for (Mapping mapping : this.valueMap)
            mapping.store(record, qTarget);
        return new QueryRelationshipInstance(targetInstance);
    }

    @Override
    protected RelationshipInstance getReverseInstance(DbInstance db, Record record, EntityInstance sourceInstance,
            EntityInstance targetInstance) {
        return new QueryRelationshipInstance(sourceInstance);
    }

}
