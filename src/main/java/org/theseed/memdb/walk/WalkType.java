package org.theseed.memdb.walk;

import java.io.File;
import java.io.IOException;

import org.theseed.basic.ParseFailureException;
import org.theseed.memdb.DbDefinition;
import org.theseed.memdb.text.TextDbDefinition;

/**
 * This enum represents the different types of walks that can be performed using an in-memory database.
 */
public enum WalkType {
    /** create a text narrative from the walk */
    TEXT {
        @Override
        public DbDefinition getDbDefinition(File dbdFile) throws IOException, ParseFailureException {
            return new TextDbDefinition(dbdFile);
        }
    },
    /** create a word list from the walk */
    WORD {
        @Override
        public DbDefinition getDbDefinition(File dbdFile) throws IOException, ParseFailureException {
            return new TextDbDefinition(dbdFile);
        }
    };

    /**
     * Create a database definition for this walk type.
     * 
     * @param dbdFile		file containing the database definition
     * 
     * @return a database definition for this walk type
     * 
     * @throws ParseFailureException 
     * @throws IOException 
     */
    public abstract DbDefinition getDbDefinition(File dbdFile) throws IOException, ParseFailureException;

}
