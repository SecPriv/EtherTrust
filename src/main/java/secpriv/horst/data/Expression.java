package secpriv.horst.data;

import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Expression {

    public interface Visitor<T> {
        T visit(IntConst expression);

        T visit(BoolConst expression);

        T visit(ArrayInitExpression expression);

        T visit(VarExpression expression);

        T visit(FreeVarExpression expression);

        T visit(ParVarExpression expression);

        T visit(BinaryIntExpression expression);

        T visit(BinaryBoolExpression expression);

        T visit(SelectExpression expression);

        T visit(StoreExpression expression);

        T visit(AppExpression expression);

        T visit(ConstructorAppExpression expression);

        T visit(MatchExpression expression);

        T visit(NegationExpression expression);

        T visit(ConditionalExpression expression);

        T visit(ComparisonExpression expression);

        T visit(ConstExpression expression);

        T visit(SumExpression expression);

        T visit(BitvectorNegationExpression expression);
    }

    public abstract <T> T accept(Visitor<T> visitor);

    public static class IntConst extends Expression {
        public final BigInteger value;

        public IntConst(BigInteger value) {
            this.value = Objects.requireNonNull(value, "Value may not be null!");
        }

        @Override
        public Type getType() {
            return Type.Integer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntConst intConst = (IntConst) o;
            return Objects.equals(value, intConst.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "IntConst{" +
                    "value=" + value +
                    '}';
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BoolConst extends Expression {
        public final boolean value;
        public static final BoolConst TRUE = new BoolConst(true);
        public static final BoolConst FALSE = new BoolConst(false);

        private BoolConst(boolean value) {
            this.value = value;
        }

        @Override
        public Type getType() {
            return Type.Boolean;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BoolConst boolConst = (BoolConst) o;
            return value == boolConst.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "BoolConst{" +
                    "value=" + value +
                    '}';
        }
    }

    public static class ArrayInitExpression extends Expression {
        public final Expression initializer;

        public ArrayInitExpression(Expression initializer) {
            //TODO check for constness of initializer
            this.initializer = Objects.requireNonNull(initializer, "Initializer may not be null!");
        }

        @Override
        public Type getType() {
            return Type.Array.of(initializer.getType());
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static abstract class VariableReferenceExpression extends Expression {
        public final Type type;
        public final String name;

        public VariableReferenceExpression(Type type, String name) {
            this.type = Objects.requireNonNull(type, "Type may not be null!");
            this.name = Objects.requireNonNull(name, "Name may not be null!");
        }

        @Override
        public Type getType() {
            return type;
        }
    }

    public static class VarExpression extends VariableReferenceExpression {
        public VarExpression(Type type, String name) {
            super(type, name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VarExpression that = (VarExpression) o;

            if (!type.equals(that.type)) return false;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "VarExpression{name=" + name + ",type=" + type + "}";
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FreeVarExpression extends VariableReferenceExpression {
        public FreeVarExpression(Type type, String name) {
            super(type, name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FreeVarExpression that = (FreeVarExpression) o;

            if (!type.equals(that.type)) return false;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ParVarExpression extends VariableReferenceExpression {
        public ParVarExpression(Type type, String name) {
            super(type, name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ParVarExpression that = (ParVarExpression) o;

            if (!type.equals(that.type)) return false;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static abstract class UnaryExpression extends Expression {
        public final Expression expression;

        public UnaryExpression(Expression expression) {
            this.expression = expression;
        }
    }

    public static abstract class BinaryExpression extends Expression {
        public final Expression expression1;
        public final Expression expression2;

        public BinaryExpression(Expression expression1, Expression expression2) {
            this.expression1 = Objects.requireNonNull(expression1, "Expression1 may not be null!");
            this.expression2 = Objects.requireNonNull(expression2, "Expression2 may not be null!");
        }
    }

    public static abstract class TernaryExpression extends Expression {
        public final Expression expression1;
        public final Expression expression2;
        public final Expression expression3;

        public TernaryExpression(Expression expression1, Expression expression2, Expression expression3) {
            this.expression1 = Objects.requireNonNull(expression1, "Expression1 may not be null!");
            this.expression2 = Objects.requireNonNull(expression2, "Expression2 may not be null!");
            this.expression3 = Objects.requireNonNull(expression3, "Expression3 may not be null!");
        }
    }

    public enum IntOperation {ADD, SUB, MUL, DIV, MOD, BVAND, BVXOR, BVOR}

    public enum BoolOperation {AND, OR}

    public enum CompOperation {GT, LT, GE, LE, EQ, NEQ}


    public static class BinaryIntExpression extends BinaryExpression {
        public final IntOperation operation;

        public BinaryIntExpression(Expression expression1, Expression expression2, IntOperation operation) {
            super(expression1, expression2);
            this.operation = Objects.requireNonNull(operation, "Operation may not be null!");
        }

        @Override
        public Type getType() {
            return Type.Integer;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BinaryBoolExpression extends BinaryExpression {
        public final BoolOperation operation;

        public BinaryBoolExpression(Expression expression1, Expression expression2, BoolOperation operation) {
            super(expression1, expression2);
            this.operation = Objects.requireNonNull(operation, "Operation may not be null!");
        }

        @Override
        public Type getType() {
            return Type.Boolean;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ComparisonExpression extends BinaryExpression {
        public final CompOperation operation;

        public ComparisonExpression(Expression expression1, Expression expression2, CompOperation operation) {
            super(expression1, expression2);
            this.operation = Objects.requireNonNull(operation, "Operation may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Type getType() {
            return Type.Boolean;
        }
    }

    public static class SelectExpression extends BinaryExpression {
        public SelectExpression(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        @Override
        public Type getType() {
            return ((Type.ArrayType) (expression1.getType())).type;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class StoreExpression extends TernaryExpression {
        public StoreExpression(Expression expression1, Expression expression2, Expression expression3) {
            super(expression1, expression2, expression3);
        }

        @Override
        public Type getType() {
            return expression1.getType();
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ConditionalExpression extends TernaryExpression {
        public ConditionalExpression(Expression expression1, Expression expression2, Expression expression3) {
            super(expression1, expression2, expression3);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Type getType() {
            return expression2.getType();
        }
    }

    public static class SumExpression extends Expression {
        public final SumOperation operation;
        public final Expression body;
        public final CompoundSelectorFunctionInvocation selectorFunctionInvocation;

        public SumExpression(CompoundSelectorFunctionInvocation selectorFunctionInvocation, Expression body, SumOperation operation) {
            this.operation = Objects.requireNonNull(operation, "Operation may not be null!");
            this.body = Objects.requireNonNull(body, "SelectorFunction may not be null!");
            this.selectorFunctionInvocation = Objects.requireNonNull(selectorFunctionInvocation, "SelectorFunctionInvocation may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Type getType() {
            return operation.getType();
        }
    }

    public static abstract class VariadicExpression extends Expression {
        public final List<Expression> expressions;

        public VariadicExpression(List<Expression> expressions) {
            if (Objects.requireNonNull(expressions, "Expressions may not be null").contains(null)) {
                throw new IllegalArgumentException("Expressions may not contain null!");
            }
            this.expressions = Collections.unmodifiableList(expressions);
        }
    }

    public static class AppExpression extends VariadicExpression {
        public final Operation operation;
        public final List<Expression> parameters;

        public AppExpression(Operation operation, List<Expression> parameters, List<Expression> arguments) {
            super(arguments);
            this.parameters = Collections.unmodifiableList(Objects.requireNonNull(parameters, "Parameters may not be null!"));
            this.operation = Objects.requireNonNull(operation, "Operation may not be null!");
        }

        @Override
        public Type getType() {
            return operation.body.getType();
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ConstructorAppExpression extends VariadicExpression {
        public final Constructor constructor;
        private final Type.CustomType type;

        public ConstructorAppExpression(Constructor constructor, Type.CustomType type, List<Expression> expressions) {
            super(expressions);
            this.constructor = Objects.requireNonNull(constructor, "Constructor may not be null!");
            this.type = Objects.requireNonNull(type, "Type may not be null!");
        }

        @Override
        public Type getType() {
            return type;
        }

        public Type.CustomType getCustomType() {
            return type;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MatchExpression extends Expression {
        public final List<List<Pattern>> branchPatterns;
        public final List<Expression> matchedExpressions;
        public final List<Expression> resultExpressions;

        public MatchExpression(List<List<Pattern>> branchPatterns, List<Expression> matchedExpressions, List<Expression> resultExpressions) {
            this.matchedExpressions = Collections.unmodifiableList(Objects.requireNonNull(matchedExpressions, "MatchedExpression may not be null!"));
            this.resultExpressions = Collections.unmodifiableList(Objects.requireNonNull(resultExpressions, "ResultExpressions may not be null!"));
            List<List<Pattern>> tmpBranchPattern = new ArrayList<>();
            for (List<Pattern> patterns : Objects.requireNonNull(branchPatterns, "BranchPatterns may not be null!")) {
                tmpBranchPattern.add(Collections.unmodifiableList(patterns));
            }
            this.branchPatterns = Collections.unmodifiableList(tmpBranchPattern);
        }

        @Override
        public Type getType() {
            return resultExpressions.get(0).getType();
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class NegationExpression extends UnaryExpression {
        public NegationExpression(Expression expression) {
            super(expression);
        }

        @Override
        public Type getType() {
            return Type.Boolean;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BitvectorNegationExpression extends UnaryExpression {
        public BitvectorNegationExpression(Expression expression) {
            super(expression);
        }

        @Override
        public Type getType() {
            return Type.Integer;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ConstExpression extends Expression {
        public final Expression value;
        public final String name;

        public ConstExpression(String name, Expression value) {
            this.name = Objects.requireNonNull(name, "Name may not be null!");
            this.value = Objects.requireNonNull(value, "Value may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Type getType() {
            return value.getType();
        }
    }

    public abstract Type getType();

}
