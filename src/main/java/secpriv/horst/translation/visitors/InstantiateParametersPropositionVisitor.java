package secpriv.horst.translation.visitors;

import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Proposition;
import secpriv.horst.internals.SelectorFunctionInvoker;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class InstantiateParametersPropositionVisitor implements Proposition.Visitor<Proposition> {
    private final Map<String, BaseTypeValue> parameterMap;
    private final SelectorFunctionInvoker selectorFunctionInvoker;

    public InstantiateParametersPropositionVisitor(Map<String, BaseTypeValue> parameterMap, SelectorFunctionInvoker selectorFunctionInvoker) {
        this.parameterMap = Objects.requireNonNull(parameterMap, "ParameterMap may not be null!");
        this.selectorFunctionInvoker = Objects.requireNonNull(selectorFunctionInvoker, "SelectorFunctionInvoker may not be null!");
    }

    @Override
    public Proposition visit(Proposition.PredicateProposition proposition) {
        EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor(parameterMap);
        InstantiateParametersExpressionVisitor expressionVisitor = new InstantiateParametersExpressionVisitor(parameterMap, selectorFunctionInvoker);

        List<BaseTypeValue> parameterValues = proposition.parameters.stream().map(p -> p.accept(expressionVisitor)).map(p -> p.accept(evaluateExpressionVisitor)).collect(Collectors.toList());
        Predicate instantiatedPredicate = instantiatePredicate(proposition.predicate, parameterValues);

        return new Proposition.PredicateProposition(instantiatedPredicate, Collections.emptyList(), proposition.arguments.stream().map(a -> a.accept(expressionVisitor)).collect(Collectors.toList()));
    }

    @Override
    public Proposition visit(Proposition.ExpressionProposition proposition) {
        InstantiateParametersExpressionVisitor expressionVisitor = new InstantiateParametersExpressionVisitor(parameterMap, selectorFunctionInvoker);

        return new Proposition.ExpressionProposition(proposition.expression.accept(expressionVisitor));
    }

    private Predicate instantiatePredicate(Predicate predicate, List<BaseTypeValue> parameterValues) {
        String predicateName = predicate.name;
        if (!parameterValues.isEmpty()) {
            predicateName += "_" + String.join("_", parameterValues.stream().map(b -> b.accept(new ToStringRepresentationBaseTypeValueVisitor())).collect(Collectors.toList()));
        }

        return new Predicate(predicateName, Collections.emptyList(), predicate.argumentsTypes);
    }
}
