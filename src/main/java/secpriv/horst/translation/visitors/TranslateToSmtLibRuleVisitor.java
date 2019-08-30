package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Rule;
import secpriv.horst.data.tuples.Tuple2;

import java.util.Map;

public class TranslateToSmtLibRuleVisitor implements Rule.Visitor<Void> {
    private final TranslateToSmtLibClauseVisitor clauseVisitor;

    public TranslateToSmtLibRuleVisitor(TranslateToSmtLibVisitorState state, StringBuilder sb, Map<Clause, Tuple2<Rule, Integer>> clauseToRuleMap) {
        clauseVisitor = new TranslateToSmtLibClauseVisitor(state, sb, clauseToRuleMap);
    }

    @Override
    public Void visit(Rule rule) {
        rule.clauses.forEach(c -> c.accept(clauseVisitor));
        return null;
    }


}
