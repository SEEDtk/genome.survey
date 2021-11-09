/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.Shuffler;
import org.theseed.io.TabbedLineReader;
import org.theseed.p3api.P3Connection;
import org.theseed.sequence.FastaInputStream;
import org.theseed.sequence.Sequence;
import org.theseed.sequence.TetramerProfile;
import org.theseed.utils.BaseProcessor;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * This command takes as input a list of genome IDs, domain names, and counts.  It produces tetramer profiles
 * taken randomly from the specified genomes.  Each genome will produce a number of profiles equal to the
 * count.  The intent is to produce a training file for detecting domain from tetramer profiles.
 *
 * There are no positional parameter.  The input is the standard input and the output is the standard output.
 *
 * 	genome_id		genome ID (used for generating identifiers)
 * 	kingdom			domain of the genome
 * 	count			number of profiles to produce from the genome
 *  source			either "PATRIC" (indicating the genome is in PATRIC) or the name of a FASTA file
 *
 * The command-line options are as follows.
 *
 * -h	display command-line usage
 * -v	show more detailed log messages
 * -k	length of DNA sequence to use for the profile; the default is 100
 * -i	name of the input file (if not STDIN)
 *
 * @author Bruce Parrello
 *
 */
public class TetraProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TetraProcessor.class);
    /** input stream to use for input */
    private InputStream inStream;
    /** connection to PATRIC */
    private P3Connection p3;
    /** random number generator */
    private Random rand;

    // COMMAND-LINE OPTIONS

    /** DNA sequence length */
    @Option(name = "--kLen", aliases = { "-k" }, metaVar = "150", usage = "DNA length to use for each profile")
    private int profileLen;

    /** input file name */
    @Option(name = "--input", aliases = { "-i" }, metaVar = "genomes.tbl", usage = "input file (if not STDIN)")
    private File inFile;


    @Override
    protected void setDefaults() {
        this.profileLen = 100;
        this.inFile = null;
    }

    @Override
    protected boolean validateParms() throws IOException, ParseFailureException {
        // Verify the profile length.
        if (this.profileLen < 10)
            throw new ParseFailureException("Profile length (--kLen) is too short.  The minimum is 10.");
        // Process the input file.
        if (this.inFile == null)
            this.inStream = System.in;
        else {
            if (! this.inFile.canRead())
                throw new FileNotFoundException("Input file is not found or unreadable.");
            this.inStream = new FileInputStream(this.inFile);
        }
        return true;
    }

    @Override
    protected void runCommand() throws Exception {
        try (TabbedLineReader genomeStream = new TabbedLineReader(inStream)) {
            // Get the randomizer.
            this.rand = new Random();
            // Find the columns of interest.
            int idCol = genomeStream.findField("genome_id");
            int kingCol = genomeStream.findField("kingdom");
            int countCol = genomeStream.findField("count");
            int sourceCol = genomeStream.findField("source");
            this.p3 = new P3Connection();
            // Write the output headers.
            System.out.println("id\t" + TetramerProfile.headers() + "\tdomain");
            // Loop through the input.
            for (TabbedLineReader.Line line : genomeStream) {
                // Get the genome ID.
                String genome_id = line.get(idCol);
                // Get the domain.
                String domain = line.get(kingCol);
                // Get the tetramers.
                List<TetramerProfile> tetramers = this.getTetramers(genome_id, line.get(sourceCol), line.getInt(countCol));
                // Write them out.
                int i = 1;
                for (TetramerProfile tetramer : tetramers) {
                    System.out.format("%s.%d\t%s\t%s%n", genome_id, i, tetramer.toString(), domain);
                    i++;
                }
            }
        }
    }

    /**
     * @return a list of tetramer profiles taken from the specified genome.
     *
     * @param genome_id		ID of the target genome
     * @param source		source of genome
     * @param count			number of profiles to return
     *
     * @throws IOException
     */
    private List<TetramerProfile> getTetramers(String genome_id, String source, int count) throws IOException {
        // Get all the contigs for the genome.
        log.info("Loading contigs for genome {}.  {} profiles requested.", genome_id, count);
        Shuffler<String> sequences;
        switch (source) {
        case "PATRIC" :
            sequences = getPatricSequences(genome_id);
            break;
        default :
            sequences = this.getFastaSequences(source);
        }
        log.info("{} sequences of sufficient length found.", sequences.size());
        // Get the desired profiles.  First, randomize the contigs.
        int size = (count < sequences.size() ? count : sequences.size());
        sequences.shuffle(size);
        // This will track the current position in each contig.
        int[] locs = new int[size];
        Arrays.fill(locs, 0);
        // This will track the number of profiles we still need.
        int needed = count;
        // Loop through the contigs.
        List<TetramerProfile> retVal = new ArrayList<TetramerProfile>(count);
        for (int i = 0; i < size && needed > 0; i++) {
            // Get the current sequence and determine its length.
            String sequence = sequences.get(i);
            int seqLen = sequence.length();
            // Figure out how many profiles we need from this contig and divide it into sections.
            int neededInContig = needed / (size - i);
            int sectionLen = seqLen / neededInContig;
            if (sectionLen < this.profileLen) sectionLen = this.profileLen;
            // Compute the gap length between profiles.
            int gap = seqLen - this.profileLen * neededInContig;
            // Now loop through the contig, getting the profiles using random gaps.
            int pos = 0;
            while (pos + this.profileLen <= seqLen && neededInContig > 0) {
                if (gap > 0) {
                    int incr = this.rand.nextInt(gap);
                    pos += incr;
                    gap -= incr;
                }
                // Get the tetramer profile.  Note we convert from offset (0-based) to position (1-based).
                TetramerProfile profile = new TetramerProfile(sequence, pos + 1, this.profileLen);
                retVal.add(profile);
                needed--;
                neededInContig--;
                pos += this.profileLen;
            }
        }
        log.info("{} profiles computed from genome {}.", retVal.size(), genome_id);
        return retVal;
    }

    /**
     * @return the list of contig sequences from PATRIC for the specified genome
     *
     * @param genome_id		ID of desired genome
     */
    protected Shuffler<String> getPatricSequences(String genome_id) {
        Shuffler<String> retVal;
        List<JsonObject> contigs = this.p3.getRecords(P3Connection.Table.CONTIG, "genome_id", Collections.singleton(genome_id),
            "sequence_id,sequence");
        log.info("{} contigs found.", contigs.size());
        // Get the long ones into a shuffler.
        retVal = new Shuffler<String>(contigs.size());
        contigs.stream().map(x -> P3Connection.getString(x, "sequence")).filter(x -> x.length() >= this.profileLen)
                .forEach(x -> retVal.add(x));
        return retVal;
    }

    /**
     * @return the list of contig sequences from the specified FASTA file
     *
     * @param source	name of the FASTA file
     *
     * @throws IOException
     */
    private Shuffler<String> getFastaSequences(String source) throws IOException {
        Shuffler<String> retVal = new Shuffler<String>(100);
        try (FastaInputStream fastaStream = new FastaInputStream(new File(source))) {
            for (Sequence seq : fastaStream)
                retVal.add(seq.getSequence());
        }
        return retVal;
    }

}
