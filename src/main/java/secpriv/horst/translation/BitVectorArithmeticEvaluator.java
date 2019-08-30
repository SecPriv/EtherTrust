package secpriv.horst.translation;

import java.math.BigInteger;

public interface BitVectorArithmeticEvaluator {
    BigInteger bvand(BigInteger v1, BigInteger v2);
    BigInteger bvxor(BigInteger v1, BigInteger v2);
    BigInteger bvor(BigInteger v1, BigInteger v2);
    BigInteger bvneg(BigInteger v);
    public BigInteger trim(BigInteger v);
}
