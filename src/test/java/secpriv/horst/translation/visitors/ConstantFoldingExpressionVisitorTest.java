package secpriv.horst.translation.visitors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.*;
import secpriv.horst.data.Expression.*;
import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;

import java.math.BigInteger;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConstantFoldingExpressionVisitorTest {
    private ConstantFoldingExpressionVisitor expressionVisitor;

    @BeforeEach
    public void setUp() {
        expressionVisitor = new ConstantFoldingExpressionVisitor();
    }

    @AfterEach
    public void tearDown() {
        expressionVisitor = null;
    }

    @Test
    public void testIntConstExpressionReturnsUnmodified() {
        IntConst expression = new IntConst(BigInteger.valueOf(100));

        Expression visited = expressionVisitor.visit(expression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.valueOf(100)));
    }

    @Test
    public void testBoolConstExpressionReturnsUnmodified() {
        BoolConst expression = BoolConst.TRUE;

        Expression visited = expressionVisitor.visit(expression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testArrayInitExpressionReturnsUnmodified() {
        Expression initializer = new IntConst(BigInteger.ONE);
        ArrayInitExpression expression = new ArrayInitExpression(initializer);

        Expression visited = expressionVisitor.visit(expression);

        assertThat(visited).isInstanceOfSatisfying(ArrayInitExpression.class, e -> assertThat(e.initializer).isEqualTo(initializer));
    }

    @Test
    public void testVarExpressionThrowsUnsupportedException() {
        VarExpression varExpression = new VarExpression(Type.Integer, "a");

        assertThatThrownBy(() -> expressionVisitor.visit(varExpression)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testFreeVarExpressionReturnUnmodified() {
        FreeVarExpression freeVarExpression = new FreeVarExpression(Type.Integer, "?a");

        Expression visited = expressionVisitor.visit(freeVarExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.type).isEqualTo(Type.Integer));
        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testParVarExpressionThrowsUnsupportedException() {
        ParVarExpression parVarExpression = new ParVarExpression(Type.Integer, "!a");

        assertThatThrownBy(() -> expressionVisitor.visit(parVarExpression)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testBinaryIntExpressionReturnFolded1() {
        Expression child1 = new IntConst(BigInteger.TEN);
        Expression child2 = new IntConst(BigInteger.ONE);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.ADD);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.valueOf(11)));
    }

    @Test
    public void testBinaryIntExpressionReturnFolded2() {
        Expression child1 = new BinaryIntExpression(new IntConst(BigInteger.valueOf(5)), new IntConst(BigInteger.valueOf(7)), IntOperation.MUL);
        Expression child2 = new IntConst(BigInteger.TEN);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.ADD);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.valueOf(45)));
    }

    @Test
    public void testBinaryIntExpressionReturnFolded3() {
        Expression grandChild1 = new BinaryIntExpression(new IntConst(BigInteger.valueOf(4)), new IntConst(BigInteger.valueOf(4)), IntOperation.ADD); // 4+4=8
        Expression child1 = new BinaryIntExpression(grandChild1, new IntConst(BigInteger.valueOf(7)), IntOperation.MUL); // 8*7=56

        Expression grandChild2 = new BinaryIntExpression(new IntConst(BigInteger.valueOf(24)), new IntConst(BigInteger.valueOf(6)), IntOperation.DIV); // 24/6=4
        Expression grandChild3 = new BinaryIntExpression(new IntConst(BigInteger.valueOf(2)), new IntConst(BigInteger.ONE), IntOperation.SUB); // 2-1=1
        Expression child2 = new BinaryIntExpression(grandChild2, grandChild3, IntOperation.ADD); // 4+1=5

        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.ADD);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.valueOf(61)));
    }

    @Test
    public void testBinaryIntExpressionReturnFolded4() {
        Expression grandChild1 = new BinaryIntExpression(new FreeVarExpression(Type.Integer, "?b"), new IntConst(BigInteger.ZERO), IntOperation.MUL);
        Expression child1 = new BinaryIntExpression(grandChild1, new IntConst(BigInteger.valueOf(2)), IntOperation.ADD);

        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, new IntConst(BigInteger.ONE), IntOperation.ADD);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.valueOf(3)));
    }

    @Test
    public void testBinaryIntExpressionReturnFolded5() {
        Expression child1 = new FreeVarExpression(Type.Integer, "?a");
        Expression child2 = new IntConst(BigInteger.ONE);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.DIV);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.type).isEqualTo(Type.Integer));
        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testBinaryIntExpressionWithFreeVarCantFold1() {
        Expression child1 = new IntConst(BigInteger.ONE);
        Expression child2 = new FreeVarExpression(Type.Integer, "?a");
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.DIV);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression1).isInstanceOf(IntConst.class));
        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression2).isInstanceOf(FreeVarExpression.class));
        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.operation).isEqualTo(IntOperation.DIV));
    }

    @Test
    public void testBinaryIntExpressionReturnFolded6() {
        Expression child1 = new FreeVarExpression(Type.Integer, "?a");
        Expression child2 = new IntConst(BigInteger.ONE);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.MOD);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.ZERO));
    }

    @Test
    public void testBinaryIntExpressionWithFreeVarCantFold2() {
        Expression child1 = new FreeVarExpression(Type.Integer, "?a");
        Expression child2 = new IntConst(BigInteger.valueOf(50));
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.ADD);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression1).isInstanceOf(FreeVarExpression.class));
        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression1.getType()).isEqualTo(Type.Integer));
        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression2).isInstanceOf(IntConst.class));
        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.operation).isEqualTo(IntOperation.ADD));
    }

    @Test
    public void testBinaryIntExpressionReturnFolded7() {
        Expression child1 = new FreeVarExpression(Type.Integer, "?a");
        Expression child2 = new IntConst(BigInteger.ZERO);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.ADD);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testBinaryIntExpressionReturnFolded8() {
        Expression child1 = new FreeVarExpression(Type.Integer, "?a");
        Expression child2 = new IntConst(BigInteger.ZERO);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.MUL);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.ZERO));
    }

    @Test
    public void testBinaryIntExpressionReturnFolded9() {
        Expression child1 = new FreeVarExpression(Type.Integer, "?a");
        Expression child2 = new IntConst(BigInteger.ONE);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.MUL);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testBinaryIntExpressionDivisionByZeroReturnUnfolded() {
        Expression child1 = new IntConst(BigInteger.TEN);
        Expression child2 = new IntConst(BigInteger.ZERO);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.DIV);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e ->  {
            assertThat(e.expression1).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(BigInteger.TEN));
            assertThat(e.expression2).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(BigInteger.ZERO));
            assertThat(e.operation).isEqualTo(IntOperation.DIV);
        });
    }

    @Test
    public void testBinaryIntExpressionModuloByZeroReturnUnfolded() {
        Expression child1 = new IntConst(BigInteger.TEN);
        Expression child2 = new IntConst(BigInteger.ZERO);
        BinaryIntExpression binaryIntExpression = new BinaryIntExpression(child1, child2, IntOperation.MOD);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(BinaryIntExpression.class, e ->  {
            assertThat(e.expression1).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(BigInteger.TEN));
            assertThat(e.expression2).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(BigInteger.ZERO));
            assertThat(e.operation).isEqualTo(IntOperation.MOD);
        });
    }


    @Test
    public void testBinaryBoolExpressionReturnFolded() {
        Expression child1 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.AND);
        Expression child2 = BoolConst.TRUE;
        BinaryBoolExpression binaryIntExpression = new BinaryBoolExpression(child1, child2, BoolOperation.OR);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testBinaryBoolExpressionWithFreeVarCantFold() {

        Expression child1 = new FreeVarExpression(Type.Boolean, "?a");
        Expression child2 = new FreeVarExpression(Type.Boolean, "?b");
        BinaryBoolExpression binaryIntExpression = new BinaryBoolExpression(child1, child2, BoolOperation.AND);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(BinaryBoolExpression.class, e -> assertThat(e.expression1).isInstanceOf(FreeVarExpression.class));
        assertThat(visited).isInstanceOfSatisfying(BinaryBoolExpression.class, e -> assertThat(e.expression1.getType()).isEqualTo(Type.Boolean));
        assertThat(visited).isInstanceOfSatisfying(BinaryBoolExpression.class, e -> assertThat(e.expression2).isInstanceOf(FreeVarExpression.class));
        assertThat(visited).isInstanceOfSatisfying(BinaryBoolExpression.class, e -> assertThat(e.expression2.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void testBinaryBoolExpressionWithFreeVarReturnFolded1() {
        Expression child1 = new FreeVarExpression(Type.Boolean, "?a");
        Expression child2 = BoolConst.TRUE;
        BinaryBoolExpression binaryIntExpression = new BinaryBoolExpression(child1, child2, BoolOperation.OR);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testBinaryBoolExpressionWithFreeVarReturnFolded2() {
        Expression child1 = new FreeVarExpression(Type.Boolean, "?a");
        Expression child2 = BoolConst.TRUE;
        BinaryBoolExpression binaryIntExpression = new BinaryBoolExpression(child1, child2, BoolOperation.AND);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.type).isEqualTo(Type.Boolean));
        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testBinaryBoolExpressionWithFreeVarReturnFolded3() {
        Expression child1 = new FreeVarExpression(Type.Boolean, "?a");
        Expression child2 = BoolConst.FALSE;
        BinaryBoolExpression binaryIntExpression = new BinaryBoolExpression(child1, child2, BoolOperation.AND);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));
    }

    @Test
    public void testBinaryBoolExpressionWithFreeVarReturnFolded4() {
        Expression child1 = new FreeVarExpression(Type.Boolean, "?a");
        Expression child2 = BoolConst.FALSE;
        BinaryBoolExpression binaryIntExpression = new BinaryBoolExpression(child1, child2, BoolOperation.OR);

        Expression visited = expressionVisitor.visit(binaryIntExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.type).isEqualTo(Type.Boolean));
        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testSelectExpressionReturnFolded1() {
        Expression child1 = new ArrayInitExpression(BoolConst.TRUE);
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.ZERO), new FreeVarExpression(Type.Integer, "?c"), IntOperation.MUL);

        SelectExpression selectExpression = new SelectExpression(child1, child2);

        Expression visited = expressionVisitor.visit(selectExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testSelectExpressionReturnFolded2() {
        Expression child1 = new ArrayInitExpression(new BinaryIntExpression(new IntConst(BigInteger.valueOf(4)), new IntConst(BigInteger.valueOf(4)), IntOperation.ADD));
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.ZERO), new FreeVarExpression(Type.Integer, "?c"), IntOperation.MUL);

        SelectExpression selectExpression = new SelectExpression(child1, child2);

        Expression visited = expressionVisitor.visit(selectExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.valueOf(8)));
    }

    @Test
    public void testSelectExpressionReturnFolded3() {
        Expression child1 = new ArrayInitExpression(new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.FALSE, BoolOperation.AND));
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.ZERO), new FreeVarExpression(Type.Integer, "?c"), IntOperation.MUL);
        SelectExpression selectExpression = new SelectExpression(child1, child2);

        Expression visited = expressionVisitor.visit(selectExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));
    }

    @Test
    public void testSelectExpressionReturnFolded4() {
        Expression child1 = new ArrayInitExpression(new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.FALSE, BoolOperation.AND));
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.ONE), new FreeVarExpression(Type.Integer, "?c"), IntOperation.MUL);
        SelectExpression selectExpression = new SelectExpression(child1, child2);

        Expression visited = expressionVisitor.visit(selectExpression);

        assertThat(visited).isInstanceOfSatisfying(SelectExpression.class, e -> assertThat(e.expression1).isInstanceOf(ArrayInitExpression.class));
        assertThat(visited).isInstanceOfSatisfying(SelectExpression.class, e -> assertThat(e.expression2).isInstanceOf(FreeVarExpression.class));
    }

    @Test
    public void testStoreExpressionReturnFolded1() {
        Expression child1 = new ArrayInitExpression(new BinaryIntExpression(new IntConst(BigInteger.TEN), new IntConst(BigInteger.valueOf(2)), IntOperation.ADD));
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.valueOf(5)), new IntConst(BigInteger.ONE), IntOperation.SUB);
        Expression child3 = new IntConst(BigInteger.ZERO);
        StoreExpression storeExpression = new StoreExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(storeExpression);

        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression1).isInstanceOf(ArrayInitExpression.class));
        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression2).isInstanceOf(IntConst.class));
        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression3).isInstanceOf(IntConst.class));
    }

    @Test
    public void testStoreExpressionReturnFolded2() {
        Expression arrayInitializer = new ArrayInitExpression(BoolConst.TRUE);
        Expression testerValue = BoolConst.FALSE;

        Expression position1 = new IntConst(BigInteger.ONE);
        Expression storeGrandChild = new StoreExpression(arrayInitializer, position1, testerValue);

        Expression position2 = new IntConst(BigInteger.valueOf(4));
        Expression storeChild = new StoreExpression(storeGrandChild, position2, testerValue);

        Expression position3 = new BinaryIntExpression(new IntConst(BigInteger.valueOf(7)), new IntConst(BigInteger.ONE), IntOperation.SUB);

        StoreExpression storeExpression = new StoreExpression(storeChild, position3, testerValue);

        Expression visited = expressionVisitor.visit(storeExpression);

        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression1).isInstanceOf(StoreExpression.class));
        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression2).isInstanceOf(IntConst.class));
        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression3).isInstanceOf(BoolConst.class));

        SelectExpression selectExpressionAtPositionOne = new SelectExpression(storeExpression, position1);
        visited = expressionVisitor.visit(selectExpressionAtPositionOne);
        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));

        SelectExpression selectExpressionAtPositionTwo = new SelectExpression(storeExpression, position2);
        visited = expressionVisitor.visit(selectExpressionAtPositionTwo);
        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));

        SelectExpression selectExpressionAtPositionThree = new SelectExpression(storeExpression, position3);
        visited = expressionVisitor.visit(selectExpressionAtPositionThree);
        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));


        SelectExpression selectExpressionEveryWhereElse = new SelectExpression(storeExpression, new IntConst(BigInteger.TEN));
        visited = expressionVisitor.visit(selectExpressionEveryWhereElse);
        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));

        selectExpressionEveryWhereElse = new SelectExpression(storeExpression, new IntConst(BigInteger.ZERO));
        visited = expressionVisitor.visit(selectExpressionEveryWhereElse);
        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testStoreExpressionReturnFolded3() {
        Expression child1 = new ArrayInitExpression(new IntConst(BigInteger.TEN));
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.ZERO), new FreeVarExpression(Type.Integer, "?a"), IntOperation.MUL);
        Expression child3 = new IntConst(BigInteger.ZERO);
        StoreExpression storeExpression = new StoreExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(storeExpression);

        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression1).isInstanceOf(ArrayInitExpression.class));
        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression2).isInstanceOf(IntConst.class));
        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression3).isInstanceOf(IntConst.class));

        SelectExpression selectExpressionAtPositionZero = new SelectExpression(storeExpression, child3);
        visited = expressionVisitor.visit(selectExpressionAtPositionZero);
        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.ZERO));

        SelectExpression allOtherPositions = new SelectExpression(storeExpression, new IntConst(BigInteger.ONE));
        visited = expressionVisitor.visit(allOtherPositions);
        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.TEN));
    }

    @Test
    public void testStoreExpressionReturnFolded4() {
        Expression child1 = new ArrayInitExpression(new IntConst(BigInteger.TEN));
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.ONE), new FreeVarExpression(Type.Integer, "?a"), IntOperation.MUL);
        Expression child3 = new IntConst(BigInteger.ZERO);
        StoreExpression storeExpression = new StoreExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(storeExpression);

        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression1).isInstanceOf(ArrayInitExpression.class));
        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression2).isInstanceOf(FreeVarExpression.class));
        assertThat(visited).isInstanceOfSatisfying(StoreExpression.class, e -> assertThat(e.expression3).isInstanceOf(IntConst.class));
    }

    @Test
    public void testAppExpressionThrowsUnsupportedException() {
        Expression body = new BinaryIntExpression(new ParVarExpression(Type.Integer, "!a"), new VarExpression(Type.Integer, "b"), IntOperation.ADD);
        Operation operation = new Operation("op1", body, new ArrayList<>(), new ArrayList<>());
        AppExpression appExpression = new AppExpression(operation, new ArrayList<>(), new ArrayList<>());

        assertThatThrownBy(() -> expressionVisitor.visit(appExpression)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testConstructorAppExpressionThrowsUnsupportedException() {
        Constructor constructor = new Constructor("TV", new ArrayList<>());
        Type.CustomType customType = new Type.CustomType("custom", new ArrayList<>());
        ConstructorAppExpression constructorAppExpression = new ConstructorAppExpression(constructor, customType, new ArrayList<>());

        assertThatThrownBy(() -> expressionVisitor.visit(constructorAppExpression)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testMatchExpressionThrowsUnsupportedException() {
        MatchExpression matchExpression = new MatchExpression(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        assertThatThrownBy(() -> expressionVisitor.visit(matchExpression)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testNegationExpressionReturnFolded1() {

        Expression expression = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.AND);
        NegationExpression negationExpression = new NegationExpression(expression);

        Expression visited = expressionVisitor.visit(negationExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testNegationExpressionReturnFolded2() {
        Expression child1 = new FreeVarExpression(Type.Boolean, "?a");
        Expression child2 = BoolConst.FALSE;
        BinaryBoolExpression binaryBoolExpression = new BinaryBoolExpression(child1, child2, BoolOperation.AND);

        NegationExpression negationExpression = new NegationExpression(binaryBoolExpression);

        Expression visited = expressionVisitor.visit(negationExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testNegationExpressionReturnFolded3() {
        Expression child1 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.FALSE, BoolOperation.AND);
        Expression child2 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?b"), BoolConst.TRUE, BoolOperation.OR);
        BinaryBoolExpression binaryBoolExpression = new BinaryBoolExpression(child1, child2, BoolOperation.OR);

        NegationExpression negationExpression = new NegationExpression(binaryBoolExpression);

        Expression visited = expressionVisitor.visit(negationExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));
    }

    @Test
    public void testNegationExpressionReturnFolded4() {
        Expression grandChild = new FreeVarExpression(Type.Boolean, "?a");
        Expression child = new NegationExpression(grandChild);

        NegationExpression negationExpression = new NegationExpression(child);

        Expression visited = expressionVisitor.visit(negationExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.type).isEqualTo(Type.Boolean));
        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testNegationExpressionReturnFolded5() {
        Expression grandChild1 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.TRUE, BoolOperation.AND);
        Expression grandChild2 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?b"), BoolConst.TRUE, BoolOperation.OR);
        Expression child = new BinaryBoolExpression(grandChild1, grandChild2, BoolOperation.AND);
        Expression negation = new NegationExpression(child);

        NegationExpression negationExpression = new NegationExpression(negation);

        Expression visited = expressionVisitor.visit(negationExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.type).isEqualTo(Type.Boolean));
        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testNegationExpressionWithFreeVarCantFold() {
        Expression child1 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.FALSE, BoolOperation.OR);
        Expression child2 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?b"), BoolConst.TRUE, BoolOperation.AND);
        BinaryBoolExpression binaryBoolExpression = new BinaryBoolExpression(child1, child2, BoolOperation.OR);

        NegationExpression negationExpression = new NegationExpression(binaryBoolExpression);

        Expression visited = expressionVisitor.visit(negationExpression);

        assertThat(visited).isInstanceOfSatisfying(NegationExpression.class, e -> assertThat(e.expression).isInstanceOf(BinaryBoolExpression.class));
    }

    @Test
    public void testConditionalExpressionReturnFolded1() {
        Expression child1 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.OR);
        Expression child2 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.AND);
        Expression child3 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.TRUE, BoolOperation.AND);

        ConditionalExpression conditionalExpression = new ConditionalExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));
    }

    @Test
    public void testConditionalExpressionReturnFolded2() {
        Expression child1 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.FALSE, BoolOperation.AND);
        Expression child2 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?b"), BoolConst.FALSE, BoolOperation.AND);
        Expression child3 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?c"), BoolConst.TRUE, BoolOperation.OR);

        ConditionalExpression conditionalExpression = new ConditionalExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testConditionalExpressionReturnFolded3() {
        Expression child1 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.TRUE, BoolOperation.OR);
        Expression child2 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?b"), BoolConst.TRUE, BoolOperation.AND);
        Expression child3 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?c"), BoolConst.FALSE, BoolOperation.OR);

        ConditionalExpression conditionalExpression = new ConditionalExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.type).isEqualTo(Type.Boolean));
        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?b"));
    }

    @Test
    public void testConditionalExpressionReturnFolded4() {
        Expression grandChild1 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.AND);
        Expression grandChild2 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.TRUE, BoolOperation.OR);
        Expression child1 = new BinaryBoolExpression(grandChild1, grandChild2, BoolOperation.OR);
        Expression child2 = new IntConst(BigInteger.TEN);
        Expression child3 = BoolConst.FALSE;

        ConditionalExpression conditionalExpression = new ConditionalExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.TEN));
    }

    @Test
    public void testConditionalExpressionWithFreeVarCanFoldIfBranchesAreTrueAndFalse() {
        ConditionalExpression conditionalExpression = new ConditionalExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.TRUE, BoolConst.FALSE);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testConditionalExpressionWithFreeVarCanFoldIfBranchesAreFalseAndTrue1() {
        ConditionalExpression conditionalExpression = new ConditionalExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.FALSE, BoolConst.TRUE);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(NegationExpression.class, n -> assertThat(n.expression).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a")));
    }

    @Test
    public void testConditionalExpressionWithFreeVarCanFoldIfBranchesAreFalseAndTrue2() {
        ConditionalExpression conditionalExpression = new ConditionalExpression(new NegationExpression(new FreeVarExpression(Type.Boolean, "?a")), BoolConst.FALSE, BoolConst.TRUE);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testConditionalExpressionWithFreeVarCantFold() {
        ConditionalExpression conditionalExpression = new ConditionalExpression(new FreeVarExpression(Type.Boolean, "?a"), new FreeVarExpression(Type.Boolean, "?b"), BoolConst.FALSE);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(ConditionalExpression.class, e -> {
            assertThat(e.expression1).isInstanceOf(FreeVarExpression.class);
            assertThat(e.expression2).isInstanceOf(FreeVarExpression.class);
            assertThat(e.expression3).isInstanceOf(BoolConst.class);
        });
    }

    @Test
    public void testConditionalExpressionReturnFolded5() {
        Expression greatGrandChild1 = new ConditionalExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.TRUE, BoolConst.FALSE);
        Expression greatGrandChild2 = new ConditionalExpression(new FreeVarExpression(Type.Boolean, "?b"), BoolConst.TRUE, BoolConst.FALSE);
        Expression grandChild1 = new BinaryBoolExpression(greatGrandChild1, greatGrandChild2, BoolOperation.OR);
        Expression child1 = new BinaryBoolExpression(grandChild1, BoolConst.TRUE, BoolOperation.OR);
        Expression child2 = new IntConst(BigInteger.TEN);
        Expression child3 = BoolConst.FALSE;

        ConditionalExpression conditionalExpression = new ConditionalExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.TEN));
    }

    @Test
    public void testConditionalExpressionReturnFolded6() {
        Expression sameExpression = new IntConst(BigInteger.TEN);
        ConditionalExpression conditionalExpression = new ConditionalExpression(new FreeVarExpression(Type.Boolean, "?a"), sameExpression, sameExpression);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.TEN));
    }

    @Test
    public void testConditionalExpressionReturnFolded7() {
        Expression child1 = new FreeVarExpression(Type.Integer, "?a");
        Expression child2 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.OR);
        Expression child3 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.AND);

        ConditionalExpression conditionalExpression = new ConditionalExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.type).isEqualTo(Type.Integer));
        assertThat(visited).isInstanceOfSatisfying(FreeVarExpression.class, e -> assertThat(e.name).isEqualTo("?a"));
    }

    @Test
    public void testConditionalExpressionReturnUnfolded() {
        Expression grandChild1 = new BinaryIntExpression(new IntConst(BigInteger.ZERO), new IntConst(BigInteger.ZERO), IntOperation.DIV);
        Expression child1 = new ComparisonExpression(grandChild1, new IntConst(BigInteger.valueOf(2)), CompOperation.GE);
        Expression child2 = new FreeVarExpression(Type.Integer, "?a");
        Expression child3 = new FreeVarExpression(Type.Integer, "?b");

        ConditionalExpression conditionalExpression = new ConditionalExpression(child1, child2, child3);

        Expression visited = expressionVisitor.visit(conditionalExpression);

        assertThat(visited).isInstanceOfSatisfying(ConditionalExpression.class, e ->  {
            assertThat(e.expression1).isInstanceOfSatisfying(ComparisonExpression.class, c ->
                    assertThat(c.expression1).isInstanceOfSatisfying(BinaryIntExpression.class, b -> {
                        assertThat(b.expression1).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(BigInteger.ZERO));
                        assertThat(b.expression2).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(BigInteger.ZERO));
                        assertThat(b.operation).isEqualTo(IntOperation.DIV);
                    }));
            assertThat(e.expression2).isInstanceOfSatisfying(FreeVarExpression.class, f -> assertThat(f.name).isEqualTo("?a"));
            assertThat(e.expression3).isInstanceOfSatisfying(FreeVarExpression.class, f -> assertThat(f.name).isEqualTo("?b"));
        });

    }

    @Test
    public void testComparisonExpressionReturnFolded1() {
        Expression grandChild1 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.OR);
        Expression child1 = new BinaryBoolExpression(grandChild1, BoolConst.FALSE, BoolOperation.AND);
        Expression child2 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.TRUE, BoolOperation.AND);

        ComparisonExpression comparisonExpression = new ComparisonExpression(child1, child2, CompOperation.NEQ);

        Expression visited = expressionVisitor.visit(comparisonExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testComparisonExpressionReturnFolded2() {
        Expression child1 = new BinaryBoolExpression(new FreeVarExpression(Type.Boolean, "?a"), BoolConst.FALSE, BoolOperation.AND);
        Expression child2 = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.AND);

        ComparisonExpression comparisonExpression = new ComparisonExpression(child1, child2, CompOperation.EQ);

        Expression visited = expressionVisitor.visit(comparisonExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testComparisonExpressionReturnFolded3() {
        Expression child1 = new BinaryIntExpression(new IntConst(BigInteger.valueOf(5)), new IntConst(BigInteger.valueOf(7)), IntOperation.MUL);
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.TEN), new IntConst(BigInteger.valueOf(2)), IntOperation.DIV);

        ComparisonExpression comparisonExpression = new ComparisonExpression(child1, child2, CompOperation.GE);

        Expression visited = expressionVisitor.visit(comparisonExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testComparisonExpressionReturnFolded4() {
        Expression grandChild1 = new BinaryIntExpression(new FreeVarExpression(Type.Integer, "?b"), new IntConst(BigInteger.ZERO), IntOperation.MUL);
        Expression child1 = new BinaryIntExpression(grandChild1, new IntConst(BigInteger.valueOf(2)), IntOperation.ADD);
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.valueOf(100)), new IntConst(BigInteger.valueOf(6)), IntOperation.MOD);

        ComparisonExpression comparisonExpression = new ComparisonExpression(child1, child2, CompOperation.GT);

        Expression visited = expressionVisitor.visit(comparisonExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));
    }

    @Test
    public void testComparisonExpressionReturnFolded5() {
        Expression child1 = new BinaryIntExpression(new FreeVarExpression(Type.Integer, "?b"), new IntConst(BigInteger.ZERO), IntOperation.MUL);
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.ONE), new IntConst(BigInteger.ONE), IntOperation.SUB);

        ComparisonExpression comparisonExpression = new ComparisonExpression(child1, child2, CompOperation.LT);

        Expression visited = expressionVisitor.visit(comparisonExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(false));
    }

    @Test
    public void testComparisonExpressionReturnFolded6() {
        Expression child1 = new BinaryIntExpression(new FreeVarExpression(Type.Integer, "?b"), new IntConst(BigInteger.ZERO), IntOperation.MUL);
        Expression child2 = new BinaryIntExpression(new IntConst(BigInteger.ONE), new IntConst(BigInteger.ONE), IntOperation.SUB);

        ComparisonExpression comparisonExpression = new ComparisonExpression(child1, child2, CompOperation.LE);

        Expression visited = expressionVisitor.visit(comparisonExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testConstExpressionReturnFolded1() {
        Expression child = new BinaryBoolExpression(BoolConst.TRUE, BoolConst.FALSE, BoolOperation.OR);

        ConstExpression constExpression = new ConstExpression("const", child);

        Expression visited = expressionVisitor.visit(constExpression);

        assertThat(visited).isInstanceOfSatisfying(BoolConst.class, e -> assertThat(e.value).isEqualTo(true));
    }

    @Test
    public void testConstExpressionReturnFolded2() {
        Expression grandChild1 = new BinaryIntExpression(new IntConst(BigInteger.TEN), new IntConst(BigInteger.valueOf(5)), IntOperation.MUL);
        Expression grandChild2 = new BinaryIntExpression(new IntConst(BigInteger.ONE), new IntConst(BigInteger.ONE), IntOperation.ADD);
        Expression child = new BinaryIntExpression(grandChild1, grandChild2, IntOperation.SUB);

        ConstExpression constExpression = new ConstExpression("const", child);

        Expression visited = expressionVisitor.visit(constExpression);

        assertThat(visited).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.valueOf(48)));
    }

    @Test
    public void testConstExpressionReturnFolded3() {
        Expression array = new ArrayInitExpression(new IntConst(BigInteger.TEN));
        Expression position = new IntConst(BigInteger.ONE);
        Expression newValue = new IntConst(BigInteger.ZERO);
        StoreExpression storeExpression = new StoreExpression(array, position, newValue);

        SelectExpression selectExpressionWithNewValue = new SelectExpression(storeExpression, position);
        SelectExpression selectExpressionWithInitializer = new SelectExpression(storeExpression, new IntConst(BigInteger.ZERO));

        ConstExpression constExpressionForSelectWithNewValue = new ConstExpression("const", selectExpressionWithNewValue);
        ConstExpression constExpressionForSelectWithInitializer = new ConstExpression("const", selectExpressionWithInitializer);

        Expression visitedSelectWithNewValue = expressionVisitor.visit(constExpressionForSelectWithNewValue);
        Expression visitedSelectWithInitializer = expressionVisitor.visit(constExpressionForSelectWithInitializer);

        assertThat(visitedSelectWithInitializer).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.TEN));
        assertThat(visitedSelectWithNewValue).isInstanceOfSatisfying(IntConst.class, e -> assertThat(e.value).isEqualTo(BigInteger.ZERO));
    }

    @Test
    public void testSumExpressionThrowsUnsupportedException() {
        Expression body = new IntConst(BigInteger.TEN);
        SumExpression sumExpression = new SumExpression(CompoundSelectorFunctionInvocation.UnitInvocation, body, SumOperation.ADD);

        assertThatThrownBy(() -> expressionVisitor.visit(sumExpression)).isInstanceOf(UnsupportedOperationException.class);
    }

}
