package secpriv.horst.data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Operation {
    public final List<Expression.ParVarExpression> parameters;
    public final List<Expression.VarExpression> arguments;
    public final String name;
    public final Expression body;

    public Operation(String name, Expression body, List<Expression.ParVarExpression> parameters, List<Expression.VarExpression> arguments)  {
        this.name = Objects.requireNonNull(name, "Name may not be null!");
        this.body = Objects.requireNonNull(body, "Body may not be null!");
        this.parameters = Collections.unmodifiableList(Objects.requireNonNull(parameters, "Parameters may not be null!"));
        this.arguments = Collections.unmodifiableList(Objects.requireNonNull(arguments, "Body may not be null!"));
    }
}
