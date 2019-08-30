package secpriv.horst.evm;

import secpriv.horst.translation.BitVectorArithmeticEvaluator;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.BiFunction;

public class EvmBitVectorArithmeticEvaluator implements BitVectorArithmeticEvaluator {
    private static final int BIT_WIDTH = 256;

    @Override
    public BigInteger trim(BigInteger v) {
        if (v.bitLength() > BIT_WIDTH) {
            byte[] a = v.toByteArray();
            a = Arrays.copyOfRange(a, a.length - (BIT_WIDTH / 8), a.length);
            return new BigInteger(a);
        }
        return v;
    }

    @Override
    public BigInteger bvand(BigInteger v1, BigInteger v2) {
        return onTrimmed(BigInteger::and, v1, v2);
    }

    @Override
    public BigInteger bvxor(BigInteger v1, BigInteger v2) {
        return onTrimmed(BigInteger::xor, v1, v2);
    }

    @Override
    public BigInteger bvor(BigInteger v1, BigInteger v2) {
        return onTrimmed(BigInteger::or, v1, v2);
    }

    @Override
    public BigInteger bvneg(BigInteger v) {
        return trim(v).not();
    }

    private BigInteger onTrimmed(BiFunction<BigInteger, BigInteger, BigInteger> operation, BigInteger v1, BigInteger v2) {
        return operation.apply(trim(v1), trim(v2));
    }

}
