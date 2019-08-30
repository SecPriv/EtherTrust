package secpriv.horst.data;

import secpriv.horst.types.Type;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Predicate {
    public final String name;
    public final List<Type> parameterTypes;
    public final List<Type> argumentsTypes;

    public Predicate(String name, List<Type> parameterTypes, List<Type> argumentsTypes) {
        this.name = Objects.requireNonNull(name, "Name may not be null!");
        this.parameterTypes = Collections.unmodifiableList(Objects.requireNonNull(parameterTypes, "ParameterTypes may not be null!"));
        this.argumentsTypes = Collections.unmodifiableList(Objects.requireNonNull(argumentsTypes, "ArgumentTypes may not be null!"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate predicate = (Predicate) o;
        return Objects.equals(name, predicate.name) &&
                Objects.equals(parameterTypes, predicate.parameterTypes) &&
                Objects.equals(argumentsTypes, predicate.argumentsTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameterTypes, argumentsTypes);
    }
}
