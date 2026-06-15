package org.theseed.memdb.words;

import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.LineTemplate;
import org.theseed.memdb.DbInstance;
import org.theseed.memdb.EntityInstance;
import org.theseed.memdb.RelationBuilder;
import org.theseed.memdb.RelationshipInstance;

public class WordRelationBuilder extends RelationBuilder {

    // FIELDS
    /** source line template */
    private final LineTemplate sourceTemplate;
    /** relationship name line template */
    private final LineTemplate nameTemplate;
    /** target line template */
    private final LineTemplate targetTemplate;

    public WordRelationBuilder(WordRelationshipType relType, FieldInputStream inStream) throws IOException, ParseFailureException {
        super(relType, inStream);
        // Compile the three templates.
        this.sourceTemplate = new LineTemplate(inStream, relType.getSourceString(), null);
        this.nameTemplate = new LineTemplate(inStream, relType.getNameString(), null);
        this.targetTemplate = new LineTemplate(inStream, relType.getTargetString(), null);
    }

    @Override
    protected RelationshipInstance getForwardInstance(DbInstance db, Record record, EntityInstance sourceInstance,
            EntityInstance targetInstance) {
        String sourceId = this.sourceTemplate.apply(record);
        String name = this.nameTemplate.apply(record);
        String targetId = this.targetTemplate.apply(record);
        return new WordRelationshipInstance(sourceId, name, targetId, targetInstance);
    }

    @Override
    protected RelationshipInstance getReverseInstance(DbInstance db, Record record, EntityInstance sourceInstance,
            EntityInstance targetInstance) {
        String sourceId = this.sourceTemplate.apply(record);
        String name = this.nameTemplate.apply(record);
        String targetId = this.targetTemplate.apply(record);
        return new WordRelationshipInstance(targetId, name, sourceId, sourceInstance);
    }

}
