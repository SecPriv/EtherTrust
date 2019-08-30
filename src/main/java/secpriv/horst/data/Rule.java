package secpriv.horst.data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Rule {
    public interface Visitor <T> {
        T visit(Rule rule);
    }

    public final String name;
    public final CompoundSelectorFunctionInvocation selectorFunctionInvocation;
    public final List<Clause> clauses;

    public Rule(String name, CompoundSelectorFunctionInvocation selectorFunctionInvocation, List<Clause> clauses) {
        this.name = Objects.requireNonNull(name, "Name may not be null!");
        this.selectorFunctionInvocation = Objects.requireNonNull(selectorFunctionInvocation, "SelectorFunctionInvocation may not be null!");
        this.clauses = Collections.unmodifiableList(Objects.requireNonNull(clauses, "Clauses may not be null!"));
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
