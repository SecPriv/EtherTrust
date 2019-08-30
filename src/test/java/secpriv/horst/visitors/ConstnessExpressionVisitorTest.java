package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ConstnessExpressionVisitorTest {
    private ConstnessExpressionVisitor visitor;
    private VisitorState state;

    private Expression getExpressionFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optionalExpression = expressionVisitor.visit(parser.exp());
        assertThat(optionalExpression).isPresent();

        return optionalExpression.get();
    }

    @BeforeEach
    void setUp() {
        visitor = new ConstnessExpressionVisitor();
        state = new VisitorState();
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
    }

    @AfterEach
    void tearDown() {
        visitor = null;
        state = null;
    }

    @Test
    void testParameterConst() {
        state.defineParameterVar("!a", Type.Integer);
        Expression expression = getExpressionFromString("!a");
        assertThat(expression.accept(visitor)).isTrue();
    }

    @Test
    void testVariableNonConst() {
        state.defineVar("a", Type.Integer);
        Expression expression = getExpressionFromString("a");
        assertThat(expression.accept(visitor)).isFalse();
    }

    @Test
    void testMatchWithConstBranchesConst() {
        String s = "match 3 with | x => 2 | _ => 4";
        Expression expression = getExpressionFromString(s);
        assertThat(expression.accept(visitor)).isTrue();
    }

    @Test
    void testMatchWithVariablesInBranchesConst1() {
        String s = "match 3 with | x => x | _ => 4";
        Expression expression = getExpressionFromString(s);
        assertThat(expression.accept(visitor)).isTrue();
    }

    @Test
    void testMatchWithVariablesInBranchesConst2() {
        String s = "match 3 with | x => (match x with | y => y | _ => 0) | _ => 4";
        Expression expression = getExpressionFromString(s);
        assertThat(expression.accept(visitor)).isTrue();
    }

    @Test
    void testMatchWithVariablesInBranchesConst3() {
        String s = "match 3 with | x => (match x with | y => y+x | _ => x) | _ => 4";
        Expression expression = getExpressionFromString(s);
        assertThat(expression.accept(visitor)).isTrue();
    }

    @Test
    void testMatchWithVariablesInBranchesConst4() {
        String s = "match 3 with | x => (match x with | y => (match (x+y) with | z => x+y-z | _ => 2) | _ => x) | _ => 4";
        Expression expression = getExpressionFromString(s);
        assertThat(expression.accept(visitor)).isTrue();
    }

    @Test
    void testMatchWithNonConstInLastBranchNonConst() {
        state.defineVar("a", Type.Integer);
        String s = "match 3 with | 3 => 1 | _ => a";
        Expression expression = getExpressionFromString(s);
        assertThat(expression.accept(visitor)).isFalse();
    }
}