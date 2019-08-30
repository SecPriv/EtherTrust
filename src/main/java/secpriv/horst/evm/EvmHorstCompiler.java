package secpriv.horst.evm;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import picocli.CommandLine;
import secpriv.horst.data.Rule;
import secpriv.horst.execution.ExecutionResult;
import secpriv.horst.execution.ExecutionResultHandler;
import secpriv.horst.execution.Z3QueryExecutor;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.SelectorFunctionInvoker;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.translation.MediumStepTransformer;
import secpriv.horst.translation.TranslationPipeline;
import secpriv.horst.translation.layout.FlatTypeLayouterWithBoolean;
import secpriv.horst.translation.visitors.*;
import secpriv.horst.visitors.ProgramVisitor;
import secpriv.horst.visitors.RuleTypeOracle;
import secpriv.horst.visitors.VisitorState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "EvmRegressionTester", mixinStandardHelpOptions = true, version = "RegressionTester version 0.0")
public class EvmHorstCompiler implements Runnable {
    @CommandLine.Option(names = {"-s", "--spec"}, description = "Provide the HoRSt spec to compile. You can specify multiple files. The definitions of one file " +
            "will be visible in the the subsequent files.", arity = "1..*")
    private String[] horstFiles = new String[0];

    @CommandLine.Option(names = {"-b", "--big-step-encoding"}, description = "Apply big-step encoding")
    private boolean bigStep = false;

    @CommandLine.Option(names = {"-p", "--preanalysis"}, description = "Apply pre-analysis")
    private boolean pre = false;

    @CommandLine.Parameters
    private File[] contractFiles;

    @CommandLine.Option(names = {"--out-dir"}, description = "Directory files are written.")
    private String outDir;

    @CommandLine.Option(names = {"--json-out-dir"}, description = "Directory files are written.")
    private String jsonOutDir;

    public static void main(String[] args) {
        CommandLine.run(new EvmHorstCompiler(), args);
    }

    @Override
    public void run() {
        for (File contractFile : contractFiles) {
            List<ExecutionResultHandler> resultHandlers = initializeResultHandlers(contractFile);

            System.out.println(contractFile.getName());
            SelectorFunctionHelper compiler = new SelectorFunctionHelper();
            ContractInfoReader contractInfoReader = new ContractInfoReader(EvmSourceProvider.fromAnyFile(contractFile), false);
            EvmSelectorFunctionProviderTemplate providerTemplate;

            if (pre) {
                ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
                ca.getBlocksFromBytecode();
                ca.runBlocks();
                //ca.splitToStandardAndRichOpcodeToPosition();
                providerTemplate = ca;
            } else {
                providerTemplate = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
                //contractInfoReader.populateStandardOpcodes();
            }

            compiler.registerProvider(providerTemplate);

            VisitorState state = new VisitorState();
            state.setSelectorFunctionHelper(compiler);

            state = parseAllHorstFiles(state);

            RuleTypeOracle ruleTypeOracle = new RuleTypeOracle(state);

            TranslationPipeline.TranslationPipelineBuilder pipelineBuilder = TranslationPipeline
                    .builder()
                    .addStep(new InlineOperationsRuleVisitor(new ArrayList<>(state.getOperations().values())))
                    .addStep(new InlineTypesRuleVisitor(new InlineTypesExpressionVisitor(new FlatTypeLayouterWithBoolean())))
                    .addFlatMappingStep(new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(compiler)))
                    .addStep(new SimplifyPredicateArgumentsRuleVisitor())
                    .addStep(new RenameFreeVariablesRuleVisitor())
                    .addStep(new ConstantFoldingRuleVisitor())
                    .addStep(new FilterUnapplicableClausesRuleVisitor(ruleTypeOracle))
                    .addStep(new RemoveTruePremiseRuleVisitor());

            TranslationPipeline pipeline = pipelineBuilder.build();
            List<Rule> allRules = pipeline.apply(new ArrayList<>(state.getRules().values()));

            if (bigStep) {
                allRules = MediumStepTransformer.foldToMediumSteps(allRules, ruleTypeOracle);
            }

            List<ExecutionResult> executionResults = (new Z3QueryExecutor()).executeQueries(allRules, ruleTypeOracle);

            for(ExecutionResultHandler resultHandler : resultHandlers) {
                resultHandler.handle(executionResults);
            }
        }
    }

    private List<ExecutionResultHandler> initializeResultHandlers(File contractFile) {
        List<ExecutionResultHandler> resultHandlers = new ArrayList<>();

        if(jsonOutDir != null) {
            resultHandlers.add(new ExecutionResultHandler.JsonOutputExecutionResultHandler(jsonOutDir + "/" + contractFile.getName() + ".json"));
        }

        return resultHandlers;
    }


    private VisitorState parseAllHorstFiles(VisitorState state) {
        try {
            for (String horstFile : horstFiles) {
                ASLexer lexer = new ASLexer(CharStreams.fromFileName(horstFile));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                ASParser parser = new ASParser(tokens);

                ProgramVisitor visitor = new ProgramVisitor(state);
                state = visitor.visit(parser.abstractProgram()).get();
            }
            return state;
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing Horst files", e);
        }
    }
}
