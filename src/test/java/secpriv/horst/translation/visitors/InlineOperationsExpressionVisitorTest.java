package secpriv.horst.translation.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.*;
import secpriv.horst.data.Expression.*;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.ProgramVisitor;
import secpriv.horst.visitors.SExpressionExpressionVisitor;
import secpriv.horst.visitors.VisitorState;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InlineOperationsExpressionVisitorTest {
    private InlineOperationsExpressionVisitor expressionVisitor;

    @AfterEach
    public void tearDown() {
        expressionVisitor = null;
    }

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }


    @Test
    public void testFlatten1() {
        Expression body = new BinaryIntExpression(new ParVarExpression(Type.Integer, "!a"), new VarExpression(Type.Integer, "b"), IntOperation.ADD);
        Operation operation = new Operation("op1", body, Arrays.asList(new ParVarExpression(Type.Integer, "!a")), Arrays.asList(new VarExpression(Type.Integer, "b")));

        expressionVisitor = new InlineOperationsExpressionVisitor(Collections.singletonList(operation));
        Expression app = expressionVisitor.visit(new AppExpression(operation, Arrays.asList(new IntConst(BigInteger.valueOf(123))), Arrays.asList(new IntConst(BigInteger.valueOf(456)))));

        assertThat(app).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.operation).isEqualTo(IntOperation.ADD));
        assertThat(app).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression1).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(123)));
        assertThat(app).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression2).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(456)));
    }

    @Test
    public void testFlatten2() {
        Expression body1 = new BinaryIntExpression(new ParVarExpression(Type.Integer, "!a"), new VarExpression(Type.Integer, "b"), IntOperation.ADD);
        Operation operation1 = new Operation("op1", body1, Arrays.asList(new ParVarExpression(Type.Integer, "!a")), Arrays.asList(new VarExpression(Type.Integer, "b")));
        Expression body2 = new AppExpression(operation1, Arrays.asList(new ParVarExpression(Type.Integer, "!a")), Arrays.asList(new VarExpression(Type.Integer, "b")));
        Operation operation2 = new Operation("op2", body2, Arrays.asList(new ParVarExpression(Type.Integer, "!a")), Arrays.asList(new VarExpression(Type.Integer, "b")));

        expressionVisitor = new InlineOperationsExpressionVisitor(Arrays.asList(operation1, operation2));
        Expression app = expressionVisitor.visit(new AppExpression(operation2, Arrays.asList(new IntConst(BigInteger.valueOf(123))), Arrays.asList(new IntConst(BigInteger.valueOf(456)))));

        assertThat(app).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.operation).isEqualTo(IntOperation.ADD));
        assertThat(app).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression1).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(123)));
        assertThat(app).isInstanceOfSatisfying(BinaryIntExpression.class, e -> assertThat(e.expression2).isInstanceOfSatisfying(IntConst.class, i -> assertThat(i.value).isEqualTo(456)));
    }

    @Test
    public void testFlatten3() {
        Expression body1 = new SumExpression(
                new CompoundSelectorFunctionInvocation(
                        Collections.singletonList(
                                new SelectorFunctionInvocation(
                                        new SelectorFunction("sel1", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Integer)),
                                        Collections.singletonList(new ParVarExpression(Type.Integer, "!o")),
                                        Collections.singletonList(new IntConst(BigInteger.valueOf(123)))
                                ))),
                new ParVarExpression(Type.Integer, "!o"),
                SumOperation.MUL);
        Operation operation1 = new Operation("op1", body1, Arrays.asList(new ParVarExpression(Type.Integer, "!a")), Arrays.asList(new VarExpression(Type.Integer, "b")));

        expressionVisitor = new InlineOperationsExpressionVisitor(Collections.singletonList(operation1));
        Expression app = expressionVisitor.visit(new AppExpression(operation1, Arrays.asList(new IntConst(BigInteger.valueOf(123))), Arrays.asList(new IntConst(BigInteger.valueOf(456)))));

        assertThat(app).isInstanceOfSatisfying(SumExpression.class, e -> {
            assertThat(e.operation).isEqualTo(SumOperation.MUL);
            assertThat(e.selectorFunctionInvocation.selectorFunctionInvocations.get(0).parameters.get(0).name).isEqualTo("op1&&!o");
            assertThat(e.body).isInstanceOfSatisfying(ParVarExpression.class, b -> assertThat(b.name).isEqualTo("op1&&!o"));
        });
    }

    @Test
    public void testRenameCustomSumExpressions() {
        String programCode =
                "" +
                        "sel interval : int*int -> [int];\n" +
                        "op op1(a :int): int := (for (!i : int) in interval(1,4) : x : int -> x + !i, 0);\n" +
                        "op op2(a :int): int := (for (!i : int) in interval(1,4) : x : int -> op1(x), 0);\n" +
                        "op op3{!a : int} (x :int): int := op2(x);\n" +
                        "";

        class Provider {
            public Iterable<BigInteger> interval(BigInteger a, BigInteger b) {
                return null;
            }
        }

        VisitorState state = new VisitorState();
        SelectorFunctionHelper compiler = new SelectorFunctionHelper();
        compiler.registerProvider(new Provider());
        state.setSelectorFunctionHelper(compiler);
        ProgramVisitor programVisitor = new ProgramVisitor(state);
        programVisitor.visit(getParserFromString(programCode).abstractProgram());


        assertThat(state.getOperations()).hasSize(3);
        expressionVisitor = new InlineOperationsExpressionVisitor(new ArrayList<>(state.getOperations().values()));

        Expression app = expressionVisitor.visit(new AppExpression(state.getOperation("op3").get(), Collections.singletonList(new IntConst(BigInteger.valueOf(123))), Arrays.asList(new IntConst(BigInteger.valueOf(456)))));

        assertThat(app).isInstanceOfSatisfying(SumExpression.class, s1 -> {
                    assertThat(s1.operation).isInstanceOfSatisfying(SumOperation.CustomSumOperation.class, cso1 -> assertThat(cso1.toString()).contains("op2"));
                    assertThat(s1.body).isInstanceOfSatisfying(SumExpression.class, s2 -> assertThat(s2.operation).isInstanceOfSatisfying(SumOperation.CustomSumOperation.class, cso2 -> assertThat(cso2.toString()).contains("op1")));

                }
        );
    }

    @Test
    public void testWrongOrderThrows() {
        Expression body1 = new BinaryIntExpression(new ParVarExpression(Type.Integer, "!a"), new VarExpression(Type.Integer, "b"), IntOperation.ADD);
        Operation operation1 = new Operation("op1", body1, Collections.singletonList(new ParVarExpression(Type.Integer, "!a")), Collections.singletonList(new VarExpression(Type.Integer, "b")));
        Expression body2 = new AppExpression(operation1, Collections.singletonList(new ParVarExpression(Type.Integer, "!a")), Collections.singletonList(new VarExpression(Type.Integer, "b")));
        Operation operation2 = new Operation("op2", body2, Collections.singletonList(new ParVarExpression(Type.Integer, "!a")), Collections.singletonList(new VarExpression(Type.Integer, "b")));

        assertThatThrownBy(() ->
                expressionVisitor = new InlineOperationsExpressionVisitor(Arrays.asList(operation2, operation1))
        ).isInstanceOf(IllegalStateException.class);
    }
}