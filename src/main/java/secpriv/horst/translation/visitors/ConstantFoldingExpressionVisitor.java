package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.visitors.ConstnessExpressionVisitor;

import java.math.BigInteger;

public class ConstantFoldingExpressionVisitor implements Expression.Visitor<Expression> {

    private final EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();
    private final ConstnessExpressionVisitor constnessExpressionVisitor = new ConstnessExpressionVisitor();
    private final ToConstExpressionBaseTypeValueVisitor toConstExpressionVisitor = new ToConstExpressionBaseTypeValueVisitor();

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
        throw new UnsupportedOperationException("Expression should have already been inlined/translated.");
    }

    @Override
    public Expression visit(Expression.FreeVarExpression expression) {
        return expression;
    }

    @Override
    public Expression visit(Expression.ParVarExpression expression) {
        throw new UnsupportedOperationException("Expression should have already been instantiated.");
    }

    private Expression evaluateToConstIfPossible(Expression expression) {
        if (expression.accept(constnessExpressionVisitor)) {
            try {
                return expression.accept(evaluateExpressionVisitor).accept(toConstExpressionVisitor);
            } catch (ArithmeticException e) {
                // if a undefined (w.r.t to BigInteger semantics) arithmetic operation occurs, we return the unmodified
                // expression, to let z3 handle it
                return expression;
            }
        }
        return expression;
    }

    private Expression optimizeBinaryIntExpression(Expression child1, Expression child2, Expression.IntOperation operation) throws ArithmeticException {
        boolean isConst1 = child1 instanceof Expression.IntConst;
        boolean isConst2 = child2 instanceof Expression.IntConst;

        if (isConst1 && isConst2) {
            return evaluateToConstIfPossible(new Expression.BinaryIntExpression(child1, child2, operation));
        } else if (!isConst1 && !isConst2) {
            return new Expression.BinaryIntExpression(child1, child2, operation);
        }

        Expression.IntConst constExp = (Expression.IntConst) (isConst1 ? child1 : child2);
        Expression nonConstExp = isConst2 ? child1 : child2;

        boolean isConstEqualToOne = constExp.value.equals(BigInteger.ONE);

        if (operation == Expression.IntOperation.ADD) {
            if (constExp.value.equals(BigInteger.ZERO)) {
                return nonConstExp;
            }
        } else if (operation == Expression.IntOperation.MUL) {
            if (constExp.value.equals(BigInteger.ZERO)) {
                return constExp;
            }
            if (isConstEqualToOne) {
                return nonConstExp;
            }
        } else if (operation == Expression.IntOperation.DIV) {
            if (isConst2 && isConstEqualToOne) {
                return nonConstExp;
            }
        } else if (operation == Expression.IntOperation.MOD) {
            if (isConst2 && isConstEqualToOne) {
                return new Expression.IntConst(BigInteger.ZERO);
            }
        }
        return new Expression.BinaryIntExpression(child1, child2, operation);
    }


    @Override
    public Expression visit(Expression.BinaryIntExpression expression) {

        Expression child1 = expression.expression1.accept(this);
        Expression child2 = expression.expression2.accept(this);

        Expression result = optimizeBinaryIntExpression(child1, child2, expression.operation);

        // There is a chance the previous steps reduced the expression to all constants, so check again
        return evaluateToConstIfPossible(result);
    }

    private Expression optimizeBinaryBoolExpression(Expression child1, Expression child2, Expression.BoolOperation operation) {
        boolean isConst1 = child1 instanceof Expression.BoolConst;
        boolean isConst2 = child2 instanceof Expression.BoolConst;

        if (isConst1 && isConst2) {
            return evaluateToConstIfPossible(new Expression.BinaryBoolExpression(child1, child2, operation));
        } else if (!isConst1 && !isConst2) {
            return new Expression.BinaryBoolExpression(child1, child2, operation);
        }

        Expression.BoolConst constExp = (Expression.BoolConst) (isConst1 ? child1 : child2);
        Expression nonConstExp = isConst2 ? child1 : child2;

        if (operation == Expression.BoolOperation.OR) {
            if (constExp.value) {
                return constExp;
            } else {
                return nonConstExp;
            }
        } else if (operation == Expression.BoolOperation.AND) {
            if (!constExp.value) {
                return constExp;
            } else {
                return nonConstExp;
            }
        }
        return new Expression.BinaryBoolExpression(child1, child2, operation);
    }

    @Override
    public Expression visit(Expression.BinaryBoolExpression expression) {

        Expression child1 = expression.expression1.accept(this);
        Expression child2 = expression.expression2.accept(this);

        Expression result = optimizeBinaryBoolExpression(child1, child2, expression.operation);

        // There is a chance the previous steps reduced the expression to all constants, so check again
        return evaluateToConstIfPossible(result);
    }

    @Override
    public Expression visit(Expression.SelectExpression expression) {

        Expression child1 = expression.expression1.accept(this);
        Expression child2 = expression.expression2.accept(this);

        Expression result = new Expression.SelectExpression(child1, child2);

        // There is a chance the previous steps reduced the expression to all constants, so check again
        return evaluateToConstIfPossible(result);
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
        throw new UnsupportedOperationException("AppExpression should be eliminated at this point!");
    }

    @Override
    public Expression visit(Expression.ConstructorAppExpression expression) {
        throw new UnsupportedOperationException("ConstructorAppExpression should be eliminated at this point!");
    }

    @Override
    public Expression visit(Expression.MatchExpression expression) {
        throw new UnsupportedOperationException("MatchExpression should be eliminated at this point!");
    }

    @Override
    public Expression visit(Expression.NegationExpression expression) {

        Expression child = expression.expression.accept(this);

        if (child instanceof Expression.NegationExpression) {
            return ((Expression.NegationExpression) child).expression;
        }
        Expression result = new Expression.NegationExpression(child);

        // There is a chance the previous steps reduced the expression to all constants, so check again
        return evaluateToConstIfPossible(result);
    }

    @Override
    public Expression visit(Expression.ConditionalExpression expression) {

        Expression child1 = expression.expression1.accept(this);
        Expression child2 = expression.expression2.accept(this);
        Expression child3 = expression.expression3.accept(this);

        return optimizeConditionalExpression(child1, child2, child3);
    }

    private Expression optimizeConditionalExpression(Expression child1, Expression child2, Expression child3) {
        if (child1 instanceof Expression.BoolConst) {
            Expression.BoolConst boolConst = (Expression.BoolConst) child1;
            if (boolConst.value) {
                return child2;
            } else {
                return child3;
            }
        } else {
            if (child2.accept(constnessExpressionVisitor) && child3.accept(constnessExpressionVisitor)) {
                if (child2.equals(child3)) {
                    return child2;
                }
                if (child2 instanceof Expression.BoolConst && child3 instanceof Expression.BoolConst) {
                    Expression.BoolConst child2BoolConst = (Expression.BoolConst) child2;
                    Expression.BoolConst child3BoolConst = (Expression.BoolConst) child3;
                    if (child2BoolConst.value && !child3BoolConst.value) {
                        return child1;
                    }
                    if (!child2BoolConst.value && child3BoolConst.value) {
                        if(child1 instanceof Expression.NegationExpression) {
                            return ((Expression.NegationExpression) child1).expression;
                        } else {
                            return new Expression.NegationExpression(child1);
                        }
                    }
                }
            }
        }
        Expression result = new Expression.ConditionalExpression(child1, child2, child3);

        // There is a chance the previous steps reduced the expression to all constants, so check again
        return evaluateToConstIfPossible(result);
    }

    @Override
    public Expression visit(Expression.ComparisonExpression expression) {

        Expression child1 = expression.expression1.accept(this);
        Expression child2 = expression.expression2.accept(this);

        Expression result = new Expression.ComparisonExpression(child1, child2, expression.operation);

        // There is a chance the previous steps reduced the expression to all constants, so check again
        return evaluateToConstIfPossible(result);
    }

    @Override
    public Expression visit(Expression.ConstExpression expression) {

        Expression child = expression.value.accept(this);
        Expression result = new Expression.ConstExpression(expression.name, child);

        // There is a chance the previous steps reduced the expression to all constants, so check again
        return evaluateToConstIfPossible(result);
    }

    @Override
    public Expression visit(Expression.SumExpression expression) {
        throw new UnsupportedOperationException("SumExpression should be eliminated at this point!");
    }

    @Override
    public Expression visit(Expression.BitvectorNegationExpression expression) {
        return evaluateToConstIfPossible(expression);
    }

}
