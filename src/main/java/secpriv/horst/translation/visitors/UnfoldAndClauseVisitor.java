package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;

import java.util.List;
import java.util.stream.Collectors;

public class UnfoldAndClauseVisitor implements Clause.Visitor<Clause> {
    @Override
    public Clause visit(Clause clause) {
        UnfoldAndPropositionVisitor propositionVisitor = new UnfoldAndPropositionVisitor();
        List<Proposition> mappedPremises = clause.premises.stream().flatMap(p -> p.accept(propositionVisitor).stream()).collect(Collectors.toList());

        return new Clause(mappedPremises, clause.conclusion, clause.freeVars);
    }
}
