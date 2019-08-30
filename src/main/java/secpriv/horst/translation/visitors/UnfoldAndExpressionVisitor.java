package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.translation.visitors.FunctionMappingExpressionVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnfoldAndExpressionVisitor extends FunctionMappingExpressionVisitor<List<Expression>> {
    public UnfoldAndExpressionVisitor() {
        super(Collections::singletonList);
    }

    @Override
    public List<Expression> visit(Expression.BinaryBoolExpression expression) {
        if(expression.operation != Expression.BoolOperation.AND) {
            return Collections.singletonList(expression);
        }

        ArrayList<Expression> ret = new ArrayList<>();
        ret.addAll(expression.expression1.accept(this));
        ret.addAll(expression.expression2.accept(this));
        return Collections.unmodifiableList(ret);
    }
}
