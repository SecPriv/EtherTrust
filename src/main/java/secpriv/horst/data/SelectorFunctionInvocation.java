package secpriv.horst.data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SelectorFunctionInvocation {
    public static final SelectorFunctionInvocation UnitInvocation = new SelectorFunctionInvocation(SelectorFunction.Unit, Collections.emptyList(), Collections.emptyList());
    public final SelectorFunction selectorFunction;
    public final List<Expression.ParVarExpression> parameters;
    public final List<Expression> arguments;

    public SelectorFunctionInvocation(SelectorFunction selectorFunction, List<Expression.ParVarExpression> parameters, List<Expression> selectorFunctionArguments) {
        this.selectorFunction = Objects.requireNonNull(selectorFunction, "SelectorFunction may not be null!");
        this.parameters = Collections.unmodifiableList(Objects.requireNonNull(parameters, "Parameters may not be null!"));
        this.arguments = Collections.unmodifiableList(Objects.requireNonNull(selectorFunctionArguments, "SelectorFunctionArguments may not be null!"));
    }

    public SelectorFunctionInvocation mapArguments(Function<Expression, Expression> function) {
        return new SelectorFunctionInvocation(this.selectorFunction, this.parameters, this.arguments.stream().map(function).collect(Collectors.toList()));
    }

    public SelectorFunctionInvocation mapParameters(Function<Expression.ParVarExpression, Expression.ParVarExpression> function) {
        return new SelectorFunctionInvocation(this.selectorFunction, this.parameters.stream().map(function).collect(Collectors.toList()), this.arguments);
    }
}
