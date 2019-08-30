package secpriv.horst.data;

import secpriv.horst.types.Type;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Clause {
    public interface Visitor <T> {
        T visit(Clause clause);
    }

    public final List<Proposition> premises;
    public final Proposition.PredicateProposition conclusion;
    public final Map<String, Type> freeVars;

    public Clause(List<Proposition> premises, Proposition.PredicateProposition conclusion, Map<String, Type> freeVars) {
        this.premises = Collections.unmodifiableList(Objects.requireNonNull(premises, "Premises may not be null!"));
        this.conclusion = Objects.requireNonNull(conclusion, "Conclusion may not be null!");
        this.freeVars = Collections.unmodifiableMap(Objects.requireNonNull(freeVars, "FreeVars may not be null!"));
    }

    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
