package secpriv.horst.tools;

import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.data.tuples.Tuple4;
import secpriv.horst.data.tuples.Tuple5;

import java.util.*;
import java.util.function.Function;

public class CartesianHelper {
    private CartesianHelper() {
    }

    /**
     * A "normal" CartesianIterator is just a DependentCartesianIterator where the function ignores its argument and
     * always return a fixed set
     */
    private static class CartesianIterator<U, V> extends DependentCartesianIterator<U, V> {
        private CartesianIterator(Iterable<U> a, Iterable<V> b) {
            super(a, x -> b);
        }
    }

    private static class DependentCartesianIterator<U, V> extends OptionalIterator<Tuple2<U, V>> {
        U mostRecentA = null;
        Iterator<U> aIterator;
        Function<U, Iterable<V>> b;
        Iterator<V> bIterator;

        private DependentCartesianIterator(Iterable<U> a, Function<U, Iterable<V>> b) {
            this.b = b;
            aIterator = a.iterator();
            nextA();
        }

        private boolean nextA() {
            if(!aIterator.hasNext()) {
                return false;
            }
            mostRecentA = aIterator.next();
            bIterator = b.apply(mostRecentA).iterator();
            return true;
        }

        @Override
        public Optional<Tuple2<U, V>> maybeNext() {
            if (mostRecentA == null) {
                return Optional.empty();
            }

            while (!bIterator.hasNext()) {
                if (!nextA()) {
                    return Optional.empty();
                }
            }
            return Optional.of(new Tuple2<>(mostRecentA, bIterator.next()));
        }
    }

    public static <U, V> Iterable<Tuple2<U, V>> product(Iterable<U> a, Iterable<V> b) {
        return () -> new CartesianIterator<>(a, b);
    }

    public static <U, V, W> Iterable<Tuple3<U, V, W>> product(Iterable<U> a, Iterable<V> b, Iterable<W> c) {
        return () -> new MappingIterator<>(new CartesianIterator<>(a, product(b, c)), CartesianHelper::flatten2);
    }

    public static <U, V, W, X> Iterable<Tuple4<U, V, W, X>> product(Iterable<U> a, Iterable<V> b, Iterable<W> c, Iterable<X> d) {
        return () -> new MappingIterator<>(new CartesianIterator<>(a, product(b, c, d)), CartesianHelper::flatten3);
    }

    public static <U, V, W, X, Y> Iterable<Tuple5<U, V, W, X, Y>> product(Iterable<U> a, Iterable<V> b, Iterable<W> c, Iterable<X> d,  Iterable<Y> e) {
        return () -> new MappingIterator<>(new CartesianIterator<>(a, product(b, c, d, e)), CartesianHelper::flatten4);
    }

    /**
     * Creates an iterable of tuples, where the second component is dependent on the the first.
     *
     * if a contains 1 and 2 and b associates all odd positive numbers below 5 with 1 and all even positive numbers with 2
     * the returned iterable will iterate over [(1,1), (1,3), (2,2), (2,4)]
     *
     */
    public static <U, V> Iterable<Tuple2<U, V>> dependentProduct(Iterable<U> a, Function<U, Iterable<V>> b) {
        return () -> new DependentCartesianIterator<>(a, b);
    }

    private static <U, V, W> Tuple3<U, V, W> flatten2(Tuple2<U, Tuple2<V, W>> t) {
        return new Tuple3<>(t.v0, t.v1.v0, t.v1.v1);
    }

    private static <U, V, W, X> Tuple4<U, V, W, X> flatten3(Tuple2<U, Tuple3<V, W, X>> t) {
        return new Tuple4<>(t.v0, t.v1.v0, t.v1.v1, t.v1.v2);
    }

    private static <U, V, W, X, Y> Tuple5<U, V, W, X, Y> flatten4(Tuple2<U, Tuple4<V, W, X, Y>> t) {
        return new Tuple5<>(t.v0, t.v1.v0, t.v1.v1, t.v1.v2, t.v1.v3);
    }
}
