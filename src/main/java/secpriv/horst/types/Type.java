package secpriv.horst.types;

import java.util.*;

public abstract class Type {
    public static final NonParameterizedType Boolean = new BooleanType();
    public static final NonParameterizedType Integer = new IntegerType();

    public interface Visitor<T> {
        T visit(BooleanType type);

        T visit(IntegerType type);

        T visit(CustomType type);

        T visit(ArrayType type);
    }

    public final String name;

    private Type(String name) {
        this.name = Objects.requireNonNull(name, "Name may not be null!");
    }

    @Override
    public String toString() {
        return "Type{name=" + name + "}";
    }

    public abstract <T> T accept(Visitor<T> visitor);

    public static abstract class NonParameterizedType extends Type {
        public NonParameterizedType(String name) {
            super(name);
        }

        public abstract boolean hasConstructor(String constructorName);

        public abstract Optional<Constructor> getConstructorByName(String constructorName);
    }


    public static abstract class FinitelyConstructedType extends NonParameterizedType {
        public final List<Constructor> constructors;

        public FinitelyConstructedType(String name, List<Constructor> constructors) {
            super(name);
            this.constructors = Collections.unmodifiableList(Objects.requireNonNull(constructors, "Constructors may not be null!"));
        }

        @Override
        public boolean hasConstructor(String constructorName) {
            return constructors.stream().anyMatch(c -> c.name.equals(constructorName));
        }

        @Override
        public Optional<Constructor> getConstructorByName(String constructorName) {
            return constructors.stream().filter(c -> c.name.equals(constructorName)).findFirst();
        }
    }

    public static class IntegerType extends NonParameterizedType {
        private static final String INTEGER_PATTERN = "(~)?[\\d]+";

        private IntegerType() {
            super("int");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean hasConstructor(String constructorName) {
            return constructorName.matches(INTEGER_PATTERN);
        }

        @Override
        public Optional<Constructor> getConstructorByName(String constructorName) {
            if(!constructorName.matches(INTEGER_PATTERN)) {
                return Optional.empty();
            }
            return Optional.of(new Constructor.IntegerConstructor(constructorName.replaceFirst("~", "-")));
        }
    }

    public static class BooleanType extends FinitelyConstructedType {
        private BooleanType() {
            super("bool", booleanConstructors());
        }

        private static List<Constructor> booleanConstructors() {
            ArrayList<Constructor> ret = new ArrayList<>();
            ret.add(new Constructor("true", Collections.emptyList()));
            ret.add(new Constructor("false", Collections.emptyList()));
            return ret;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CustomType extends FinitelyConstructedType {
        public CustomType(String name, List<Constructor> constructors) {
            super(name, constructors);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomType that = (CustomType) o;
            return Objects.equals(constructors, that.constructors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(constructors);
        }
    }

    public static final class ArrayCreationHelper {
        private ArrayCreationHelper() {}
        public ArrayType of(Type type) {
            return new ArrayType(type);
        }
    }

    public static final ArrayCreationHelper Array = new ArrayCreationHelper();

    public static class ArrayType extends Type {
        public final Type type;

        private ArrayType(Type type) {
            super("ArrayOf(" + type.name + ")");
            this.type = Objects.requireNonNull(type, "Type may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArrayType arrayType = (ArrayType) o;
            return Objects.equals(type, arrayType.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }

}
