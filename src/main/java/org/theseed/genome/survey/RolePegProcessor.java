/**
 *
 */
package org.theseed.genome.survey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.iterator.GenomeSource;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.reports.RolePegReporter;
import org.theseed.utils.BaseReportProcessor;
import org.theseed.utils.ParseFailureException;

/**
 * This command produces a report mapping pegs to specified roles.  In particular, if it is given a list
 * of single-occurring universal roles (SOURs), it will output all the pegs containing those roles found
 * in the directory.
 *
 * The positional parameters are the name of the definition file for the roles of interest and the name
 * of the input genome source (directory or file).  The report will be written to the standard output.
 *
 * The command-line options are as follows.
 *
 * -h	display command-line usage
 * -v	display more frequent log messages
 * -o	output file for report (if not STDIN)
 * -t	type of genome input source (PATRIC, DIR, MASTER)
 *
 * --format		type of report to generate (default PEGS)
 *
 * @author Bruce Parrello
 *
 */
public class RolePegProcessor extends BaseReportProcessor implements RolePegReporter.IParms {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(RolePegProcessor.class);
    /** input genome source */
    private GenomeSource genomes;
    /** role definition map */
    private RoleMap roleMap;
    /** report writer */
    private RolePegReporter reporter;

    // COMMAND-LINE OPTIONS

    /** type of input source */
    @Option(name = "--type", aliases = { "-t", "--source" }, usage = "genome input source type")
    private GenomeSource.Type sourceType;

    /** output report format */
    @Option(name = "--format", usage = "output report foramt")
    private RolePegReporter.Type reportType;

    /** role definition file */
    @Argument(index = 0, metaVar = "role.definitions", usage = "definition file for roles of interest", required = true)
    private File roleFile;

    /** input genome source */
    @Argument(index = 1, metaVar = "inDir", usage = "input genome source (directory or file)")
    private File inDir;

    @Override
    protected void setReporterDefaults() {
        this.sourceType = GenomeSource.Type.DIR;
        this.reportType = RolePegReporter.Type.PEGS;
    }

    @Override
    protected void validateReporterParms() throws IOException, ParseFailureException {
        // Load the role map.
        if (! this.roleFile.canRead())
            throw new FileNotFoundException("Role definition file " + this.roleFile + " is not found or unreadable.");
        this.roleMap = RoleMap.load(this.roleFile);
        log.info("{} roles loaded from {}.", this.roleMap.size(), this.roleFile);
        // Initialize the genome input source.
        if (! this.inDir.exists())
            throw new FileNotFoundException("Genome source " + this.inDir + " is not found.");
        this.genomes = this.sourceType.create(this.inDir);
        // Create the reporter.
        this.reporter = this.reportType.create(this);

    }

    @Override
    protected void runReporter(PrintWriter writer) throws Exception {
        // Initialize the report.
        this.reporter.openReport(writer);
        // Loop through the genomes, looking for matching roles.
        for (Genome genome : this.genomes) {
            log.info("Processing genome {}.", genome);
            // Loop through the features, writing pegs.
            int pegCount = 0;
            int roleCount = 0;
            for (Feature feat : genome.getPegs()) {
                List<Role> roles = feat.getUsefulRoles(this.roleMap);
                for (Role role : roles) {
                    this.reporter.recordPeg(feat, role.getId());
                    roleCount++;
                }
                pegCount++;
            }
            log.info("{} roles found out of {} pegs in {}.", roleCount, pegCount, genome.getId());
        }
    }

}
