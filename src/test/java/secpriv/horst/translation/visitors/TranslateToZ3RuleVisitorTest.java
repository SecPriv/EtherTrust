package secpriv.horst.translation.visitors;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.Log;
import com.microsoft.z3.Status;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Rule;
import secpriv.horst.data.SelectorFunction;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.SelectorFunctionInvoker;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.translation.TranslateToZ3VisitorState;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.PredicateDefinitionVisitor;
import secpriv.horst.visitors.RuleVisitor;
import secpriv.horst.visitors.VisitorState;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TranslateToZ3RuleVisitorTest {
    private SelectorFunctionHelper selectorFunctionHelper = null;
    private VisitorState state = null;
    private TranslateToZ3VisitorState z3state = null;
    private RuleVisitor ruleVisitor = null;
    private PredicateDefinitionVisitor predicateDefinitionVisitor = null;
    private TranslateToZ3RuleVisitor translateToZ3RuleVisitor = null;

    @BeforeAll
    public static void registerZ3() {
        com.microsoft.z3.Global.ToggleWarningMessages(true);
        Log.open("test.log");
    }

    @BeforeEach
    void setUp() {
        selectorFunctionHelper = new SelectorFunctionHelper();

        Object o = new Object() {
            public Iterable<BigInteger> oneToFive() {
                return Stream.of(1, 2, 3, 4, 5).map(BigInteger::valueOf).collect(Collectors.toList());
            }

            public Iterable<Boolean> allBools() {
                return Arrays.asList(false, true);
            }

            public Iterable<Tuple2<BigInteger, Boolean>> intPlusParity(BigInteger b) {
                ArrayList<Tuple2<BigInteger, Boolean>> ret = new ArrayList<>();

                for (int i = 0; i < b.intValue(); ++i) {
                    ret.add(new Tuple2<>(BigInteger.valueOf(i), i % 2 == 0));
                }
                return ret;
            }
        };

        selectorFunctionHelper.registerProvider(o);

        state = new VisitorState();
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        state.setSelectorFunctionHelper(selectorFunctionHelper);
        state.defineSelectorFunction(new SelectorFunction("oneToFive", Collections.emptyList(), Collections.singletonList(Type.Integer)));
        state.defineSelectorFunction(new SelectorFunction("allBools", Collections.emptyList(), Collections.singletonList(Type.Boolean)));
        state.defineSelectorFunction(new SelectorFunction("intPlusParity", Collections.singletonList(Type.Integer), Arrays.asList(Type.Integer, Type.Boolean)));

        predicateDefinitionVisitor = new PredicateDefinitionVisitor(state);
        ruleVisitor = new RuleVisitor(state);

        z3state = TranslateToZ3VisitorState.withGeneralIntegers();
        translateToZ3RuleVisitor = new TranslateToZ3RuleVisitor(z3state);
    }

    @AfterEach
    void tearDown() {
        selectorFunctionHelper = null;
        state = null;
        z3state = null;
        ruleVisitor = null;
        predicateDefinitionVisitor = null;
    }

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ASParser(tokens);
    }

    private Optional<Rule> getRuleFromString(String s) {
        return ruleVisitor.visitRuleDefinition(getParserFromString(s).ruleDefinition());
    }

    private Optional<Predicate> getPredicateFromString(String s) {
        return predicateDefinitionVisitor.visitPredicateDeclaration(getParserFromString(s).predicateDeclaration());
    }


    @Test
    public void testSimpleSatisfiableRule1() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=\n" +
                "clause             \n" +
                "true               \n" +
                "=> FunnyFun(1)  \n" +
                ";";

        Optional<Predicate> optPred = getPredicateFromString(p);
        assertThat(optPred).isPresent();

        state.definePredicate(optPred.get());

        Optional<Rule> optRule = getRuleFromString(s);
        assertThat(optRule).isPresent();

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(optRule.get());

        Fixedpoint fixedpoint = z3state.context.mkFixedpoint();

        List<BoolExpr> z3rules = instantiatedRules.get(0).accept(translateToZ3RuleVisitor);
        z3state.registerRelations(fixedpoint, false);

        for (BoolExpr b : z3rules) {
            System.out.println(fixedpoint);
            fixedpoint.addRule(b, null);
        }

        Status status = fixedpoint.query((BoolExpr) z3state.getZ3PredicateDeclaration(optPred.get()).apply(z3state.context.mkInt(1)));

        assertThat(status).isEqualTo(Status.SATISFIABLE);
    }

    @Test
    public void testSimpleSatisfiableRule2() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=\n" +
                "clause [?i : int]  \n" +
                "2 < 10             \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        Optional<Predicate> optPred = getPredicateFromString(p);
        assertThat(optPred).isPresent();

        state.definePredicate(optPred.get());

        Optional<Rule> optRule = getRuleFromString(s);
        assertThat(optRule).isPresent();

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(optRule.get());

        Fixedpoint fixedpoint = z3state.context.mkFixedpoint();

        List<BoolExpr> z3rules = instantiatedRules.get(0).accept(translateToZ3RuleVisitor);
        z3state.registerRelations(fixedpoint, false);

        for (BoolExpr b : z3rules) {
            System.out.println(fixedpoint);
            fixedpoint.addRule(b, null);
        }

        System.out.println(fixedpoint);

        Status status = fixedpoint.query((BoolExpr) z3state.getZ3PredicateDeclaration(optPred.get()).apply(z3state.context.mkInt(1)));

        assertThat(status).isEqualTo(Status.SATISFIABLE);
    }

    @Test
    public void testSimpleUnsatisfiableRule1() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=\n" +
                "clause             \n" +
                "true               \n" +
                "=> FunnyFun(1)  \n" +
                ";";

        Optional<Predicate> optPred = getPredicateFromString(p);
        assertThat(optPred).isPresent();

        state.definePredicate(optPred.get());

        Optional<Rule> optRule = getRuleFromString(s);
        assertThat(optRule).isPresent();

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(optRule.get());

        Fixedpoint fixedpoint = z3state.context.mkFixedpoint();

        List<BoolExpr> z3rules = instantiatedRules.get(0).accept(translateToZ3RuleVisitor);
        z3state.registerRelations(fixedpoint, false);

        for (BoolExpr b : z3rules) {
            System.out.println(fixedpoint);
            fixedpoint.addRule(b, null);
        }

        Status status = fixedpoint.query((BoolExpr) z3state.getZ3PredicateDeclaration(optPred.get()).apply(z3state.context.mkInt(2)));

        assertThat(status).isEqualTo(Status.UNSATISFIABLE);
    }
}
