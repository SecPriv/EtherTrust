package secpriv.horst.translation.visitors;

import com.microsoft.z3.*;
import secpriv.horst.data.Expression;
import secpriv.horst.evm.EvmBitVectorBasedZ3BitVectorTranslator;
import secpriv.horst.evm.EvmIntegerBasedZ3BitVectorTranslator;
import secpriv.horst.translation.TranslateToZ3VisitorState;
import secpriv.horst.translation.Z3BitVectorTranslator;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class TranslateToZ3ExpressionVisitor implements Expression.Visitor<Expr> {
    private final TranslateToZ3VisitorState state;
    private final Context context;
    private final Z3BitVectorTranslator bitVectorTranslator = new EvmIntegerBasedZ3BitVectorTranslator();

    public TranslateToZ3ExpressionVisitor(TranslateToZ3VisitorState state) {
        this.context = state.context;
        this.state = state;
    }

    @Override
    public Expr visit(Expression.IntConst expression) {
        return context.mkInt(expression.value.toString());
    }

    @Override
    public Expr visit(Expression.BoolConst expression) {
        return context.mkBool(expression.value);
    }

    @Override
    public Expr visit(Expression.ArrayInitExpression expression) {
        return context.mkConstArray(context.mkIntSort(), expression.initializer.accept(this));
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
        IntExpr i1 = (IntExpr) expression.expression1.accept(this);
        IntExpr i2 = (IntExpr) expression.expression2.accept(this);

        switch (expression.operation) {
            case ADD:
                return context.mkAdd(i1, i2);
            case SUB:
                return context.mkSub(i1, i2);
            case MUL:
                return context.mkMul(i1, i2);
            case DIV:
                return context.mkDiv(i1, i2);
            case MOD:
                return context.mkMod(i1, i2);
            case BVAND:
                return bitVectorTranslator.bvand(context, i1, i2);
            case BVXOR:
                return bitVectorTranslator.bvxor(context, i1, i2);
            case BVOR:
                return bitVectorTranslator.bvor(context, i1, i2);
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
        IntExpr i = (IntExpr) expression.expression2.accept(this);
        return context.mkSelect(a, i);
    }

    @Override
    public Expr visit(Expression.StoreExpression expression) {
        ArrayExpr a = (ArrayExpr) expression.expression1.accept(this);
        IntExpr i = (IntExpr) expression.expression2.accept(this);
        Expr e = expression.expression3.accept(this);
        return context.mkStore(a, i, e);
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
                return context.mkLt((IntExpr) e1, (IntExpr) e2);
            case LE:
                return context.mkLe((IntExpr) e1, (IntExpr) e2);
            case GT:
                return context.mkGt((IntExpr) e1, (IntExpr) e2);
            case GE:
                return context.mkGe((IntExpr) e1, (IntExpr) e2);
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
        return bitVectorTranslator.bvneg(context, (IntExpr) expression.expression.accept(this));
    }

}
