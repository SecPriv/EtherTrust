package secpriv.horst;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.Global;
import com.microsoft.z3.Status;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.jgrapht.io.ExportException;
import picocli.CommandLine;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Rule;
import secpriv.horst.evm.*;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.SelectorFunctionInvoker;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.SmtLibGenerator;
import secpriv.horst.translation.BigStepClauseWalker;
import secpriv.horst.translation.MediumStepTransformer;
import secpriv.horst.translation.TranslateToZ3VisitorState;
import secpriv.horst.translation.TranslationPipeline;
import secpriv.horst.translation.layout.FlatTypeLayouterWithBoolean;
import secpriv.horst.translation.visitors.*;
import secpriv.horst.visitors.ProgramVisitor;
import secpriv.horst.visitors.RuleTypeOracle;
import secpriv.horst.visitors.SExpressionRuleVisitor;
import secpriv.horst.visitors.VisitorState;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "HoRStCompiler", mixinStandardHelpOptions = true, version = "HoRSt Compiler version 0.0")
public class HorstCompiler implements Runnable {
    private static final Logger logger = LogManager.getLogger(HorstCompiler.class);
    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose mode. Helpful for troubleshooting. " +
            "Multiple -v options increase the verbosity.")
    private boolean[] verbose = new boolean[0];

    @CommandLine.Option(names = {"-p", "--preanalysis"}, description = "Apply pre-analysis")
    private boolean pre = false;
    @CommandLine.Option(names = {"-sat", "--expect-sat"}, description = "Expect SAT as the analysis result")
    private boolean sat = false;
    @CommandLine.Option(names = {"-unsat", "--expect-unsat"}, description = "Expect UNSAT as the analysis result")
    private boolean unsat = false;
    @CommandLine.Option(names = {"-w", "--statistics"}, description = "Compute statistics")
    private boolean stat = false;

    @CommandLine.Option(names = {"-b", "--big-step-encoding"}, description = "Apply big-step encoding")
    private boolean bigStep = false;

    @CommandLine.Option(names = {"-a", "--arithmetic-with-bv"}, description = "Use bitvectors as an abstract domain (integer by default)")
    private boolean bv = false;

    @CommandLine.Option(names = {"-o", "--one-sat-to-stop"}, description = "Finding a SAT instance stops the execution")
    private boolean oneSat = false;

    @CommandLine.Option(names = {"-l", "--write-to-log"}, description = "Write to only to log. Omitting this flag will cause output on stdout.")
    private boolean log = false;

    @CommandLine.Option(names = {"-r", "--use-relation-settings and bound_relation"},
            description = "Use bound_relation and interval_relation settings when registering the relation.")
    private boolean settings = false;

    @CommandLine.Option(names = {"-f", "--selector-function-provider"}, description = "Provide a java file that will be used for evaluating selector functions. " +
            "Multiple files can be given. In case of a naming conflict, the first class containing a matching declaration will be used. If a file does not end " +
            "with .java it will not be used as a selector function provider but as a argument to the preceding selector function provider.", arity = "1..*")
    private String[] selectorFunctionProviders = new String[0];

    @CommandLine.Option(names = {"--evm"}, description = "Loads the default selector function provider for EVM byte code and analyses the contracts given in the arguments.", arity = "1..*")
    private String[] evmSelectorFunctionProviderArguments = new String[0];

    @CommandLine.Option(names = {"--evm-tests"}, description = "Loads the default selector function provider for EVM byte code tests and analyses the contracts given in the arguments.", arity = "1..*")
    private String[] evmTestsSelectorFunctionProviderArguments = new String[0];

    @CommandLine.Option(names = {"-s", "--spec"}, description = "Provide the HoRSt spec to compile. You can specify multiple files. The definitions of one file " +
            "will be visible in the the subsequent files.", arity = "1..*")
    private String[] horstFiles = new String[0];

    public static void main(String[] args) {
        CommandLine.run(new HorstCompiler(), args);
    }

    private static Map<String, List<String>> groupParameters(Iterable<String> args) {
        List<String> accumulator = new ArrayList<>();
        HashMap<String, List<String>> ret = new HashMap<>();

        for (String arg : args) {
            if (arg.endsWith(".java")) {
                accumulator = new ArrayList<>();
                ret.put(arg, accumulator);
            } else {
                accumulator.add(arg);
            }
        }

        return ret;
    }

    @Override
    public void run() {
        if (stat){
            EvmStatistics es = new EvmStatistics(evmSelectorFunctionProviderArguments[0]);
            es.compute();
            es.display();
            System.exit(0);
        }
        configureLogger();
        ContractInfoReader contractInfoReader;

        // TODO: This data flow is the ugliest thing in the whole code base. Rework after deadline.
        EvmTestSelectorFunctionProvider testSelectorFunctionProvider = null;
        try {
            if(evmTestsSelectorFunctionProviderArguments.length > 0) {
                testSelectorFunctionProvider = new EvmTestSelectorFunctionProvider(evmTestsSelectorFunctionProviderArguments, stat);
                contractInfoReader = testSelectorFunctionProvider.getContractInfoReader();
            } else {
                contractInfoReader = new ContractInfoReader(Arrays.asList(evmSelectorFunctionProviderArguments), stat);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

            SelectorFunctionHelper compiler = new SelectorFunctionHelper();

            try {
                for (Map.Entry<String, List<String>> se : groupParameters(Arrays.asList(selectorFunctionProviders)).entrySet()) {
                    compiler.compileSelectorFunctionsProvider(se.getKey(), se.getValue());
                }

                EvmSelectorFunctionProviderTemplate providerTemplate;
                final boolean testMode = evmTestsSelectorFunctionProviderArguments.length > 0;

                // TODO: We might want to move this to the constructor of ConstantAnalysis/EVMFunctionProvider
                if (evmSelectorFunctionProviderArguments.length > 0 || testMode) {
                    if (pre) {
                        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
                        logger.info("");
                        logger.info("#######################");
                        logger.info("#  Constant Analysis  #");
                        logger.info("#######################");
                        logger.info("");
                        ca.getBlocksFromBytecode();
                        /*ca.getCFGs();
                        // No cycles found, let's walk topologically
                        if (!ca.checkCycles()){
                            ca.runBlocksTopologically();
                        }
                        else{
                            ca.runBlocks();
                        }
                        */
                        ca.runBlocks();
                        ca.printBlocks();
                        providerTemplate = ca;
                    } else {
                        providerTemplate = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
                    }

                    if(testMode) {
                        testSelectorFunctionProvider.setEvmSelectorFunctionProvider(providerTemplate);
                        compiler.registerProvider(testSelectorFunctionProvider);
                    } else {
                        compiler.registerProvider(providerTemplate);
                    }
                }
            } catch (IOException e) {
                logger.error("Error while handling file!", e);
                System.exit(1);
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                logger.error("Error while compiling selector function definition!", e);
                System.exit(1);
            } /*catch (ExportException e) {
                e.printStackTrace();
            } */

            try {
                VisitorState state = new VisitorState();
                state.setSelectorFunctionHelper(compiler);

                for (String horstFile : horstFiles) {
                    ASLexer lexer = new ASLexer(CharStreams.fromFileName(horstFile));
                    CommonTokenStream tokens = new CommonTokenStream(lexer);
                    ASParser parser = new ASParser(tokens);

                    ProgramVisitor visitor = new ProgramVisitor(state);
                    state = visitor.visit(parser.abstractProgram()).get();
                }

                RuleTypeOracle ruleTypeOracle = new RuleTypeOracle(state);

                TranslationPipeline pipeline = TranslationPipeline
                        .builder()
                        //.enableDebug()
                        .addStep(new InlineOperationsRuleVisitor(new ArrayList<>(state.getOperations().values())))
                        .addStep(new InlineTypesRuleVisitor(new InlineTypesExpressionVisitor(new FlatTypeLayouterWithBoolean())))
                        .addFlatMappingStep(new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(compiler)))
                        .addStep(new SimplifyPredicateArgumentsRuleVisitor())
                        .addStep(new RenameFreeVariablesRuleVisitor())
                        .addStep(new ConstantFoldingRuleVisitor())
                        .addStep(new FilterUnapplicableClausesRuleVisitor(ruleTypeOracle))
                        .build();

                List<Rule> renamedFreeVarRules = pipeline.apply(new ArrayList<>(state.getRules().values()));
                List<Rule> finalRules = renamedFreeVarRules;

                Map<Boolean, List<Rule>> rulePartitions = finalRules.stream().collect(Collectors.partitioningBy(ruleTypeOracle::isQueryOrTest));

                List<Rule> queryRules = rulePartitions.get(true);
        //        List<Rule> nonQueryRules = rulePartitions.get(false);

                if (bigStep) {
                    /*
                    logger.info("");
                    logger.info("###################");
                    logger.info("# BIG STEP WALKER #");
                    logger.info("###################");
                    logger.info("");

                    BigStepClauseWalker bscw = new BigStepClauseWalker(nonQueryRules);
                    bscw.run();

                    Set<List<Rule>> merges = bscw.getMerges();

                    logger.info("");
                    logger.info("################");
                    logger.info("# APPLY MERGES #");
                    logger.info("################");
                    logger.info("");

                    List<Rule> mergedRules = new ArrayList<>();
                    ClauseMerger cm = new ClauseMerger();
                    for (List<Rule> merge : merges) {
                        if (merge.size() > 1) {
                            // filter rules: delete the merged rules
                            for (Rule r : merge) {
                                nonQueryRules.remove(r);
                            }
                            mergedRules.add(cm.projectAndMerge(merge));
                        }
                    }

                    //mergedRules.forEach(r -> System.out.println(r.accept(new SExpressionRuleVisitor())));

                    nonQueryRules.addAll(mergedRules);

                     */

                    finalRules = MediumStepTransformer.foldToMediumSteps(finalRules, ruleTypeOracle);
                }

           //     List<Rule> finalRules = new ArrayList<>(nonQueryRules);
            //    finalRules.addAll(queryRules);

                //SmtLibGenerator.writeSmtLibToFile("/Users/ilgris/Desktop/with_big_step.smt2", finalRules, finalRules.stream().filter(ruleTypeOracle::isQueryOrTest).collect(Collectors.toList()));

                logger.info("");
                logger.info("###################");
                logger.info("#     RESULTS     #");
                logger.info("###################");
                logger.info("");

                //SmtLibGenerator.writeSmtLibToFile("/tmp/non-preanalysis.smt", finalRules, queryRules);

                Global.resetParameters();

                Global.setParameter("fp.engine", "spacer");

                TranslateToZ3VisitorState z3TranslationState;
                if (bv) {
                    z3TranslationState = TranslateToZ3VisitorState.withBitVectorIntegers();
                } else {
                    z3TranslationState = TranslateToZ3VisitorState.withGeneralIntegers();
                }

                List<BoolExpr> rulesForZ3 = new ArrayList<>();

                for (Rule rule : finalRules) {
                    TranslateToZ3RuleVisitor translateToZ3RuleVisitor = new TranslateToZ3RuleVisitor(z3TranslationState);
                    rulesForZ3.addAll(rule.accept(translateToZ3RuleVisitor));
                }

                execQueries(queryRules, z3TranslationState, rulesForZ3, ruleTypeOracle, state);

                logger.info("Done!");
            } catch (IOException e) {
                logger.error("Input/Output error:", e);
                System.exit(1);
            }
    }
    /*private void splitQueries(List<Rule> queryRules, List<Rule> queryReentrancy,
                              List<Rule> querySelfdestruct, List<Rule> queryCallcode, List<Rule> queryDelegateCall,
                              List<Rule> queryOthers){
        for (Rule query : queryRules) {
            if (query.name.split("_")[0].equals("reentrancy")){
                queryReentrancy.add(query);
            }
            else if (query.name.split("_")[0].equals("q_selfdestruct")){
                querySelfdestruct.add(query);
            }
            else if (query.name.split("_")[0].equals("q_callcode")){
                queryCallcode.add(query);
            }
            else if (query.name.split("_")[0].equals("q_delegatecall")){
                queryDelegateCall.add(query);
            }
            else{
                queryOthers.add(query);
            }
        }
    }*/

    private boolean execQueries(List<Rule> queryRules, TranslateToZ3VisitorState z3TranslationState,
                             List<BoolExpr> rulesForZ3, RuleTypeOracle ruleTypeOracle, VisitorState state){
        boolean once = false;
        int testCount = 0;
        int failedTestCount = 0;
        int failedSoundTestCount = 0;
        boolean satDerived = false;
        for (Rule query : queryRules) {
            Fixedpoint fixedpoint = z3TranslationState.context.mkFixedpoint();
            for (BoolExpr b : rulesForZ3) {
                fixedpoint.addRule(b, null);
            }
            z3TranslationState.registerRelations(fixedpoint, settings);
            if (!once) {
                logger.info(fixedpoint);
                once = true;
            }
            String queryId = query.name;
            long start = System.currentTimeMillis();
            Predicate predicate = new Predicate(queryId, Collections.emptyList(), Collections.emptyList());
            BoolExpr z3query = (BoolExpr) z3TranslationState.getZ3PredicateDeclaration(predicate).apply();
            logger.info("Start query ID " + queryId);
            //logger.contractInfoReader("Query: " + query.accept(new SExpressionRuleVisitor()));
            try {
                Status result = fixedpoint.query(z3query);

                if (ruleTypeOracle.isTest(query)) {
                    ++testCount;
                    if (result != Status.UNKNOWN && state.isExpectedTestResult(query, z3ToHorstResult(result))) {
                        continue;
                    } else {
                        ++failedTestCount;
                        if(query.name.startsWith("correctValues")){
                            ++failedSoundTestCount;
                        }
                        logger.warn(" Test " + queryId + " did not yield expected result!");
                    }
                }

                logger.info("    result: " + result.toString());
                logger.info("    query execution time: " + ((System.currentTimeMillis() - start)) + " ms");
                if (result.toString().equals("SATISFIABLE")){
                    satDerived = true;
                    if (oneSat){
                        logger.warn("We found a SAT instance and the option has been enabled to stop right after it");
                        break;
                    }
                }
                if (sat && unsat){
                    logger.warn("Please be precise about expected result, it should be SAT or UNSAT, no both");
                }
                else{
                    if (sat){
                        if (result.toString().equals("SATISFIABLE")){
                            logger.info("Bench test passed " + evmSelectorFunctionProviderArguments[0]);
                        }
                        else{
                            logger.error("Bench test failed " + evmSelectorFunctionProviderArguments[0]);
                        }
                    }
                    if (unsat){
                        if (result.toString().equals("UNSATISFIABLE")){
                            logger.info("Bench test passed " + evmSelectorFunctionProviderArguments[0]);
                        }
                        else{
                            logger.error("Bench test failed " + evmSelectorFunctionProviderArguments[0]);
                        }
                    }
                }
            }
            // deal with exceptions properly
            catch (Exception e) {
                logger.error("It was impossible to find an answer to query... " + queryId + " because of " + e.getMessage());
            }
        }

        if (testCount != 0) {
            String testFileName = evmTestsSelectorFunctionProviderArguments.length > 0 ? evmTestsSelectorFunctionProviderArguments[0] : evmSelectorFunctionProviderArguments[0];

            if (failedTestCount == 0) {
                logger.info("In " + testFileName +  " All tests passed! (" + testCount + "/" + testCount + ")");
            } else {
                if (failedSoundTestCount > 0) {
                    logger.warn("(correctness affected!) In " + testFileName + " " + failedTestCount + " of " + testCount + " tests failed!");
                }
                else{
                    logger.warn("(precision affected!) In " + testFileName + " " + failedTestCount + " of " + testCount + " tests failed!");
                }
            }
        }
        return satDerived;
    }

    private static VisitorState.TestResult z3ToHorstResult(Status result) {
        switch (result) {
            case SATISFIABLE:
                return VisitorState.TestResult.SAT;
            case UNSATISFIABLE:
                return VisitorState.TestResult.UNSAT;
        }
        throw new IllegalArgumentException("Provided " + result + " but only " + Status.SATISFIABLE + " and " + Status.UNSATISFIABLE + " are valid Arguments!");
    }

    private void configureLogger() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Level level = getLoggerLevel();
        context.getConfiguration().getRootLogger().setLevel(level);

        if (!log) {
            Appender consoleAppender = context.getConfiguration().getAppender("Console");
            context.getConfiguration().getRootLogger().addAppender(consoleAppender, level, null);
        }

        if (verbose.length == 1) {
            logger.warn("I will compile your HoRSt files!");
        } else if (verbose.length == 2) {
            logger.info("I will verbosely compile your HoRSt files!");
        } else if (verbose.length == 3) {
            logger.info("I will verbosemostly compile your HoRSt files!");
        } else if (verbose.length > 3) {
            logger.info("Dear Sir or Madam! I herewith assure you in the most sincere manner that I will apply the utmost verbosity while I compile your HoRSt files!");
        }
    }

    private Level getLoggerLevel() {
        switch (verbose.length) {
            case 0:
                return Level.ERROR;
            case 1:
                return Level.WARN;
            case 2:
                return Level.INFO;
            case 3:
                return Level.DEBUG;
        }
        return Level.TRACE;
    }
}
