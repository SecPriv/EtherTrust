package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Rule;
import secpriv.horst.data.SelectorFunction;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.error.handling.ExceptionThrowingErrorHandler;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class RuleVisitorTest {
    private VisitorState state;
    private TestingErrorHandler testingHandler;
    private Predicate predicateAbcInt;
    private SelectorFunction selectorFunctionIndex;
    private SelectorFunction selectorFunctionSet;


    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @BeforeEach
    void setUp() {
        testingHandler = new TestingErrorHandler();
        state = new VisitorState(testingHandler);
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        predicateAbcInt = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(predicateAbcInt);
        selectorFunctionIndex = new SelectorFunction("index", Arrays.asList(Type.Integer, Type.Integer), Collections.singletonList(Type.Integer));
        selectorFunctionSet = new SelectorFunction("set", Collections.emptyList(), Collections.singletonList(Type.Integer));
        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Object() {
            public Iterable<BigInteger> index(BigInteger a, BigInteger b) {
                return null;
            }

            public Iterable<BigInteger> set() {
                return null;
            }

            public Iterable<Tuple2<BigInteger,BigInteger>> zipInts(BigInteger a, BigInteger b) {
                return null;
            }
        });
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        state.defineSelectorFunction(selectorFunctionIndex);
        state.defineSelectorFunction(selectorFunctionSet);
    }

    @AfterEach
    void tearDown() {
        state = null;
        predicateAbcInt = null;
        selectorFunctionIndex = null;
        selectorFunctionSet = null;
    }

    @Test
    public void parseRule1() {
        String s = "rule rule1 := clause 1 != 2, Abc(34) => Abc(345);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);

        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isPresent();
        assertThat(optRule.get().name).isEqualTo("rule1");
        assertThat(optRule.get().clauses).hasSize(1);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();

    }

    @Test
    public void parseRule2() {
        String s = "rule rule1 := clause [?a:int] 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);

        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isPresent();
        assertThat(optRule.get().name).isEqualTo("rule1");
        assertThat(optRule.get().clauses).hasSize(2);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).isEmpty();
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(SelectorFunction.Unit);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).isEmpty();
    }

    @Test
    public void parseRule3() {
        String s = "rule rule1 := for (!a:int) in index(123,423) clause [?a:int] 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);


        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isPresent();
        assertThat(optRule.get().name).isEqualTo("rule1");
        assertThat(optRule.get().clauses).hasSize(2);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).hasSize(1);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(selectorFunctionIndex);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).hasSize(2);
    }

    @Test
    public void parseRule4() {
        String s = "rule rule1 := for (!a:int) in index(123,423) let macro #Hallo := Abc(23), Abc(321), macro #Hallo1($a:int) := #Hallo, Abc($a) in clause [?a:int] #Hallo1(?a), 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);


        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isPresent();
        assertThat(optRule.get().name).isEqualTo("rule1");
        assertThat(optRule.get().clauses).hasSize(2);
        assertThat(optRule.get().clauses.get(0).premises).hasSize(5);
        assertThat(optRule.get().clauses.get(1).premises).hasSize(2);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).hasSize(1);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(selectorFunctionIndex);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).hasSize(2);
    }

    @Test
    public void parseRule5() {
        String s = "rule rule1 := for (!a:int) in set() clause [?a:int] 1 != 2, Abc(!a) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);


        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isPresent();
        assertThat(optRule.get().name).isEqualTo("rule1");
        assertThat(optRule.get().clauses).hasSize(2);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters).hasSize(1);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(selectorFunctionSet);
        assertThat(optRule.get().selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments).hasSize(0);
    }

    @Test
    public void parseRuleUndefinedSelectorFunction() {
        String s = "rule rule1 := for (!a:int) in index1(123,423) let macro #Hallo := Abc(23), Abc(321), macro #Hallo1($a:int) := #Hallo, Abc($a) in clause [?a:int] #Hallo1(?a), 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);

        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);

    }

    @Test
    public void parseRuleSelectorFunctionReturnTypeMismatch() {
        String s = "rule rule1 := for (!a:bool) in index(123,423) let macro #Hallo := Abc(23), Abc(321), macro #Hallo1($a:int) := #Hallo, Abc($a) in clause [?a:int] #Hallo1(?a), 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);

        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parseRuleSelectorFunctionArgumentNonConst() {
        String s = "rule rule1 := for (!a:int) in index(123,b) let macro #Hallo := Abc(23), Abc(321), macro #Hallo1($a:int) := #Hallo, Abc($a) in clause [?a:int] #Hallo1(?a), 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        state.defineVar("b", Type.Integer);
        RuleVisitor ruleVisitor = new RuleVisitor(state);

        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.ElementNotConst.class);
    }

    @Test
    public void parseRuleSelectorFunctionArgumentTypeMismatch() {
        String s = "rule rule1 := for (!a:int) in index(123,false) let macro #Hallo := Abc(23), Abc(321), macro #Hallo1($a:int) := #Hallo, Abc($a) in clause [?a:int] #Hallo1(?a), 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);

        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parseRuleSelectorFunctionWithNonUniqueParameters() {
        state.defineSelectorFunction(new SelectorFunction("zipInts", Arrays.asList(Type.Integer, Type.Integer), Arrays.asList(Type.Integer, Type.Integer)));
        String s = "rule rule1 := for (!a:int, !a:int) in zipInts(123,234) let macro #Hallo := Abc(23), Abc(321), macro #Hallo1($a:int) := #Hallo, Abc($a) in clause [?a:int] #Hallo1(?a), 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);";
        ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);

        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }

    @Test
    public void parseRuleSelectorFunctionNonUniqueMacroDefinitions() {
        String s = "rule rule1 := for (!a:int) in index(123,423) let macro #Hallo := Abc(999), macro #Hallo := Abc(23), Abc(321), macro #Hallo1($a:int) := #Hallo, Abc($a) in clause [?a:int] #Hallo1(?a), 1 != 2, Abc(34) => Abc(?a), clause [?a:bool] 2 = 2,?a => Abc(21);"; ASParser parser = getParserFromString(s);
        RuleVisitor ruleVisitor = new RuleVisitor(state);

        Optional<Rule> optRule = ruleVisitor.visit(parser.ruleDefinition());

        assertThat(optRule).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }

    @Test
    public void parseInit() {
        String s = "init clause 1 != 2 => Abc(345);";
        RuleVisitor ruleVisitor = new RuleVisitor(state);
        ASParser parser = getParserFromString(s);

        Optional<Rule> optRule = ruleVisitor.visitInitialization ("init0", parser.initialization());

        assertThat(optRule).isPresent();
        assertThat(optRule.get().clauses).hasSize(1);
        assertThat(optRule.get().name).isEqualTo("init0");
    }

    @Test
    public void parseInitWithPremiseFails() {
        String s = "init clause 1 != 2, Abc(34) => Abc(345);";
        RuleVisitor ruleVisitor = new RuleVisitor(state);
        ASParser parser = getParserFromString(s);

        Optional<Rule> optRule = ruleVisitor.visitInitialization ("init0", parser.initialization());

        assertThat(optRule).isNotPresent();
        assertThat(testingHandler.errorObjects).hasSize(1);
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.InitRuleContainsPredicateProposition.class);
    }
}