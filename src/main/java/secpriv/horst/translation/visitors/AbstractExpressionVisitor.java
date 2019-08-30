package secpriv.horst.translation.visitors;

import secpriv.horst.data.CompoundSelectorFunctionInvocation;
import secpriv.horst.data.Expression;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class AbstractExpressionVisitor implements Expression.Visitor<Expression> {
    @Override
    public Expression visit(Expression.IntConst expression) {
        return expression;
    }

    @Override
    public Expression visit(Expression.BoolConst expression) {
        return expression;
    }

    @Override
    public Expression visit(Expression.ArrayInitExpression expression) {
        return new Expression.ArrayInitExpression(expression.initializer.accept(this));
    }

    @Override
    public Expression visit(Expression.VarExpression expression) {
        return expression;
    }

    @Override
    public Expression visit(Expression.FreeVarExpression expression) {
        return expression;
    }

    @Override
    public Expression visit(Expression.ParVarExpression expression) {
        return expression;
    }

    private Expression visitBinaryExpression(Expression.BinaryExpression expression, BiFunction<Expression, Expression, Expression> constructor) {
        Expression child1 = expression.expression1.accept(this);
        Expression child2 = expression.expression2.accept(this);

        return constructor.apply(child1, child2);
    }

    @Override
    public Expression visit(Expression.BinaryIntExpression expression) {
        return visitBinaryExpression(expression, (x, y) -> new Expression.BinaryIntExpression(x, y, expression.operation));
    }

    @Override
    public Expression visit(Expression.BinaryBoolExpression expression) {
        return visitBinaryExpression(expression, (x, y) -> new Expression.BinaryBoolExpression(x, y, expression.operation));
    }

    @Override
    public Expression visit(Expression.SelectExpression expression) {
        return visitBinaryExpression(expression, Expression.SelectExpression::new);
    }

    @Override
    public Expression visit(Expression.StoreExpression expression) {
        Expression child1 = expression.expression1.accept(this);
        Expression child2 = expression.expression2.accept(this);
        Expression child3 = expression.expression3.accept(this);

        return new Expression.StoreExpression(child1, child2, child3);
    }

    @Override
    public Expression visit(Expression.AppExpression expression) {
        List<Expression> visitedParameters = expression.parameters.stream().map(e -> e.accept(this)).collect(Collectors.toList());
        List<Expression> visitedArgumentExpressions = expression.expressions.stream().map(e -> e.accept(this)).collect(Collectors.toList());

        return new Expression.AppExpression(expression.operation, visitedParameters, visitedArgumentExpressions);
    }

    @Override
    public Expression visit(Expression.ConstructorAppExpression expression) {
        List<Expression> visitedArgumentExpressions = expression.expressions.stream().map(e -> e.accept(this)).collect(Collectors.toList());

        return new Expression.ConstructorAppExpression(expression.constructor, expression.getCustomType(), visitedArgumentExpressions);
    }

    @Override
    public Expression visit(Expression.MatchExpression expression) {
        List<Expression> visitedMatchedExpressions = expression.matchedExpressions.stream().map(e -> e.accept(this)).collect(Collectors.toList());
        List<Expression> visitedResultExpressions = expression.resultExpressions.stream().map(e -> e.accept(this)).collect(Collectors.toList());

        return new Expression.MatchExpression(expression.branchPatterns, visitedMatchedExpressions, visitedResultExpressions);
    }

    @Override
    public Expression visit(Expression.NegationExpression expression) {
        return new Expression.NegationExpression(expression.expression.accept(this));
    }

    @Override
    public Expression visit(Expression.BitvectorNegationExpression expression) {
        return new Expression.BitvectorNegationExpression(expression.expression.accept(this));
    }

    @Override
    public Expression visit(Expression.ConditionalExpression expression) {
        Expression child1 = expression.expression1.accept(this);
        Expression child2 = expression.expression2.accept(this);
        Expression child3 = expression.expression3.accept(this);

        return new Expression.ConditionalExpression(child1, child2, child3);
    }

    @Override
    public Expression visit(Expression.ComparisonExpression expression) {
        return visitBinaryExpression(expression, (x, y) -> new Expression.ComparisonExpression(x, y, expression.operation));
    }

    @Override
    public Expression visit(Expression.ConstExpression expression) {
        return new Expression.ConstExpression(expression.name, expression.value.accept(this));
    }

    @Override
    public Expression visit(Expression.SumExpression expression) {
        Expression visitedBody = expression.body.accept(this);
        //TODO maybe map for both at once
        CompoundSelectorFunctionInvocation visitedSelectorFunctionInvocation = expression.selectorFunctionInvocation
                .mapArguments(e -> e.accept(this))
                .mapParameters(e -> (Expression.ParVarExpression) e.accept(this));

        return new Expression.SumExpression(visitedSelectorFunctionInvocation, visitedBody, expression.operation);
    }
}
