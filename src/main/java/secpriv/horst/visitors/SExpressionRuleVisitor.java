package secpriv.horst.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Rule;

public class SExpressionRuleVisitor implements Rule.Visitor<String> {
    @Override
    public String visit(Rule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("(Rule ");
        sb.append(rule.name).append(" ");
        for(Clause clause : rule.clauses) {
            SExpressionClauseVisitor clauseVisitor = new SExpressionClauseVisitor(1);
            sb.append("\n").append("  ").append(clauseVisitor.visit(clause));
        }
        sb.append(")");
        return sb.toString();
    }
}
