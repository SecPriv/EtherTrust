package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;

import java.util.Collections;
import java.util.Map;

public class InlineEqualitiesExpressionVisitor extends AbstractExpressionVisitor {
    private final Map<Expression.FreeVarExpression, Expression> inlines;

    public InlineEqualitiesExpressionVisitor(Map<Expression.FreeVarExpression, Expression> inlines) {
        this.inlines = Collections.unmodifiableMap(inlines);
    }

    @Override
    public Expression visit(Expression.FreeVarExpression expression) {
        return inlines.getOrDefault(expression, expression);

    }
}
