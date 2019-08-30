package secpriv.horst.translation.layout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.internals.error.handling.ExceptionThrowingErrorHandler;
import secpriv.horst.tools.TestBuilder;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.VisitorState;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class FlatTypeLayouterTest {
    private static final Expression.IntConst INT_CONST_0 = new Expression.IntConst(BigInteger.ZERO);
    private static final Expression.IntConst INT_CONST_1 = new Expression.IntConst(BigInteger.ONE);
    private static final Expression.IntConst INT_CONST_2 = new Expression.IntConst(BigInteger.valueOf(2));

    private TestBuilder testBuilder;
    private FlatTypeLayouter flatTypeLayouter;
    private Type.CustomType absDomType;
    private Type.CustomType absDomTupleType;
    private Type.CustomType absDomStateType;

    @BeforeEach
    public void setUp() {
        VisitorState state = new VisitorState();
        state.errorHandler = new ExceptionThrowingErrorHandler();
        testBuilder = new TestBuilder(state);
        flatTypeLayouter = new FlatTypeLayouter();

        absDomType = testBuilder.defineType("eqtype AbsDom := @T | @V<bool>;");
        absDomTupleType = testBuilder.defineType("eqtype AbsDomTuple := @Empty | @VI<AbsDom> | @VII<AbsDom*AbsDom>;");
        absDomStateType = testBuilder.defineType("datatype AbsDomState := @State<int*array<AbsDom>*bool>;");
    }

    @AfterEach
    public void tearDown() {
        testBuilder = null;
        flatTypeLayouter = null;
    }

    @Test
    public void testUnfoldToBaseTypes1() {
        assertThat(flatTypeLayouter.unfoldToBaseTypes(Type.Integer)).containsExactly(Type.Integer);
    }

    @Test
    public void testUnfoldToBaseTypes2() {
        assertThat(flatTypeLayouter.unfoldToBaseTypes(Type.Boolean)).containsExactly(Type.Boolean);
    }

    @Test
    public void testUnfoldToBaseTypesAbsDomType() {
        assertThat(flatTypeLayouter.unfoldToBaseTypes(absDomType)).containsExactly(Type.Integer, Type.Boolean);
    }

    @Test
    public void testLayOutExpressionAbsDomType1() {
        Expression.ConstructorAppExpression expression = (Expression.ConstructorAppExpression) testBuilder.parseExpression("@T");
        assertThat(flatTypeLayouter.layoutExpression(expression, Collections.emptyList()))
                .containsExactly(INT_CONST_0, Expression.BoolConst.FALSE);
    }

    @Test
    public void testLayOutExpressionAbsDomType2() {
        Expression.ConstructorAppExpression expression = (Expression.ConstructorAppExpression) testBuilder.parseExpression("@V(true)");

        assertThat(flatTypeLayouter.layoutExpression(expression,
                Collections.singletonList(Collections.singletonList(Expression.BoolConst.TRUE))))
                .containsExactly(INT_CONST_1, Expression.BoolConst.TRUE);
    }

    @Test
    public void testLayOutExpressionAbsDomType3() {
        Expression.ConstructorAppExpression expression = (Expression.ConstructorAppExpression) testBuilder.parseExpression("@V(false)");

        assertThat(flatTypeLayouter.layoutExpression(expression,
                Collections.singletonList(Collections.singletonList(Expression.BoolConst.TRUE))))
                .containsExactly(INT_CONST_1, Expression.BoolConst.TRUE);
    }

    @Test
    public void testGetSelectExpressionAbsDomType1() {
        Expression expression = flatTypeLayouter.getSelectExpression(absDomType, absDomType.constructors.get(0), Collections.singletonList(INT_CONST_0));

        assertThat(expression).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isEqualTo(INT_CONST_0);
            assertThat(c.expression2).isEqualTo(INT_CONST_0);
        });
    }

    @Test
    public void testGetSelectExpressionAbsDomType2() {
        Expression expression = flatTypeLayouter.getSelectExpression(absDomType, absDomType.constructors.get(1), Collections.singletonList(INT_CONST_0));

        assertThat(expression).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isEqualTo(INT_CONST_1);
            assertThat(c.expression2).isEqualTo(INT_CONST_0);
        });
    }

    @Test
    public void testUnfoldToBaseTypesAbsDomTupleType() {
        assertThat(flatTypeLayouter.unfoldToBaseTypes(absDomTupleType)).containsExactly(Type.Integer, Type.Integer, Type.Boolean, Type.Integer, Type.Boolean, Type.Integer, Type.Boolean);
    }

    @Test
    public void testLayOutExpressionAbsDomTupleType1() {
        Expression.ConstructorAppExpression expression = (Expression.ConstructorAppExpression) testBuilder.parseExpression("@Empty");

        assertThat(flatTypeLayouter.layoutExpression(expression,
                Collections.emptyList()))
                .containsExactly(INT_CONST_0, INT_CONST_0, Expression.BoolConst.FALSE, INT_CONST_0, Expression.BoolConst.FALSE, INT_CONST_0, Expression.BoolConst.FALSE);
    }

    @Test
    public void testLayOutExpressionAbsDomTupleType2() {
        Expression.ConstructorAppExpression expression = (Expression.ConstructorAppExpression) testBuilder.parseExpression("@VI(@V(true))");

        assertThat(flatTypeLayouter.layoutExpression(expression,
                Collections.singletonList(Arrays.asList(INT_CONST_1, Expression.BoolConst.TRUE))))
                .containsExactly(INT_CONST_1, INT_CONST_1, Expression.BoolConst.TRUE, INT_CONST_0, Expression.BoolConst.FALSE, INT_CONST_0, Expression.BoolConst.FALSE);
    }

    @Test
    public void testLayOutExpressionAbsDomTupleType3() {
        Expression.ConstructorAppExpression expression = (Expression.ConstructorAppExpression) testBuilder.parseExpression("@VII(@V(true),@V(true))");

        assertThat(flatTypeLayouter.layoutExpression(expression,
                Arrays.asList(Arrays.asList(INT_CONST_1, Expression.BoolConst.TRUE), Arrays.asList(INT_CONST_1, Expression.BoolConst.TRUE))))
                .containsExactly(INT_CONST_2, INT_CONST_0, Expression.BoolConst.FALSE, INT_CONST_1, Expression.BoolConst.TRUE, INT_CONST_1, Expression.BoolConst.TRUE);
    }

    @Test
    public void testGetSelectExpressionAbsDomTupleType1() {
        Expression expression = flatTypeLayouter.getSelectExpression(absDomTupleType, absDomTupleType.constructors.get(0), Collections.singletonList(INT_CONST_0));

        assertThat(expression).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isEqualTo(INT_CONST_0);
            assertThat(c.expression2).isEqualTo(INT_CONST_0);
        });
    }

    @Test
    public void testGetSelectExpressionAbsDomTupleType2() {
        Expression expression = flatTypeLayouter.getSelectExpression(absDomTupleType, absDomTupleType.constructors.get(1), Collections.singletonList(INT_CONST_0));

        assertThat(expression).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isEqualTo(INT_CONST_1);
            assertThat(c.expression2).isEqualTo(INT_CONST_0);
        });
    }

    @Test
    public void testGetSelectExpressionAbsDomTupleType3() {
        Expression expression = flatTypeLayouter.getSelectExpression(absDomTupleType, absDomTupleType.constructors.get(2), Collections.singletonList(INT_CONST_0));

        assertThat(expression).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isEqualTo(INT_CONST_2);
            assertThat(c.expression2).isEqualTo(INT_CONST_0);
        });
    }

    @Test
    public void testUnfoldToBaseTypesState() {
        //TODO check if                                                                v-v-v-this-v-v guy should be optimized away (OTOH: constant folding should handle this case anyway)
        assertThat(flatTypeLayouter.unfoldToBaseTypes(absDomStateType)).containsExactly(Type.Integer, Type.Integer, Type.Array.of(Type.Integer), Type.Array.of(Type.Boolean), Type.Boolean);
    }
}