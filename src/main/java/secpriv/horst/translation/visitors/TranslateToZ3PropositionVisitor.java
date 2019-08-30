package secpriv.horst.translation.visitors;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;
import secpriv.horst.translation.TranslateToZ3VisitorState;

public class TranslateToZ3PropositionVisitor implements Proposition.Visitor<BoolExpr> {
    private final TranslateToZ3VisitorState state;
    private final Context context;

    public TranslateToZ3PropositionVisitor(TranslateToZ3VisitorState state) {
        this.context = state.context;
        this.state = state;
    }

    @Override
    public BoolExpr visit(Proposition.PredicateProposition proposition) {
        Expression.Visitor<Expr> translateToZ3ExpressionVisitor = state.getExpressionVisitor();

        Expr[] arguments = proposition.arguments.stream().map(a -> a.accept(translateToZ3ExpressionVisitor)).toArray(Expr[]::new);
        FuncDecl predicate = state.getZ3PredicateDeclaration(proposition.predicate);

        return (BoolExpr) predicate.apply(arguments);
    }

    @Override
    public BoolExpr visit(Proposition.ExpressionProposition proposition) {
        Expression.Visitor<Expr> translateToZ3ExpressionVisitor = state.getExpressionVisitor();

        return (BoolExpr) proposition.expression.accept(translateToZ3ExpressionVisitor);
    }
}
