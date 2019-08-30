package secpriv.horst.translation.visitors;

import secpriv.horst.data.CompoundSelectorFunctionInvocation;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Pattern;
import secpriv.horst.data.SumOperation;
import secpriv.horst.translation.layout.TypeLayouter;
import secpriv.horst.types.Type;

import java.util.*;
import java.util.stream.Collectors;

import static secpriv.horst.tools.Transposer.transpose;
import static secpriv.horst.tools.Zipper.zipList;

public class InlineTypesExpressionVisitor implements Expression.Visitor<List<Expression>> {
    private final TypeLayouter typeLayouter;
    private final Map<String, List<Expression>> varBindings;

    public InlineTypesExpressionVisitor(TypeLayouter typeLayouter) {
        this(typeLayouter, new HashMap<>());
    }

    //TODO rethink this
    public InlineTypesExpressionVisitor(TypeLayouter typeLayouter, Map<String, List<Expression>> varBindings) {
        this.typeLayouter = typeLayouter;
        this.varBindings = Collections.unmodifiableMap(varBindings);
    }

    @Override
    public List<Expression> visit(Expression.IntConst expression) {
        return Collections.singletonList(expression);
    }

    @Override
    public List<Expression> visit(Expression.BoolConst expression) {
        return Collections.singletonList(expression);
    }

    @Override
    public List<Expression> visit(Expression.ArrayInitExpression expression) {
        return expression.initializer.accept(this).stream().map(Expression.ArrayInitExpression::new).collect(Collectors.toList());
    }

    @Override
    public List<Expression> visit(Expression.VarExpression expression) {
        return varBindings.get(expression.name);
    }

    @Override
    public List<Expression> visit(Expression.FreeVarExpression expression) {
        return typeLayouter.translateFreeVars(expression);
    }

    @Override
    public List<Expression> visit(Expression.ParVarExpression expression) {
        return Collections.singletonList(expression);
    }

    @Override
    public List<Expression> visit(Expression.BinaryIntExpression expression) {
        List<Expression> l1 = expression.expression1.accept(this);
        List<Expression> l2 = expression.expression2.accept(this);

        return Collections.singletonList(new Expression.BinaryIntExpression(l1.get(0), l2.get(0), expression.operation));
    }

    @Override
    public List<Expression> visit(Expression.BinaryBoolExpression expression) {
        List<Expression> l1 = expression.expression1.accept(this);
        List<Expression> l2 = expression.expression2.accept(this);

        return Collections.singletonList(new Expression.BinaryBoolExpression(l1.get(0), l2.get(0), expression.operation));
    }

    @Override
    public List<Expression> visit(Expression.SelectExpression expression) {
        Expression index = expression.expression2.accept(this).get(0);

        List<Expression> selectee = expression.expression1.accept(this);

        return selectee.stream().map(s -> new Expression.SelectExpression(s, index)).collect(Collectors.toList());
    }

    @Override
    public List<Expression> visit(Expression.StoreExpression expression) {
        Expression index = expression.expression2.accept(this).get(0);
        List<Expression> array = expression.expression1.accept(this);
        List<Expression> element = expression.expression3.accept(this);

        List<Expression> ret = new ArrayList<>();
        Iterator<Expression> elementIterator = element.iterator();

        for (Expression arrayExpression : array) {
            ret.add(new Expression.StoreExpression(arrayExpression, index, elementIterator.next()));
        }
        return ret;
    }

    @Override
    public List<Expression> visit(Expression.AppExpression expression) {
        throw new UnsupportedOperationException("At this stage AppExpression should already be inlined!");
    }

    @Override
    public List<Expression> visit(Expression.ConstructorAppExpression expression) {
        List<List<Expression>> visitedSubExpression = expression.expressions.stream().map(e -> e.accept(this)).collect(Collectors.toList());

        return typeLayouter.layoutExpression(expression, visitedSubExpression);
    }

    @Override
    public List<Expression> visit(Expression.MatchExpression expression) {
        List<List<Expression>> flattenedMatchedExpressions = expression.matchedExpressions.stream().map(e -> e.accept(this)).collect(Collectors.toList());
        Iterator<Expression> resultExpressionIterator = expression.resultExpressions.iterator();
        List<List<Expression>> flattenedResultExpressions = new ArrayList<>();
        List<Expression> conditions = new ArrayList<>();

        for (List<Pattern> patternList : expression.branchPatterns) {
            Iterator<Type> patternTypeIterator = expression.matchedExpressions.stream().map(Expression::getType).iterator();
            Iterator<List<Expression>> flattenedMatchedExpressionsIterator = flattenedMatchedExpressions.iterator();

            Map<String, List<Expression>> bindings = new HashMap<>();
            Expression condition = Expression.BoolConst.TRUE;

            for (Pattern pattern : patternList) {
                List<Expression> flattenedMatchedExpression = flattenedMatchedExpressionsIterator.next();
                Type patternType = patternTypeIterator.next();
                GenerateBindingPatternVisitor generateBindingPatternVisitor = new GenerateBindingPatternVisitor(flattenedMatchedExpression, patternType, typeLayouter);
                GenerateMatchConditionVisitor generateMatchConditionVisitor = new GenerateMatchConditionVisitor(flattenedMatchedExpression, patternType, typeLayouter);

                bindings.putAll(pattern.accept(generateBindingPatternVisitor));
                condition = new Expression.BinaryBoolExpression(condition, pattern.accept(generateMatchConditionVisitor), Expression.BoolOperation.AND);
            }
            conditions.add(condition);

            Map<String, List<Expression>> childVarBindings = new HashMap<>(varBindings);
            childVarBindings.putAll(bindings);
            flattenedResultExpressions.add(resultExpressionIterator.next().accept(new InlineTypesExpressionVisitor(typeLayouter, childVarBindings)));
        }
        List<Expression> ret = new ArrayList<>();

        for (List<Expression> flattenedResultExpressionComponent : transpose(flattenedResultExpressions)) {
            ListIterator<Expression> conditionIterator = conditions.listIterator(conditions.size());

            ListIterator<Expression> flattenedResultExpressionComponentIterator = flattenedResultExpressionComponent.listIterator(flattenedResultExpressionComponent.size());

            Expression acc = flattenedResultExpressionComponentIterator.previous();
            // Throw away first condition. This condition has ALWAYS evaluate to true if it is reached
            conditionIterator.previous();

            while (flattenedResultExpressionComponentIterator.hasPrevious()) {
                acc = new Expression.ConditionalExpression(conditionIterator.previous(), flattenedResultExpressionComponentIterator.previous(), acc);
            }

            ret.add(acc);
        }
        return ret;
    }

    @Override
    public List<Expression> visit(Expression.NegationExpression expression) {
        return Collections.singletonList(new Expression.NegationExpression(expression.expression.accept(this).get(0)));
    }

    @Override
    public List<Expression> visit(Expression.ConditionalExpression expression) {
        Expression condition = expression.expression1.accept(this).get(0);
        List<Expression> trueExp = expression.expression2.accept(this);
        List<Expression> falseExp = expression.expression3.accept(this);

        return zipList(trueExp, falseExp, (t, f) -> new Expression.ConditionalExpression(condition, t, f));
    }

    @Override
    public List<Expression> visit(Expression.ComparisonExpression expression) {
        List<Expression> l1 = expression.expression1.accept(this);
        List<Expression> l2 = expression.expression2.accept(this);

        List<Expression> zippedEqualities = zipList(l1, l2, (a, b) -> new Expression.ComparisonExpression(a, b, expression.operation));

        switch (expression.operation) {
            case EQ:
                if (l1.size() < 2) {
                    break;
                }
                Expression.BinaryBoolExpression acc = new Expression.BinaryBoolExpression(zippedEqualities.get(0), zippedEqualities.get(1), Expression.BoolOperation.AND);
                for (int i = 2; i < l1.size(); ++i) {
                    acc = new Expression.BinaryBoolExpression(acc, zippedEqualities.get(i), Expression.BoolOperation.AND);
                }
                return Collections.singletonList(acc);
            case NEQ:
                return new Expression.NegationExpression(new Expression.ComparisonExpression(expression.expression1, expression.expression2, Expression.CompOperation.EQ)).accept(this);
            case GE:
            case GT:
            case LE:
            case LT:
                break;
            default:
                throw new RuntimeException("Unreachable code!");
        }
        return zippedEqualities;
    }

    @Override
    public List<Expression> visit(Expression.ConstExpression expression) {
        List<Expression> ret = new ArrayList<>();
        int i = 0;

        for(Expression value :expression.value.accept(this)) {
            ret.add(new Expression.ConstExpression(expression.name + "&&c" + i++, value));
        }

        return ret;
    }

    @Override
    public List<Expression> visit(Expression.SumExpression expression) {
        class InlineTypesCaseDistinctionSumOperationVisitor implements SumOperation.Visitor<List<Expression>> {
            @Override
            public List<Expression> visit(SumOperation.CustomSumOperation sumOperation) {
                List<SumOperation.InlinedCustomSumOperation> inlinedSumOperations = sumOperation.splitToFamilyOfOperations(typeLayouter, expression.body, varBindings);

                CompoundSelectorFunctionInvocation selectorFunctionInvocation = expression.selectorFunctionInvocation.mapArguments(e -> e.accept(InlineTypesExpressionVisitor.this).get(0));

                return inlinedSumOperations.stream().map(operation -> new Expression.SumExpression(selectorFunctionInvocation, operation.body, operation)).collect(Collectors.toList());
            }

            @Override
            public List<Expression> visit(SumOperation.SimpleSumOperation sumOperation) {
                return Collections.singletonList(new Expression.SumExpression(expression.selectorFunctionInvocation.mapArguments(e -> e.accept(InlineTypesExpressionVisitor.this).get(0)), expression.body.accept(InlineTypesExpressionVisitor.this).get(0), expression.operation));
            }

            @Override
            public List<Expression> visit(SumOperation.InlinedCustomSumOperation sumOperation) {
                throw new UnsupportedOperationException("At this stage we there should be no InlineCustomOperations yet!");
            }
        }

        return expression.operation.accept(new InlineTypesCaseDistinctionSumOperationVisitor());
    }

    @Override
    public List<Expression> visit(Expression.BitvectorNegationExpression expression) {
        return Collections.singletonList(new Expression.BitvectorNegationExpression(expression.expression.accept(this).get(0)));
    }

    public TypeLayouter getTypeLayouter() {
        return typeLayouter;
    }
}
