package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.types.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RenameFreeVariablesClauseVisitor implements Clause.Visitor<Clause> {
    private final RenameFreeVariablesPropositionVisitor propositionVisitor;
    private final Map<String,String> renames;

    public RenameFreeVariablesClauseVisitor(Map<String, String> renames) {
        this.propositionVisitor = new RenameFreeVariablesPropositionVisitor(renames);
        this.renames = renames;
    }

    @Override
    public Clause visit(Clause clause) {
        List<Proposition> visitedPremises = clause.premises.stream().map(p -> p.accept(propositionVisitor)).collect(Collectors.toList());
        Proposition.PredicateProposition visitedConclusion = (Proposition.PredicateProposition) clause.conclusion.accept(propositionVisitor);
        Map<String, Type> visitedFreevars = new HashMap<>();
        clause.freeVars.entrySet().stream().map(e -> new Tuple2<>(renames.get(e.getKey()), e.getValue())).forEach(t -> visitedFreevars.put(t.v0, t.v1));
        return new Clause(visitedPremises, visitedConclusion, visitedFreevars);
    }
}
