package secpriv.horst.data;

import java.math.BigInteger;
import java.util.*;

public abstract class BaseTypeValue {
    public interface Visitor<T> {
        T visit(BaseTypeIntegerValue baseTypeValue);

        T visit(BaseTypeBooleanValue baseTypeValue);

        T visit(BaseTypeArrayValue baseTypeValue);
    }

    public static class BaseTypeIntegerValue extends BaseTypeValue {
        public final BigInteger value;

        private BaseTypeIntegerValue(BigInteger value) {
            this.value = Objects.requireNonNull(value, "Value may not be null!");
        }

        @Override
        public String toString() {
            return "BaseTypeIntegerValue{" +
                    "value=" + value +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BaseTypeIntegerValue that = (BaseTypeIntegerValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class BaseTypeBooleanValue extends BaseTypeValue {
        public final Boolean value;

        private BaseTypeBooleanValue(Boolean value) {
            this.value = Objects.requireNonNull(value, "Value may not be null!");
        }

        @Override
        public String toString() {
            return "BaseTypeBooleanValue{" +
                    "value=" + value +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BaseTypeBooleanValue that = (BaseTypeBooleanValue) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public abstract <T> T accept(Visitor<T> visitor);

    public static BaseTypeValue unsafeFromObject(Object o) {
        if (o instanceof BigInteger) {
            return fromBigInteger((BigInteger) o);
        } else if (o instanceof Boolean) {
            return fromBoolean((Boolean) o);
        }

        throw new IllegalArgumentException("Argument must be of type BigInteger or Boolean!");
    }

    public static BaseTypeIntegerValue fromBigInteger(BigInteger i) {
        return new BaseTypeIntegerValue(i);
    }

    public static BaseTypeBooleanValue fromBoolean(Boolean b) {
        return new BaseTypeBooleanValue(b);
    }

    public static class BaseTypeArrayValue extends BaseTypeValue {
        public final BaseTypeValue initializer;
        public final Map<BigInteger, BaseTypeValue> stores;


        private BaseTypeArrayValue(BaseTypeValue initializer) {
            this.initializer = Objects.requireNonNull(initializer, "Initializer may not be null!");
            this.stores = Collections.unmodifiableMap(new HashMap<>());
        }

        private BaseTypeArrayValue(BaseTypeArrayValue arr, BigInteger i, BaseTypeValue val) {
            this.initializer = Objects.requireNonNull(arr.initializer, "BaseTypeArrayValue or its initializer may not be null!");
            Map<BigInteger, BaseTypeValue> stores = new HashMap<>(Objects.requireNonNull(arr.stores, "BaseTypeArrayValue or its stores variable may not be null!"));
            stores.put(i, val);
            this.stores = Collections.unmodifiableMap(stores);
        }

        @Override
        public String toString() {
            return "BaseTypeArrayValue{" +
                    "initializer=" + this.initializer +
                    ", stores=" + this.stores +
                    '}';
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static BaseTypeArrayValue fromInitializer(BaseTypeValue initializer) {
        return new BaseTypeArrayValue(initializer);
    }

    public static BaseTypeArrayValue extendArray(BaseTypeArrayValue arr, BigInteger i, BaseTypeValue val) {
        return new BaseTypeArrayValue(arr, i, val);
    }
}
