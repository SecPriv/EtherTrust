package secpriv.horst.translation.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.Expression;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.SelectorFunctionInvoker;
import secpriv.horst.internals.error.handling.ExceptionThrowingErrorHandler;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.TestBuilder;
import secpriv.horst.translation.layout.FlatTypeLayouterWithBoolean;
import secpriv.horst.visitors.ExpressionVisitor;
import secpriv.horst.visitors.FilterExpressionVisitor;
import secpriv.horst.visitors.SExpressionExpressionVisitor;
import secpriv.horst.visitors.VisitorState;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class InstantiateParametersExpressionVisitorTest {
    private SelectorFunctionHelper selectorFunctionHelper = null;
    private VisitorState state = null;
    private ExpressionVisitor expressionVisitor = null;
    private TestBuilder testBuilder;


    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new ASParser(tokens);
    }

    private Optional<Expression> getExpressionFromString(String s) {
        return expressionVisitor.visit(getParserFromString(s).exp());
    }

    public class Provider {
        public Iterable<BigInteger> oneToFive() {
            return Stream.of(1, 2, 3, 4, 5).map(BigInteger::valueOf).collect(Collectors.toList());
        }

        public Iterable<BigInteger> interval(BigInteger i) {
            List<BigInteger> ret = new ArrayList<>();

            for (int j = 0; j < i.intValue(); ++j) {
                ret.add(BigInteger.valueOf(j));
            }

            return ret;
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

        testBuilder.defineSelectorFunction("sel interval: int -> [int];");
        testBuilder.defineSelectorFunction("sel oneToFive: unit -> [int];");
        testBuilder.defineSelectorFunction("sel allBools: unit -> [bool];");
        testBuilder.defineSelectorFunction("sel intPlusParity: int -> [int*bool];");

        expressionVisitor = new ExpressionVisitor(state);
    }

    @AfterEach
    void tearDown() {
        state = null;
        testBuilder = null;
        selectorFunctionHelper = null;
        expressionVisitor = null;
    }

    @Test
    void testMultiplicationSumExpression() {
        String s = "for (!i: int) in oneToFive() : * !i";

        Optional<Expression> optExp = getExpressionFromString(s);

        assertThat(optExp).isPresent();

        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));

        Expression expression = optExp.get().accept(instantiateParametersExpressionVisitor);

        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        BaseTypeValue value = expression.accept(evaluateExpressionVisitor);

        assertThat(value).isInstanceOfSatisfying(BaseTypeValue.BaseTypeIntegerValue.class, i -> {
            assertThat(i.value).isEqualTo(120);
        });
    }

    @Test
    void testAdditionSumExpression() {
        String s = "for (!i: int) in oneToFive() : + !i";

        Optional<Expression> optExp = getExpressionFromString(s);

        assertThat(optExp).isPresent();

        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));

        Expression expression = optExp.get().accept(instantiateParametersExpressionVisitor);

        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        BaseTypeValue value = expression.accept(evaluateExpressionVisitor);

        assertThat(value).isInstanceOfSatisfying(BaseTypeValue.BaseTypeIntegerValue.class, i -> {
            assertThat(i.value).isEqualTo(15);
        });
    }

    @Test
    void testConjunctionSumExpression() {
        String s = "for (!b: bool) in allBools() : && !b";

        Optional<Expression> optExp = getExpressionFromString(s);

        assertThat(optExp).isPresent();

        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));

        Expression expression = optExp.get().accept(instantiateParametersExpressionVisitor);

        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        BaseTypeValue value = expression.accept(evaluateExpressionVisitor);

        assertThat(value).isInstanceOfSatisfying(BaseTypeValue.BaseTypeBooleanValue.class, b -> {
            assertThat(b.value).isFalse();
        });
    }

    @Test
    void testDisjunctionSumExpression() {
        String s = "for (!b: bool) in allBools() : || !b";

        Optional<Expression> optExp = getExpressionFromString(s);

        assertThat(optExp).isPresent();

        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));

        Expression expression = optExp.get().accept(instantiateParametersExpressionVisitor);

        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        BaseTypeValue value = expression.accept(evaluateExpressionVisitor);

        assertThat(value).isInstanceOfSatisfying(BaseTypeValue.BaseTypeBooleanValue.class, b -> {
            assertThat(b.value).isTrue();
        });
    }

    @Test
    void testTupleSumExpression() {
        String s = "for (!i: int, !b: bool) in intPlusParity(100) : && (~ ((!i mod 2) = 0)) || !b";

        Optional<Expression> optExp = getExpressionFromString(s);

        assertThat(optExp).isPresent();

        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));

        Expression expression = optExp.get().accept(instantiateParametersExpressionVisitor);

        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        BaseTypeValue value = expression.accept(evaluateExpressionVisitor);

        assertThat(value).isInstanceOfSatisfying(BaseTypeValue.BaseTypeBooleanValue.class, b -> {
            assertThat(b.value).isTrue();
        });
    }

    @Test
    void testMatchInCustomSumExpression1() {
        testBuilder.defineType("datatype AbsDom := @T | @V<int>;");

        String s = "for (!i:int) in interval(5) : x : AbsDom -> match x with | @V(k) => (k<4) ? (@V(k+1)) :(@T) | _ => @T, @V(0)";
        Expression e = testBuilder.parseExpression(s);

        InlineTypesExpressionVisitor inlineTypesExpressionVisitor = new InlineTypesExpressionVisitor(new FlatTypeLayouterWithBoolean());
        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));

        List<Expression> inlinedExpressions = e.accept(inlineTypesExpressionVisitor);

        assertThat(inlinedExpressions).hasSize(2);

        FilterExpressionVisitor filterExpressionVisitor = new FilterExpressionVisitor(p -> p instanceof Expression.VarExpression);

        List<Expression> instantiatedExpressions = new ArrayList<>();

        for (Expression ee : inlinedExpressions) {
            Expression eee = ee.accept(instantiateParametersExpressionVisitor);
            assertThat(eee.accept(filterExpressionVisitor)).isEmpty();
            System.out.println(eee.accept(new SExpressionExpressionVisitor(0)));
            instantiatedExpressions.add(eee);
        }


        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        List<Expression> inlinedExpectedResult = testBuilder.parseExpression("@T").accept(inlineTypesExpressionVisitor);//.stream().map(e -> e.accept(inlineTypesExpressionVisitor)).collect(Collectors.toList());

        assertThat(inlinedExpectedResult).hasSize(inlinedExpressions.size());

        for (int i = 0; i < inlinedExpressions.size(); ++i) {
            assertThat(instantiatedExpressions.get(i).accept(evaluateExpressionVisitor)).isEqualTo(inlinedExpectedResult.get(i).accept(evaluateExpressionVisitor));
        }
    }

    @Test
    void testMatchInCustomSumExpression2() {
        testBuilder.defineType("datatype AbsDom := @T | @V<int>;");

        for (int i = 0; i < 10; ++i) {
            String s = "for (!i:int) in interval(" + i + ") : x : AbsDom -> match x with | @V(k) => (k<4) ? (@V(k+1)) :(@T) | _ => @T, @V(0)";
            Expression e = testBuilder.parseExpression(s);

            InlineTypesExpressionVisitor inlineTypesExpressionVisitor = new InlineTypesExpressionVisitor(new FlatTypeLayouterWithBoolean());
            InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));

            List<Expression> inlinedExpressions = e.accept(inlineTypesExpressionVisitor);

            assertThat(inlinedExpressions).hasSize(2);

            FilterExpressionVisitor filterExpressionVisitor = new FilterExpressionVisitor(p -> p instanceof Expression.VarExpression);

            List<Expression> instantiatedExpressions = new ArrayList<>();

            for (Expression ee : inlinedExpressions) {
                Expression eee = ee.accept(instantiateParametersExpressionVisitor);
                assertThat(eee.accept(filterExpressionVisitor)).isEmpty();
                instantiatedExpressions.add(eee);
            }

            EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

            String resultExpressionString = i <= 4 ? "@V(" + i + ")" : "@T";

            List<Expression> inlinedExpectedResult = testBuilder.parseExpression(resultExpressionString).accept(inlineTypesExpressionVisitor);//.stream().map(e -> e.accept(inlineTypesExpressionVisitor)).collect(Collectors.toList());

            assertThat(inlinedExpectedResult).hasSize(inlinedExpressions.size());

            for (int j = 0; j < inlinedExpressions.size(); ++j) {
                assertThat(instantiatedExpressions.get(j).accept(evaluateExpressionVisitor)).isEqualTo(inlinedExpectedResult.get(j).accept(evaluateExpressionVisitor));
            }
        }
    }


    @Test
    void testMatchInCustomSumExpression3() {
        testBuilder.defineType("datatype AbsDom := @T | @V<int>;");

        testBuilder.defineOperation("op concreteOrDefault(x: AbsDom, y:int) : int := match x with | @V(k) => k | _ => y;");
        state.errorHandler = new ExceptionThrowingErrorHandler();


        for (int p = 0; p < 8; ++p) {
            String s1 = "for (!i:int) in interval(!a+1) : y : AbsDom -> match (y,x) with | (@V(j),@V(l)) => (!i < 3) ? (@V(!a+!i+k+j+l)) :(@T) | _ => @T, @V(3)";
            String s2 = "for (!a:int) in interval(" + p + ") : x : AbsDom -> match x with | @V(k) => @V(k + concreteOrDefault(" + s1 + ",~1000-k)) |  _ => @T, @V(5)";

            int result;
            {
                int k = 5;
                result = k;
                for (int a = 0; a < p; ++a) {
                    int j = 3;
                    int innerResult = j;
                    for (int i = 0; i < (a + 1); ++i) {
                        int l = k;
                        innerResult = a + i + k + j + l;
                        j = innerResult;
                    }
                    result = innerResult + k;
                    k = result;
                }
            }

            Expression e = testBuilder.parseExpression(s2);

            InlineOperationsExpressionVisitor inlineOperationsExpressionVisitor = new InlineOperationsExpressionVisitor(new ArrayList<>(state.getOperations().values()));
            InlineTypesExpressionVisitor inlineTypesExpressionVisitor = new InlineTypesExpressionVisitor(new FlatTypeLayouterWithBoolean());
            InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));
            EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

            List<Expression> inlinedExpressions = e.accept(inlineOperationsExpressionVisitor).accept(inlineTypesExpressionVisitor);

            assertThat(inlinedExpressions).hasSize(2);

            List<Expression> instantiatedExpressions = new ArrayList<>();

            for (Expression ee : inlinedExpressions) {
                Expression eee = ee.accept(instantiateParametersExpressionVisitor);
                instantiatedExpressions.add(eee);
            }

            String resultExpressionString = p < 4 ? "@V(" + result + ")" : "@V(~1000)";

            List<Expression> inlinedExpectedResult = testBuilder.parseExpression(resultExpressionString).accept(inlineTypesExpressionVisitor);

            assertThat(inlinedExpectedResult).hasSize(inlinedExpressions.size());

            for (int i = 0; i < inlinedExpressions.size(); ++i) {
                assertThat(instantiatedExpressions.get(i).accept(evaluateExpressionVisitor)).isEqualTo(inlinedExpectedResult.get(i).accept(evaluateExpressionVisitor));
            }
        }
    }

    @Test
    void testCustomAdd() {
        final int n = 100;
        String s = "for (!i:int) in interval(" + n + "): x : int -> x + !i, 0";

        InlineTypesExpressionVisitor inlineTypesExpressionVisitor = new InlineTypesExpressionVisitor(new FlatTypeLayouterWithBoolean());
        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));
        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        Expression e = testBuilder.parseExpression(s);

        BaseTypeValue result = e.accept(instantiateParametersExpressionVisitor).accept(evaluateExpressionVisitor);

        assertThat(result).isInstanceOfSatisfying(BaseTypeValue.BaseTypeIntegerValue.class, v -> assertThat(v.value).isEqualTo(BigInteger.valueOf(((n) * (n - 1)) / 2)));
        assertThat(result).isEqualTo(e.accept(inlineTypesExpressionVisitor).get(0).accept(instantiateParametersExpressionVisitor).accept(evaluateExpressionVisitor));
    }

    @Test
    void testSumExpressionInStartElementOfInlinedCustomSumExpression() {
        testBuilder.defineType("datatype AbsDom := @T | @V<int>;");
        testBuilder.defineOperation("op absadd(x: AbsDom, y:AbsDom) : AbsDom := match (x,y) with | (@V(a),@V(b)) => @V(a+b) | _ => @T;");
        testBuilder.defineOperation("op concreteOrDefault(x: AbsDom, y:int) : int := match x with | @V(k) => k | _ => y;");

        String s = "concreteOrDefault(for (!i:int) in interval(8): x : AbsDom -> absadd(x, @V(!i)), @V((for (!j:int) in interval(4) : + !j)), ~1)";

        InlineTypesExpressionVisitor inlineTypesExpressionVisitor = new InlineTypesExpressionVisitor(new FlatTypeLayouterWithBoolean());
        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));
        InlineOperationsExpressionVisitor inlineOperationsExpressionVisitor = new InlineOperationsExpressionVisitor(new ArrayList<>(state.getOperations().values()));
        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        Expression e = testBuilder.parseExpression(s);
        List<Expression> inlinedExpressions = e.accept(inlineOperationsExpressionVisitor).accept(inlineTypesExpressionVisitor).stream().map(ee -> ee.accept(instantiateParametersExpressionVisitor)).collect(Collectors.toList());


        assertThat(inlinedExpressions).hasSize(1);
        assertThat(inlinedExpressions.get(0).accept(evaluateExpressionVisitor)).isEqualTo(BaseTypeValue.fromBigInteger(BigInteger.valueOf(28+6)));
    }

    @Test
    void testSumExpressionInStartElementOfCustomSumExpression() {
        String s = "for (!i:int) in interval(8): x : int -> x + !i, (for (!j:int) in interval(4) : + !j )";
        InstantiateParametersExpressionVisitor instantiateParametersExpressionVisitor = new InstantiateParametersExpressionVisitor(Collections.emptyMap(), new SelectorFunctionInvoker(selectorFunctionHelper));

        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

        Expression e = testBuilder.parseExpression(s);
        e = e.accept(instantiateParametersExpressionVisitor);

        BaseTypeValue result = e.accept(evaluateExpressionVisitor);

        assertThat(result).isEqualTo(BaseTypeValue.fromBigInteger(BigInteger.valueOf(28+6)));
    }

}
