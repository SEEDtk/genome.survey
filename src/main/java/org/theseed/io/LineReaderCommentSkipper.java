package org.theseed.io;

import java.util.Iterator;

/**
 * This is a simple iterator for the LineReader class that skips comment lines (those starting with '#').
 */
public class LineReaderCommentSkipper implements Iterator<String> {

    // FIELDS
    /** line reader to use */
    private final LineReader reader;
    /** current line */
    private String currentLine;

    /**
     * Construct a line reader that skips comment lines.
     *
     * @param reader	underlying line reader
     */
    public LineReaderCommentSkipper(LineReader reader) {
        this.reader = reader;
        this.currentLine = null;
        this.advance();
    }

    @Override
    public boolean hasNext() {
        return this.currentLine != null;
    }

    @Override
    public String next() {
        String retVal = this.currentLine;
        this.advance();
        return retVal;
    }

    /**
     * Advance to the next non-comment line.
     */
    private void advance() {
        this.currentLine = null;
        while (this.reader.hasNext() && this.currentLine == null) {
            String line = this.reader.next();
            if (!line.startsWith("#"))
                this.currentLine = line;
        }
    }

}
