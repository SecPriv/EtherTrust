package secpriv.horst.internals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IntervalProviderTest {
    private IntervalProvider provider;

    @BeforeEach
    public void setUp() {
        provider = new IntervalProvider();
    }

    @AfterEach
    public void tearDown() {
        provider = null;
    }

    @Test
    public void testIntervalEmpty() {
        Iterable<BigInteger> iterable = provider.interval(BigInteger.ZERO, BigInteger.ZERO);

        assertThat(iterable).isEmpty();
    }

    @Test
    public void testInterval1() {
        Iterable<BigInteger> iterable = provider.interval(BigInteger.ZERO, BigInteger.TEN);

        BigInteger[] expected = new BigInteger[10];

        for (int i = 0; i < 10; ++i) {
            expected[i] = BigInteger.valueOf(i);
        }

        assertThat(iterable).containsExactly(expected);
    }

    @Test
    public void testInterval2() {
        Iterable<BigInteger> iterable = provider.interval(BigInteger.ONE, BigInteger.TEN);

        BigInteger[] expected = new BigInteger[9];

        for (int i = 0; i < 9; ++i) {
            expected[i] = BigInteger.valueOf(i + 1);
        }

        assertThat(iterable).containsExactly(expected);
    }
}