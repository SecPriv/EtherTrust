package secpriv.horst.translation.visitors;

import secpriv.horst.data.Predicate;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.Proposition.ExpressionProposition;
import secpriv.horst.translation.layout.TypeLayouter;
import secpriv.horst.types.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class InlineTypesPropositionVisitor implements Proposition.Visitor<Proposition> {
    private InlineTypesExpressionVisitor expressionVisitor;
    private Map<Predicate, Predicate> flattenedPredicates = new HashMap<>();
    private TypeLayouter typeLayouter;

    public InlineTypesPropositionVisitor(InlineTypesExpressionVisitor expressionVisitor) {
        this.expressionVisitor = expressionVisitor;
        typeLayouter = expressionVisitor.getTypeLayouter();
    }

    @Override
    public Proposition visit(Proposition.PredicateProposition proposition) {
        //We ignore parameter types, because they have to be primitive by definition i.e. there is no reason for any inlining
        Predicate predicate = flattenedPredicates.computeIfAbsent(proposition.predicate, this::flattenPredicate);

        return new Proposition.PredicateProposition(predicate, proposition.parameters, proposition.arguments.stream().flatMap(a -> a.accept(expressionVisitor).stream()).collect(Collectors.toList()));
    }

    private Predicate flattenPredicate(Predicate predicate) {
        return new Predicate(predicate.name, predicate.parameterTypes, predicate.argumentsTypes.stream().flatMap(t -> typeLayouter.unfoldToBaseTypes(t).stream()).collect(Collectors.toList()));
    }

    @Override
    public Proposition visit(ExpressionProposition proposition) {
        //Only boolean expressions are allowed at this point and the only boolean expressions involving types (equality) have to conjoined at this stage
        return new ExpressionProposition(proposition.expression.accept(expressionVisitor).get(0));
    }
}
