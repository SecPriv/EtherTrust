package secpriv.horst.evm;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

class EvmTypesTest {
    @Test
    public void simpleTestSignExtend() {
        EvmTypes.UInt256 x = new EvmTypes.UInt256(BigInteger.valueOf(255));

        assertThat(x.signExtend(0).getValue()).isEqualTo(BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE));
        for(int i = 1; i < 32; i++) {
            assertThat(x.signExtend(i).getValue()).isEqualTo(x.getValue());
        }
    }

    @Test
    public void testMultiplesOf255() {
        BigInteger maxInt = BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE);
        String binaryString = "1";
        for(int k = 0; k < 256; ++k) {
            EvmTypes.UInt256 x = new EvmTypes.UInt256(new BigInteger(binaryString, 2));

            for(int j = 0; j < 32; ++j) {
                if((j+1)*8 > binaryString.length()) {
                    assertThat(x.signExtend(j).getValue()).as(binaryString + " extend " + j + " is unaffected").isEqualTo(x.getValue());
                } else {
                    assertThat(x.signExtend(j).getValue()).as(binaryString + " extend " + j + " is affected").isEqualTo(maxInt);
                }
            }
        }
    }

    @Test
    public void fuzzSignExtend() {
        Random r = new Random(1);

        for(int k = 0; k < 2000; ++k) {

            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < 256; ++i) {
                sb.append(r.nextBoolean() ? '0' : '1');
            }

            String binaryString = sb.toString();
            EvmTypes.UInt256 x = new EvmTypes.UInt256(new BigInteger(binaryString, 2));

            for(int j = 0; j < 32; ++j) {
                final int signPosition = 256-((j+1)*8);
                char sign = binaryString.charAt(signPosition);

                String originalPart = binaryString.substring(signPosition, 256);
                String extendedSign = String.join("", Collections.nCopies(signPosition, Character.toString( sign)));
                String extendedBinaryString = extendedSign + originalPart;
                BigInteger extendedValue = new BigInteger(extendedBinaryString, 2);

                assertThat(x.signExtend(j).getValue()).as(binaryString + " extend " + j + " equal to " + extendedBinaryString).isEqualTo(extendedValue);
            }
        }
    }

}