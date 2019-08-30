package secpriv.horst.data;

import secpriv.horst.types.Type;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SelectorFunction {
    public static final SelectorFunction Unit = new SelectorFunction("unit", Collections.emptyList(), Collections.emptyList());
    public final String name;
    public final List<Type> parameterTypes;
    public final List<Type> returnTypes;

    public SelectorFunction(String name, List<Type> parameterTypes, List<Type> returnTypes) {
        this.name = Objects.requireNonNull(name, "Name may not be null!");
        this.parameterTypes  = Collections.unmodifiableList(Objects.requireNonNull(parameterTypes, "ParameterTypes may not be null!"));
        this.returnTypes  = Collections.unmodifiableList(Objects.requireNonNull(returnTypes, "ReturnTypes may not be null!"));
    }
}
