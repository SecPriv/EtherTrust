package secpriv.horst.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.translation.visitors.AbstractExpressionVisitor;
import secpriv.horst.types.Type;

public class TypeCheckingExpressionVisitor extends AbstractExpressionVisitor {
    private void expectTypeUnary(Expression.UnaryExpression expression, Type type) {
        if (!expression.getType().equals(type)) {
            throw new IllegalStateException("UnaryExpression has operand of type " + expression.expression.getType() + " expected " + type);
        }
    }

    private void expectTypesBinary(Expression.BinaryExpression expression, Type type1, Type type2) {
        if (!expression.expression1.getType().equals(type1)) {
            throw new IllegalStateException("BinaryExpression has operand 1 of type " + expression.expression1.getType() + " expected " + type1);
        }
        if (!expression.expression2.getType().equals(type2)) {
            throw new IllegalStateException("BinaryExpression has operand 2 of type " + expression.expression2.getType() + " expected " + type2);
        }
    }

    private void expectTypesTernary(Expression.TernaryExpression expression, Type type1, Type type2, Type type3) {
        if (!expression.expression1.getType().equals(type1)) {
            throw new IllegalStateException("TernaryExpression has operand 1 of type " + expression.expression1.getType() + " expected " + type1);
        }
        if (!expression.expression2.getType().equals(type2)) {
            throw new IllegalStateException("TernaryExpression has operand 2 of type " + expression.expression2.getType() + " expected " + type2);
        }
        if (!expression.expression3.getType().equals(type3)) {
            throw new IllegalStateException("TernaryExpression has operand 3 of type " + expression.expression3.getType() + " expected " + type3);
        }
    }

    private void expectSameTypeBinary(Expression.BinaryExpression expression, Type type) {
        expectTypesBinary(expression, type, type);
    }

    @Override
    public Expression visit(Expression.BinaryIntExpression expression) {
        expectSameTypeBinary(expression, Type.Integer);
        return super.visit(expression);
    }

    @Override
    public Expression visit(Expression.BinaryBoolExpression expression) {
        expectSameTypeBinary(expression, Type.Boolean);
        return super.visit(expression);
    }

    @Override
    public Expression visit(Expression.SelectExpression expression) {
        expectTypesBinary(expression, Type.Array.of(expression.getType()), Type.Integer);
        return super.visit(expression);
    }

    @Override
    public Expression visit(Expression.StoreExpression expression) {
        Type elementType = ((Type.ArrayType) expression.getType()).type;
        expectTypesTernary(expression, Type.Array.of(elementType), Type.Integer, elementType);
        return super.visit(expression);
    }

    @Override
    public Expression visit(Expression.AppExpression expression) {
        throw new UnsupportedOperationException("Type checking for AppExpressions in not yet implemented!");
    }

    @Override
    public Expression visit(Expression.ConstructorAppExpression expression) {
        throw new UnsupportedOperationException("Type checking for ConstructorAppExpressions in not yet implemented!");
    }

    @Override
    public Expression visit(Expression.MatchExpression expression) {
        throw new UnsupportedOperationException("Type checking for MatchExpressions in not yet implemented!");
    }

    @Override
    public Expression visit(Expression.NegationExpression expression) {
        expectTypeUnary(expression, Type.Boolean);
        return super.visit(expression);
    }

    @Override
    public Expression visit(Expression.ConditionalExpression expression) {
        expectTypesTernary(expression, Type.Boolean, expression.getType(), expression.getType());
        return super.visit(expression);
    }

    @Override
    public Expression visit(Expression.ComparisonExpression expression) {
        expectSameTypeBinary(expression, expression.expression1.getType());

        if (expression.operation != Expression.CompOperation.EQ && expression.operation != Expression.CompOperation.NEQ) {
            expectSameTypeBinary(expression, Type.Integer);
        }

        return super.visit(expression);
    }

    @Override
    public Expression visit(Expression.ConstExpression expression) {
        return super.visit(expression);
    }

    @Override
    public Expression visit(Expression.SumExpression expression) {
        throw new UnsupportedOperationException("Type checking for MatchExpressions in not yet implemented!");
    }

    @Override
    public Expression visit(Expression.BitvectorNegationExpression expression) {
        expectTypeUnary(expression, Type.Integer);
        return super.visit(expression);
    }
}
