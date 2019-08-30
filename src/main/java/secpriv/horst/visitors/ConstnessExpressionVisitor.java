package secpriv.horst.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Pattern;

import java.util.*;

public class ConstnessExpressionVisitor implements Expression.Visitor<Boolean> {
    private final Set<Expression.VarExpression> variableDefinitions;

    public ConstnessExpressionVisitor() {
        this(new HashSet<>());
    }

    public ConstnessExpressionVisitor(Set<Expression.VarExpression> variableDefinitions) {
        this.variableDefinitions = Collections.unmodifiableSet(Objects.requireNonNull(variableDefinitions, "VariableDefinitions may not be null!"));
    }

    @Override
    public Boolean visit(Expression.IntConst expression) {
        return true;
    }

    @Override
    public Boolean visit(Expression.BoolConst expression) {
        return true;
    }

    @Override
    public Boolean visit(Expression.ArrayInitExpression expression) {
        return true;
    }

    @Override
    public Boolean visit(Expression.VarExpression expression) {
        return variableDefinitions.contains(expression);
    }

    @Override
    public Boolean visit(Expression.FreeVarExpression expression) {
        return false;
    }

    @Override
    public Boolean visit(Expression.ParVarExpression expression) {
        return true;
    }

    private Boolean visitBinaryExpression(Expression.BinaryExpression expression) {
        return expression.expression1.accept(this) && expression.expression2.accept(this);
    }

    @Override
    public Boolean visit(Expression.BinaryIntExpression expression) {
        return visitBinaryExpression(expression);
    }

    @Override
    public Boolean visit(Expression.BinaryBoolExpression expression) {
        return visitBinaryExpression(expression);
    }

    @Override
    public Boolean visit(Expression.SelectExpression expression) {
        return visitBinaryExpression(expression);
    }

    private Boolean visitTernaryExpression(Expression.TernaryExpression expression) {
        return expression.expression1.accept(this) && expression.expression2.accept(this) && expression.expression3.accept(this);
    }

    @Override
    public Boolean visit(Expression.StoreExpression expression) {
        return visitTernaryExpression(expression);
    }

    @Override
    public Boolean visit(Expression.ConditionalExpression expression) {
        return visitTernaryExpression(expression);
    }

    @Override
    public Boolean visit(Expression.ComparisonExpression expression) {
        return visitBinaryExpression(expression);
    }

    @Override
    public Boolean visit(Expression.ConstExpression expression) {
        return true;
    }

    @Override
    public Boolean visit(Expression.SumExpression expression) {
        return expression.body.accept(this);
    }

    private Boolean visitVariadicExpression(Expression.VariadicExpression expression) {
        return expression.expressions.stream().allMatch(e -> e.accept(this));
    }

    @Override
    public Boolean visit(Expression.AppExpression expression) {
        return visitVariadicExpression(expression);
    }

    @Override
    public Boolean visit(Expression.ConstructorAppExpression expression) {
        return visitVariadicExpression(expression);
    }

    @Override
    public Boolean visit(Expression.MatchExpression expression) {
        Iterator<Expression> resultIterator = expression.resultExpressions.iterator();
        for (List<Pattern> patterns : expression.branchPatterns) {
            Iterator<Expression> iterator = expression.matchedExpressions.iterator();
            Set<Expression.VarExpression> branchVarDefinitions = new HashSet<>(variableDefinitions);
            for (Pattern pattern : patterns) {
                CollectVariablesPatternVisitor visitor = new CollectVariablesPatternVisitor(iterator.next().getType());
                branchVarDefinitions.addAll(pattern.accept(visitor));
            }

            if (!resultIterator.next().accept(new ConstnessExpressionVisitor(branchVarDefinitions))) {
                return false;
            }
        }
        return true;
    }

    private Boolean visitUnaryExpression(Expression.UnaryExpression expression) {
        return expression.expression.accept(this);
    }

    @Override
    public Boolean visit(Expression.NegationExpression expression) {
        return visitUnaryExpression(expression);
    }

    @Override
    public Boolean visit(Expression.BitvectorNegationExpression expression) {
        return visitUnaryExpression(expression);
    }
}
