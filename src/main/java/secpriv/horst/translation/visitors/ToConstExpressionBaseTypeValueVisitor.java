package secpriv.horst.translation.visitors;

import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.Expression;

import java.math.BigInteger;
import java.util.Map;

public class ToConstExpressionBaseTypeValueVisitor implements BaseTypeValue.Visitor<Expression> {
    @Override
    public Expression visit(BaseTypeValue.BaseTypeIntegerValue baseTypeValue) {
        return new Expression.IntConst(baseTypeValue.value);
    }

    @Override
    public Expression visit(BaseTypeValue.BaseTypeBooleanValue baseTypeValue) {
        return baseTypeValue.value ? Expression.BoolConst.TRUE : Expression.BoolConst.FALSE;
    }

    @Override
    public Expression visit(BaseTypeValue.BaseTypeArrayValue baseTypeValue) {
        Expression expression = new Expression.ArrayInitExpression(baseTypeValue.initializer.accept(this));

        for (Map.Entry<BigInteger, BaseTypeValue> entry : baseTypeValue.stores.entrySet()) {
            expression = new Expression.StoreExpression(expression, new Expression.IntConst(entry.getKey()), entry.getValue().accept(this));
        }

        return expression;
    }
}
