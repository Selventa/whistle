package com.selventa.whistle.cli;

import static java.lang.String.format;
import static java.lang.System.err;
import static org.openbel.framework.common.BELUtilities.readable;
import static org.openbel.framework.common.enums.RelationshipType.DECREASES;
import static org.openbel.framework.common.enums.RelationshipType.INCREASES;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.openbel.framework.api.DefaultSpeciesDialect;
import org.openbel.framework.api.Dialect;
import org.openbel.framework.api.Kam;
import org.openbel.framework.api.Kam.KamNode;
import org.openbel.framework.api.KamDialect;
import org.openbel.framework.api.KamSpecies;
import org.openbel.framework.api.KamStore;
import org.openbel.framework.api.KamStoreImpl;
import org.openbel.framework.common.bel.parser.BELParser;
import org.openbel.framework.common.cfg.SystemConfiguration;
import org.openbel.framework.common.model.Namespace;
import org.openbel.framework.common.model.Term;
import org.openbel.framework.core.df.DBConnection;
import org.openbel.framework.core.df.DatabaseService;
import org.openbel.framework.core.df.DatabaseServiceImpl;
import org.openbel.framework.core.df.beldata.namespace.NamespaceHeader;
import org.openbel.framework.core.df.beldata.namespace.NamespaceHeaderParser;
import org.openbel.framework.core.df.cache.CacheableResourceService;
import org.openbel.framework.core.df.cache.DefaultCacheableResourceService;
import org.openbel.framework.core.df.cache.ResolvedResource;
import org.openbel.framework.core.df.cache.ResourceType;
import org.openbel.framework.internal.KAMStoreDaoImpl.BelTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import com.selventa.whistle.cli.license.LicenseAgreement;
import com.selventa.whistle.cli.license.LicenseCallback;
import com.selventa.whistle.cli.license.LicensePromptOptions;
import com.selventa.whistle.data.enums.DirectionType;
import com.selventa.whistle.data.model.Comparison;
import com.selventa.whistle.data.model.Measurement;
import com.selventa.whistle.data.service.DataFileService;
import com.selventa.whistle.data.service.DefaultCollapsingStrategy;
import com.selventa.whistle.data.service.DefaultIdAMPDataFileService;
import com.selventa.whistle.score.model.Cutoffs;
import com.selventa.whistle.score.model.Downstream;
import com.selventa.whistle.score.model.Hypothesis;
import com.selventa.whistle.score.model.MappedMeasurement;
import com.selventa.whistle.score.model.ScoredHypothesis;
import com.selventa.whistle.score.service.BasicHypothesisFinder;
import com.selventa.whistle.score.service.DefaultMeasurementMappingService;
import com.selventa.whistle.score.service.MeasurementMappingService;
import com.selventa.whistle.score.service.MeasurementMappingService.MappingResult;
import com.selventa.whistle.score.service.Scorer;
import com.selventa.whistle.score.service.Scorer.Prediction;

/**
 * Command line application to perform Reverse Causal Reasoning of a dataset
 * against a given Kam.
 *
 * @author Steve Ungerer
 * @author Anthony Bargnesi
 */
public class Rcr {
    private static final Logger logger = LoggerFactory.getLogger(Rcr.class);

    // cli constants
    private static final String KAM_SHORT_OPT = "k";
    private static final String KAM_LONG_OPT = "kam";
    private static final String DATA_SHORT_OPT = "f";
    private static final String DATA_LONG_OPT = "input-file";
    private static final String RUN_NAME_SHORT_OPT = "r";
    private static final String RUN_NAME_LONG_OPT = "run-name";
    private static final String FOLD_CHANGE_SHORT_OPT = "m";
    private static final String FOLD_CHANGE_LONG_OPT = "fold-change-cutoff";
    private static final String PVAL_SHORT_OPT = "p";
    private static final String PVAL_LONG_OPT = "p-value-cutoff";
    private static final String ABUN_SHORT_OPT = "a";
    private static final String ABUN_LONG_OPT = "abundance-cutoff";
    private static final String ANLST_SHORT_OPT = "s";
    private static final String ANLST_LONG_OPT = "use-analyst-selection";
    private static final String POP_SIZE_SHORT_OPT = "n";
    private static final String POP_SIZE_LONG_OPT = "population-size";
    private static final String NS_URL_SHORT_OPT = "u";
    private static final String NS_URL_LONG_OPT = "namespace-url";
    private static final String DETAIL_LONG_OPT = "detail";
    private static final String SPECIES_TAXID_SHORT_OPT = "t";
    private static final String SPECIES_TAXID_LONG_OPT = "taxid";
    private static final String CSV = ".csv";
    private static final String RESULT_FILE_SUFFIX = "_result" + CSV;
    private static final String MAPPING_FILE_SUFFIX = "_mapping" + CSV;
    private static final String DETAIL_FILE_SUFFIX = "_detail" + CSV;

    // reporting constants
    private static final String NOT_SIGNIFICANT = "Not significant";
    private static final String AMBIGUOUS = "Ambiguous";
    private static final String CONTRA = "Contra";
    private static final String CORRECT = "Correct";
    private static final String COLLAPSED_STATUS = "Collapsed to: %s";
    private static final String NOT_PRESENT_IN_POPULATION_STATUS = "Not present in population: %s";
    private static final String NOT_MAPPED_TO_KAM_STATUS = "Not mapped to KAM";

    // header constants
    private static final String ID_HEADER = "Id";
    private static final String DIRECTION_HEADER = "Direction";
    private static final String CORRECT_HEADER = "Correct";
    private static final String RICHNESS_HEADER = "Richness";
    private static final String CONCORDANCE_HEADER = "Concordance";
    private static final String AMBIGUOUS_HEADER = "Ambiguous";
    private static final String CONTRA_HEADER = "Contra";
    private static final String POSSIBLE_HEADER = "Possible";
    private static final String OBSERVED_HEADER = "Observed";
    private static final String STATUS_HEADER = "STATUS";
    private static final String KAM_NODE_HEADER = "KAM_NODE";
    private static final String SOURCE_HEADER = "Source";
    private static final String RELATIONSHIP_HEADER = "Relationship";
    private static final String TARGET_HEADER = "Target";
    private static final String TYPE_HEADER = "Type";

    // license constants
    private static final String APPLICATION = "Whistle";
    private static final String LICENSE_PATH = "LICENSE";
    private static final String LICENSE_PROMPT = "Do you accept the license agreement above?";
    private static final String LICENSE_ACCEPT = "Yes";
    private static final String LICENSE_REJECT = "No";
    private static final String LICENSE_REJECTED_EXIT =
            "License Agreement not accepted.  Whistle will now exit.";

    // service objects, for custom subclassing override the applicable getters.
    private SystemConfiguration sysCfg;
    private DatabaseService dbService;
    private KamStore kamStore;
    private CacheableResourceService cacheService;
    private DataFileService dataFileService;
    private MeasurementMappingService mappingService;
    private Dialect dialect;

    // run state
    protected final CommandLine commandLine;

    /**
     * Entry point from CLI execution. Simply constructs a new instance and runs it.
     *
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        LicensePromptOptions options = new LicensePromptOptions(LICENSE_PROMPT,
                LICENSE_ACCEPT, LICENSE_REJECT);
        LicenseCallback callback = new LicenseCallback() {

            @Override
            public void onAcceptance() {
                try {
                    Rcr rcr = new Rcr(args);
                    rcr.run();
                } catch (Exception e) {
                    err.println(format("Error running %s: %s", APPLICATION,
                            e.getMessage()));
                    e.printStackTrace();
                }
                System.exit(0);
            }

            @Override
            public void onRejection() {
                err.println(LICENSE_REJECTED_EXIT);
                System.exit(1);
            }
        };

        LicenseAgreement license;

        final InputStream is = Rcr.class.getResourceAsStream("/" + LICENSE_PATH);
        if (is != null) {
            license = new LicenseAgreement(APPLICATION, is,
                    options, callback);
        } else {
            File licenseFile = new File(LICENSE_PATH);
            if (!readable(licenseFile)) {
                err.println(format("Cannot find %s file.", LICENSE_PATH));
            }

            license = new LicenseAgreement(APPLICATION, licenseFile, options,
                    callback);
        }

        license.promptLicense();
    }

    /**
     * Constructs the Rcr instance and validates the provided arguments
     * @param args
     * @throws Exception
     */
    public Rcr(String[] args) throws Exception {
        // Are they asking for help?
        if (args.length == 0 || ArrayUtils.contains(args, "-h")
                || ArrayUtils.contains(args, "--help")
                || ArrayUtils.contains(args, "-?")) {
            printHelp();
            System.exit(0);
        }

        // parse options
        GnuParser parser = new GnuParser();
        this.commandLine = parser.parse(getCommandLineOptions(), args);

        // setup
        try {
            setup();
        } catch (Exception e) {
            System.out.println("ERROR: Failed to set up RCR application: "
                    + e.getMessage());
            System.exit(1);
        }

        // validate
        if (!validateOptions()) {
            System.exit(1);
        }
    }

    /**
     * Sets up the services, etc needed for Rcr execution.<br>
     * Note to subclasses: overriding specific getters is preferred to
     * overriding the entire setup() method.
     *
     * @throws Exception any exception that occurs during setup will terminate
     *             the application.
     */
    protected void setup() throws Exception {
        this.sysCfg = getSystemConfiguration();
        this.dbService = getDatabaseService();
        this.kamStore = getKamStore();
        this.cacheService = getCacheableResourceService();
        this.dataFileService = getDataFileService();
        this.mappingService = getMappingService();
        this.dialect = getDialect();

        if (sysCfg == null || dbService == null || kamStore == null
                || cacheService == null || dataFileService == null
                || mappingService == null || dialect == null) {
            throw new IllegalStateException("null service created");
        }
    }

    /**
     * Prints the help/usage for Rcr.
     */
    private void printHelp() {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp(" whistle [-r <run_name> -f <data_file> -k <kam_name> -u <namespace_url>]", getCommandLineOptions());
    }

    /**
     * Validate the provided arguments.
     *
     * @return
     */
    protected boolean validateOptions() {
        boolean valid = true;
        if (commandLine.hasOption(ANLST_SHORT_OPT)) {
            if (commandLine.hasOption(FOLD_CHANGE_SHORT_OPT)) {
                System.out
                        .println("WARNING: fold change cutoff specified when using analyst selection. Cutoff will be ignored.");
            }
            if (commandLine.hasOption(PVAL_SHORT_OPT)) {
                System.out
                        .println("WARNING: p-value cutoff specified when using analyst selection. Cutoff will be ignored.");
            }
            if (commandLine.hasOption(ABUN_SHORT_OPT)) {
                System.out
                        .println("WARNING: abundance cutoff specified when using analyst selection. Cutoff will be ignored.");
            }
        } else {
            if (!commandLine.hasOption(FOLD_CHANGE_SHORT_OPT)
                    || !commandLine.hasOption(PVAL_SHORT_OPT)
                    || !commandLine.hasOption(ABUN_SHORT_OPT)) {
                System.err
                        .println("ERROR: Fold change, p-value and abundance cutoffs must be specified if not using analyst selection");
                valid = false;
            }
        }
        if (commandLine.hasOption(FOLD_CHANGE_SHORT_OPT)
                && !isDouble(commandLine.getOptionValue(FOLD_CHANGE_SHORT_OPT))) {
            System.err
                    .println("ERROR: Invalid fold change cutoff. Value must be a decimal.");
            valid = false;
        }
        if (commandLine.hasOption(PVAL_SHORT_OPT)
                && !isDouble(commandLine.getOptionValue(PVAL_SHORT_OPT))) {
            System.err
                    .println("ERROR: Invalid p-value cutoff. Value must be a decimal.");
            valid = false;
        }
        if (commandLine.hasOption(ABUN_SHORT_OPT)
                && !isDouble(commandLine.getOptionValue(ABUN_SHORT_OPT))) {
            System.err
                    .println("ERROR: Invalid abundance cutoff. Value must be a decimal.");
            valid = false;
        }

        try {
            parseNamespace(cacheService,
                    commandLine.getOptionValue(NS_URL_SHORT_OPT));
        } catch (Exception e) {
            System.err
                    .println("ERROR: Could not validate namespace URL. Confirm the URL is correct and accessible.");
            valid = false;
        }

        File f = new File(commandLine.getOptionValue(DATA_SHORT_OPT));
        if (!f.exists() || !f.canRead()) {
            System.err
                    .println("ERROR: Could not open data file for reading. Confirm data file path");
            valid = false;
        }

        String runName = commandLine.getOptionValue(RUN_NAME_SHORT_OPT);
        valid = touchFile(valid, new File(runName + RESULT_FILE_SUFFIX));

        boolean isDetailedOutput = commandLine.hasOption(DETAIL_LONG_OPT);
        if (isDetailedOutput) {
            valid = touchFile(valid, new File(runName + MAPPING_FILE_SUFFIX));
            valid = touchFile(valid, new File(runName + DETAIL_FILE_SUFFIX));
        }

        if (commandLine.hasOption(POP_SIZE_SHORT_OPT)) {
            try {
                Integer.valueOf(commandLine.getOptionValue(POP_SIZE_SHORT_OPT));
            } catch (NumberFormatException e) {
                System.err
                        .println("ERROR: Invalid population size. Value must be a positive integer.");
            }
        }

        if (commandLine.hasOption(SPECIES_TAXID_LONG_OPT)) {
            try {
                Integer.valueOf(commandLine.getOptionValue(SPECIES_TAXID_LONG_OPT));
            } catch (NumberFormatException e) {
                System.err
                        .println("ERROR: Invalid species taxonomy id. Value must be a positive integer.");
            }
        }

        return valid;
    }

    protected void run() throws Exception {
        logger.debug("Parsing Namespace");
        Namespace ns = parseNamespace(cacheService,
                commandLine.getOptionValue(NS_URL_SHORT_OPT));
        File input = new File(commandLine.getOptionValue(DATA_SHORT_OPT));
        logger.debug("Parsing input file");
        Collection<Comparison> comparisons = dataFileService.process(input, ns);

        if (comparisons.isEmpty()) {
            System.err.println("No comparisons were found in input file.");
            System.exit(1);
        }
        logger.info("Parsed {} comparisons from file", comparisons.size());

        // If > 1 comparison parsed, prompt the user for the comparison they want to use
        Comparison comparison;
        if (comparisons.size() > 1) {
            List<Comparison> compList = new ArrayList<Comparison>(comparisons);
            System.out.println("Select the comparison to use:");
            int idx = 0;
            for (Comparison c : compList) {
                System.out.println(++idx + ": " + c.getName());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    System.in));
            int selection = -1;
            while (selection == -1) {
                String in = br.readLine();
                if (StringUtils.isNumeric(in)) {
                    int tmp = Integer.parseInt(in);
                    if (tmp > 0 && tmp <= compList.size()) {
                        selection = tmp;
                    }
                } else {
                    System.err
                            .println("Invalid selection. Please select a comparison");
                }
            }
            comparison = compList.get(--selection);
        } else {
            comparison = comparisons.iterator().next();
        }

        // if details is enabled, write additional files
        boolean showDetail = commandLine.hasOption(DETAIL_LONG_OPT);
        Map<Measurement,String> debugInfo = new HashMap<Measurement, String>();

        Collection<Measurement> measurements = comparison.getMeasurements();
        logger.info("Comparison {} contains {} measurements",
                comparison.getName(), measurements.size());

        Cutoffs cutoffs;
        if (commandLine.hasOption(ANLST_SHORT_OPT)) {
            cutoffs = new Cutoffs(true);
        } else {

            cutoffs = new Cutoffs(Double.valueOf(commandLine
                    .getOptionValue(FOLD_CHANGE_SHORT_OPT)), Double.valueOf(commandLine
                    .getOptionValue(PVAL_SHORT_OPT)),
                    Double.valueOf(commandLine.getOptionValue(ABUN_SHORT_OPT)));
        }

        String kamName = commandLine.getOptionValue(KAM_SHORT_OPT);
        Kam kam;
        logger.debug("Retrieving KAM '{}' from KamStore", kamName);
        if (commandLine.hasOption(SPECIES_TAXID_LONG_OPT)) {
            final String taxId = commandLine.getOptionValue(SPECIES_TAXID_LONG_OPT);

            // taxId string has already been validated as a numeric
            final int speciesTaxId = Integer.parseInt(taxId);

            logger.debug("Collapsing KAM '{}' to tax id {}.", kamName,
                    String.valueOf(speciesTaxId));
            kam = kamStore.getKam(kamName);
            kam = new KamSpecies(new KamDialect(kam, dialect),
                new DefaultSpeciesDialect(
                kam.getKamInfo(), kamStore, speciesTaxId, false),
                kamStore);
        } else {
            kam = new KamDialect(kamStore.getKam(kamName), dialect);
        }
        logger.info("Completed KAM retrieval");

        logger.debug("Finding mechanisms");
        BasicHypothesisFinder hypFinder = new BasicHypothesisFinder();
        List<Hypothesis> hyps = hypFinder.findAll(kam, 2);
        logger.info("Found {} mechanisms in KAM", hyps.size());

        logger.debug("Mapping measurements to Mechanisms");
        MappingResult mappingResult = mappingService.map(
                kam, hyps, measurements);
        logger.info("Mapped {} measurements to Mechanisms", mappingResult.getMappedMeasurements().size());

        if (showDetail) {
            // if detail option is enabled the debug service is required
            assert mappingService instanceof DebugMeasurementMappingService;
            DebugMeasurementMappingService msvc = (DebugMeasurementMappingService) mappingService;

            Collection<Measurement> unmapped = msvc.getUnmapped();
            String status = NOT_MAPPED_TO_KAM_STATUS;
            for (Measurement m : unmapped) {
                debugInfo.put(m, status);
            }

            Map<KamNode,Set<Measurement>> notInPopulation = msvc.getNotInPopulation();
            for (Map.Entry<KamNode, Set<Measurement>> entry : notInPopulation.entrySet()) {
                status = format(NOT_PRESENT_IN_POPULATION_STATUS, entry.getKey().getLabel());
                for (Measurement m : entry.getValue()) {
                    debugInfo.put(m, status);
                }
            }

            Map<KamNode,Set<Measurement>> collapsed = msvc.getCollapsed();
            for (Map.Entry<KamNode, Set<Measurement>> entry : collapsed.entrySet()) {
                status = format(COLLAPSED_STATUS, entry.getKey().getLabel());
                for (Measurement m : entry.getValue()) {
                    debugInfo.put(m, status);
                }
            }
        }

        int popSize = commandLine.hasOption(POP_SIZE_SHORT_OPT)
                ? Integer.parseInt(commandLine.getOptionValue(POP_SIZE_SHORT_OPT))
                : mappingResult.getPopulationSize();

        logger.info("Using population size: {}", popSize);

        logger.debug("Computing scores");
        Scorer scorer = showDetail
                ? new DebugScorer(debugInfo)
                : new Scorer();
        Collection<ScoredHypothesis> scores = scorer.score(hyps,
                mappingResult.getMappedMeasurements(), cutoffs, popSize);
        logger.info("Found {} scores", scores.size());

        String runName = commandLine.getOptionValue(RUN_NAME_SHORT_OPT);
        File resultFile = new File(runName + RESULT_FILE_SUFFIX);

        // write scored hypothesis file
        FileWriter out = new FileWriter(resultFile);
        writeOutput(out, scores);
        logger.info("Complete: scores have been saved to {}",
                resultFile.getAbsolutePath());

        // write detail files if detailed output requested
        if (showDetail) {
            // if detail option is enabled the debug service is required
            assert mappingService instanceof DebugMeasurementMappingService;
            assert scorer instanceof DebugScorer;
            DebugMeasurementMappingService msvc = (DebugMeasurementMappingService) mappingService;
            DebugScorer debugScorer = (DebugScorer) scorer;

            // write mapping file
            File mappingFile = new File(runName + MAPPING_FILE_SUFFIX);
            logger.info("Saving measurement information");
            writeMeasurementDebug(new FileWriter(mappingFile),
                    mappingResult.getMappedMeasurements(), debugInfo);
            logger.info("Mapping output saved to {}", mappingFile);

            // map hyps to all downstreams in the population
            Map<KamNode, Set<Downstream>> hypMap = computeHypMap(
                    hyps,
                    msvc.getInPopulation());

            Map<KamNode, MappedMeasurement> stateChanges = debugScorer
                    .getStateChangeMap();

            // write mechanism detail file
            File detailFile = new File(runName + DETAIL_FILE_SUFFIX);
            FileWriter mechout = new FileWriter(detailFile);
            writeMechanismDetail(mechout, hypMap, scores, stateChanges);
            logger.info("Mechanism detail saved to {}", detailFile);
        }
    }

    /**
     * Compute the {@link KamNode hypothesis node} to
     * {@link Downstream downstreams} that are in the population measured.
     *
     * @param hyps the hypothesis {@link List list}
     * @param population the {@link Set set} of measurements in the
     * population
     * @return a {@link Map map} of {@link KamNode hypothesis node} to
     * {@link Downstream downstreams} in the measured population
     */
    private static Map<KamNode, Set<Downstream>> computeHypMap(List<Hypothesis> hyps,
            Map<Integer, KamNode> population) {
        Map<KamNode, Set<Downstream>> hypDownstreams =
                new HashMap<Kam.KamNode, Set<Downstream>>();
        for (final Hypothesis hyp : hyps) {
            // new set of downstreams
            Set<Downstream> newset = new HashSet<Downstream>();

            // remove all downstreams not in population
            Set<Downstream> oldset = hyp.getDownstreams();
            Iterator<Downstream> it = oldset.iterator();
            while (it.hasNext()) {
                Downstream down = it.next();
                KamNode dn = down.getKamNode();

                KamNode pn = population.get(dn.getId());
                if (pn != null) {
                    newset.add(new Downstream(pn, down.getDirectionType()));
                }
            }

            // store mechanism to downstreams in population
            hypDownstreams.put(hyp.getKamNode(), newset);
        }
        return hypDownstreams;
    }

    private boolean touchFile(boolean valid, File f) {
        boolean touchOk = true;
        try {
            FileUtils.touch(f);
        } catch (IOException e) {
            touchOk = false;
        }
        if (!touchOk || !f.canWrite()) {

            System.err.println("ERROR: Could not open file for " +
                    "writing. Do you have permission to write the file here?");
            valid = false;
        }
        return valid;
    }

    /**
     * Parse a namespace for a given URL
     *
     * @param cacheResource
     * @param namespaceUrl
     * @return
     * @throws Exception
     */
    private Namespace parseNamespace(CacheableResourceService cacheResource,
            String namespaceUrl) throws Exception {
        NamespaceHeaderParser nhp = new NamespaceHeaderParser();
        ResolvedResource nsResource = cacheResource.resolveResource(
                ResourceType.NAMESPACES, namespaceUrl);
        NamespaceHeader header = nhp.parseNamespace(namespaceUrl,
                nsResource.getCacheResourceCopy());
        return new Namespace(header.getNamespaceBlock().getKeyword(),
                namespaceUrl);
    }

    /**
     * Write the score output
     *
     * @param out
     * @param scores
     * @throws IOException
     */
    protected void writeOutput(FileWriter out,
            Collection<ScoredHypothesis> scores) throws IOException {
        CSVWriter writer = new CSVWriter(out);
        String[] line = new String[] { ID_HEADER,
                DIRECTION_HEADER, CORRECT_HEADER, RICHNESS_HEADER, CONCORDANCE_HEADER, AMBIGUOUS_HEADER,
                CONTRA_HEADER, POSSIBLE_HEADER, OBSERVED_HEADER };
        writer.writeNext(line);
        for (ScoredHypothesis score : scores) {
            int idx = -1;
            line[++idx] = score.getKamNode().getLabel();
            // line[++idx] = String.valueOf(score.getDepth());
            line[++idx] = valueOf(score.getDirectionType().getValue());
            line[++idx] = valueOf(score.getNumberCorrect());
            line[++idx] = valueOf(score.getRichness());
            line[++idx] = valueOf(score.getConcordance());
            line[++idx] = valueOf(score.getNumberAmbiguous());
            line[++idx] = valueOf(score.getNumberContra());
            line[++idx] = valueOf(score.getPossible());
            line[++idx] = valueOf(score.getObserved());
            writer.writeNext(line);
        }
        writer.flush();
        writer.close();
    }

    protected void writeMeasurementDebug(FileWriter out,
            Collection<MappedMeasurement> mappedMeasurements,
            Map<Measurement,String> debugInfo) throws IOException {

        Map<Measurement, KamNode> nodeMap = new HashMap<Measurement, Kam.KamNode>();
        for (MappedMeasurement mm : mappedMeasurements) {
            nodeMap.put(mm.getMeasurement(), mm.getKamNode());
        }

        CSVWriter writer = new CSVWriter(out);
        String[] line = new String[] { ID_HEADER, KAM_NODE_HEADER, STATUS_HEADER };
        writer.writeNext(line);
        Map<Measurement, String> idMap = ((DebugIdAmpDataFileService) dataFileService).getIdMap();

        for (Map.Entry<Measurement, String> entry : idMap.entrySet()) {
            Measurement m = entry.getKey();
            int idx = -1;
            line[++idx] = entry.getValue();
            line[++idx] = nodeMap.get(m) == null ? "" : nodeMap.get(m).getLabel();
            line[++idx] = debugInfo.get(m);
            writer.writeNext(line);
        }
        writer.flush();
        writer.close();
    }

    protected void writeMechanismDetail(FileWriter out,
            Map<KamNode, Set<Downstream>> hypDownstreams,
            Collection<ScoredHypothesis> scores,
            Map<KamNode, MappedMeasurement> stateChanges) throws IOException {

        CSVWriter csv = new CSVWriter(out);
        String[] line = new String[] { SOURCE_HEADER, RELATIONSHIP_HEADER, TARGET_HEADER,
                TYPE_HEADER, DIRECTION_HEADER };
        csv.writeNext(line);

        for (final ScoredHypothesis score : scores) {
            Prediction pred = score.getPrediction();

            line[0] = score.getKamNode().getLabel();

            Set<Downstream> allDownstreams = hypDownstreams.get(score
                    .getKamNode());
            for (final Downstream down : allDownstreams) {
                final KamNode downNode = down.getKamNode();
                final MappedMeasurement mm = stateChanges.get(downNode);
                final String scoreType = getScoreType(pred, down);

                line[1] = translateDirection(down.getDirectionType());
                line[2] = downNode.getLabel();
                line[3] = scoreType;

                if (mm == null) {
                    line[4] = NOT_SIGNIFICANT;
                } else {
                    line[4] = mm.getMeasurement().getDirection().getDisplayValue();
                }

                csv.writeNext(line);
            }
            csv.flush();
        }
        csv.close();
    }

    protected String getScoreType(final Prediction pred, final Downstream down) {
        if (pred == null) {
            return NOT_SIGNIFICANT;
        }

        if (pred.getCorrect().contains(down)) {
            return CORRECT;
        } else if (pred.getContra().contains(down)) {
            return CONTRA;
        } else if (pred.getAmbiguous().contains(down)) {
            return AMBIGUOUS;
        }

        return NOT_SIGNIFICANT;
    }

    protected String translateDirection(final DirectionType dir) {
        if (dir == null) {
            return null;
        }

        switch (dir) {
        case UP:
            return INCREASES.getDisplayValue();
        case DOWN:
            return DECREASES.getDisplayValue();
        default:
            return AMBIGUOUS;
        }
    }

    /**
     * Retrieve the {@link Options} applicable to Rcr.
     *
     * @return
     */
    protected Options getCommandLineOptions() {
        Options ret = new Options();
        Option o = new Option(KAM_SHORT_OPT, KAM_LONG_OPT, true,
                "KAM name. Must be present in the local KAM catalog.");
        o.setRequired(true);
        ret.addOption(o);

        o = new Option(
                DATA_SHORT_OPT,
                DATA_LONG_OPT,
                true,
                "Dataset input file. Must be a valid IdAMP formatted file of a single comparison.");
        o.setRequired(true);
        ret.addOption(o);

        o = new Option(RUN_NAME_SHORT_OPT, RUN_NAME_LONG_OPT, true,
                "Name of the whistle run used as a file prefix.");
        o.setRequired(true);
        ret.addOption(o);

        o = new Option(
                NS_URL_SHORT_OPT,
                NS_URL_LONG_OPT,
                true,
                "Resource location URL of the namespace containing all values of the Dataset input file.");
        o.setRequired(true);
        ret.addOption(o);

        ret.addOption(new Option(ANLST_SHORT_OPT, ANLST_LONG_OPT, false,
                "Use analyst selection to filter Measurements to state changes."));
        ret.addOption(new Option(
                FOLD_CHANGE_SHORT_OPT,
                FOLD_CHANGE_LONG_OPT,
                true,
                "Fold change cutoff to apply to Measurements for state change generation. Applicable only if not using analyst selection."));
        ret.addOption(new Option(
                PVAL_SHORT_OPT,
                PVAL_LONG_OPT,
                true,
                "P-value cutoff to apply to Measurements for state change generation. Applicable only if not using analyst selection."));
        ret.addOption(new Option(
                ABUN_SHORT_OPT,
                ABUN_LONG_OPT,
                true,
                "Abundance cutoff to apply to Measurements for state change generation. Applicable only if not using analyst selection."));
        ret.addOption(new Option(
                POP_SIZE_SHORT_OPT,
                POP_SIZE_LONG_OPT,
                true,
                "Population size. The default is to calculate population based on the data set measurements that exist in the KAM."));

        ret.addOption(new Option(DETAIL_LONG_OPT, false,
                "Output a mapping and mechanism detail file that shows additional information"));

        ret.addOption(new Option(SPECIES_TAXID_SHORT_OPT, SPECIES_TAXID_LONG_OPT, true,
                "The species taxonomy id used to collapse orthologous nodes."));

        return ret;
    }

    /**
     * Obtain the {@link SystemConfiguration}.<br>
     * Defaults to obtaining via the BELFRAMEWORK_HOME environment variable.
     *
     * @return
     * @throws Exception
     */
    protected SystemConfiguration getSystemConfiguration() throws Exception {
        return sysCfg == null ? SystemConfiguration
                .createSystemConfiguration(null) : sysCfg;
    }

    /**
     * Obtain the {@link DatabaseService} for use in Rcr.<br>
     * Defaults to {@link DatabaseServiceImpl}
     *
     * @return
     * @throws Exception
     */
    protected DatabaseService getDatabaseService() throws Exception {
        return dbService == null ? new DatabaseServiceImpl() : dbService;
    }

    /**
     * Obtain the {@link CacheableResourceService} for use in Rcr.<br>
     * Defaults to {@link DefaultCacheableResourceService}
     *
     * @return
     * @throws Exception
     */
    protected CacheableResourceService getCacheableResourceService()
            throws Exception {
        return cacheService == null ? new DefaultCacheableResourceService()
                : cacheService;
    }

    /**
     * Obtain the {@link KamStore} for use in Rcr.<br>
     * Defaults to {@link KamStoreImpl}
     *
     * @return
     * @throws Exception
     */
    protected KamStore getKamStore() throws Exception {
        if (kamStore != null) {
            return kamStore;
        }
        if (sysCfg == null) {
            throw new IllegalStateException(
                    "System Configuration must be valid for KamStore creation");
        }
        if (dbService == null) {
            throw new IllegalStateException(
                    "DBService must be valid for KamStore creation");
        }
        DBConnection dbc = dbService.dbConnection(sysCfg.getKamURL(),
                sysCfg.getKamUser(), sysCfg.getKamPassword());
        return new KamStoreImpl(dbc);
    }

    /**
     * Obtain the {@link DataFileService} for use in processing the input data
     * file.
     *
     * @return
     * @throws Exception
     */
    protected DataFileService getDataFileService() throws Exception {

        return dataFileService == null ?
                (commandLine.hasOption(DETAIL_LONG_OPT)
                        ? new DebugIdAmpDataFileService()
                        : new DefaultIdAMPDataFileService())
                : dataFileService;
    }

    /**
     * Obtain the {@link MeasurementMappingService} for use in mapping
     * {@link Measurement}s to a {@link Kam}
     * @return
     * @throws Exception
     */
    protected MeasurementMappingService getMappingService() throws Exception {
        if (mappingService != null) {
            return mappingService;
        }
        if (getKamStore() == null) {
            throw new IllegalStateException(
                    "KamStore must be valid for MeasurementMappingService creation");
        }
        // if debug is enabled, use the debug mappingService
        DefaultMeasurementMappingService mappingService;
        if (commandLine.hasOption(DETAIL_LONG_OPT)) {
            mappingService = new DebugMeasurementMappingService();
        } else {
            mappingService = new DefaultMeasurementMappingService();
        }

        mappingService.setCollapsingStrategy(new DefaultCollapsingStrategy(commandLine.hasOption(ANLST_SHORT_OPT)));
        mappingService.setKamStore(getKamStore());
        return mappingService;
    }

    /**
     * Obtain the {@link Dialect} for use in Rcr.<br>
     * Defaults to {@link RcrDialect}
     *
     * @return
     */
    protected Dialect getDialect() {
        if (dialect != null) {
            return dialect;
        }
        if (kamStore == null) {
            throw new IllegalStateException(
                    "KamStore must be valid for Dialect creation");
        }
        return new RcrDialect(kamStore);
    }

    /**
     * Verify a {@link Double} can be parsed from the provided {@link String}.
     *
     * @param d
     * @return
     */
    protected static boolean isDouble(String d) {
        try {
            Double.valueOf(d);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Get a string for a number.<br>
     * This implementation returns "NA" if the number is null; useful for R processing
     * @param number
     * @return
     */
    protected String valueOf(Number number) {
        return number == null ? "NA" : String.valueOf(number);
    }

    /**
     * Simple {@link Dialect} for usage in RCR:<br>
     * Uses the label from the first (arbitrary) supporting term found for the
     * node.
     *
     * @author Steve Ungerer
     */
    private class RcrDialect implements Dialect {
        private KamStore kamStore;

        public RcrDialect(KamStore kamStore) {
            this.kamStore = kamStore;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getLabel(KamNode kamNode) {
            String label = kamNode.getLabel();
            try {
                List<BelTerm> terms = kamStore.getSupportingTerms(kamNode);
                if (!terms.isEmpty()) {
                    BelTerm bt = terms.get(0);
                    Term t = BELParser.parseTerm(bt.getLabel());
                    label = t.toBELShortForm();
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to retrieve label for kamNode", e);
            }
            return label;
        }
    }

}
