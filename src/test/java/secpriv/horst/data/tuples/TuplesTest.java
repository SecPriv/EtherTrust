package secpriv.horst.data.tuples;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.*;

class TuplesTest {

    public Iterable<Tuple2> testMethod1() {
        return null;
    }

    public Iterable<Tuple2<Boolean,Boolean>> testMethod2() {
        return null;
    }

    public Iterable<Boolean> testMethod3() {
        return null;
    }

    @Test
    public void rawTupleTypeIsTupleType() throws NoSuchMethodException {
        Method m = this.getClass().getMethod("testMethod1");
        Type t = m.getGenericReturnType();

        assertThat(t).isInstanceOfSatisfying(ParameterizedType.class, tt -> assertThat(Tuples.isTupleType(tt.getActualTypeArguments()[0])).isTrue());
    }

    @Test
    public void genericTupleTypeIsTupleType() throws NoSuchMethodException {
        Method m = this.getClass().getMethod("testMethod2");
        Type t = m.getGenericReturnType();

        assertThat(t).isInstanceOfSatisfying(ParameterizedType.class, tt -> assertThat(Tuples.isTupleType(tt.getActualTypeArguments()[0])).isTrue());
    }

    @Test
    public void iterableIsNotTupleType() throws NoSuchMethodException {
        Method m = this.getClass().getMethod("testMethod3");
        Type t = m.getGenericReturnType();

        assertThat(t).isInstanceOfSatisfying(ParameterizedType.class, tt -> assertThat(Tuples.isTupleType(tt.getActualTypeArguments()[0])).isFalse());
    }

}