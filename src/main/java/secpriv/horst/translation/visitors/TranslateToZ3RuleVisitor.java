package secpriv.horst.translation.visitors;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Solver;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Rule;
import secpriv.horst.translation.TranslateToZ3VisitorState;

import java.util.List;
import java.util.stream.Collectors;

public class TranslateToZ3RuleVisitor implements Rule.Visitor<List<BoolExpr>> {
    private TranslateToZ3VisitorState state;

    public TranslateToZ3RuleVisitor(TranslateToZ3VisitorState state) {
        this.state = state;
    }

    @Override
    public List<BoolExpr> visit(Rule rule) {
        TranslateToZ3ClauseVisitor translateToZ3ClauseVisitor = new TranslateToZ3ClauseVisitor(state);
        return rule.clauses.stream().map(c -> c.accept(translateToZ3ClauseVisitor)).collect(Collectors.toList());
    }
}
