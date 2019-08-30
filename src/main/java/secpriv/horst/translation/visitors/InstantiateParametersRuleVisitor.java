package secpriv.horst.translation.visitors;

import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.CompoundSelectorFunctionInvocation;
import secpriv.horst.data.Rule;
import secpriv.horst.internals.SelectorFunctionInvoker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class InstantiateParametersRuleVisitor implements Rule.Visitor<List<Rule>> {
    private final SelectorFunctionInvoker selectorFunctionInvoker;

    public InstantiateParametersRuleVisitor(SelectorFunctionInvoker selectorFunctionInvoker) {
        this.selectorFunctionInvoker = Objects.requireNonNull(selectorFunctionInvoker, "SelectorFunctionInvoker may not be null!");
    }

    @Override
    public List<Rule> visit(Rule rule) {
        CompoundSelectorFunctionInvocation invocation = rule.selectorFunctionInvocation;
        List<Rule> rules = new ArrayList<>();
        List<String> names = rule.selectorFunctionInvocation.parameters().stream().map(e -> e.name).collect(Collectors.toList());

        try {
            for (Map<String, BaseTypeValue> parameterMap : selectorFunctionInvoker.invoke(invocation)) {
                InstantiateParametersClauseVisitor clauseVisitor = new InstantiateParametersClauseVisitor(parameterMap, selectorFunctionInvoker);

                String instantiatedRuleName = instantiateRuleName(rule, names.stream().map(parameterMap::get).collect(Collectors.toList()));

                rules.add(new Rule(instantiatedRuleName, CompoundSelectorFunctionInvocation.UnitInvocation, rule.clauses.stream().map(c -> c.accept(clauseVisitor)).collect(Collectors.toList())));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return rules;
    }


    private String instantiateRuleName(Rule rule, List<BaseTypeValue> parameterValues) {
        String ruleName = rule.name;
        if (!parameterValues.isEmpty()) {
            ruleName += "_" + String.join("_", parameterValues.stream().map(b -> b.accept(new ToStringRepresentationBaseTypeValueVisitor())).collect(Collectors.toList()));
        }
        return ruleName;
    }
}
