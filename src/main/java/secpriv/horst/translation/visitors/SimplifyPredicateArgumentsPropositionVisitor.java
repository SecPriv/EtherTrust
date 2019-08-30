package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;
import secpriv.horst.types.Type;

import java.util.*;

class SimplifyPredicatesData {
    public final Map<String, Type> newFreeVars;
    public final List<Proposition> simplifiedPropositions;

    SimplifyPredicatesData(Map<String, Type> newFreeVars, List<Proposition> simplifiedPropositions) {
        this.newFreeVars = Collections.unmodifiableMap(Objects.requireNonNull(newFreeVars, "NewFreeVars may not be null!"));
        this.simplifiedPropositions = Collections.unmodifiableList(Objects.requireNonNull(simplifiedPropositions, "SimplifiedPropositions may not be null!"));
    }
}

public class SimplifyPredicateArgumentsPropositionVisitor implements Proposition.Visitor<SimplifyPredicatesData> {
    private int extractedArgumentCount = 0;

    @Override
    public SimplifyPredicatesData visit(Proposition.PredicateProposition proposition) {
        Map<String, Type> newFreeVars = new HashMap<>();
        List<Expression> newArguments = new ArrayList<>();
        List<Proposition> simplifiedPropositions = new ArrayList<>();

        for (Expression argument : proposition.arguments) {
            if (argument instanceof Expression.FreeVarExpression) {
                newArguments.add(argument);
            } else {
                String newFreeVarName = extractedArgumentName();

                Expression.FreeVarExpression freeVar = new Expression.FreeVarExpression(argument.getType(), newFreeVarName);
                simplifiedPropositions.add(simplifiedProposition(freeVar, argument));

                newFreeVars.put(newFreeVarName, argument.getType());
                newArguments.add(freeVar);
            }
        }

        simplifiedPropositions.add(new Proposition.PredicateProposition(proposition.predicate, proposition.parameters, newArguments));

        return new SimplifyPredicatesData(newFreeVars, simplifiedPropositions);
    }

    private Proposition simplifiedProposition(Expression.FreeVarExpression freeVar, Expression argument) {
        return new Proposition.ExpressionProposition(new Expression.ComparisonExpression(freeVar, argument, Expression.CompOperation.EQ));
    }

    private String extractedArgumentName() {
        return "??xt?" + (extractedArgumentCount++);
    }

    @Override
    public SimplifyPredicatesData visit(Proposition.ExpressionProposition proposition) {
        return new SimplifyPredicatesData(Collections.emptyMap(), Collections.singletonList(proposition));
    }
}
