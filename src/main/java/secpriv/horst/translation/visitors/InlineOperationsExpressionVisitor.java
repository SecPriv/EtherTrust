package secpriv.horst.translation.visitors;

import secpriv.horst.data.*;

import java.util.*;
import java.util.stream.Collectors;

public class InlineOperationsExpressionVisitor extends AbstractExpressionVisitor {
    private class PatternRenameVisitor implements Pattern.Visitor<Pattern> {
        @Override
        public Pattern visit(Pattern.ValuePattern pattern) {
            return new Pattern.ValuePattern(pattern.constructor, pattern.patterns.stream().map(p -> p.accept(this)).collect(Collectors.toList()));
        }

        @Override
        public Pattern visit(Pattern.WildcardPattern pattern) {
            if (pattern.name.equals("_")) {
                return pattern;
            }
            return new Pattern.WildcardPattern(renamePrefix + pattern.name);
        }
    }

    private final Map<Expression.ParVarExpression, Expression> parameterBindings;
    private final Map<Expression.VarExpression, Expression> variableBindings;
    private final Map<Operation, Operation> flattenedOperations;
    private final String renamePrefix;
    private final PatternRenameVisitor patternRenameVisitor = new PatternRenameVisitor();

    /**
     * Create new InlineOperationsExpressionVisitor to eliminate AppExpressions
     *
     * @param operations Operation definitions ordered such that operations[i].body contains no call to operations[j] where j >= i
     */
    public InlineOperationsExpressionVisitor(List<Operation> operations) {
        this(Collections.emptyMap(), Collections.emptyMap(), flattenOperations(operations), "");
    }

    private InlineOperationsExpressionVisitor(Map<Expression.ParVarExpression, Expression> parameterBindings, Map<Expression.VarExpression, Expression> variableBindings, Map<Operation, Operation> flattenedOperations, String renamePrefix) {
        this.renamePrefix = renamePrefix;
        this.parameterBindings = Collections.unmodifiableMap(parameterBindings);
        this.variableBindings = Collections.unmodifiableMap(variableBindings);
        this.flattenedOperations = Collections.unmodifiableMap(flattenedOperations);
    }

    private static Operation flattenOperation(Operation operation, Map<Operation, Operation> flattenedOperations) {
        String renamePrefix = operation.name + "&&";
        Expression flattenedBody = operation.body.accept(visitorForFlatten(flattenedOperations, renamePrefix));
        List<Expression.ParVarExpression> renamedParameters = operation.parameters.stream().map(pv -> new Expression.ParVarExpression(pv.type, renamePrefix + pv.name)).collect(Collectors.toList());
        List<Expression.VarExpression> renamedArguments = operation.arguments.stream().map(v -> new Expression.VarExpression(v.type, renamePrefix + v.name)).collect(Collectors.toList());

        return new Operation(operation.name, flattenedBody, renamedParameters, renamedArguments);
    }

    private static Map<Operation, Operation> flattenOperations(List<Operation> operations) {
        Map<Operation, Operation> flattenedOperations = new HashMap<>();
        for (Operation operation : operations) {
            flattenedOperations.put(operation, flattenOperation(operation, flattenedOperations));
        }
        return flattenedOperations;
    }

    private static InlineOperationsExpressionVisitor copyWithoutRenamePrefix(InlineOperationsExpressionVisitor other) {
        return new InlineOperationsExpressionVisitor(other.parameterBindings, other.variableBindings, other.flattenedOperations, "");
    }

    private InlineOperationsExpressionVisitor visitorWithNewBindings(Map<Expression.ParVarExpression, Expression> parameterBindings, Map<Expression.VarExpression, Expression> variableBindings, Map<Operation, Operation> flattenedOperations) {
        return new InlineOperationsExpressionVisitor(parameterBindings, variableBindings, flattenedOperations, "");
    }

    private static InlineOperationsExpressionVisitor visitorForFlatten(Map<Operation, Operation> flattenedOperations, String renamePrefix) {
        return new InlineOperationsExpressionVisitor(new HashMap<>(), new HashMap<>(), flattenedOperations, renamePrefix);
    }

    @Override
    public Expression visit(Expression.VarExpression expression) {
        if (variableBindings.containsKey(expression)) {
            return variableBindings.get(expression).accept(copyWithoutRenamePrefix(this));
        }
        return new Expression.VarExpression(expression.type, renamePrefix + expression.name);
    }

    @Override
    public Expression visit(Expression.ParVarExpression expression) {
        if (parameterBindings.containsKey(expression)) {
            return parameterBindings.get(expression).accept(copyWithoutRenamePrefix(this));
        }
        return new Expression.ParVarExpression(expression.type, renamePrefix + expression.name);
    }

    @Override
    public Expression visit(Expression.AppExpression expression) {
        Operation flattenedOperation = flattenedOperations.get(expression.operation);

        if (flattenedOperation == null) {
            throw new IllegalStateException("Referenced undefined operation, perhaps the supplied list of operations was in the wrong order.");
        }

        Iterator<Expression.ParVarExpression> parameterVarIterator = flattenedOperation.parameters.iterator();

        List<Expression> parameters = expression.parameters.stream().map(e -> e.accept(this)).collect(Collectors.toList());
        List<Expression> arguments = expression.expressions.stream().map(e -> e.accept(this)).collect(Collectors.toList());

        Map<Expression.ParVarExpression, Expression> parameterBindings = new HashMap<>();

        for (Expression p : parameters) {
            parameterBindings.put(parameterVarIterator.next(), p);
        }

        Iterator<Expression.VarExpression> varIterator = flattenedOperation.arguments.iterator();

        Map<Expression.VarExpression, Expression> variableBindings = new HashMap<>();

        for (Expression a : arguments) {
            variableBindings.put(varIterator.next(), a);
        }

        return flattenedOperation.body.accept(visitorWithNewBindings(parameterBindings, variableBindings, flattenedOperations));
    }

    @Override
    public Expression visit(Expression.MatchExpression expression) {
        if (renamePrefix.isEmpty()) {
            return new Expression.MatchExpression(expression.branchPatterns, expression.matchedExpressions.stream().map(e -> e.accept(this)).collect(Collectors.toList()), expression.resultExpressions.stream().map(e -> e.accept(this)).collect(Collectors.toList()));
        }
        return new Expression.MatchExpression(expression.branchPatterns.stream().map(l -> l.stream().map(p -> p.accept(patternRenameVisitor)).collect(Collectors.toList())).collect(Collectors.toList()), expression.matchedExpressions.stream().map(e -> e.accept(this)).collect(Collectors.toList()), expression.resultExpressions.stream().map(e -> e.accept(this)).collect(Collectors.toList()));
    }

    @Override
    public Expression visit(Expression.SumExpression expression) {
        CompoundSelectorFunctionInvocation visitedSelectorFunctionInvocation = expression.selectorFunctionInvocation;
        if (!renamePrefix.isEmpty()) {
            visitedSelectorFunctionInvocation = visitedSelectorFunctionInvocation.mapParameters(p -> new Expression.ParVarExpression(p.type, renamePrefix + p.name));
        }
        SumOperation visitedSumOperation = expression.operation.mapSubExpressions(e -> e.accept(this));
        visitedSelectorFunctionInvocation = visitedSelectorFunctionInvocation.mapArguments(e -> e.accept(this));

        return new Expression.SumExpression(visitedSelectorFunctionInvocation, expression.body.accept(this), visitedSumOperation);
    }
}
