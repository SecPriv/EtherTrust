package secpriv.horst.types;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Constructor {
    public final String name;
    public final List<Type> typeParameters;

    public Constructor(String name, List<Type> typeParameters) {
        this.name = Objects.requireNonNull(name, "name may not be null!");
        this.typeParameters = Collections.unmodifiableList(Objects.requireNonNull(typeParameters, "typeParameters may not be null!"));
    }

    public Constructor(String constructorName) {
        this(constructorName, Collections.emptyList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Constructor that = (Constructor) o;

        if (!name.equals(that.name)) return false;
        return typeParameters.equals(that.typeParameters);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + typeParameters.hashCode();
        return result;
    }

    public static class IntegerConstructor extends Constructor {
        public IntegerConstructor(String name) {
            super(name);
        }
    }
}
