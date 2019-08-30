package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;
import secpriv.horst.types.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimplifyPredicateArgumentsClauseVisitor implements Clause.Visitor<Clause> {
    @Override
    public Clause visit(Clause clause) {
        SimplifyPredicateArgumentsPropositionVisitor propositionVisitor = new SimplifyPredicateArgumentsPropositionVisitor();

        List<SimplifyPredicatesData> simplifiedPremises = clause.premises.stream().map(p -> p.accept(propositionVisitor)).collect(Collectors.toList());
        SimplifyPredicatesData simplifiedConclusion = clause.conclusion.accept(propositionVisitor);

        Map<String, Type> newFreeVars = new HashMap<>(clause.freeVars);
        simplifiedPremises.forEach(d -> newFreeVars.putAll(d.newFreeVars));
        newFreeVars.putAll(simplifiedConclusion.newFreeVars);

        List<Proposition> newPremises = new ArrayList<>();
        simplifiedPremises.forEach(d -> newPremises.addAll(d.simplifiedPropositions));

        Proposition.PredicateProposition newConclusion = null;

        for (Proposition proposition : simplifiedConclusion.simplifiedPropositions) {
            if (proposition instanceof Proposition.PredicateProposition) {
                if (newConclusion != null) {
                    throw new RuntimeException("Simplified conclusion must have exactly one PredicateProposition!");
                }
                newConclusion = (Proposition.PredicateProposition) proposition;
            } else {
                newPremises.add(proposition);
            }
        }

        return new Clause(newPremises, newConclusion, newFreeVars);
    }
}
