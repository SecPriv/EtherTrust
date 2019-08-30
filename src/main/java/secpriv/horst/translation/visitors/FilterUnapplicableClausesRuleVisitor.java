package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.Rule;
import secpriv.horst.visitors.RuleTypeOracle;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FilterUnapplicableClausesRuleVisitor implements Rule.Visitor<Rule> {
    private  final RuleTypeOracle ruleTypeOracle;
    public FilterUnapplicableClausesRuleVisitor(RuleTypeOracle ruleTypeOracle) {
        this.ruleTypeOracle = Objects.requireNonNull(ruleTypeOracle, "RuleTypeOracle may not be null!");
    }

    @Override
    public Rule visit(Rule rule) {
        if(ruleTypeOracle.isQueryOrTest(rule)) {
            return rule;
        }

        List<Clause> filteredClauses = rule.clauses.stream().filter(this::doesNotContainFalsePremise).collect(Collectors.toList());

        return new Rule(rule.name, rule.selectorFunctionInvocation, filteredClauses);
    }

    private class FilterFalsePropositionVisitor implements Proposition.Visitor<Boolean> {
        @Override
        public Boolean visit(Proposition.PredicateProposition proposition) {
            return true;
        }

        @Override
        public Boolean visit(Proposition.ExpressionProposition proposition) {
            return proposition.expression != Expression.BoolConst.FALSE;
        }
    }

    private boolean doesNotContainFalsePremise(Clause clause) {
        FilterFalsePropositionVisitor filterFalsePropositionVisitor = new FilterFalsePropositionVisitor();
        return clause.premises.stream().allMatch(p -> p.accept(filterFalsePropositionVisitor));
    }
}
