package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;
import secpriv.horst.translation.layout.TypeLayouter;
import secpriv.horst.types.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class InlineTypesClauseVisitor implements Clause.Visitor<Clause> {
    private InlineTypesExpressionVisitor expressionVisitor;

    public InlineTypesClauseVisitor(InlineTypesExpressionVisitor expressionVisitor) {
        this.expressionVisitor = expressionVisitor;
    }

    @Override
    public Clause visit(Clause clause) {
        InlineTypesPropositionVisitor propositionVisitor = new InlineTypesPropositionVisitor(expressionVisitor);
        TypeLayouter typeLayouter = expressionVisitor.getTypeLayouter();

        Map<String, Type> freeVars = new HashMap<>();

        for(Map.Entry<String, Type> p : clause.freeVars.entrySet()) {
            for(Expression e : typeLayouter.translateFreeVars(new Expression.FreeVarExpression(p.getValue(), p.getKey()))) {
                freeVars.put(((Expression.FreeVarExpression) e).name, ((Expression.FreeVarExpression) e).type);
            }
        }

        return new Clause(clause.premises.stream().map(p -> p.accept(propositionVisitor)).collect(Collectors.toList()), (Proposition.PredicateProposition) clause.conclusion.accept(propositionVisitor), freeVars);
    }
}
