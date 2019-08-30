package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Clause;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Proposition;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClauseVisitorTest {

    private VisitorState state;
    private Predicate predicate;
    private TestingErrorHandler testingHandler;

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @BeforeEach
    void setUp() {
        state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;
    }

    @AfterEach
    void tearDown() {
        state = null;
        predicate = null;
    }

    @Test
    void parseClause1() {
        predicate = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(predicate);
        String s = "clause 1 != 3 => Abc(3)";

        ASParser parser = getParserFromString(s);
        ClauseVisitor clauseVisitor = new ClauseVisitor(state);


        Optional<Clause> optClause = clauseVisitor.visit(parser.clauseDef());

        assertThat(optClause).isPresent();
        assertThat(optClause.get().premises).hasSize(1);
        assertThat(optClause.get().conclusion.predicate).isEqualTo(predicate);
        assertThat(optClause.get().freeVars).isEmpty();
    }

    @Test
    void parseClause2() {
        predicate = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(predicate);
        String s = "clause [?b : int, ?a : bool, ?c : int] ?a, 1 != ?c, ?b = ?c => Abc(?b) ";

        ASParser parser = getParserFromString(s);
        ClauseVisitor clauseVisitor = new ClauseVisitor(state);


        Optional<Clause> optClause = clauseVisitor.visit(parser.clauseDef());

        assertThat(optClause).isPresent();
        assertThat(optClause.get().premises).hasSize(3);
        assertThat(optClause.get().conclusion.predicate).isEqualTo(predicate);
        assertThat(optClause.get().freeVars).hasSize(3);
        assertThat(optClause.get().freeVars).hasEntrySatisfying("?a", t-> assertThat(t).isEqualTo(Type.Boolean));
        assertThat(optClause.get().freeVars).hasEntrySatisfying("?b", t-> assertThat(t).isEqualTo(Type.Integer));
        assertThat(optClause.get().freeVars).hasEntrySatisfying("?c", t-> assertThat(t).isEqualTo(Type.Integer));
    }

    @Test
    void parseClauseUndeclaredFreeVar() {
        predicate = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(predicate);
        String s = "clause [?b : int, ?a : bool] ?a, 1 != ?c, ?b = ?c => Abc(?b) ";

        ASParser parser = getParserFromString(s);
        ClauseVisitor clauseVisitor = new ClauseVisitor(state);


        Optional<Clause> optClause = clauseVisitor.visit(parser.clauseDef());

        assertThat(optClause).isNotPresent();
        assertEquals(3, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
        assertThat(testingHandler.errorObjects.get(1)).isInstanceOf(Error.UndefinedValue.class);
        assertThat(testingHandler.errorObjects.get(2)).isInstanceOf(Error.InvalidPremises.class);
    }

    @Test
    void parseClausePredicateDoubleFreeVars() {
        predicate = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(predicate);
        String s = "clause [?b : int, ?a : bool, ?c :int, ?c : int] ?a, 1 != ?c => Abc(?b) ";

        ASParser parser = getParserFromString(s);
        ClauseVisitor clauseVisitor = new ClauseVisitor(state);

        Optional<Clause> optClause = clauseVisitor.visit(parser.clauseDef());

        assertThat(optClause).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.SizeDoesntMatch.class);
    }

    @Test
    void parseClausePredicateFreeVarTypeMismatch() {
        predicate = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(predicate);
        String s = "clause [?b : bool, ?a : bool, ?c :int] ?a, 1 != ?c => Abc(?b) ";

        ASParser parser = getParserFromString(s);
        ClauseVisitor clauseVisitor = new ClauseVisitor(state);

        Optional<Clause> optClause = clauseVisitor.visit(parser.clauseDef());

        assertThat(optClause).isNotPresent();
        assertEquals(2, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
        assertThat(testingHandler.errorObjects.get(1)).isInstanceOf(Error.SizeDoesntMatch.class);

    }

    @Test
    void parseClauseWithMacros() {
        predicate = new Predicate("Abc", Collections.emptyList(), Collections.singletonList(Type.Integer));
        state.definePredicate(predicate);
        String m = "g-macro #Hallo($y : bool, $x : int) := Abc(($y) ? (2) : (3)), Abc(?a), Abc($x),?b,$y free [?b:bool, ?a :int];";
        String s = "clause [?b : bool, ?a : int, ?c :int] ?a > 1, 1 != ?c, #Hallo(?b,?a) => Abc(?a) ";

        ASParser parser = getParserFromString(m);
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(state);

        Optional<VisitorState.MacroDefinition> optMacro = macroDefinitionVisitor.visit(parser.globalMacroDefinition());

        assertThat(optMacro).isPresent();

        state.defineMacro(optMacro.get());

        parser = getParserFromString(s);
        ClauseVisitor clauseVisitor = new ClauseVisitor(state);

        Optional<Clause> optClause = clauseVisitor.visit(parser.clauseDef());

        assertThat(optClause).isPresent();
        assertThat(optClause.get().premises).hasSize(7);
        assertThat(optClause.get().premises.get(6)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, p -> assertThat(p.expression).isInstanceOfSatisfying(Expression.FreeVarExpression.class, f -> assertThat(f.name).isEqualTo("?b")));
        assertThat(optClause.get().premises.get(5)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, p -> assertThat(p.expression).isInstanceOfSatisfying(Expression.FreeVarExpression.class, f -> assertThat(f.name).isNotEqualTo("?b")));
    }
}