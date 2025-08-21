/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.BaseGenomeProcessor;
import org.theseed.p3api.P3Genome;

/**
 * This command writes a list of the roles present in a genome source. The number of occurrences of each role is also output.
 * The input is a genome source. The role list will be printed on the standard output.
 *
 * The first positional parameter is the genome source file or directory.
 *
 * The command-line option are as follows:
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -t	genome source type (default DIR)
 * -o	output file for report (if not STDOUT)
 *
 * @author Bruce Parrello
 *
 */
public class RoleListProcessor extends BaseGenomeProcessor {

	// FIELDS
	/** logging facility */
	private static final Logger log = LoggerFactory.getLogger(RoleListProcessor.class);
	/** map of roles to counts */
	private CountMap<String> roleCounts;

	// COMMAND-LINE OPTIONS

    /** output file (if not STDOUT) */
    @Option(name = "-o", aliases = { "--output" }, usage = "output file for report (if not STDOUT)")
    private File outFile;

	@Override
	protected void setSourceDefaults() {
		this.setLevel(P3Genome.Details.STRUCTURE_ONLY);
		this.outFile = null;
	}

	@Override
	protected void validateSourceParms() throws IOException, ParseFailureException {
		// Verify that we can write to the output file, if any.
		if (this.outFile != null) {
			log.info("Role report will be written to {}.", this.outFile);
			try (PrintWriter writer = new PrintWriter(this.outFile)) {
				writer.println("Role name\tcount\n");
			}
		} else
			log.info("Role report will be written to the standard output.");
		// Create the role-count map.
		this.roleCounts = new CountMap<>();
	}

	@Override
	protected void runCommand() throws Exception {
		// Get the list of genome IDs to process.
		Set<String> genomeIDs = this.getGenomeIds();
		final int nGenomes = genomeIDs.size();
		log.info("{} genomes to process.", nGenomes);
		// Loop through the genomes.
		int gCount = 0;
		int pegCount = 0;
		for (String genomeID : genomeIDs) {
			Genome genome = this.getGenome(genomeID);
			gCount++;
			// Loop through all the features, retrieving the protein function if the feature
			// is a PEG.
			log.info("Processing genome {} of {}: {}.", gCount, nGenomes, genome);
			for (Feature feat : genome.getFeatures()) {
				if (feat.getType().equals("CDS")) {
					pegCount++;
					String function = feat.getPegFunction();
					String[] roles = Feature.rolesOfFunction(function);
					for (String role : roles)
						this.roleCounts.count(role);
				}
			}
		}
		// Now that we've counted the roles, we can write the report.
		log.info("{} roles found in {} CDS features of {} genomes.", this.roleCounts.size(), pegCount, gCount);
		try (PrintWriter writer = this.openWriter(this.outFile)) {
			int lineCount = 0;
			// Write the output header line.
			writer.println("Role name\tcount");
			for (var counter : this.roleCounts.sortedCounts()) {
				writer.println(counter.getKey() + "\t" + counter.getCount());
				lineCount++;
			}
			log.info("Report completed. {} counts output.", lineCount);
		}
	}

}
