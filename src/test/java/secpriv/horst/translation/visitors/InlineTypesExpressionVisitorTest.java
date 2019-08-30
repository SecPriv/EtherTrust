package secpriv.horst.translation.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Rule;
import secpriv.horst.data.SumOperation;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.translation.layout.FlatTypeLayouter;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.ExpressionVisitor;
import secpriv.horst.visitors.ProgramVisitor;
import secpriv.horst.visitors.SExpressionExpressionVisitor;
import secpriv.horst.visitors.VisitorState;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InlineTypesExpressionVisitorTest {
    private InlineTypesExpressionVisitor inlineTypesExpressionVisitor;
    private VisitorState state;
    private ASParser parser;

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @BeforeEach
    void setUp() {
        state = new VisitorState();
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        inlineTypesExpressionVisitor = new InlineTypesExpressionVisitor(new FlatTypeLayouter());
    }

    @AfterEach
    void tearDown() {
        state = null;
        inlineTypesExpressionVisitor = null;
        parser = null;
    }

    private ASParser getParserFromFileName(String s) {
        ASLexer lexer = null;
        try {
            lexer = new ASLexer(CharStreams.fromFileName(s));
        } catch (IOException e) {
        }
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @Test
    void parseProgram1() {
        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        try {
            selectorFunctionHelper.compileSelectorFunctionsProvider(System.getProperty("user.dir") + "/grammar/WellTypedSelectorFunctionProvider.java", Collections.emptyList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        parser = getParserFromFileName(System.getProperty("user.dir") + "/grammar/test_welltyped.txt");

        ProgramVisitor visitor = new ProgramVisitor(state);
        Optional<VisitorState> state = visitor.visitAbstractProgram(parser.abstractProgram());

        assertThat(state).isPresent();

        InlineOperationsRuleVisitor inlineOperationsRuleVisitor = new InlineOperationsRuleVisitor(new ArrayList<>(state.get().getOperations().values()));

        CombiningRuleVisitor<Rule> ruleVisitor = new CombiningRuleVisitor<>(new InlineTypesRuleVisitor(inlineTypesExpressionVisitor), inlineOperationsRuleVisitor);

        for (Rule rule : state.get().getRules().values()) {
            rule.accept(ruleVisitor);
        }
    }

    @Test
    void testInlineCustomSumOperation() {
        String programCode =
                "" +
                        "sel interval : int*int -> [int];\n" +
                        "eqtype TwoBools := @N | @O<bool> | @T<bool*bool>;\n" +
                        "";

        String expressionCode = "(for (!i : int) in interval(1,4) : x : array<TwoBools> -> store x !i @T(true,true), [@O(false)])";

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

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);
        Optional<Expression> optExpression = expressionVisitor.visit(getParserFromString(expressionCode).exp());

        Optional<? extends Type> optType = state.getType("TwoBools");

        assertThat(optType).isPresent();
        assertThat(optExpression).isPresent();

        List<Expression> inlinedExpressions = optExpression.get().accept(inlineTypesExpressionVisitor);

        assertThat(inlinedExpressions).hasSize(4);
        inlinedExpressions.forEach(e -> System.out.println(e.accept(new SExpressionExpressionVisitor(0))));

        assertThat(inlinedExpressions.get(0)).isInstanceOfSatisfying(Expression.SumExpression.class, s -> {
            assertThat(s.operation).isInstanceOfSatisfying(SumOperation.InlinedCustomSumOperation.class, so -> {
                assertThat(so.boundVariable.name).endsWith("&&s" + 0);
                assertThat(so.getType()).isEqualTo(Type.Array.of(Type.Integer));
            });
        });

        assertThat(inlinedExpressions.get(1)).isInstanceOfSatisfying(Expression.SumExpression.class, s -> {
            assertThat(s.operation).isInstanceOfSatisfying(SumOperation.InlinedCustomSumOperation.class, so -> {
                assertThat(so.boundVariable.name).endsWith("&&s" + 1);
                assertThat(so.getType()).isEqualTo(Type.Array.of(Type.Boolean));
            });
        });
        assertThat(inlinedExpressions.get(2)).isInstanceOfSatisfying(Expression.SumExpression.class, s -> {
            assertThat(s.operation).isInstanceOfSatisfying(SumOperation.InlinedCustomSumOperation.class, so -> {
                assertThat(so.boundVariable.name).endsWith("&&s" + 2);
                assertThat(so.getType()).isEqualTo(Type.Array.of(Type.Boolean));
            });
        });
        assertThat(inlinedExpressions.get(3)).isInstanceOfSatisfying(Expression.SumExpression.class, s -> {
            assertThat(s.operation).isInstanceOfSatisfying(SumOperation.InlinedCustomSumOperation.class, so -> {
                assertThat(so.boundVariable.name).endsWith("&&s" + 3);
                assertThat(so.getType()).isEqualTo(Type.Array.of(Type.Boolean));
            });
        });
    }

    @Test
    void testInlineConstOfCustomType() {
        String programCode =
                "" +
                        "eqtype TwoBools := @N | @O<bool> | @T<bool*bool>;\n" +
                        "const TT := @T(true,true);\n" +
                        "";

        String expressionCode = "TT";

        VisitorState state = new VisitorState();
        ProgramVisitor programVisitor = new ProgramVisitor(state);
        programVisitor.visit(getParserFromString(programCode).abstractProgram());

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);
        Optional<Expression> optExpression = expressionVisitor.visit(getParserFromString(expressionCode).exp());

        Optional<? extends Type> optType = state.getType("TwoBools");

        assertThat(optType).isPresent();
        assertThat(optExpression).isPresent();

        List<Expression> inlinedExpressions = optExpression.get().accept(inlineTypesExpressionVisitor);

        assertThat(inlinedExpressions).hasSize(4);
        assertThat(inlinedExpressions.get(0)).isInstanceOfSatisfying(Expression.ConstExpression.class, c -> {
            assertThat(c.name).startsWith("TT");
            assertThat(c.name).endsWith("0");
            assertThat(c.value).isEqualTo(new Expression.IntConst(BigInteger.valueOf(2)));
        });
        assertThat(inlinedExpressions).hasSize(4);
        assertThat(inlinedExpressions.get(1)).isInstanceOfSatisfying(Expression.ConstExpression.class, c -> {
            assertThat(c.name).startsWith("TT");
            assertThat(c.name).endsWith("1");
            assertThat(c.value).isEqualTo(Expression.BoolConst.FALSE);
        });
        assertThat(inlinedExpressions).hasSize(4);
        assertThat(inlinedExpressions.get(2)).isInstanceOfSatisfying(Expression.ConstExpression.class, c -> {
            assertThat(c.name).startsWith("TT");
            assertThat(c.name).endsWith("2");
            assertThat(c.value).isEqualTo(Expression.BoolConst.TRUE);
        });
        assertThat(inlinedExpressions).hasSize(4);
        assertThat(inlinedExpressions.get(3)).isInstanceOfSatisfying(Expression.ConstExpression.class, c -> {
            assertThat(c.name).startsWith("TT");
            assertThat(c.name).endsWith("3");
            assertThat(c.value).isEqualTo(Expression.BoolConst.TRUE);
        });

    }
}