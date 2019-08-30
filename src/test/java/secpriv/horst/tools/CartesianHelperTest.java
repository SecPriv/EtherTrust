package secpriv.horst.tools;

import org.junit.jupiter.api.Test;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.data.tuples.Tuple4;
import secpriv.horst.data.tuples.Tuple5;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartesianHelperTest {
    @Test
    void testProduct2() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple2<BigInteger, BigInteger>> c = CartesianHelper.product(a, b).iterator();

        for (BigInteger aa : a) {
            for (BigInteger bb : b) {
                assertThat(new Tuple2<>(aa, bb)).isEqualTo(c.next());
            }
        }

        assertThat(c.hasNext()).isFalse();
        assertThatThrownBy(c::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct2empty1() {
        List<BigInteger> a = Collections.emptyList();
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple2<BigInteger, BigInteger>> c = CartesianHelper.product(a, b).iterator();

        assertThat(c.hasNext()).isFalse();
        assertThatThrownBy(c::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct2empty2() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Collections.emptyList();

        Iterator<Tuple2<BigInteger, BigInteger>> c = CartesianHelper.product(a, b).iterator();

        assertThat(c.hasNext()).isFalse();
        assertThatThrownBy(c::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct3() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> c = Stream.of(13, 14, 15, 16, 17, 18).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple3<BigInteger, BigInteger, BigInteger>> d = CartesianHelper.product(a, b, c).iterator();

        for (BigInteger aa : a) {
            for (BigInteger bb : b) {
                for (BigInteger cc : c) {
                    assertThat(new Tuple3<>(aa, bb, cc)).isEqualTo(d.next());
                }
            }
        }

        assertThat(d.hasNext()).isFalse();
        assertThatThrownBy(d::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct3empty1() {
        List<BigInteger> a = Collections.emptyList();
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> c = Stream.of(13, 14, 15, 16, 17, 18).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple3<BigInteger, BigInteger, BigInteger>> d = CartesianHelper.product(a, b, c).iterator();

        assertThat(d.hasNext()).isFalse();
        assertThatThrownBy(d::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct3empty2() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Collections.emptyList();
        List<BigInteger> c = Stream.of(13, 14, 15, 16, 17, 18).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple3<BigInteger, BigInteger, BigInteger>> d = CartesianHelper.product(a, b, c).iterator();

        assertThat(d.hasNext()).isFalse();
        assertThatThrownBy(d::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct3empty3() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> c = Collections.emptyList();

        Iterator<Tuple3<BigInteger, BigInteger, BigInteger>> d = CartesianHelper.product(a, b, c).iterator();

        assertThat(d.hasNext()).isFalse();
        assertThatThrownBy(d::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct4() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> c = Stream.of(13, 14, 15, 16, 17, 18).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> d = Stream.of(19, 20, 21, 22, 23, 24).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> e = CartesianHelper.product(a, b, c, d).iterator();

        for (BigInteger aa : a) {
            for (BigInteger bb : b) {
                for (BigInteger cc : c) {
                    for (BigInteger dd : d) {
                        assertThat(new Tuple4<>(aa, bb, cc, dd)).isEqualTo(e.next());
                    }
                }
            }
        }

        assertThat(e.hasNext()).isFalse();
        assertThatThrownBy(e::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct4empty1() {
        List<BigInteger> a = Collections.emptyList();
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> c = Stream.of(13, 14, 15, 16, 17, 18).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> d = Stream.of(19, 20, 21, 22, 23, 24).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> e = CartesianHelper.product(a, b, c, d).iterator();
        assertThat(e.hasNext()).isFalse();
        assertThatThrownBy(e::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct4empty2() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Collections.emptyList();
        List<BigInteger> c = Stream.of(13, 14, 15, 16, 17, 18).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> d = Stream.of(19, 20, 21, 22, 23, 24).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> e = CartesianHelper.product(a, b, c, d).iterator();
        assertThat(e.hasNext()).isFalse();
        assertThatThrownBy(e::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct4empty3() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> c = Collections.emptyList();
        List<BigInteger> d = Stream.of(19, 20, 21, 22, 23, 24).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> e = CartesianHelper.product(a, b, c, d).iterator();
        assertThat(e.hasNext()).isFalse();
        assertThatThrownBy(e::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct4empty4() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> c = Stream.of(13, 14, 15, 16, 17, 18).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> d = Collections.emptyList();

        Iterator<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> e = CartesianHelper.product(a, b, c, d).iterator();
        assertThat(e.hasNext()).isFalse();
        assertThatThrownBy(e::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testProduct5() {
        List<BigInteger> a = Stream.of(1, 2, 3, 4, 5, 6).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> b = Stream.of(7, 8, 9, 10, 11, 12).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> c = Stream.of(13, 14, 15, 16, 17, 18).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> d = Stream.of(19, 20, 21, 22, 23, 24).map(BigInteger::valueOf).collect(Collectors.toList());
        List<BigInteger> e = Stream.of(25, 26, 27, 28, 29, 30).map(BigInteger::valueOf).collect(Collectors.toList());

        Iterator<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> f = CartesianHelper.product(a, b, c, d, e).iterator();

        for (BigInteger aa : a) {
            for (BigInteger bb : b) {
                for (BigInteger cc : c) {
                    for (BigInteger dd : d) {
                        for (BigInteger ee : e) {
                            assertThat(new Tuple5<>(aa, bb, cc, dd, ee)).isEqualTo(f.next());
                        }
                    }
                }
            }
        }

        assertThat(f.hasNext()).isFalse();
        assertThatThrownBy(f::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testDependentProduct() {
        List<Integer> a = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> b1 = Arrays.asList(1, 2, 3);
        List<Integer> b2 = Collections.singletonList(5);

        Function<Integer, Iterable<Integer>> evenOneToThreeOddFive = (x -> x % 2 == 0 ? b1 : b2);

        Iterator<Tuple2<Integer, Integer>> c = CartesianHelper.dependentProduct(a, evenOneToThreeOddFive).iterator();

        for (Integer aa : a) {
            for (Integer bb : aa % 2 == 0 ? b1 : b2) {
                assertThat(new Tuple2<>(aa, bb)).isEqualTo(c.next());
            }
        }

        assertThat(c.hasNext()).isFalse();
        assertThatThrownBy(c::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testDependentProductEmpty1() {
        List<Integer> a = Collections.emptyList();
        List<Integer> b1 = Arrays.asList(1, 2, 3);
        List<Integer> b2 = Collections.singletonList(5);

        Function<Integer, Iterable<Integer>> evenOneToThreeOddFive = (x -> x % 2 == 0 ? b1 : b2);

        Iterator<Tuple2<Integer, Integer>> c = CartesianHelper.dependentProduct(a, evenOneToThreeOddFive).iterator();

        assertThat(c.hasNext()).isFalse();
        assertThatThrownBy(c::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testDependentProductEmpty2() {
        List<Integer> a = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> b1 = Arrays.asList(1, 2, 3);
        List<Integer> b2 = Collections.emptyList();

        Function<Integer, Iterable<Integer>> evenOneToThreeOddEmpty = (x -> x % 2 == 0 ? b1 : b2);

        Iterator<Tuple2<Integer, Integer>> c = CartesianHelper.dependentProduct(a, evenOneToThreeOddEmpty).iterator();

        for (Integer aa : a) {
            for (Integer bb : aa % 2 == 0 ? b1 : b2) {
                assertThat(new Tuple2<>(aa, bb)).isEqualTo(c.next());
            }
        }

        assertThat(c.hasNext()).isFalse();
        assertThatThrownBy(c::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testDependentProductEmpty3() {
        List<Integer> a = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> b1 = Collections.emptyList();
        List<Integer> b2 = Collections.singletonList(5);

        Function<Integer, Iterable<Integer>> evenEmptyOddFive = (x -> x % 2 == 0 ? b1 : b2);

        Iterator<Tuple2<Integer, Integer>> c = CartesianHelper.dependentProduct(a, evenEmptyOddFive).iterator();

        for (Integer aa : a) {
            for (Integer bb : aa % 2 == 0 ? b1 : b2) {
                assertThat(new Tuple2<>(aa, bb)).isEqualTo(c.next());
            }
        }

        assertThat(c.hasNext()).isFalse();
        assertThatThrownBy(c::next).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testDependentProductEmpty4() {
        List<Integer> a = Arrays.asList(0,1,2,3,4);

        Function<Integer, Iterable<Integer>> returnEmpty = x -> Collections.emptyList();

        Iterator<Tuple2<Integer, Integer>> c = CartesianHelper.dependentProduct(a, returnEmpty).iterator();

        assertThat(c.hasNext()).isFalse();
        assertThatThrownBy(c::next).isInstanceOf(NoSuchElementException.class);
    }
}