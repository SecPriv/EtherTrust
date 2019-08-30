package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Rule;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterClauseRuleVisitor implements Rule.Visitor<Optional<Rule>> {
    private final Set<Clause> filteredClauses;

    public FilterClauseRuleVisitor(Set<Clause> filteredClauses) {
        this.filteredClauses = Collections.unmodifiableSet(filteredClauses);
    }

    @Override
    public Optional<Rule> visit(Rule rule) {
        List<Clause> filteredClauses = rule.clauses.stream().filter(c -> !this.filteredClauses.contains(c)).collect(Collectors.toList());

        if(filteredClauses.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Rule(rule.name, rule.selectorFunctionInvocation, filteredClauses));
    }
}
