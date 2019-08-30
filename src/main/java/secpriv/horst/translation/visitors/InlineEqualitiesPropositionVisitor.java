package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;

import java.util.Map;

public class InlineEqualitiesPropositionVisitor extends ExpressionMappingPropositionVisitor {

    public InlineEqualitiesPropositionVisitor(Map<Expression.FreeVarExpression, Expression> inlines) {
        super(new InlineEqualitiesExpressionVisitor(inlines));
    }
}
