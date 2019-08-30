package secpriv.horst.translation.visitors;

import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.Expression;
import secpriv.horst.internals.SelectorFunctionInvoker;
import secpriv.horst.visitors.ConstnessExpressionVisitor;

import java.util.Map;
import java.util.Objects;

public class InstantiateParametersExpressionVisitor extends AbstractExpressionVisitor {
    private final Map<String, BaseTypeValue> parameterMap;
    private final SelectorFunctionInvoker selectorFunctionInvoker;

    private final ConstnessExpressionVisitor constnessExpressionVisitor = new ConstnessExpressionVisitor();
    private final EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();

    public InstantiateParametersExpressionVisitor(Map<String, BaseTypeValue> parameterMap, SelectorFunctionInvoker selectorFunctionInvoker) {
        this.parameterMap = Objects.requireNonNull(parameterMap, "ParameterMap may not be null!");
        this.selectorFunctionInvoker = Objects.requireNonNull(selectorFunctionInvoker, "SelectorFunctionInvoker may not be null!");
    }

    // This override is not necessary from a semantic perspective but can reduce the time need to compile
    // match expressions immensely
    @Override
    public Expression visit(Expression.ConditionalExpression expression) {
        Expression condition = expression.expression1.accept(this);

        if(condition.accept(constnessExpressionVisitor)) {
            BaseTypeValue.BaseTypeBooleanValue b = (BaseTypeValue.BaseTypeBooleanValue) condition.accept(evaluateExpressionVisitor);
            if(b.value) {
                return expression.expression2.accept(this);
            } else {
                return expression.expression3.accept(this);
            }
        } else {
            return new Expression.ConditionalExpression(condition, expression.expression2.accept(this), expression.expression3.accept(this));
        }
    }

    @Override
    public Expression visit(Expression.ParVarExpression expression) {
        return parameterMap.get(expression.name).accept(new ToConstExpressionBaseTypeValueVisitor());
    }

    @Override
    public Expression visit(Expression.AppExpression expression) {
        throw new UnsupportedOperationException("AppExpressions should be eliminated at this point!");
    }

    @Override
    public Expression visit(Expression.ConstructorAppExpression expression) {
        throw new UnsupportedOperationException("ConstructorExpressions should be eliminated at this point!");
    }

    @Override
    public Expression visit(Expression.MatchExpression expression) {
        throw new UnsupportedOperationException("MatchExpressions should be eliminated at this point!");
    }

    @Override
    public Expression visit(Expression.SumExpression expression) {
        return expression.operation.apply(expression.selectorFunctionInvocation, selectorFunctionInvoker, parameterMap, expression.body);
    }
}
