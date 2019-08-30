package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;

import java.util.Objects;
import java.util.function.Function;

public class FunctionMappingExpressionVisitor<T> implements Expression.Visitor<T> {
    private final Function<Expression, T> function;

    public FunctionMappingExpressionVisitor(Function<Expression, T> function) {
        this.function = Objects.requireNonNull(function, "Function may not be null");
    }

    @Override
    public T visit(Expression.IntConst expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.BoolConst expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.ArrayInitExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.VarExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.FreeVarExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.ParVarExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.BinaryIntExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.BinaryBoolExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.SelectExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.StoreExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.AppExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.ConstructorAppExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.MatchExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.NegationExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.ConditionalExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.ComparisonExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.ConstExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.SumExpression expression) {
        return function.apply(expression);
    }

    @Override
    public T visit(Expression.BitvectorNegationExpression expression) {
        return function.apply(expression);
    }
}
