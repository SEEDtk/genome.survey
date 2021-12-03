package org.theseed.genome.survey;

import java.util.Arrays;

import org.theseed.utils.BaseProcessor;

/**
 * Commands for extracting general data from genomes.
 *
 * tetra		generate tetramer profiles for domain detection
 * goodCore		generate a directory of the good coreSEED genomes
 * gaps			categorize the gaps between genes in a GTO
 * fidCompare	compare two feature lists and output the roles and subsystems of the differing features
 * bbh			find bidirectional best hits between two genomes
 * validate		test all genomes in a directory to make sure they load
 *
 */
public class App
{
    public static void main( String[] args )
    {
        // Get the control parameter.
        String command = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        BaseProcessor processor;
        // Determine the command to process.
        switch (command) {
        case "tetra" :
            processor = new TetraProcessor();
            break;
        case "goodCore" :
            processor = new GoodCoreProcessor();
            break;
        case "gaps" :
            processor = new GapProcessor();
            break;
        case "fidCompare" :
            processor = new CompareProcessor();
            break;
        case "bbh" :
            processor = new BbhProcessor();
            break;
        case "validate" :
            processor = new ValidateProcessor();
            break;
        default:
            throw new RuntimeException("Invalid command " + command);
        }
        // Process it.
        boolean ok = processor.parseCommand(newArgs);
        if (ok) {
            processor.run();
        }
    }
}
