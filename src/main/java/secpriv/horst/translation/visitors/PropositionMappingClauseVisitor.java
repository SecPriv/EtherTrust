package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;

import java.util.List;
import java.util.stream.Collectors;

public class PropositionMappingClauseVisitor implements Clause.Visitor<Clause> {
    private final Proposition.Visitor<Proposition> propositionVisitor;

    public PropositionMappingClauseVisitor(Proposition.Visitor<Proposition> propositionVisitor) {
        this.propositionVisitor = propositionVisitor;
    }

    @Override
    public Clause visit(Clause clause) {
        List<Proposition> visitedPremises = clause.premises.stream().map(p -> p.accept(propositionVisitor)).collect(Collectors.toList());
        Proposition.PredicateProposition visitedConclusion = (Proposition.PredicateProposition) clause.conclusion.accept(propositionVisitor);

        return new Clause(visitedPremises, visitedConclusion, clause.freeVars);
    }
}
