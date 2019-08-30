package secpriv.horst.translation.visitors;

import secpriv.horst.data.Rule;

import java.util.Objects;

public class CombiningRuleVisitor<T> implements Rule.Visitor<T> {
    private final Rule.Visitor<T> outerVisitor;
    private final Rule.Visitor<Rule> innerVisitor;

    public CombiningRuleVisitor(Rule.Visitor<T> outerVisitor, Rule.Visitor<Rule> innerVisitor) {
        this.outerVisitor = Objects.requireNonNull(outerVisitor, "OuterVisitor may not be null!");
        this.innerVisitor = Objects.requireNonNull(innerVisitor, "InnerVisitor may not be null!");
    }

    @Override
    public T visit(Rule rule) {
        return rule.accept(innerVisitor).accept(outerVisitor);
    }
}
