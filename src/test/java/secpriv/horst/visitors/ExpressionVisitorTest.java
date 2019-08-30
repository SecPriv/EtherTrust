package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Operation;
import secpriv.horst.data.SelectorFunction;
import secpriv.horst.data.SumOperation;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpressionVisitorTest {
    private VisitorState state;
    private TestingErrorHandler testingErrorHandler;

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @BeforeEach
    public void setUp() {
        testingErrorHandler = new TestingErrorHandler();
        state = new VisitorState(testingErrorHandler);
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        defineSelectorFunctions();
    }

    private void defineSelectorFunctions() {
        class Provider {
            public Iterable<BigInteger> interval(BigInteger a, BigInteger b) {
                return null;
            }

            public Iterable<Boolean> parity(BigInteger a) {
                return null;
            }
        }
        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);
    }

    @AfterEach
    public void tearDown() {
        state = null;
        testingErrorHandler = null;
    }

    @Test
    public void parseBoolConstantFalse() {
        String s = "false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Expression exp = visitor.visit(parser.exp()).get();
        assertThat(exp).isInstanceOfSatisfying(Expression.BoolConst.class, b -> assertThat(b.value).isFalse());
    }

    @Test
    public void parseBoolConstantTrue() {
        String s = "true";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Expression exp = visitor.visit(parser.exp()).get();
        assertThat(exp).isInstanceOfSatisfying(Expression.BoolConst.class, b -> assertThat(b.value).isTrue());
    }

    @Test
    public void parseBigIntConstant1() {
        String s = "12341234123412341234123412341234";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Expression exp = visitor.visit(parser.exp()).get();
        assertThat(exp).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(new BigInteger(s)));
    }

    @Test
    public void parseBigIntConstant2() {
        String s = "12341234123412341234123412341234";
        ASParser parser = getParserFromString("~" + s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Expression exp = visitor.visit(parser.exp()).get();
        assertThat(exp).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(new BigInteger("-" + s)));
    }

    @Test
    public void parseParenExpression1() {
        String s = "(123)";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> optExp = visitor.visit(parser.exp());
        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(new BigInteger("123")));
    }

    @Test
    public void parseParenExpression2() {
        String s = "((((123))))";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> optExp = visitor.visit(parser.exp());
        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(new BigInteger("123")));
    }

    @Test
    public void parseParenExpression3() {
        String s = "((((123 + 1234) *123) * 89789) + 132434)";
        ASParser parser = getParserFromString(s);

        state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> optExp = visitor.visit(parser.exp());
        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOfSatisfying(Expression.BinaryIntExpression.class, e -> assertThat(e.operation).isEqualTo(Expression.IntOperation.ADD));
    }

    @Test
    public void parseParenExpression4() {
        String s = "((((123 + false))))";
        ASParser parser = getParserFromString(s);

        state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> optExp = visitor.visit(parser.exp());
        assertThat(optExp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseBooleanNegation1() {
        String s = "~ true";
        assertExpressionAsStringIsOfType(s, Expression.NegationExpression.class);
    }

    @Test
    public void parseBooleanNegation2() {
        String s = "~ false";
        assertExpressionAsStringIsOfType(s, Expression.NegationExpression.class);
    }

    @Test
    public void parseBooleanNegation3() {
        String s = "~ ~ true";
        assertExpressionAsStringIsOfType(s, Expression.NegationExpression.class);
    }

    @Test
    public void parseBooleanNegationOfInteger() {
        String s = "~ ~ 123";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isEmpty();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseBooleanExpression1() {
        String s = "true && false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryBoolExpression.class, e -> assertThat(e.operation).isEqualTo(Expression.BoolOperation.AND));
    }

    @Test
    public void parseBooleanExpression2() {
        String s = "false || true";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryBoolExpression.class, e -> assertThat(e.operation).isEqualTo(Expression.BoolOperation.OR));
    }

    @Test
    public void parseBooleanExpressionNegationPrecedence() {
        String s = "~ false || true";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryBoolExpression.class, e -> {
            assertThat(e.operation).isEqualTo(Expression.BoolOperation.OR);
            assertThat(e.expression1).isInstanceOf(Expression.NegationExpression.class);
        });
    }

    @Test
    public void parseBooleanExpressionAndBindsStrongerThanOr() {
        String s = "true && false || true && false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryBoolExpression.class, e -> {
            assertThat(e.operation).isEqualTo(Expression.BoolOperation.OR);
            assertThat(e.expression1).isInstanceOfSatisfying(Expression.BinaryBoolExpression.class, e1 -> {
                assertThat(e1.operation).isEqualTo(Expression.BoolOperation.AND);
                assertThat(e1.expression1).isEqualTo(Expression.BoolConst.TRUE);
                assertThat(e1.expression2).isEqualTo(Expression.BoolConst.FALSE);
            });
            assertThat(e.expression2).isInstanceOfSatisfying(Expression.BinaryBoolExpression.class, e2 -> {
                assertThat(e2.operation).isEqualTo(Expression.BoolOperation.AND);
                assertThat(e2.expression1).isEqualTo(Expression.BoolConst.TRUE);
                assertThat(e2.expression2).isEqualTo(Expression.BoolConst.FALSE);
            });
        });
    }

    @Test
    public void parseBooleanExpression3() {
        String s = "false || a";
        ASParser parser = getParserFromString(s);

        state = new VisitorState(new TestingErrorHandler());
        state.defineVar("a", Type.Boolean);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryBoolExpression.class, e -> assertThat(e.operation).isEqualTo(Expression.BoolOperation.OR));
    }


    @Test
    public void parseBitvectorAddExpression() {
        String s = "1 bvand 3";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryIntExpression.class, e -> {
            assertThat(e.operation).isEqualTo(Expression.IntOperation.BVAND);
            assertThat(e.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i1 -> assertThat(i1.value).isEqualTo(1));
            assertThat(e.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i2 -> assertThat(i2.value).isEqualTo(3));
        });
    }

    @Test
    public void parseBitvectorXorExpression() {
        String s = "2 bvxor 5";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryIntExpression.class, e -> {
            assertThat(e.operation).isEqualTo(Expression.IntOperation.BVXOR);
            assertThat(e.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i1 -> assertThat(i1.value).isEqualTo(2));
            assertThat(e.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i2 -> assertThat(i2.value).isEqualTo(5));
        });
    }

    @Test
    public void parseBitvectorOrExpression() {
        String s = "3 bvor 6";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryIntExpression.class, e -> {
            assertThat(e.operation).isEqualTo(Expression.IntOperation.BVOR);
            assertThat(e.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i1 -> assertThat(i1.value).isEqualTo(3));
            assertThat(e.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i2 -> assertThat(i2.value).isEqualTo(6));
        });
    }

    @Test
    public void parseBitvectorNegationExpression() {
        String s = "bvneg 7";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BitvectorNegationExpression.class, e ->
                assertThat(e.expression).isInstanceOfSatisfying(Expression.IntConst.class, i1 -> assertThat(i1.value).isEqualTo(7))
        );
    }

    @Test
    public void parseBitvectorExpressionPredence() {
        String s = "5 bvand 3 bvor 6 bvxor bvneg 8";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.BinaryIntExpression.class, e -> {
            assertThat(e.operation).isEqualTo(Expression.IntOperation.BVOR);
            assertThat(e.expression1).isInstanceOfSatisfying(Expression.BinaryIntExpression.class, i1 -> {
                assertThat(i1.operation).isEqualTo(Expression.IntOperation.BVAND);
                assertThat(i1.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i11 -> assertThat(i11.value).isEqualTo(5));
                assertThat(i1.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i12 -> assertThat(i12.value).isEqualTo(3));
            });
            assertThat(e.expression2).isInstanceOfSatisfying(Expression.BinaryIntExpression.class, i2 -> {
                assertThat(i2.operation).isEqualTo(Expression.IntOperation.BVXOR);
                assertThat(i2.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i21 -> assertThat(i21.value).isEqualTo(6));
                assertThat(i2.expression2).isInstanceOfSatisfying(Expression.BitvectorNegationExpression.class, i22 ->
                        assertThat(i22.expression).isInstanceOfSatisfying(Expression.IntConst.class, i21 -> assertThat(i21.value).isEqualTo(8)));
            });
        });
    }

    @Test
    public void parseComparisonExpression1() {
        parseComparision(">", Expression.CompOperation.GT);
    }

    @Test
    public void parseComparisonExpression2() {
        parseComparision("<", Expression.CompOperation.LT);
    }

    @Test
    public void parseComparisonExpression3() {
        parseComparision(">=", Expression.CompOperation.GE);
    }

    @Test
    public void parseComparisonExpression4() {
        parseComparision("<=", Expression.CompOperation.LE);
    }

    @Test
    public void parseComparisonExpression5() {
        parseComparision("=", Expression.CompOperation.EQ);
    }

    @Test
    public void parseComparisonExpression6() {
        String t = "eqtype MyColor := @R | @G | @B;";
        String s = "@R = @G";
        ASParser parser = getParserFromString(t);

        TypeVisitor typeVisitor = new TypeVisitor();
        Optional<Type.CustomType> optType = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();

        state = new VisitorState(new TestingErrorHandler());
        state.defineType(optType.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ComparisonExpression.class, e -> assertThat(e.operation).isEqualTo(Expression.CompOperation.EQ));
    }

    @Test
    public void parseComparisonExpression7() {
        String t = "eqtype MyColor := @R | @G | @B;";
        String s = "@R != @G";
        ASParser parser = getParserFromString(t);

        TypeVisitor typeVisitor = new TypeVisitor();
        Optional<Type.CustomType> optType = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();

        state = new VisitorState(new TestingErrorHandler());
        state.defineType(optType.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ComparisonExpression.class, e -> assertThat(e.operation).isEqualTo(Expression.CompOperation.NEQ));
    }

    @Test
    public void parseComparisonExpressionNonIntOperand1() {
        String s = "1 > false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseComparisonExpressionNonIntOperand2() {
        String s = "true > 8";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseComparisonExpressionNonIntOperand3() {
        String s = "true > false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(2, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
        assertThat(testingErrorHandler.errorObjects.get(1)).isInstanceOf(Error.TypeMismatch.class);
    }

    private void parseComparision(String opString, Expression.CompOperation expectedOperation) {
        String s = "1 " + opString + " 2";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());
        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ComparisonExpression.class, e -> assertThat(e.getType()).isEqualTo(Type.Boolean));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ComparisonExpression.class, e -> assertThat(e.expression1.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ComparisonExpression.class, e -> assertThat(e.expression2.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ComparisonExpression.class, e -> assertThat(e.operation).isEqualTo(expectedOperation));
    }

    @Test
    public void parseArrayConst1() {
        String s = "[true]";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.getType()).isEqualTo(Type.Array.of(Type.Boolean)));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer.getType()).isEqualTo(Type.Boolean));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer).isInstanceOfSatisfying(Expression.BoolConst.class, b -> assertThat(b.value).isTrue()));
    }

    @Test
    public void parseArrayConst2() {
        String s = "[0]";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.getType()).isEqualTo(Type.Array.of(Type.Integer)));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer).isInstanceOfSatisfying(Expression.IntConst.class, b -> assertThat(b.value).isEqualTo(0)));
    }

    @Test
    public void parseArrayConst3() {
        String s = "[[0]]";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.getType()).isEqualTo(Type.Array.of(Type.Array.of(Type.Integer))));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer.getType()).isEqualTo(Type.Array.of(Type.Integer)));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer).isInstanceOf(Expression.ArrayInitExpression.class));
    }

    @Test
    public void parseArrayConst4() {
        String s = "[[match 3 with | x => x | _ => 4]]";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.getType()).isEqualTo(Type.Array.of(Type.Array.of(Type.Integer))));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer.getType()).isEqualTo(Type.Array.of(Type.Integer)));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer).isInstanceOf(Expression.ArrayInitExpression.class));
    }

    @Test
    public void parseArrayNonConst() {
        String s = "[[a]]";
        ASParser parser = getParserFromString(s);

        state = new VisitorState(new TestingErrorHandler());
        state.defineVar("a", Type.Integer);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ElementNotConst.class);
    }

    @Test
    public void parseCondExpression1() {
        String s = "(true) ? (2) : (3)";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConditionalExpression.class, a -> assertThat(a.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConditionalExpression.class, a -> assertThat(a.expression1.getType()).isEqualTo(Type.Boolean));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConditionalExpression.class, a -> assertThat(a.expression2.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConditionalExpression.class, a -> assertThat(a.expression3.getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseCondExpression2() {
        String s = "(a) ? (2) : (3)";
        ASParser parser = getParserFromString(s);

        state = new VisitorState(new TestingErrorHandler());
        state.defineVar("a", Type.Boolean);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConditionalExpression.class, a -> assertThat(a.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConditionalExpression.class, a -> assertThat(a.expression1.getType()).isEqualTo(Type.Boolean));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConditionalExpression.class, a -> assertThat(a.expression2.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConditionalExpression.class, a -> assertThat(a.expression3.getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseCondExpressionNonBoolean1() {
        String s = "(3) ? (2) : (3)";
        ASParser parser = getParserFromString(s);

        state = new VisitorState(new TestingErrorHandler());
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseCondExpressionNonBoolean2() {
        String s = "(a) ? (2) : (3)";
        ASParser parser = getParserFromString(s);

        state = new VisitorState(new TestingErrorHandler());
        state.defineVar("a", Type.Integer);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseCondExpressionNonMatchingOperands1() {
        String s = "(true) ? (1) : (false)";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseCondExpressionNonMatchingOperands2() {
        String s = "(true) ? (a) : (false)";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineVar("a", Type.Integer);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseSelectExpression1() {
        String s = "select [true] 2";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.SelectExpression.class, a -> assertThat(a.getType()).isEqualTo(Type.Boolean));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.SelectExpression.class, a -> assertThat(a.expression1.getType()).isEqualTo(Type.Array.of(Type.Boolean)));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.SelectExpression.class, a -> assertThat(a.expression2.getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseSelectExpression2() {
        String s = "select [true] true";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseSelectExpression3() {
        String s = "select true true";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(2, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.IsNotInstanceOf.class);
        assertThat(testingErrorHandler.errorObjects.get(1)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseStoreExpression1() {
        String s = "store [true] 9 false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.StoreExpression.class, e -> assertThat(e.expression1.getType()).isEqualTo(Type.Array.of(Type.Boolean)));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.StoreExpression.class, e -> assertThat(e.expression2.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.StoreExpression.class, e -> assertThat(e.expression3.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseStoreExpression2() {
        String s = "store [true] 9*3 false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.StoreExpression.class, e -> assertThat(e.expression1.getType()).isEqualTo(Type.Array.of(Type.Boolean)));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.StoreExpression.class, e -> assertThat(e.expression2.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.StoreExpression.class, e -> assertThat(e.expression3.getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseStoreExpression3() {
        String s = "store true 3 false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.IsNotInstanceOf.class);
    }

    @Test
    public void parseStoreExpression4() {
        String s = "store [8] 3 false";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseStoreExpression5() {
        String s = "store [8] true 3";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseIntegerExpression() {
        String s = "1111122223333444455555 + 11188888899999 * 888888777";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Expression exp = visitor.visit(parser.exp()).get();

        assertThat(exp).isInstanceOf(Expression.BinaryIntExpression.class);
    }

    @Test
    public void parseDefinedVar() {
        String s = "a";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineVar("a", Type.Integer);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Expression exp = visitor.visit(parser.exp()).get();

        assertThat(exp).isInstanceOf(Expression.VarExpression.class);
        assertThat(exp.getType()).isEqualTo(Type.Integer);
    }

    @Test
    public void parseUndefinedVar() {
        String s = "a";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);

    }

    @Test
    public void parseDefinedFreeVar() {
        String s = "?a";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineFreeVar("?a", Type.Integer);
        ExpressionVisitor visitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = visitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOf(Expression.FreeVarExpression.class);
        assertThat(optExp.get().getType()).isEqualTo(Type.Integer);
    }

    @Test
    public void parseUndefinedFreeVar() {
        String s = "?a";
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseConstructorAppExpression1() {
        String t = "datatype MyColor := @R | @G | @B;";
        String s = "@R";
        ASParser parser = getParserFromString(t);

        TypeVisitor typeVisitor = new TypeVisitor();
        Optional<Type.CustomType> optType = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(optType.get());


        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConstructorAppExpression.class, c -> assertThat(c.getType()).isEqualTo(optType.get()));

    }

    @Test
    public void parseConstructorAppExpression2() {
        String t = "datatype Tuple5 := @TV<int*int*int*int*int>;";
        String s = "@TV(1,2,3,4,5)";
        ASParser parser = getParserFromString(t);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optType = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();

        state.defineType(optType.get());


        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConstructorAppExpression.class, c -> assertThat(c.getType()).isEqualTo(optType.get()));

    }

    @Test
    public void parseConstructorAppExpression3() {
        String t = "datatype Tuple5 := @TV<int*int*int*int*int>;";
        String s = "@TV(1,2,false,4,5)";
        ASParser parser = getParserFromString(t);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optType = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();

        state.defineType(optType.get());


        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseConstructorAppExpression4() {
        String t1 = "datatype MyColor := @R | @G | @B;";
        String t2 = "datatype MyTuple := @TV<MyColor*MyColor*MyColor*int*bool>;";
        String s = "@TV(@R,@G,@B,4,true)";
        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optType1 = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType1).isPresent();

        state.defineType(optType1.get());
        parser = getParserFromString(t2);

        Optional<Type.CustomType> optType2 = typeVisitor.visit(parser.abstractDomainDeclaration());
        assertThat(optType2).isPresent();

        state.defineType(optType2.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.ConstructorAppExpression.class, c -> assertThat(c.getType()).isEqualTo(optType2.get()));
    }

    @Test
    public void parseMatchExpression1() {
        String s = "match true with | true => 1 | false => 1*4 | _ => 0";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.branchPatterns.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.resultExpressions.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.size()).isEqualTo(1));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(0).getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseMatchExpression2() {
        String s = "match (true, 3) with | (true,y) => y | (false,3) => 1*4 | _ => 0";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> {
            assertThat(c.getType()).isEqualTo(Type.Integer);
            assertThat(c.branchPatterns.size()).isEqualTo(3);
            assertThat(c.resultExpressions.size()).isEqualTo(3);
            assertThat(c.matchedExpressions.size()).isEqualTo(2);
            assertThat(c.matchedExpressions.get(0).getType()).isEqualTo(Type.Boolean);
            assertThat(c.matchedExpressions.get(1).getType()).isEqualTo(Type.Integer);
        });
    }

    @Test
    public void parseMatchExpression3() {
        String t1 = "datatype MyColor := @R | @G | @B;";
        String t2 = "datatype MyTuple := @TV<MyColor*MyColor*MyColor*int*bool>;";
        String s = "match (true, @R, @TV(@R,@G,@B,1234,false)) with | (true,@R,y) => 1 | (false,@B, @TV(@B,_,x,y,true)) => 1*4*y | _ => 0";
        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optMyColor = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optMyColor).isPresent();

        state.defineType(optMyColor.get());
        parser = getParserFromString(t2);

        Optional<Type.CustomType> optMyTuple = typeVisitor.visit(parser.abstractDomainDeclaration());
        assertThat(optMyTuple).isPresent();

        state.defineType(optMyTuple.get());

        parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.branchPatterns.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.resultExpressions.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(0).getType()).isEqualTo(Type.Boolean));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(1).getType()).isEqualTo(optMyColor.get()));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(2).getType()).isEqualTo(optMyTuple.get()));
    }

    @Test
    public void parseMatchExpression4() {
        String s = "match (true, 3) with | (true,y) => y | (false,3) => 1*4 | _ => 0";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.branchPatterns.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.resultExpressions.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.size()).isEqualTo(2));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(0).getType()).isEqualTo(Type.Boolean));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(1).getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseMatchExpression5() {
        String s = "match (true, 3) with | (true,y) => z | (false,3) => 1*4 | _ => 0";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        state.defineVar("z", Type.Integer);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.branchPatterns.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.resultExpressions.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.size()).isEqualTo(2));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(0).getType()).isEqualTo(Type.Boolean));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(1).getType()).isEqualTo(Type.Integer));
    }


    @Test
    public void parseMatchExpression6() {
        String s3 = "(match 1234 with | z => z | _ => 23)";
        String s2 = "(match (x,y) with | (@G, true) => 2 | (@B, false) => " + s3 + " | _ => 3)";
        String s1 = "(match @R with | x => " + s2 + "  | _ => 4 )";
        String s = "match true with | false => 0 | y => " + s1 + "  | _ => 2";


        String t1 = "datatype MyColor := @R | @G | @B;";
        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optMyColor = typeVisitor.visit(parser.abstractDomainDeclaration());
        assertThat(optMyColor).isPresent();

        state.defineType(optMyColor.get());

        ExpressionVisitor visitor = new ExpressionVisitor(state);

        parser = getParserFromString(s);

        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.branchPatterns.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.resultExpressions.size()).isEqualTo(3));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.size()).isEqualTo(1));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.MatchExpression.class, c -> assertThat(c.matchedExpressions.get(0).getType()).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseMatchExpressionBranchAndPatternSizeDontMatch1() {
        String t1 = "datatype MyColor := @R | @G | @B;";
        String t2 = "datatype MyTuple := @TV<MyColor*MyColor*MyColor*int*bool>;";
        String s = "match @TV(@R,@G,@B,1234,false) with | @TV(x,x,@B,true,y) => 1 | _ => 0";
        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optMyColor = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optMyColor).isPresent();

        state.defineType(optMyColor.get());
        parser = getParserFromString(t2);

        Optional<Type.CustomType> optMyTuple = typeVisitor.visit(parser.abstractDomainDeclaration());
        assertThat(optMyTuple).isPresent();

        state.defineType(optMyTuple.get());

        parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.PatternTypeMismatch.class);
    }

    @Test
    public void parseMatchExpressionBranchAndPatternSizeDontMatch2() {
        String t1 = "datatype MyColor := @R | @G | @B;";
        String t2 = "datatype MyTuple := @TV<MyColor*MyColor*MyColor*int*bool>;";
        String s = "match @TV(@R,@G,@B,1234,false) with | @TV(@R,x,@B,x,y) => 1 | _ => 0";
        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optMyColor = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optMyColor).isPresent();

        state.defineType(optMyColor.get());
        parser = getParserFromString(t2);

        Optional<Type.CustomType> optMyTuple = typeVisitor.visit(parser.abstractDomainDeclaration());
        assertThat(optMyTuple).isPresent();

        state.defineType(optMyTuple.get());

        parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }

    @Test
    public void parseMatchExpressionBranchAndPatternTypesDontMatch1() {
        String s = "match (true,32) with | (123,false) => 1 | _  => 0";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.PatternTypeMismatch.class);
    }

    @Test
    public void parseMatchExpressionBranchAndPatternTypesDontMatch2() {
        String s = "match (true,32) with | (213,282) => 1 | _  => 0";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.PatternTypeMismatch.class);
    }

    @Test
    public void parseMatchExpressionBranchAndPatternTypesDontMatch3() {
        String t1 = "datatype MyColor := @R | @G | @B;";
        String t2 = "datatype MyTuple := @TV<MyColor*MyColor*MyColor*int*bool>;";
        String s = "match @TV(@R,@G,@B,1234,false) with | @R => 1 | _ => 0";
        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optMyColor = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optMyColor).isPresent();

        state.defineType(optMyColor.get());
        parser = getParserFromString(t2);

        Optional<Type.CustomType> optMyTuple = typeVisitor.visit(parser.abstractDomainDeclaration());
        assertThat(optMyTuple).isPresent();

        state.defineType(optMyTuple.get());

        parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.PatternTypeMismatch.class);
    }

    @Test
    public void parseMatchExpressionResultTypesDontMatch1() {
        String s = "match true with | true => 1 | _  => false";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ExpressionResultsTypesDontMatch.class);
    }


    @Test
    public void parseMatchExpressionResultTypesDontMatch2() {
        String t1 = "datatype MyColor := @R | @G | @B;";
        String t2 = "datatype MyTuple := @TV<MyColor*MyColor*MyColor*int*bool>;";
        String s = "match true with | true => @R | _  => @TV(@R,@B,@G,23,true)";
        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optMyColor = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optMyColor).isPresent();

        state.defineType(optMyColor.get());
        parser = getParserFromString(t2);

        Optional<Type.CustomType> optMyTuple = typeVisitor.visit(parser.abstractDomainDeclaration());
        assertThat(optMyTuple).isPresent();

        state.defineType(optMyTuple.get());

        parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ExpressionResultsTypesDontMatch.class);
    }

    @Test
    public void parseMatchExpressionResultTypesDontMatch3() {
        String s = "match (true, 3) with | (y,38) => y | (false,3) => 1*4 | _ => 0";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ExpressionResultsTypesDontMatch.class);
    }

    @Test
    public void parseAppExpression1() {
        String o1 = "op add(a:int, b:int): int := a + b;";
        String s = "add(123,456)";
        ASParser parser = getParserFromString(o1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        OperationVisitor operationVisitor = new OperationVisitor(state);
        Optional<Operation> optOperation = operationVisitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();

        state.defineOperation(optOperation.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.operation).isEqualTo(optOperation.get()));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.parameters.size()).isEqualTo(0));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.size()).isEqualTo(2));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(1).getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseAppExpression2() {
        String t1 = "datatype MyColor := @R | @G | @B;";

        String o1 = "op colorToInt(a:int, b:MyColor): MyColor := match b with | @R => b | @G => b | @B => b | _ => @B;";
        String s = "colorToInt(123,@R)";
        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optMyColor = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optMyColor).isPresent();

        state.defineType(optMyColor.get());

        parser = getParserFromString(o1);
        OperationVisitor operationVisitor = new OperationVisitor(state);
        Optional<Operation> optOperation = operationVisitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();

        state.defineOperation(optOperation.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.getType()).isEqualTo(optMyColor.get()));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.operation).isEqualTo(optOperation.get()));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.parameters.size()).isEqualTo(0));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.size()).isEqualTo(2));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(1).getType()).isEqualTo(optMyColor.get()));
    }

    @Test
    public void parseAppExpression3() {
        String t1 = "datatype MyColor := @R | @G | @B;";

        String o1 = "op boolToInt(a:bool): int := 1;";
        String o2 = "op add{!c:int}(a:int, b:int): int := (a + b) * 3;";

        String s3 = "(match 1234 with | z => z | _ => boolToInt(y))";
        String s2 = "(match (x,y) with | (@G, true) => 2 | (@B, false) => " + s3 + " | _ => 3)";
        String s1 = "(match @R with | x => " + s2 + "  | _ => 4 )";
        String stupidlyComplexConstExpression = "match true with | false => 0 | y => " + s1 + "  | _ => 2";

        String s = "add{" + stupidlyComplexConstExpression + "}(123,456)";

        ASParser parser = getParserFromString(t1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        TypeVisitor typeVisitor = new TypeVisitor(state);
        Optional<Type.CustomType> optType = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();
        state.defineType(optType.get());

        parser = getParserFromString(o1);
        OperationVisitor operationVisitor = new OperationVisitor(state);
        Optional<Operation> optInc = operationVisitor.visit(parser.operationDefinition());

        assertThat(optInc).isPresent();
        state.defineOperation(optInc.get());

        parser = getParserFromString(o2);
        Optional<Operation> optAdd = operationVisitor.visit(parser.operationDefinition());

        assertThat(optAdd).isPresent();
        state.defineOperation(optAdd.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.operation).isEqualTo(optAdd.get()));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.parameters.size()).isEqualTo(1));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.parameters.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.size()).isEqualTo(2));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(1).getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseAppExpression4() {
        String o1 = "op add{!c:int}(a:int, b:int): int := (a + b) * 3;";
        String s = "add{2}(123,456)";
        ASParser parser = getParserFromString(o1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        OperationVisitor operationVisitor = new OperationVisitor(state);
        Optional<Operation> optOperation = operationVisitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();

        state.defineOperation(optOperation.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.operation).isEqualTo(optOperation.get()));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.parameters.size()).isEqualTo(1));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.parameters.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.size()).isEqualTo(2));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(1).getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseAppExpressionParameterHasToBeConst1() {
        String o1 = "op add{!c:int}(a:int, b:int): int := (a + b) * 3;";
        String s = "add{c}(123,456)";
        ASParser parser = getParserFromString(o1);

        state.defineVar("c", Type.Integer);
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        OperationVisitor operationVisitor = new OperationVisitor(state);
        Optional<Operation> optOperation = operationVisitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();

        state.defineOperation(optOperation.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ElementNotConst.class);
    }

    @Test
    public void parseConstID1() {
        String s = "MAX";
        String d = "const MAX := 12345;";


        ConstDefinitionVisitor constDefinitionVisitor = new ConstDefinitionVisitor();
        ASParser parser = getParserFromString(d);
        Optional<Expression.ConstExpression> optConstExpression = constDefinitionVisitor.visit(parser.constDefinition());

        assertThat(optConstExpression).isPresent();

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineConstant(optConstExpression.get());

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);
        parser = getParserFromString(s);

        Optional<Expression> optExpression = expressionVisitor.visit(parser.exp());

        assertThat(optExpression).isPresent();
        assertThat(optExpression.get().getType()).isEqualTo(Type.Integer);
    }

    @Test
    public void parseConstID2() {
        String d = "const MAX := 12345;";

        String o1 = "op add{!c:int}(a:int, b:int): int := (a + b) * MAX;";
        String s = "add{MAX}(123,456)";

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ConstDefinitionVisitor constDefinitionVisitor = new ConstDefinitionVisitor();
        ASParser parser = getParserFromString(d);
        Optional<Expression.ConstExpression> optConstExpression = constDefinitionVisitor.visit(parser.constDefinition());

        assertThat(optConstExpression).isPresent();
        state.defineConstant(optConstExpression.get());

        parser = getParserFromString(o1);
        OperationVisitor operationVisitor = new OperationVisitor(state);
        Optional<Operation> optOperation = operationVisitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();

        state.defineOperation(optOperation.get());

        parser = getParserFromString(s);
        ExpressionVisitor visitor = new ExpressionVisitor(state);
        Optional<Expression> exp = visitor.visit(parser.exp());

        assertThat(exp).isPresent();
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.operation).isEqualTo(optOperation.get()));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.parameters.size()).isEqualTo(1));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.parameters.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.size()).isEqualTo(2));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(0).getType()).isEqualTo(Type.Integer));
        assertThat(exp.get()).isInstanceOfSatisfying(Expression.AppExpression.class, c -> assertThat(c.expressions.get(1).getType()).isEqualTo(Type.Integer));
    }

    @Test
    public void parseConstID3() {
        String s = "MAXMAX";
        String d1 = "const MAX := 12345;";
        String d2 = "const MAXMAX := MAX + MAX;";


        VisitorState state = new VisitorState(new TestingErrorHandler());
        ConstDefinitionVisitor constDefinitionVisitor = new ConstDefinitionVisitor(state);
        ASParser parser = getParserFromString(d1);
        Optional<Expression.ConstExpression> optConstExpression = constDefinitionVisitor.visit(parser.constDefinition());

        assertThat(optConstExpression).isPresent();

        state.defineConstant(optConstExpression.get());

        parser = getParserFromString(d2);
        optConstExpression = constDefinitionVisitor.visitConstDefinition(parser.constDefinition());

        assertThat(optConstExpression).isPresent();

        state.defineConstant(optConstExpression.get());

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);
        parser = getParserFromString(s);

        Optional<Expression> optExpression = expressionVisitor.visit(parser.exp());

        assertThat(optExpression).isPresent();
        assertThat(optExpression.get().getType()).isEqualTo(Type.Integer);
    }

    @Test
    public void parseConstIDWithNonConstExp() {
        String d1 = "const MAX := 12345;";
        String d2 = "const MAXMAX := MAX + MAX + a;";


        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineVar("a", Type.Integer);
        testingErrorHandler = (TestingErrorHandler) state.errorHandler;

        ConstDefinitionVisitor constDefinitionVisitor = new ConstDefinitionVisitor(state);
        ASParser parser = getParserFromString(d1);
        Optional<Expression.ConstExpression> optConstExpression = constDefinitionVisitor.visit(parser.constDefinition());

        assertThat(optConstExpression).isPresent();

        state.defineConstant(optConstExpression.get());

        parser = getParserFromString(d2);
        optConstExpression = constDefinitionVisitor.visitConstDefinition(parser.constDefinition());

        assertThat(optConstExpression).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ElementNotConst.class);
    }

    @Test
    public void parseSumExpression1() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1, 34): + !a*3";

        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOfSatisfying(Expression.SumExpression.class, e -> {
            assertThat(e.getType()).isEqualTo(Type.Integer);
            assertThat(e.selectorFunctionInvocation.parameters().size()).isEqualTo(1);
            assertThat(e.body.getType()).isEqualTo(Type.Integer);
            assertThat(e.operation).isEqualTo(SumOperation.ADD);
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(selectorFunction);
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments.get(0)).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments.get(1)).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(34));
        });
    }

    @Test
    public void parseSumExpression3() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1, 34),(!b : int) in interval(4, !a): + !a*3*!b";

        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOfSatisfying(Expression.SumExpression.class, e -> {
            assertThat(e.getType()).isEqualTo(Type.Integer);
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations).hasSize(2);
            assertThat(e.body.getType()).isEqualTo(Type.Integer);
            assertThat(e.operation).isEqualTo(SumOperation.ADD);
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(selectorFunction);
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(1).selectorFunction).isEqualTo(selectorFunction);
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments.get(0)).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(0).arguments.get(1)).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(34));
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(1).arguments.get(0)).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(4));
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(1).arguments.get(1)).isInstanceOfSatisfying(Expression.ParVarExpression.class, i -> assertThat(i.name).isEqualTo("!a"));
        });
    }

    @Test
    public void parseSumExpressionWrongParameterType() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1, false): + !a*3";


        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parseSumExpressionWrongParameterCount1() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1): + !a*3";


        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parseSumExpressionWrongParameterCount2() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1,2,3): + !a*3";


        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parseSumExpressionWrongExpressionType1() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1,2,3): && !a*3";


        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.MismatchInList.class);
    }

    @Test
    public void parseSumExpressionWrongExpressionType2() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): * !a && false";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);


        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }


    @Test
    public void parseSumExpressionWrongExpressionType3() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): * !a";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseSumExpressionUnboundVariable1() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1,!a): * !a";

        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();

        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseSumExpressionUnboundVariable2() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1,2), (!b:int) in interval(!b): * !a";

        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();

        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseSumExpressionUnboundParameter() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1,4): * !b";

        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();

        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseSumExpressionNonConstParameter() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1,b): * !a";

        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);
        state.defineVar("b", Type.Integer);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertEquals(1, testingErrorHandler.errorObjects.size());
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ElementNotConst.class);
    }

    @Test
    public void parseSumExpressionNonConstBody() {
        String sf1 = "sel interval : int*int -> [int];";
        String s = "for (!a : int) in interval(1,4): * !a*b";

        ASParser parser = getParserFromString(sf1);
        state.defineVar("b", Type.Integer);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
    }

    @Test
    public void parseSumExpression2() {
        String sf1 = "sel interval : int*int -> [int];";
        String sf2 = "sel parity : int -> [bool];";
        String s = "for (!a : int) in interval(1, 34): + for (!b : bool) in parity(!a): * (!b) ? (2) : (4)) + !a*3";

        ASParser parser = getParserFromString(sf1);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();
        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(sf2);
        Optional<SelectorFunction> optSelectorFunctionParity = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());
        assertThat(optSelectorFunctionParity).isPresent();
        SelectorFunction selectorFunctionParity = optSelectorFunctionParity.get();
        state.defineSelectorFunction(selectorFunctionParity);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOfSatisfying(Expression.SumExpression.class, e -> {
            assertThat(e.getType()).isEqualTo(Type.Integer);
            assertThat(e.selectorFunctionInvocation.parameters().size()).isEqualTo(1);
            assertThat(e.body.getType()).isEqualTo(Type.Integer);
            assertThat(e.operation).isEqualTo(SumOperation.ADD);
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(0).selectorFunction).isEqualTo(selectorFunction);
        });
    }

    @Test
    public void parseSumExpressionCustom1() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): x : array<bool> -> store x 1 !a, [false]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOfSatisfying(Expression.SumExpression.class, se -> {
            assertThat(se.operation).isNotIn(SumOperation.ADD, SumOperation.MUL, SumOperation.AND, SumOperation.OR);
            assertThat(se.operation.startElement).isInstanceOfSatisfying(Expression.ArrayInitExpression.class, a -> assertThat(a.initializer).isEqualTo(Expression.BoolConst.FALSE));
            assertThat(se.body.getType()).isEqualTo(Type.Array.of(Type.Boolean));
            assertThat(se.getType()).isEqualTo(Type.Array.of(Type.Boolean));
        });
    }

    @Test
    public void parseSumExpressionCustomWronglyTypedStartValue() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): x : array<bool> -> store x 1 !a, [0]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseSumExpressionCustomUnboundVariable() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): y : array<bool> -> (store x 1 !a), [false]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseSumExpressionCustomUnboundParameter() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!b : bool) in interval(1,2): x : array<bool> -> (store x 1 !a), [false]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseSumExpressionCustomParameterInStartValue() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): x : array<bool> -> (store x 1 !a), [!a]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseSumExpressionCustomVariableInStartValue() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): x : array<bool> -> (store x 1 !a), [(select x 1)]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseSumExpressionCustomNested() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): x : array<bool> -> (" +
                "for (!b : bool) in interval(1,3): u : array<bool> -> (store x 1 !a), [false]" +
                "), [false]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        assertThat(optExp.get()).isInstanceOfSatisfying(Expression.SumExpression.class, se -> {
            assertThat(se.body).isInstanceOfSatisfying(Expression.SumExpression.class, se1 -> {
                assertThat(se1.operation.getType()).isEqualTo(Type.Array.of(Type.Boolean));
            });
            assertThat(se.operation.getType()).isEqualTo(Type.Array.of(Type.Boolean));
        });
    }

    @Test
    public void parseSumExpressionCustomNestedReboundVariable() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): x : array<bool> -> (" +
                "for (!b : bool) in interval(1,3): x : array<bool> -> (store x 1 !a), [false]" +
                "), [false]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }

    @Test
    public void parseSumExpressionCustomNestedReboundParameter() {
        String sf1 = "sel interval : int*int -> [bool];";
        String s = "for (!a : bool) in interval(1,2): x : array<bool> -> (" +
                "for (!a : bool) in interval(1,3): u : array<bool> -> (store x 1 !a), [false]" +
                "), [false]";

        ASParser parser = getParserFromString(sf1);

        class Provider {
            public Iterable<Boolean> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new Provider());
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        SelectorFunction selectorFunction = optSelectorFunction.get();

        state.defineSelectorFunction(selectorFunction);

        parser = getParserFromString(s);
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);

        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isNotPresent();
        assertThat(testingErrorHandler.errorObjects).hasSize(1);
        assertThat(testingErrorHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }

    private void assertExpressionAsStringIsOfType(String s, Class t) {
        ASParser parser = getParserFromString(s);

        ExpressionVisitor visitor = new ExpressionVisitor();
        Expression exp = visitor.visit(parser.exp()).get();
        assertThat(exp).isInstanceOf(t);
    }


}
