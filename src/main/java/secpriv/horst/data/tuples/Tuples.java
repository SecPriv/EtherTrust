package secpriv.horst.data.tuples;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class Tuples {
    public static Class getClassForParameterCount(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Parameter count has to be positive!");
        }

        switch (n) {
            case 2:
                return Tuple2.class;
            case 3:
                return Tuple3.class;
            case 4:
                return Tuple4.class;
            case 5:
                return Tuple5.class;
            case 6:
                return Tuple6.class;
            case 7:
                return Tuple7.class;
            case 8:
                return Tuple8.class;
            case 9:
                return Tuple9.class;
            case 10:
                return Tuple10.class;
        }

        throw new IllegalArgumentException("Unsupported parameter count " + n + "!");
    }

    public static boolean isTupleType(Type returnType) {
        if (returnType instanceof Class) {
            return Tuple.class.isAssignableFrom((Class) returnType);
        } else if (returnType instanceof ParameterizedType) {
            return Tuple.class.isAssignableFrom((Class) ((ParameterizedType) returnType).getRawType());
        }
        return false;
    }
}
