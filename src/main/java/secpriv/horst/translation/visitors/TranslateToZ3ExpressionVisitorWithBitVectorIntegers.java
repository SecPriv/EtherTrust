package secpriv.horst.translation.visitors;

import com.microsoft.z3.*;
import secpriv.horst.data.Expression;
import secpriv.horst.translation.TranslateToZ3VisitorState;

public class TranslateToZ3ExpressionVisitorWithBitVectorIntegers implements Expression.Visitor<Expr> {
    private final TranslateToZ3VisitorState state;
    private final Context context;
    private final static int BIT_WIDTH = 32;//64;//256;

    public TranslateToZ3ExpressionVisitorWithBitVectorIntegers(TranslateToZ3VisitorState state) {
        this.context = state.context;
        this.state = state;
    }

    @Override
    public Expr visit(Expression.IntConst expression) {
        return context.mkBV(expression.value.toString(), BIT_WIDTH);
    }

    @Override
    public Expr visit(Expression.BoolConst expression) {
        return context.mkBool(expression.value);
    }

    @Override
    public Expr visit(Expression.ArrayInitExpression expression) {
        return context.mkConstArray(context.mkBitVecSort(BIT_WIDTH), expression.initializer.accept(this));
    }

    @Override
    public Expr visit(Expression.VarExpression expression) {
        throw new UnsupportedOperationException("VarExpressions cannot be translated to Z3!");
    }

    @Override
    public Expr visit(Expression.FreeVarExpression expression) {
        return state.getZ3FreeVar(expression);
    }

    @Override
    public Expr visit(Expression.ParVarExpression expression) {
        throw new UnsupportedOperationException("ParVarExpressions cannot be translated to Z3!");
    }

    @Override
    public Expr visit(Expression.BinaryIntExpression expression) {
        BitVecExpr b1 = (BitVecExpr) expression.expression1.accept(this);
        BitVecExpr b2 = (BitVecExpr) expression.expression2.accept(this);

        switch (expression.operation) {
            case ADD:
                return context.mkBVAdd(b1, b2);
            case SUB:
                return context.mkBVSub(b1, b2);
            case MUL:
                return context.mkBVMul(b1, b2);
            case DIV:
                return context.mkBVUDiv(b1, b2); //TODO: Check if this is the right thing to do?
            case MOD:
                return context.mkBVURem(b1, b2); //TODO: Check if this is the right thing to do?
            case BVAND:
                return context.mkBVAND(b1, b2);
            case BVXOR:
                return context.mkBVXOR(b1, b2);
            case BVOR:
                return context.mkBVOR(b1, b2);
        }
        throw new RuntimeException("Unreachable Code!");
    }

    @Override
    public Expr visit(Expression.BinaryBoolExpression expression) {
        BoolExpr b1 = (BoolExpr) expression.expression1.accept(this);
        BoolExpr b2 = (BoolExpr) expression.expression2.accept(this);

        //Check which Translation is smarter
        switch (expression.operation) {
            case AND:
                return context.mkAnd(b1, b2);
//                return context.mkITE(b1, b2, context.mkBool(false));
            case OR:
                return context.mkOr(b1, b2);
//                return context.mkITE(b1, context.mkBool(true), b2);
        }
        throw new RuntimeException("Unreachable Code!");
    }

    @Override
    public Expr visit(Expression.SelectExpression expression) {
        ArrayExpr a = (ArrayExpr) expression.expression1.accept(this);
        BitVecExpr b = (BitVecExpr) expression.expression2.accept(this);
        return context.mkSelect(a, b);
    }

    @Override
    public Expr visit(Expression.StoreExpression expression) {
        ArrayExpr a = (ArrayExpr) expression.expression1.accept(this);
        BitVecExpr b = (BitVecExpr) expression.expression2.accept(this);
        Expr e = expression.expression3.accept(this);
        return context.mkStore(a, b, e);
    }

    @Override
    public Expr visit(Expression.AppExpression expression) {
        throw new UnsupportedOperationException("AppExpressions cannot be translated to Z3!");
    }

    @Override
    public Expr visit(Expression.ConstructorAppExpression expression) {
        throw new UnsupportedOperationException("ConstructorAppExpressions cannot be translated to Z3!");
    }

    @Override
    public Expr visit(Expression.MatchExpression expression) {
        throw new UnsupportedOperationException("ConstructorAppExpressions cannot be translated to Z3!");
    }

    @Override
    public Expr visit(Expression.NegationExpression expression) {
        return context.mkNot((BoolExpr) expression.expression.accept(this));
    }

    @Override
    public Expr visit(Expression.ConditionalExpression expression) {
        BoolExpr b1 = (BoolExpr) expression.expression1.accept(this);
        Expr e1 = expression.expression2.accept(this);
        Expr e2 = expression.expression3.accept(this);
        return context.mkITE(b1, e1, e2);
    }

    @Override
    public Expr visit(Expression.ComparisonExpression expression) {
        Expr e1 = expression.expression1.accept(this);
        Expr e2 = expression.expression2.accept(this);

        switch (expression.operation) {
            case EQ:
                return context.mkEq(e1, e2);
            case NEQ:
                return context.mkNot(context.mkEq(e1, e2));
            case LT:
                return context.mkBVULT((BitVecExpr) e1, (BitVecExpr) e2);//TODO: Check if this is the right thing to do?
            case LE:
                return context.mkBVULE((BitVecExpr) e1, (BitVecExpr) e2);//TODO: Check if this is the right thing to do?
            case GT:
                return context.mkBVUGT((BitVecExpr) e1, (BitVecExpr) e2);//TODO: Check if this is the right thing to do?
            case GE:
                return context.mkBVUGE((BitVecExpr) e1, (BitVecExpr) e2);//TODO: Check if this is the right thing to do?
        }
        throw new RuntimeException("Unreachable Code!");
    }

    @Override
    public Expr visit(Expression.ConstExpression expression) {
        return state.getConstant(expression).accept(this);
    }

    @Override
    public Expr visit(Expression.SumExpression expression) {
        throw new UnsupportedOperationException("SumExpressions cannot be translated to Z3!");
    }

    @Override
    public Expr visit(Expression.BitvectorNegationExpression expression) {
        return context.mkBVNeg((BitVecExpr) expression.expression.accept(this));
    }

}
