package secpriv.horst.translation.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Pattern;
import secpriv.horst.internals.error.handling.ExceptionThrowingErrorHandler;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.TestBuilder;
import secpriv.horst.translation.layout.FlatTypeLayouter;
import secpriv.horst.translation.layout.TypeLayouter;
import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.ExpressionVisitor;
import secpriv.horst.visitors.FilterExpressionVisitor;
import secpriv.horst.visitors.PatternVisitor;
import secpriv.horst.visitors.VisitorState;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateMatchConditionVisitorTest {
    private GenerateMatchConditionVisitor visitor;
    private Type.CustomType typeAbsdom;
    private Type.CustomType typeAbsdomTuple;
    private Type.CustomType typeAbsdomState;
    private TypeLayouter typeLayouter = new FlatTypeLayouter();
    private VisitorState state;
    private ExpressionVisitor expressionVisitor;
    private PatternVisitor patternVisitor;
    private ASParser parser;
    private TestBuilder testBuilder;


    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @BeforeEach
    void setUp() {
        state = new VisitorState();
        state.errorHandler = new ExceptionThrowingErrorHandler();

        testBuilder = new TestBuilder(state);
        typeAbsdom = testBuilder.defineType("eqtype AbsDom := @T | @V<int>;");
        typeAbsdomTuple = testBuilder.defineType("eqtype AbsDomTuple := @Empty | @VI<AbsDom> | @VII<AbsDom*AbsDom>;");
        typeAbsdomState = testBuilder.defineType("datatype AbsDomState := @State<int*array<AbsDom>*bool>;");

        expressionVisitor = new ExpressionVisitor(state);
        patternVisitor = new PatternVisitor(state);
    }

    @AfterEach
    void tearDown() {
        typeAbsdom = null;
        typeAbsdomTuple = null;
        typeAbsdomState = null;
        testBuilder = null;
        state = null;
        expressionVisitor = null;
        patternVisitor = null;
        visitor = null;
        parser = null;
    }

    private List<Expression> layoutExpression(Expression e) {
        return e.accept(new InlineTypesExpressionVisitor(new FlatTypeLayouter()));
    }

    @Test
    public void matchConstructorAppWithValuePattern1() {
        String s = "@Empty";
        String p = "@VII (x, @T)";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomTuple, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        List<Expression> compExpressions = condition.accept(new FilterExpressionVisitor(e -> e instanceof Expression.ComparisonExpression));

        assertThat(compExpressions).hasSize(2);
        assertThat(compExpressions).hasOnlyElementsOfType(Expression.ComparisonExpression.class);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(0));
        })).hasSize(1);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(0));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(0));
        })).hasSize(1);
    }

    @Test
    public void matchConstructorAppWithValuePattern2() {
        String s = "@Empty";
        String p = "@VII (x, @V(y))";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomTuple, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition.getType()).isEqualTo(Type.Boolean);
        List<Expression> compExpressions = condition.accept(new FilterExpressionVisitor(e -> e instanceof Expression.ComparisonExpression));

        assertThat(compExpressions).hasSize(2);
        assertThat(compExpressions).hasOnlyElementsOfType(Expression.ComparisonExpression.class);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(0));
        })).hasSize(1);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(0));
        })).hasSize(1);
    }

    @Test
    public void matchConstructorAppWithWildcardPattern() {
        String s = "@Empty";
        String p = "_";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomTuple, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isEqualTo(Expression.BoolConst.TRUE);
    }

    @Test
    public void matchConstructorAppWithValuePattern3() {
        String s = "@VII (@T, @V(123))";
        String p = "@VII (x, @V(y))";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomTuple, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition.getType()).isEqualTo(Type.Boolean);
        List<Expression> compExpressions = condition.accept(new FilterExpressionVisitor(e -> e instanceof Expression.ComparisonExpression));

        assertThat(compExpressions).hasSize(2);
        assertThat(compExpressions).hasOnlyElementsOfType(Expression.ComparisonExpression.class);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
        })).hasSize(1);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
        })).hasSize(1);
    }

    @Test
    public void matchConstructorAppWithValuePattern4() {
        String s = "@VII (@V(48), @V(123))";
        String p = "@VII (x, @V(y))";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomTuple, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        List<Expression> compExpressions = condition.accept(new FilterExpressionVisitor(e -> e instanceof Expression.ComparisonExpression));

        assertThat(compExpressions).hasSize(2);
        assertThat(compExpressions).hasOnlyElementsOfType(Expression.ComparisonExpression.class);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
        })).hasSize(1);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
        })).hasSize(1);
    }

    @Test
    public void matchConstructorAppWithValuePattern5() {
        String s = "@VII (@V(48), @V(123))";
        String p = "@VII (@V(x), @V(y))";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomTuple, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        List<Expression> compExpressions = condition.accept(new FilterExpressionVisitor(e -> e instanceof Expression.ComparisonExpression));

        assertThat(compExpressions).hasSize(3);
        assertThat(compExpressions).hasOnlyElementsOfType(Expression.ComparisonExpression.class);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
        })).hasSize(1);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
        })).hasSize(2);
    }

    @Test
    public void matchConstructorAppWithValuePattern6() {
        String s = "@VI (@V(48))";
        String p = "@VII (@V(x), @V(y))";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomTuple, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        List<Expression> compExpressions = condition.accept(new FilterExpressionVisitor(e -> e instanceof Expression.ComparisonExpression));

        assertThat(compExpressions).hasSize(3);
        assertThat(compExpressions).hasOnlyElementsOfType(Expression.ComparisonExpression.class);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
        })).hasSize(1);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(0));
        })).hasSize(2);
    }

    @Test
    public void matchConstructorAppWithValuePattern7() {
        String s = "@VII (@V(12), @V(48))";
        String p = "@VII (@V(x),  @V(23))";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomTuple, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        List<Expression> compExpressions = condition.accept(new FilterExpressionVisitor(e -> e instanceof Expression.ComparisonExpression));

        assertThat(compExpressions).hasSize(4);
        assertThat(compExpressions).hasOnlyElementsOfType(Expression.ComparisonExpression.class);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(2));
        })).hasSize(1);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(1));
        })).hasSize(2);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(23));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(48));
        })).hasSize(1);
    }

    @Test
    public void matchConstructorAppWithValuePattern8() {
        String s = "@State (123, [@V(48)], true)";
        String p = "@State (y, x, true)";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), typeAbsdomState, typeLayouter);

        Expression condition = optPattern.get().accept(visitor);

        List<Expression> compExpressions = condition.accept(new FilterExpressionVisitor(e -> e instanceof Expression.ComparisonExpression));

        assertThat(compExpressions).hasSize(1);
        assertThat(compExpressions).filteredOnAssertions(e -> assertThat(e).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(0));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(0));
        })).hasSize(1);
    }

    @Test
    public void matchIntWithVar() {
        String s = "23";
        String p = "x";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Integer, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isEqualTo(Expression.BoolConst.TRUE);
    }

    @Test
    public void matchIntConstWithInt() {
        String s = "23";
        String p = "23";

        state.defineFreeVar("?x", Type.Integer);

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Integer, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(23));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(23));
        });
    }

    @Test
    public void matchFreeVarWithInt() {
        String s = "?x";
        String p = "23";

        state.defineFreeVar("?x", Type.Integer);

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Integer, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(23));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.FreeVarExpression.class, i -> assertThat(i.name).isEqualTo("?x"));
        });

    }

    @Test
    public void matchFreeVarWithNegativeInt() {
        String s = "?x";
        String p = "~23";

        state.defineFreeVar("?x", Type.Integer);

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Integer, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);
        assertThat(condition).isInstanceOfSatisfying(Expression.ComparisonExpression.class, c -> {
            assertThat(c.operation).isEqualTo(Expression.CompOperation.EQ);
            assertThat(c.expression1).isInstanceOfSatisfying(Expression.IntConst.class, i -> assertThat(i.value).isEqualTo(-23));
            assertThat(c.expression2).isInstanceOfSatisfying(Expression.FreeVarExpression.class, i -> assertThat(i.name).isEqualTo("?x"));
        });
    }

    @Test
    public void matchBoolConstWithVar1() {
        String s = "true";
        String p = "x";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Boolean, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isEqualTo(Expression.BoolConst.TRUE);
    }

    @Test
    public void matchBoolConstWithVar2() {
        String s = "false";
        String p = "x";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Boolean, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isEqualTo(Expression.BoolConst.TRUE);
    }

    @Test
    public void testMatchFreeVarWithBool1() {
        String s = "?x";
        String p = "true";

        state.defineFreeVar("?x", Type.Boolean);

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Boolean, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isInstanceOfSatisfying(Expression.FreeVarExpression.class, f -> {
            assertThat(f.name).isEqualTo("?x");
            assertThat(f.type).isEqualTo(Type.Boolean);
        });
    }

    @Test
    public void testMatchFreeVarWithBool2() {
        String s = "?x";
        String p = "false";

        state.defineFreeVar("?x", Type.Boolean);

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Boolean, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isInstanceOfSatisfying(Expression.NegationExpression.class, n -> {
            assertThat(n.expression).isInstanceOfSatisfying(Expression.FreeVarExpression.class, f -> {
                assertThat(f.name).isEqualTo("?x");
                assertThat(f.type).isEqualTo(Type.Boolean);
            });
        });
    }

    @Test
    public void matchBoolConstWithBool1() {
        String s = "true";
        String p = "true";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Boolean, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isEqualTo(Expression.BoolConst.TRUE);
    }

    @Test
    public void matchBoolConstWithBool2() {
        String s = "true";
        String p = "false";

        parser = getParserFromString(s);
        Optional<Expression> optExp = expressionVisitor.visit(parser.exp());

        assertThat(optExp).isPresent();
        parser = getParserFromString(p);

        Optional<Pattern> optPattern = patternVisitor.visit(parser.pattern());

        assertThat(optPattern).isPresent();

        visitor = new GenerateMatchConditionVisitor(layoutExpression(optExp.get()), Type.Boolean, typeLayouter);
        Expression condition = optPattern.get().accept(visitor);

        assertThat(condition).isInstanceOfSatisfying(Expression.NegationExpression.class, n -> {
            assertThat(n.expression).isEqualTo(Expression.BoolConst.TRUE);
        });
    }
}
