package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveTruePremiseClauseVisitor implements Clause.Visitor<Clause> {
    static class IsNotTrueExpressionPropositionVisitor implements Proposition.Visitor<Boolean> {
        @Override
        public Boolean visit(Proposition.PredicateProposition proposition) {
            return true;
        }

        @Override
        public Boolean visit(Proposition.ExpressionProposition proposition) {
            return !proposition.expression.equals(Expression.BoolConst.TRUE);
        }
    }

    private static IsNotTrueExpressionPropositionVisitor isNotTrueExpressionPropositionVisitor = new IsNotTrueExpressionPropositionVisitor();

    @Override
    public Clause visit(Clause clause) {
        List<Proposition> visitedPremises = clause.premises.stream().filter(p -> p.accept(isNotTrueExpressionPropositionVisitor)).collect(Collectors.toList());

        if(visitedPremises.isEmpty()) {
            return new Clause(Collections.singletonList(new Proposition.ExpressionProposition(Expression.BoolConst.TRUE)), clause.conclusion, clause.freeVars);
        }

        return new Clause(visitedPremises, clause.conclusion, clause.freeVars);
    }
}
