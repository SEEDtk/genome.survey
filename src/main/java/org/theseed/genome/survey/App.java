package org.theseed.genome.survey;

import java.util.Arrays;

import org.theseed.basic.BaseProcessor;

/**
 * Commands for extracting general data from genomes.
 *
 * tetra		generate tetramer profiles for domain detection
 * goodCore		generate a directory of the good coreSEED genomes
 * gaps			categorize the gaps between genes in a GTO
 * fidCompare	compare two feature lists and output the roles and subsystems of the differing features
 * subCompare	compare the subsystems of two sets of genomes
 * bbh			find bidirectional best hits between two genomes
 * validate		test all genomes in a directory to make sure they load
 * rolePegs		find all pegs in a genome directory containing roles in a specified role set
 * roleMap		create a file of protein sequences and annotations from a set of genomes
 * roleAdj		create a file that can be used to build role-adjacency training data
 * taxonScan	create a file that can be used to build taxonomic training data
 * dbWalk		generate a random walk of a database JSON dump using templates
 * modelFix		fix up a model dump to make it more compatible with the template engine
 * queryGen		generate questions for testing a large language model
 * famFile		build a protein-family file from a directory of GTOs.
 * jsonScan		scan a JSON dump and produce a report on file names and field types
 * jsonCopy		copy a JSON dump to a new directory, performing optional cleaning tasks
 *
 */

public class App
{

    protected static final String[] COMMANDS = new String[] {
             "tetra", "generate tetramer profiles for domain detection",
             "goodCore", "generate a directory of the good coreSEED genomes",
             "gaps", "categorize the gaps between genes in a GTO",
             "fidCompare", "compare two feature lists and output the roles and subsystems of the differing features",
             "subCompare", "compare the subsystems of two sets of genomes",
             "bbh", "find bidirectional best hits between two genomes",
             "validate", "test all genomes in a directory to make sure they load",
             "rolePegs", "find all pegs in a genome directory containing roles in a specified role set",
             "roleMap", "create a file of protein sequences and annotations from a set of genomes",
             "roleAdj", "create a file that can be used to build role-adjacency training data",
             "taxonScan", "create a file that can be used to build taxonomic training data",
             "dbWalk", "generate a random walk of a database JSON dump using templates",
             "modelFix", "fix up a model dump to make it more compatible with the template engine",
             "queryGen", "generate questions for testing a large language model",
             "famFile", "build a protein-family file from a genome source",
             "jsonScan", "scan a JSON dump and produce a report on file names and field types",
             "jsonCopy", "copy a JSON dump to a new directory, performing optional cleaning tasks"
    };

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
        case "subCompare" :
            processor = new SubsystemCompareProcessor();
            break;
        case "bbh" :
            processor = new BbhProcessor();
            break;
        case "validate" :
            processor = new ValidateProcessor();
            break;
        case "rolePegs" :
            processor = new RolePegProcessor();
            break;
        case "roleMap" :
            processor = new RoleMapProcessor();
            break;
        case "roleAdj" :
            processor = new RoleAdjacencyProcessor();
            break;
        case "taxonScan" :
            processor = new TaxonomyScanProcessor();
            break;
        case "dbWalk" :
            processor = new RandomWalkProcessor();
            break;
        case "modelFix" :
            processor = new ModelDumpFixProcessor();
            break;
        case "queryGen" :
            processor = new QueryGenerateProcessor();
            break;
        case "famFile" :
            processor = new FamilyFileProcessor();
            break;
        case "jsonScan" :
            processor = new JsonScanProcessor();
            break;
        case "jsonCopy" :
            processor = new JsonCopyProcessor();
            break;
        case "-h" :
        case "--help" :
            processor = null;
            break;
        default :
            throw new RuntimeException("Invalid command " + command + ".");
        }
        if (processor == null)
            BaseProcessor.showCommands(COMMANDS);
        else {
            boolean ok = processor.parseCommand(newArgs);
            if (ok) {
                processor.run();
            }
        }
    }
}
