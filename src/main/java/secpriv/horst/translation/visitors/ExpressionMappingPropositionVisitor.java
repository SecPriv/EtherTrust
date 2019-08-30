package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExpressionMappingPropositionVisitor implements Proposition.Visitor<Proposition> {
    private final Expression.Visitor<Expression> expressionVisitor;

    public ExpressionMappingPropositionVisitor(Expression.Visitor<Expression> expressionVisitor) {
        this.expressionVisitor = Objects.requireNonNull(expressionVisitor, "ExpressionVisitor may not be null");
    }

    @Override
    public Proposition visit(Proposition.PredicateProposition proposition) {
        List<Expression> visitedArguments = proposition.arguments.stream().map(e -> e.accept(expressionVisitor)).collect(Collectors.toList());
        List<Expression> visitedParameters = proposition.parameters.stream().map(e -> e.accept(expressionVisitor)).collect(Collectors.toList());

        return new Proposition.PredicateProposition(proposition.predicate, visitedParameters, visitedArguments);
    }

    @Override
    public Proposition visit(Proposition.ExpressionProposition proposition) {
        return new Proposition.ExpressionProposition(proposition.expression.accept(expressionVisitor));
    }
}
