package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;
import secpriv.horst.types.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InlineEqualitiesClauseVisitor implements Clause.Visitor<Clause> {
    @Override
    public Clause visit(Clause clause) {
        Map<Boolean, List<Proposition>> partitionedPremises = splitPremiseByInline(clause.premises);

        if (partitionedPremises.get(true).isEmpty()) {
            return clause;
        }

        Map<Expression.FreeVarExpression, Expression> inlines = getInlines(partitionedPremises.get(true));

        if (inlines.values().stream().anyMatch(e -> e instanceof Expression.FreeVarExpression)) {
            // If there are equalities between FreeVarExpressions we may to be cautious when inlining.
            // Therefore we do ignore clauses containing these equalities for now
            return clause;
        }

        InlineEqualitiesPropositionVisitor propositionVisitor = new InlineEqualitiesPropositionVisitor(inlines);

        List<Proposition> mappedPremises = partitionedPremises.get(false).stream().map(e -> e.accept(propositionVisitor)).collect(Collectors.toList());
        Proposition.PredicateProposition mappedConclusion = (Proposition.PredicateProposition) clause.conclusion.accept(propositionVisitor);

        Map<String, Type> freeVars = new HashMap<>(clause.freeVars);

        for (Expression.FreeVarExpression inline : inlines.keySet()) {
            freeVars.remove(inline.name);
        }

        return new Clause(mappedPremises, mappedConclusion, freeVars);
    }

    private Map<Expression.FreeVarExpression, Expression> getInlines(List<Proposition> premises) {
        Map<Expression.FreeVarExpression, Expression> ret = new HashMap<>();
        for (Proposition proposition : premises) {
            Expression.ComparisonExpression expression = (Expression.ComparisonExpression) ((Proposition.ExpressionProposition) proposition).expression;

            Expression.FreeVarExpression key = (Expression.FreeVarExpression) (expression.expression1 instanceof Expression.FreeVarExpression ? expression.expression1 : expression.expression2);
            Expression value = (expression.expression1 instanceof Expression.FreeVarExpression ? expression.expression2 : expression.expression1);

            ret.put(key, value);
        }

        return ret;
    }

    private Map<Boolean, List<Proposition>> splitPremiseByInline(List<Proposition> premises) {
        class GatherEqualitiesToInlineExpressionVisitor extends DefaultValueExpressionVisitor<Boolean> {
            GatherEqualitiesToInlineExpressionVisitor() {
                super(false);
            }

            @Override
            public Boolean visit(Expression.ComparisonExpression expression) {
                if (expression.operation == Expression.CompOperation.EQ) {
                    return expression.expression1 instanceof Expression.FreeVarExpression || expression.expression2 instanceof Expression.FreeVarExpression;
                }
                return false;
            }
        }

        class GatherEqualitiesPropositionVisitor implements Proposition.Visitor<Boolean> {
            private GatherEqualitiesToInlineExpressionVisitor expressionVisitor = new GatherEqualitiesToInlineExpressionVisitor();

            @Override
            public Boolean visit(Proposition.PredicateProposition proposition) {
                return false;
            }

            @Override
            public Boolean visit(Proposition.ExpressionProposition proposition) {
                return proposition.expression.accept(expressionVisitor);
            }
        }

        GatherEqualitiesPropositionVisitor propositionVisitor = new GatherEqualitiesPropositionVisitor();

        return premises.stream().collect(Collectors.partitioningBy(e -> e.accept(propositionVisitor)));

    }

}
