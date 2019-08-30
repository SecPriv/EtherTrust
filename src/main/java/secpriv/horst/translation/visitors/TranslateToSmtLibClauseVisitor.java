package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.Rule;
import secpriv.horst.data.tuples.Tuple2;

import java.util.Map;

public class TranslateToSmtLibClauseVisitor implements Clause.Visitor<Void> {
    private final StringBuilder sb;
    private final TranslateToSmtLibPropositionVisitor propositionVisitor;
    private final TranslateToSmtLibVisitorState state;
    private final Map<Clause, Tuple2<Rule, Integer>> clauseToRuleMap;

    public TranslateToSmtLibClauseVisitor(TranslateToSmtLibVisitorState state, StringBuilder sb, Map<Clause, Tuple2<Rule, Integer>> clauseToRuleMap) {
        this.state = state;
        this.sb = sb;
        this.clauseToRuleMap = clauseToRuleMap;
        propositionVisitor = new TranslateToSmtLibPropositionVisitor(sb, state);
    }

    @Override
    public Void visit(Clause clause) {
        state.newScope();
        sb.append("\n(rule");

        if (clauseToRuleMap.containsKey(clause)) {
            Tuple2<Rule, Integer> t = clauseToRuleMap.get(clause);
            sb.append(" ; ").append(t.v0.name).append(" ").append(t.v1);
        }

        sb.append("\n  (=> ");

        if (clause.premises.size() == 1) {
            clause.premises.get(0).accept(propositionVisitor);
        } else {
            sb.append("\n    ");
            sb.append("(and ");
            for (Proposition premise : clause.premises) {
                premise.accept(propositionVisitor);
            }
            sb.append(")\n");
        }
        clause.conclusion.accept(propositionVisitor);
        sb.append(")");
        sb.append(")");
        return null;
    }
}
