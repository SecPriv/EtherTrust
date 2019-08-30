package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;

public class TranslateToSmtLibExpressionVisitor implements Expression.Visitor<Void> {
    private final StringBuilder sb;
    private final TranslateToSmtLibTypeVisitor typeVisitor = new TranslateToSmtLibTypeVisitor();
    private final TranslateToSmtLibVisitorState state;

    public TranslateToSmtLibExpressionVisitor(StringBuilder sb, TranslateToSmtLibVisitorState state) {
        this.sb = sb;
        this.state = state;
    }

    @Override
    public Void visit(Expression.IntConst expression) {
        sb.append(expression.value);
        return null;
    }

    @Override
    public Void visit(Expression.BoolConst expression) {
        sb.append(expression.value);
        return null;
    }

    @Override
    public Void visit(Expression.ArrayInitExpression expression) {
        sb.append("((");
        sb.append("as const ");
        sb.append(expression.getType().accept(typeVisitor));
        sb.append(") ");
        expression.initializer.accept(this);
        sb.append(")");
        return null;
    }

    @Override
    public Void visit(Expression.VarExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Void visit(Expression.FreeVarExpression expression) {
        sb.append(state.getFreeVar(expression));
        return null;
    }

    @Override
    public Void visit(Expression.ParVarExpression expression) {
        throw new UnsupportedOperationException();
    }

    private void encloseBinary(String operator, Expression.BinaryExpression expression) {
        sb.append("(");
        sb.append(operator);
        sb.append(" ");
        expression.expression1.accept(this);
        sb.append(" ");
        expression.expression2.accept(this);
        sb.append(")");
    }

    private void encloseTernary(String operator, Expression.TernaryExpression expression) {
        sb.append("(");
        sb.append(operator);
        sb.append(" ");
        expression.expression1.accept(this);
        sb.append(" ");
        expression.expression2.accept(this);
        sb.append(" ");
        expression.expression3.accept(this);
        sb.append(")");
    }

    @Override
    public Void visit(Expression.BinaryIntExpression expression) {
        switch (expression.operation) {
            case ADD:
                encloseBinary("+", expression);
                return null;
            case SUB:
                encloseBinary("-", expression);
                return null;
            case MUL:
                encloseBinary("*", expression);
                return null;
            case DIV:
                encloseBinary("/", expression);
                return null;
            case MOD:
                encloseBinary("mod ", expression);
                return null;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Void visit(Expression.BinaryBoolExpression expression) {
        switch (expression.operation) {
            case AND:
                encloseBinary("and", expression);
                return null;
            case OR:
                encloseBinary("or", expression);
                return null;
        }
        throw new RuntimeException("Unreachable code!");
    }

    @Override
    public Void visit(Expression.SelectExpression expression) {
        encloseBinary("select", expression);
        return null;
    }

    @Override
    public Void visit(Expression.StoreExpression expression) {
        encloseTernary("store", expression);
        return null;
    }

    @Override
    public Void visit(Expression.AppExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Void visit(Expression.ConstructorAppExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Void visit(Expression.MatchExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Void visit(Expression.NegationExpression expression) {
        sb.append("(not ");
        expression.expression.accept(this);
        sb.append(")");
        return null;
    }

    @Override
    public Void visit(Expression.ConditionalExpression expression) {
        encloseTernary("ite", expression);
        return null;
    }

    @Override
    public Void visit(Expression.ComparisonExpression expression) {
        switch (expression.operation) {
            case EQ:
                encloseBinary("=", expression);
                return null;
            case GE:
                encloseBinary(">=", expression);
                return null;
            case LE:
                encloseBinary("<=", expression);
                return null;
            case GT:
                encloseBinary(">", expression);
                return null;
            case LT:
                encloseBinary("<", expression);
                return null;
            case NEQ:
                sb.append("(not (= ");
                expression.expression1.accept(this);
                sb.append(" ");
                expression.expression2.accept(this);
                sb.append("))");
                return null;

        }
        throw new RuntimeException("Unreachable code!");
    }

    @Override
    public Void visit(Expression.ConstExpression expression) {
        expression.value.accept(this);
        return null;
    }

    @Override
    public Void visit(Expression.SumExpression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Void visit(Expression.BitvectorNegationExpression expression) {
        throw new UnsupportedOperationException();
    }
}
