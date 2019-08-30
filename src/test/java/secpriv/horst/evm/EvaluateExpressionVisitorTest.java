package secpriv.horst.evm;

import org.junit.jupiter.api.Test;
import secpriv.horst.translation.BitVectorArithmeticEvaluator;

import java.math.BigInteger;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class EvmBitVectorArithmeticEvaluatorTest {
    private BitVectorArithmeticEvaluator evaluator = new EvmBitVectorArithmeticEvaluator();

    @Test
    public void testBitVectorNegationZero() {
        BigInteger v = evaluator.bvneg(BigInteger.ZERO);
        BigInteger expected = BigInteger.ONE.negate();

        assertThat(v).isEqualTo(expected);
    }

    @Test
    public void testBitVectorNegationOne() {
        BigInteger v = evaluator.bvneg(BigInteger.ONE);
        BigInteger expected = BigInteger.valueOf(2).negate();

        assertThat(v).isEqualTo(expected);
    }

    @Test
    public void testBitVectorNegationMinVal() {
        BigInteger v = evaluator.bvneg(BigInteger.valueOf(2).pow(255).negate());
        BigInteger expected = BigInteger.valueOf(2).pow(255).subtract(BigInteger.ONE);

        assertThat(v).isEqualTo(expected);
    }

    @Test
    public void testBitVectorNegationMaxVal() {
        BigInteger v = evaluator.bvneg(BigInteger.valueOf(2).pow(255).subtract(BigInteger.ONE));
        BigInteger expected = BigInteger.valueOf(2).pow(255).negate();

        assertThat(v).isEqualTo(expected);
    }

    @Test
    public void fuzzBitVectorDoubleBitVectorNegation() {
        Random r = new Random(0);

        for (int i = 0; i < 1000000; ++i) {
            BigInteger v1 = new BigInteger(r.nextInt(256), r);
            if (r.nextBoolean()) {
                v1 = v1.add(BigInteger.ONE).negate();
            }
            BigInteger v2 = evaluator.bvneg(evaluator.bvneg(v1));
            assertThat(v2).as("fuzz " + i).isEqualTo(v1);
        }
    }

    @Test
    public void testOverflow() {
        BigInteger v1 = BigInteger.valueOf(2).pow(256);
        BigInteger v2 = evaluator.bvneg(evaluator.bvneg(v1));

        assertThat(v1).isNotEqualTo(v2);
        assertThat(v2).isEqualTo(BigInteger.ZERO);
    }

    @Test
    public void fuzzRandomAndZeroIsZero() {
        Random r = new Random(1);

        for (int i = 0; i < 1000000; ++i) {
            BigInteger v1 = new BigInteger(r.nextInt(256), r);
            if (r.nextBoolean()) {
                v1 = v1.add(BigInteger.ONE).negate();
            }

            BigInteger v2 = evaluator.bvand(v1, BigInteger.ZERO);
            assertThat(v2).as("fuzz " + i).isEqualTo(BigInteger.ZERO);
        }
    }

    @Test
    public void fuzzRandomAndOnesIsRandom() {
        Random r = new Random(1);

        BigInteger ones = BigInteger.ONE.negate();

        for (int i = 0; i < 1000000; ++i) {
            BigInteger v1 = new BigInteger(r.nextInt(256), r);
            if (r.nextBoolean()) {
                v1 = v1.add(BigInteger.ONE).negate();
            }

            BigInteger v2 = evaluator.bvand(v1, ones);
            assertThat(v2).as("fuzz " + i).isEqualTo(v1);
        }
    }

    @Test
    public void fuzzRandomOrZeroIsRandom() {
        Random r = new Random(2);

        for (int i = 0; i < 1000000; ++i) {
            BigInteger v1 = new BigInteger(r.nextInt(256), r);
            if (r.nextBoolean()) {
                v1 = v1.add(BigInteger.ONE).negate();
            }

            BigInteger v2 = evaluator.bvor(v1, BigInteger.ZERO);
            assertThat(v2).as("fuzz " + i).isEqualTo(v1);
        }
    }

    @Test
    public void fuzzRandomOrOnesIsOnes() {
        Random r = new Random(3);
        BigInteger ones = BigInteger.ONE.negate();

        for (int i = 0; i < 1000000; ++i) {
            BigInteger v1 = new BigInteger(r.nextInt(256), r);
            if (r.nextBoolean()) {
                v1 = v1.add(BigInteger.ONE).negate();
            }

            BigInteger v2 = evaluator.bvor(v1, ones);
            assertThat(v2).as("fuzz " + i).isEqualTo(ones);
        }
    }

    @Test
    public void fuzzRandomXOrOnesIsNegRandom() {
        Random r = new Random(4);
        BigInteger ones = BigInteger.ONE.negate();

        for (int i = 0; i < 1000000; ++i) {
            BigInteger v1 = new BigInteger(r.nextInt(256), r);
            if (r.nextBoolean()) {
                v1 = v1.add(BigInteger.ONE).negate();
            }

            BigInteger v2 = evaluator.bvxor(v1, ones);
            BigInteger v3 = evaluator.bvneg(v1);

            assertThat(v2).as("fuzz (xor 0) " + i).isEqualTo(v3);
        }
    }

    @Test
    public void fuzzRandomXOrZeroIsRandom() {
        Random r = new Random(5);

        for (int i = 0; i < 1000000; ++i) {
            BigInteger v1 = new BigInteger(r.nextInt(256), r);
            if (r.nextBoolean()) {
                v1 = v1.add(BigInteger.ONE).negate();
            }

            BigInteger v2 = evaluator.bvxor(v1, BigInteger.ZERO);

            assertThat(v2).as("fuzz (xor 0) " + i).isEqualTo(v1);
        }
    }
}