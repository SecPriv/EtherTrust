package secpriv.horst.data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompoundSelectorFunctionInvocation {
    public static final CompoundSelectorFunctionInvocation UnitInvocation = new CompoundSelectorFunctionInvocation(Collections.singletonList(SelectorFunctionInvocation.UnitInvocation));
    public final List<SelectorFunctionInvocation> selectorFunctionInvocations;

    public CompoundSelectorFunctionInvocation(List<SelectorFunctionInvocation> selectorFunctionInvocations) {
        this.selectorFunctionInvocations = Collections.unmodifiableList(Objects.requireNonNull(selectorFunctionInvocations, "SelectorFunctionInvocations may not be null!"));
        if (selectorFunctionInvocations.isEmpty()) {
            throw new IllegalArgumentException("SelectorFunctionInvocations may not be empty!");
        }
    }

    public List<Expression> arguments() {
        return selectorFunctionInvocations.stream().flatMap(s -> s.arguments.stream()).collect(Collectors.toList());
    }

    public List<Expression.ParVarExpression> parameters() {
        return selectorFunctionInvocations.stream().flatMap(s -> s.parameters.stream()).collect(Collectors.toList());
    }

    public CompoundSelectorFunctionInvocation mapArguments(Function<Expression, Expression> function) {
        return new CompoundSelectorFunctionInvocation(selectorFunctionInvocations.stream().map(s -> s.mapArguments(function)).collect(Collectors.toList()));
    }

    public CompoundSelectorFunctionInvocation mapParameters(Function<Expression.ParVarExpression, Expression.ParVarExpression> function) {
        return new CompoundSelectorFunctionInvocation(selectorFunctionInvocations.stream().map(s -> s.mapParameters(function)).collect(Collectors.toList()));
    }
}
