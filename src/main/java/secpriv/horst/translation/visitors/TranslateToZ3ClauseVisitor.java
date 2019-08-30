package secpriv.horst.translation.visitors;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import secpriv.horst.data.Clause;
import secpriv.horst.translation.TranslateToZ3VisitorState;

public class TranslateToZ3ClauseVisitor implements Clause.Visitor<BoolExpr> {
    private final TranslateToZ3VisitorState state;
    private final Context context;

    public TranslateToZ3ClauseVisitor(TranslateToZ3VisitorState state) {
        this.context = state.context;
        this.state = state;
    }

    @Override
    public BoolExpr visit(Clause clause) {
        TranslateToZ3PropositionVisitor translateToZ3PropositionVisitor = new TranslateToZ3PropositionVisitor(state);
        BoolExpr conclusion = clause.conclusion.accept(translateToZ3PropositionVisitor);
        BoolExpr premises[] = clause.premises.stream().map(p -> p.accept(translateToZ3PropositionVisitor)).toArray(BoolExpr[]::new);

        BoolExpr premise = premises.length > 1 ? context.mkAnd(premises) : premises[0];

        return context.mkImplies(premise, conclusion);
    }
}
