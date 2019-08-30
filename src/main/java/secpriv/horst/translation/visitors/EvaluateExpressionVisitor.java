package secpriv.horst.translation.visitors;

import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.Expression;
import secpriv.horst.evm.EvmBitVectorArithmeticEvaluator;
import secpriv.horst.translation.BitVectorArithmeticEvaluator;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

public class EvaluateExpressionVisitor implements Expression.Visitor<BaseTypeValue> {
    private final Map<String, BaseTypeValue> parameterMap;
    private final BitVectorArithmeticEvaluator bitVectorArithmeticEvaluator = new EvmBitVectorArithmeticEvaluator();

    public EvaluateExpressionVisitor() {
        this(Collections.emptyMap());
    }

    public EvaluateExpressionVisitor(Map<String, BaseTypeValue> parameterMap) {
        this.parameterMap = parameterMap;
    }

    @Override
    public BaseTypeValue visit(Expression.IntConst expression) {
        return BaseTypeValue.fromBigInteger(expression.value);
    }

    @Override
    public BaseTypeValue visit(Expression.BoolConst expression) {
        return BaseTypeValue.fromBoolean(expression.value);
    }

    @Override
    public BaseTypeValue visit(Expression.ArrayInitExpression expression) {
        return BaseTypeValue.fromInitializer(expression.initializer.accept(this));
    }

    @Override
    public BaseTypeValue visit(Expression.VarExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseTypeValue visit(Expression.FreeVarExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseTypeValue visit(Expression.ParVarExpression expression) {
        return parameterMap.get(expression.name);
    }

    @Override
    public BaseTypeValue visit(Expression.BinaryIntExpression expression) {
        BigInteger v1 = ((BaseTypeValue.BaseTypeIntegerValue) expression.expression1.accept(this)).value;
        BigInteger v2 = ((BaseTypeValue.BaseTypeIntegerValue) expression.expression2.accept(this)).value;

        switch (expression.operation) {
            case MUL:
                return BaseTypeValue.fromBigInteger(v1.multiply(v2));
            case ADD:
                return BaseTypeValue.fromBigInteger(v1.add(v2));
            case DIV:
                return BaseTypeValue.fromBigInteger(v1.divide(v2));
            case MOD:
                return BaseTypeValue.fromBigInteger(v1.mod(v2));
            case SUB:
                return BaseTypeValue.fromBigInteger(v1.subtract(v2));
            case BVAND:
                return BaseTypeValue.fromBigInteger(bitVectorArithmeticEvaluator.bvand(v1, v2));
            case BVXOR:
                return BaseTypeValue.fromBigInteger(bitVectorArithmeticEvaluator.bvxor(v1, v2));
            case BVOR:
                return BaseTypeValue.fromBigInteger(bitVectorArithmeticEvaluator.bvor(v1, v2));
        }
        throw new RuntimeException("Unreachable code!");
    }

    @Override
    public BaseTypeValue visit(Expression.BinaryBoolExpression expression) {
        Boolean v1 = ((BaseTypeValue.BaseTypeBooleanValue) expression.expression1.accept(this)).value;
        Boolean v2 = ((BaseTypeValue.BaseTypeBooleanValue) expression.expression2.accept(this)).value;

        switch (expression.operation) {
            case AND:
                return BaseTypeValue.fromBoolean(v1 && v2);
            case OR:
                return BaseTypeValue.fromBoolean(v1 || v2);
        }
        throw new RuntimeException("Unreachable code!");
    }

    @Override
    public BaseTypeValue visit(Expression.SelectExpression expression) {
        BaseTypeValue.BaseTypeArrayValue array = ((BaseTypeValue.BaseTypeArrayValue) expression.expression1.accept(this));
        BigInteger index = ((BaseTypeValue.BaseTypeIntegerValue) expression.expression2.accept(this)).value;

        return array.stores.getOrDefault(index, array.initializer);
    }

    @Override
    public BaseTypeValue visit(Expression.StoreExpression expression) {
        BaseTypeValue.BaseTypeArrayValue array = ((BaseTypeValue.BaseTypeArrayValue) expression.expression1.accept(this));
        BigInteger position = ((BaseTypeValue.BaseTypeIntegerValue) expression.expression2.accept(this)).value;
        BaseTypeValue baseTypeValue = expression.expression3.accept(this);

        return BaseTypeValue.extendArray(array, position, baseTypeValue);
    }

    @Override
    public BaseTypeValue visit(Expression.AppExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseTypeValue visit(Expression.ConstructorAppExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseTypeValue visit(Expression.MatchExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseTypeValue visit(Expression.NegationExpression expression) {
        boolean b = ((BaseTypeValue.BaseTypeBooleanValue) expression.expression.accept(this)).value;
        return BaseTypeValue.fromBoolean(!b);
    }

    @Override
    public BaseTypeValue visit(Expression.BitvectorNegationExpression expression) {
        BigInteger x = ((BaseTypeValue.BaseTypeIntegerValue) expression.expression.accept(this)).value;
        return BaseTypeValue.fromBigInteger(bitVectorArithmeticEvaluator.bvneg(x));
    }

    @Override
    public BaseTypeValue visit(Expression.ConditionalExpression expression) {
        return ((BaseTypeValue.BaseTypeBooleanValue) expression.expression1.accept(this)).value ? expression.expression2.accept(this) : expression.expression3.accept(this);
    }

    @Override
    public BaseTypeValue visit(Expression.ComparisonExpression expression) {
        BaseTypeValue v1 = expression.expression1.accept(this);
        BaseTypeValue v2 = expression.expression2.accept(this);

        switch (expression.operation) {
            case EQ:
                return BaseTypeValue.fromBoolean(v1.equals(v2));
            case NEQ:
                return BaseTypeValue.fromBoolean(!v1.equals(v2));
        }

        BigInteger integerValue1 = ((BaseTypeValue.BaseTypeIntegerValue) expression.expression1.accept(this)).value;
        BigInteger integerValue2 = ((BaseTypeValue.BaseTypeIntegerValue) expression.expression2.accept(this)).value;

        switch (expression.operation) {
            case GE:
                return BaseTypeValue.fromBoolean(integerValue1.compareTo(integerValue2) >= 0);
            case GT:
                return BaseTypeValue.fromBoolean(integerValue1.compareTo(integerValue2) > 0);
            case LE:
                return BaseTypeValue.fromBoolean(integerValue1.compareTo(integerValue2) <= 0);
            case LT:
                return BaseTypeValue.fromBoolean(integerValue1.compareTo(integerValue2) < 0);
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public BaseTypeValue visit(Expression.ConstExpression expression) {
        return expression.value.accept(this);
    }

    @Override
    public BaseTypeValue visit(Expression.SumExpression expression) {
        throw new UnsupportedOperationException("SumExpression should be eliminated at this point!");
    }
}
