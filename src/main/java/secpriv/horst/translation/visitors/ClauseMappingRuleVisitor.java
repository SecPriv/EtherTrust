package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Rule;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClauseMappingRuleVisitor implements Rule.Visitor<Rule> {
    private final Clause.Visitor<Clause> clauseVisitor;

    public ClauseMappingRuleVisitor(Clause.Visitor<Clause> clauseVisitor) {
        this.clauseVisitor = Objects.requireNonNull(clauseVisitor, "ClauseVisitor may not be null!");
    }

    @Override
    public Rule visit(Rule rule) {
        List<Clause> visitedClauses = rule.clauses.stream().map(r -> r.accept(clauseVisitor)).collect(Collectors.toList());
        return new Rule(rule.name, rule.selectorFunctionInvocation, visitedClauses);
    }
}
