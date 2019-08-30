package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Proposition;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropositionVisitorTest {

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    private VisitorState state;
    private TestingErrorHandler testingHandler;

    @BeforeEach
    private void setUp() {
        state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
    }

    @AfterEach
    private void tearDown() {
        state = null;
    }

    @Test
    public void parseProposition1() {
        String p = "pred As{} : int;";
        String s = "As(123)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition.size()).isEqualTo(1);
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.predicate).isEqualTo(predicate));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.parameters.size()).isEqualTo(0));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.arguments.size()).isEqualTo(1));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.arguments.get(0)).isInstanceOf(Expression.IntConst.class));
    }

    @Test
    public void parseProposition2() {
        String p = "pred As{int*bool} : int;";
        String s = "As{456+!x,false}(?d)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Integer);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition.size()).isEqualTo(1);
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.predicate).isEqualTo(predicate));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.parameters.size()).isEqualTo(2));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.parameters.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.parameters.get(1).getType()).isEqualTo(Type.Boolean));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.arguments.size()).isEqualTo(1));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.arguments.get(0)).isInstanceOf(Expression.FreeVarExpression.class));
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, q -> assertThat(q.arguments.get(0).getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseProposition3() {
        String s = "?d > !x";


        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Integer);

        ASParser parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition).hasSize(1);
        assertThat(proposition.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, q -> assertThat(q.expression.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseProposition4() {
        String m = "g-macro #Hallo($x:int,$y:bool) := (?b && $y) ? ($x + 23 = 102020) : (?a + 3 > 8) free [?a : int, ?b : bool];";
        String s = "#Hallo(2,false)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).hasSize(1);
        assertThat(propositions.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseProposition5() {
        String m = "g-macro #Hallo($x:int,$y:bool) := ?b free [?a : int, ?b : bool];";
        String s = "#Hallo(?a,?b)";
        state.defineFreeVar("?a", Type.Integer);
        state.defineFreeVar("?b", Type.Boolean);
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).hasSize(1);
        assertThat(propositions.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression).isInstanceOfSatisfying(Expression.FreeVarExpression.class, fv -> assertThat(fv.name).isNotEqualTo("?b")));
    }

    @Test
    public void parseProposition6() {
        String m = "g-macro #Hallo($x:int,$y:bool) := ((?b && $y) ? ($x + 23) : (?a + 3)) < 0, ?b && ?a > $x free [?a : int, ?b : bool];";
        String s = "#Hallo(2,false)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).hasSize(2);
        assertThat(propositions.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(1)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseProposition7() {
        String m1 = "g-macro #HalloPrim := 123 != ((?b) ? (23) : (?a + 3)), ?a + 20 = 30 free [?a : int, ?b : bool];";
        String m2 = "g-macro #Hallo($x:int,$y:bool) := 23 < ((?b && $y) ? ($x + 23) : (?a + 3)), ?b && ?a > $x, #HalloPrim free [?a : int, ?b : bool];";
        String s = "#Hallo(2,false)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m1);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(m2);
        optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).hasSize(4);
        assertThat(propositions.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(1)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(2)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(3)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseProposition8() {
        Predicate pred = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(pred);
        String m1 = "g-macro #HalloPrim := Abc(123), 123 != ((?b) ? (23) : (?a + 3)), ?a + 20 = 30 free [?a : int, ?b : bool];";
        String m2 = "g-macro #Hallo($x:int,$y:bool) := 23 < ((?b && $y) ? ($x + 23) : (?a + 3)), ?b && ?a > $x, #HalloPrim free [?a : int, ?b : bool];";
        String s = "#Hallo(2,false)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m1);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(m2);
        optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).hasSize(5);
        assertThat(propositions.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(1)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(2)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, e -> assertThat(e.predicate).isEqualTo(pred));
        assertThat(propositions.get(3)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(4)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseProposition9() {
        Predicate pred = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(pred);
        String m1 = "g-macro #HalloPrim := Abc(123), 123 != ((?b) ? (23) : (?a + 3)), ?a + 20 + ?c = 30 free [?a : int, ?b : bool, ?c : int];";
        String m2 = "g-macro #Hallo($x:int,$y:bool) := 23 < ((?b && $y) ? ($x + 23) : (?a + 3)), (?b || ?c) && ?a > $x, #HalloPrim free [?a : int, ?b : bool, ?c : bool];";
        String s = "#Hallo(2*3+3832,false)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m1);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(m2);
        optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).hasSize(5);
        assertThat(propositions.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(1)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(2)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, e -> assertThat(e.predicate).isEqualTo(pred));
        assertThat(propositions.get(3)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
        assertThat(propositions.get(4)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, e -> assertThat(e.expression.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parsePropositionSelfReferentialMacro() {
        Predicate pred = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(pred);
        String m1 = "g-macro #Hallo := Abc(123), #Hallo;";
        String s = "#Hallo";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m1);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());
        testingHandler = (TestingErrorHandler)state.errorHandler;

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parsePropositionMacroLevelViolation() {
        Predicate pred = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(pred);
        String m1 = "g-macro #Hallo := Abc(123), #Hallo1;";
        String m2 = "g-macro #Hallo1 := Abc(123);";
        String s = "#Hallo";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m1);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();
        state.defineMacro(optMacro.get());
        testingHandler = (TestingErrorHandler)state.errorHandler;

        parser = getParserFromString(m2);
        optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();
        state.defineMacro(optMacro.get());

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parsePropositionMacroArgTypeMismatch1() {
        String m = "g-macro #Hallo($x:int,$y:bool) := ((?b && $y) ? ($x + 23) : (?a + 3)) < 0, ?b && ?a > $x free [?a : int, ?b : bool];";
        String s = "#Hallo(2,3)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());
        testingHandler = (TestingErrorHandler)state.errorHandler;

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionMacroArgTypeMismatch2() {
        String m = "g-macro #Hallo($x:int,$y:bool) := ((?b && $y) ? ($x + 23) : (?a + 3)) < 0, ?b && ?a > $x free [?a : int, ?b : bool];";
        String s = "#Hallo(true,true)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());
        testingHandler = (TestingErrorHandler)state.errorHandler;

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionMacroLengthMismatch1() {
        String m = "g-macro #Hallo($x:int,$y:bool) := ((?b && $y) ? ($x + 23) : (?a + 3)) < 0, ?b && ?a > $x free [?a : int, ?b : bool];";
        String s = "#Hallo(3,true, 3)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());
        testingHandler = (TestingErrorHandler)state.errorHandler;

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionMacroLengthMismatch2() {
        String m = "g-macro #Hallo($x:int,$y:bool) := ((?b && $y) ? ($x + 23) : (?a + 3)) < 0, ?b && ?a > $x free [?a : int, ?b : bool];";
        String s = "#Hallo(3)";
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);
        ASParser parser = getParserFromString(m);
        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());
        testingHandler = (TestingErrorHandler)state.errorHandler;

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositions = propositionVisitor.visit(parser.prop());

        assertThat(propositions).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionParameterTypeMismatch() {
        String p = "pred As{int*bool} : int;";
        String s = "As{456+!x > 0,false}(?d)";

        ASParser parser = getParserFromString(p);

        testingHandler = (TestingErrorHandler)state.errorHandler;
        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Integer);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionArgumentTypeMismatch() {
        String p = "pred As{int*bool} : int;";
        String s = "As{456+!x,false}(?d)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;

        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Boolean);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionParameterLengthMismatch1() {
        String p = "pred As{int} : int;";
        String s = "As{456+!x,false}(?d)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;

        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Integer);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionParameterLengthMismatch2() {
        String p = "pred As{int*bool*bool} : int;";
        String s = "As{456+!x,false}(?d)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;

        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Integer);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionArgumentLengthMismatch1() {
        String p = "pred As{int*bool} : int;";
        String s = "As{456+!x,false}(?d, 3)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;

        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Integer);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionArgumentLengthMismatch2() {
        String p = "pred As{int*bool} : int*int;";
        String s = "As{456+!x,false}(?d)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;

        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Integer);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parsePropositionParameterNotConst() {
        String p = "pred As{int*bool} : int;";
        String s = "As{?d+!x,false}(?d)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;

        state.defineParameterVar("!x", Type.Integer);
        state.defineFreeVar("?d", Type.Integer);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> proposition = propositionVisitor.visit(parser.prop());

        assertThat(proposition).isEmpty();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.ElementNotConst.class);
    }
}