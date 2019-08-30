package secpriv.horst.translation.visitors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.types.Type;

import static org.assertj.core.api.Assertions.assertThat;

class TranslateToSmtLibVisitorStateTest {
    private TranslateToSmtLibVisitorState state;

    @BeforeEach
    public void setUp() {
        state = new TranslateToSmtLibVisitorState();
        state.newScope();
    }

    @Test
    public void testProgress() {
        String a = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));
        String b = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "b"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    public void sameVariableReassignment1() {
        String a = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));
        String b = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));

        assertThat(a).isEqualTo(b);
    }

    @Test
    public void sameVariableReassignment2() {
        String a = state.getFreeVar(new Expression.FreeVarExpression(Type.Array.of(Type.Integer), "a"));
        String b = state.getFreeVar(new Expression.FreeVarExpression(Type.Array.of(Type.Integer), "a"));

        assertThat(a).isEqualTo(b);
    }

    @Test
    public void testProgressDifferentTypes() {
        String a = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));
        String b = state.getFreeVar(new Expression.FreeVarExpression(Type.Boolean, "b"));
        String c = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "c"));
        String d = state.getFreeVar(new Expression.FreeVarExpression(Type.Boolean, "d"));

        assertThat(a).isNotIn(b,c,d);
        assertThat(b).isNotIn(a,c,d);
        assertThat(c).isNotIn(a,b,d);
        assertThat(d).isNotIn(a,b,c);
    }

    @Test
    public void testVariableReuse1() {
        String a = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));
        String b = state.getFreeVar(new Expression.FreeVarExpression(Type.Boolean, "b"));
        state.newScope();
        String c = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "c"));
        String d = state.getFreeVar(new Expression.FreeVarExpression(Type.Boolean, "d"));

        assertThat(a).isEqualTo(c);
        assertThat(b).isEqualTo(d);
    }

    @Test
    public void testVariableReuse2() {
        String a = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));
        String b = state.getFreeVar(new Expression.FreeVarExpression(Type.Boolean, "b"));
        state.newScope();
        String c = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));
        String d = state.getFreeVar(new Expression.FreeVarExpression(Type.Boolean, "b"));

        assertThat(a).isEqualTo(c);
        assertThat(b).isEqualTo(d);
    }

    @Test
    public void testVariableReuse3() {
        String a = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));
        String b = state.getFreeVar(new Expression.FreeVarExpression(Type.Boolean, "b"));
        state.newScope();
        String d = state.getFreeVar(new Expression.FreeVarExpression(Type.Boolean, "b"));
        String c = state.getFreeVar(new Expression.FreeVarExpression(Type.Integer, "a"));

        assertThat(a).isEqualTo(c);
        assertThat(b).isEqualTo(d);
    }
}