package secpriv.horst.internals;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.types.Type;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class SelectorFunctionHelperTest {
    private SelectorFunctionHelper selectorFunctionHelper;

    @BeforeEach
    public void setUp() {
        selectorFunctionHelper = new SelectorFunctionHelper();
    }

    @AfterEach
    public void tearDown() {
        selectorFunctionHelper = null;
    }

    @Test
    public void correctObjectGetCorrectlyRegistered() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void duplicateMethodNamesGetNotRegistered() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();


        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();

        class DuplicateProvider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        DuplicateProvider p1 = new DuplicateProvider();

        assertThat(selectorFunctionHelper.registerProvider(p1)).isEqualTo(0);

    }


    @Test
    public void privateMethodsGetNotRegistered() {
        class Provider {
            private Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(3);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isNotPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void nonParameterizedTypeDoesNotGetRegistered() {
        class Provider {
            public BigInteger f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(3);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isNotPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void nonIterableReturnTypeDoesNotGetRegistered() {
        class Provider {
            public Iterator<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(3);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isNotPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void nonBaseTypeReturnTypeDoesNotGetRegistered() {
        class Provider {
            public Iterable<Integer> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(3);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isNotPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void nonBaseTypeReturnTypeIntTupleDoesNotGetRegistered() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<Integer, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(3);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isNotPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void nonGenericTupleTypeDoesNotGetRegistered() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(3);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isNotPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void nonBasicParameterTypeDoesNotGetRegistered() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(Integer i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(3);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isNotPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void wrongNameNotPresent() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("F1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isNotPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }
    @Test
    public void emptyReturnTypeThrows() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        assertThatThrownBy(() -> selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.emptyList())).isInstanceOf(IllegalArgumentException.class);

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void wrongArgumentTypes() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Integer, Type.Boolean), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isNotPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void wrongReturnTypes() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Boolean, Type.Integer));
        assertThat(optF4).isNotPresent();
    }

    @Test
    public void wrongReturnTypeCount1() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF3).isNotPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void wrongReturnType() {
        class Provider {
            public Iterable<Boolean> f1(Boolean b) { return null; }
            public Iterable<BigInteger> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isNotPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isNotPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }

    @Test
    public void wrongTupleType() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple3<BigInteger, Boolean, BigInteger>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isNotPresent();
    }

    @Test
    public void wrongTupleParameters() {
        class Provider {
            public Iterable<BigInteger> f1(Boolean b) { return null; }
            public Iterable<Boolean> f2(BigInteger i) { return null; }
            public Iterable<Tuple2<Boolean,Boolean>> f3(Boolean b, BigInteger i) { return null; }
            public Iterable<Tuple2<BigInteger, Boolean>> f4() { return null; }
        }

        Provider p = new Provider();

        assertThat(selectorFunctionHelper.registerProvider(p)).isEqualTo(4);

        Optional<Method> optF1 = selectorFunctionHelper.getMethod("f1", Collections.singletonList(Type.Boolean), Collections.singletonList(Type.Integer));
        assertThat(optF1).isPresent();

        Optional<Method> optF2 = selectorFunctionHelper.getMethod("f2", Collections.singletonList(Type.Integer), Collections.singletonList(Type.Boolean));
        assertThat(optF2).isPresent();

        Optional<Method> optF3 = selectorFunctionHelper.getMethod("f3", Arrays.asList(Type.Boolean, Type.Integer), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF3).isNotPresent();

        Optional<Method> optF4 = selectorFunctionHelper.getMethod("f4", Collections.emptyList(), Arrays.asList(Type.Integer, Type.Boolean));
        assertThat(optF4).isPresent();
    }
}