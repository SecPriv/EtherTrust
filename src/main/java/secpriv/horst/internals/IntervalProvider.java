package secpriv.horst.internals;

import java.math.BigInteger;
import java.util.*;

public class IntervalProvider {
    public Iterable<BigInteger> interval(BigInteger start, BigInteger end) {
        return new Iterable<BigInteger>() {
            @Override
            public Iterator<BigInteger> iterator() {
                return new Iterator<BigInteger>() {
                    BigInteger state = start;

                    @Override
                    public boolean hasNext() {
                        return state.compareTo(end) < 0;
                    }

                    @Override
                    public BigInteger next() {
                        BigInteger ret = state;
                        state = state.add(BigInteger.valueOf(1));
                        return ret;
                    }
                };
            }
        };
    }
}