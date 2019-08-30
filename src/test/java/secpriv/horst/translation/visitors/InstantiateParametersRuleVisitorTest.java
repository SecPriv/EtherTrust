package secpriv.horst.translation.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.Rule;
import secpriv.horst.data.SelectorFunction;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.SelectorFunctionInvoker;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.TestBuilder;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.RuleVisitor;
import secpriv.horst.visitors.SExpressionRuleVisitor;
import secpriv.horst.visitors.VisitorState;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class InstantiateParametersRuleVisitorTest {
    private SelectorFunctionHelper selectorFunctionHelper = null;
    private VisitorState state = null;
    private RuleVisitor ruleVisitor = null;
    private TestBuilder testBuilder;


    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ASParser(tokens);
    }

    private Optional<Rule> getRuleFromString(String s) {
        return ruleVisitor.visitRuleDefinition(getParserFromString(s).ruleDefinition());
    }

    public class Provider {
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
    }

    @BeforeEach
    void setUp() {
        selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());

        state = new VisitorState();
        testBuilder = new TestBuilder(state);

        testBuilder.setSelectorFunctionHelper(selectorFunctionHelper);

        testBuilder.defineSelectorFunction("sel oneToFive: unit -> [int];");
        testBuilder.defineSelectorFunction("sel allBools: unit -> [bool];");
        testBuilder.defineSelectorFunction("sel intPlusParity: int -> [int*bool];");

        ruleVisitor = new RuleVisitor(state);
    }

    @AfterEach
    void tearDown() {
        state = null;
        ruleVisitor = null;
        selectorFunctionHelper = null;
    }

    @Test
    public void testUnitFunctionUnroll() {
        String p = "pred FunnyFun{} : int;";

        String s = "rule unitRule :=   \n" +
                "clause [?i : int]  \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(1);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }

    @Test
    public void testIntFunctionUnroll() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!a:int) in oneToFive()\n" +
                "clause [?i : int]  \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(5);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }

    @Test
    public void testBoolFunctionUnroll() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!a:bool) in allBools()\n" +
                "clause [?i : int]  \n" +
                "!a,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(2);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }

    @Test
    public void testTupleFunctionUnroll1() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!i:int, !b:bool) in intPlusParity(8)\n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(8);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }

    @Test
    public void testTupleFunctionUnroll2() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!i:int, !b:bool) in intPlusParity(12)\n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(12);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }

    @Test
    public void testTupleFunctionUnrollWithCompoundFunction1() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!k:int) in oneToFive(), (!i:int, !b:bool) in intPlusParity(12) \n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(60);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }

    @Test
    public void testTupleFunctionUnrollWithCompoundFunction2() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!k:int) in oneToFive(), (!i:int, !b:bool) in intPlusParity(!k) \n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(15);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }


    @Test
    public void testTupleFunctionUnrollWithCompoundFunction3() {
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!k:int) in oneToFive(), (!i:int, !b:bool) in intPlusParity(!k*2) \n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(30);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }

    @Test
    public void testTupleFunctionUnrollWithCompoundFunctionUndefinedParameter() {
        TestingErrorHandler errorHandler = new TestingErrorHandler();
        state.errorHandler = errorHandler;
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!i:int, !b:bool) in intPlusParity(!k),(!k:int) in oneToFive() \n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);

        Optional<Rule> optRule = getRuleFromString(s);
        assertThat(optRule).isNotPresent();
        assertThat(errorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void testTupleFunctionUnrollWithCompoundFunctionDuplicateParameter() {
        TestingErrorHandler errorHandler = new TestingErrorHandler();
        state.errorHandler = errorHandler;
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!i:int, !b:bool) in intPlusParity(10),(!i:int) in oneToFive() \n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";


        testBuilder.definePredicate(p);
        Optional<Rule> optRule = getRuleFromString(s);
        assertThat(optRule).isNotPresent();
        assertThat(errorHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }

    @Test
    public void testTupleFunctionUnrollWithWrongType() {
        TestingErrorHandler errorHandler = new TestingErrorHandler();
        state.errorHandler = errorHandler;
        String p = "pred FunnyFun{}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!k:int) in oneToFive(), (!i:int, !b:bool) in intPlusParity(!k > 0) \n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Optional<Rule> optRule = getRuleFromString(s);
        assertThat(optRule).isNotPresent();

        assertThat(errorHandler.errorObjects).hasSize(1);
        assertThat(errorHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void testTupleFunctionUnrollIdempotent() {
        String p = "pred FunnyFun{int*int}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!i:int, !b:bool) in intPlusParity(12)\n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun{!i,!i*2}(?i)       \n" +
                "=> FunnyFun{!i,!i*3}(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);
        List<Rule> instantiatedRules1 = instantiatedRules.stream().flatMap(r -> r.accept(instantiateParametersRuleVisitor).stream()).collect(Collectors.toList());

        SExpressionRuleVisitor sExpressionRuleVisitor = new SExpressionRuleVisitor();

        assertThat(instantiatedRules).hasSize(12);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });

        assertThat(instantiatedRules1).hasSize(12);
        assertThat(instantiatedRules1).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });

        String t1 = String.join("", instantiatedRules.stream().map(r -> r.accept(sExpressionRuleVisitor)).collect(Collectors.toList()));
        String t2 = String.join("", instantiatedRules1.stream().map(r -> r.accept(sExpressionRuleVisitor)).collect(Collectors.toList()));


        assertThat(t1).isEqualTo(t2);
    }

    @Test
    public void testTupleFunctionUnrollWithCompoundFunctionIdempotent() {
        String p = "pred FunnyFun{int*int}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!i:int, !b:bool) in intPlusParity(12), (!p:bool) in allBools()\n" +
                "clause [?i : int]  \n" +
                "!i > 14,           \n" +
                "!b,                \n" +
                "FunnyFun{!i,!i*2}(?i)       \n" +
                "=> FunnyFun{!i,!i*3}(?i+1)  \n" +
                ";";


        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);
        List<Rule> instantiatedRules1 = instantiatedRules.stream().flatMap(r -> r.accept(instantiateParametersRuleVisitor).stream()).collect(Collectors.toList());

        SExpressionRuleVisitor sExpressionRuleVisitor = new SExpressionRuleVisitor();

        assertThat(instantiatedRules).hasSize(24);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });

        assertThat(instantiatedRules1).hasSize(24);
        assertThat(instantiatedRules1).allSatisfy(r -> {
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });

        String t1 = String.join("", instantiatedRules.stream().map(r -> r.accept(sExpressionRuleVisitor)).collect(Collectors.toList()));
        String t2 = String.join("", instantiatedRules1.stream().map(r -> r.accept(sExpressionRuleVisitor)).collect(Collectors.toList()));


        assertThat(t1).isEqualTo(t2);
    }

    @Test
    public void testSumExpressionInPremiseParameter() {
        String p = "pred FunnyFun{int}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!a:int) in oneToFive()\n" +
                "clause [?i : int]  \n" +
                "FunnyFun{for (!b:int) in oneToFive(): + !b}(?i)       \n" +
                "=> FunnyFun{2}(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(5);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.clauses.get(0).premises.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, pp -> assertThat(pp.predicate.name).endsWith("_15"));
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }

    @Test
    public void testSumExpressionInConclusionParameter() {
        String p = "pred FunnyFun{int}: int;";

        String s = "rule unitRule :=   \n" +
                "for (!a:int) in oneToFive()\n" +
                "clause [?i : int]  \n" +
                "FunnyFun{2}(?i)       \n" +
                "=> FunnyFun{17 + for (!k:int, !b:bool) in intPlusParity(!a): * (!k)}(?i+1)  \n" +
                ";";

        testBuilder.definePredicate(p);
        Rule rule = testBuilder.defineRule(s);

        InstantiateParametersRuleVisitor instantiateParametersRuleVisitor = new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper));
        List<Rule> instantiatedRules = instantiateParametersRuleVisitor.visit(rule);

        assertThat(instantiatedRules).hasSize(5);
        assertThat(instantiatedRules).allSatisfy(r -> {
            assertThat(r.clauses.get(0).conclusion.predicate.name).endsWith("_17");
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(1);
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
            assertThat(r.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        });
    }
}