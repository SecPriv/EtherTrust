package secpriv.horst.evm;

import org.jgrapht.io.ExportException;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.tuples.Tuple2;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class ContractInfoReaderTest {

    @Test // test for and_test.txt
    public void testForLoadingPrecomputedJumps() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/info_reader_test.json"), false);
        ContractLexer.ContractInfo ci = contractInfoReader.contractInfos.get(new BigInteger("156693190222160447294390249645274463400205975050"));
        assertThat(ci.jumps).hasSize(12);
        assertThat(ci.jumps.get(BigInteger.valueOf(65))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(65)).get(0)).isEqualTo(143);
        assertThat(ci.jumps.get(BigInteger.valueOf(115))).hasSize(0);
        assertThat(ci.jumps.get(BigInteger.valueOf(52))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(52)).get(0)).isEqualTo(134);
        assertThat(ci.jumps.get(BigInteger.valueOf(100))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(100)).get(0)).isEqualTo(106);
        assertThat(ci.jumps.get(BigInteger.valueOf(133))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(133)).get(0)).isEqualTo(163);
        assertThat(ci.jumps.get(BigInteger.valueOf(279))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(279)).get(0)).isEqualTo(145);
        assertThat(ci.jumps.get(BigInteger.valueOf(41))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(41)).get(0)).isEqualTo(116);
        assertThat(ci.jumps.get(BigInteger.valueOf(10))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(10)).get(0)).isEqualTo(53);
        assertThat(ci.jumps.get(BigInteger.valueOf(142))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(142)).get(0)).isEqualTo(145);
        assertThat(ci.jumps.get(BigInteger.valueOf(30))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(30)).get(0)).isEqualTo(66);
        assertThat(ci.jumps.get(BigInteger.valueOf(111))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(111)).get(0)).isEqualTo(173);
        assertThat(ci.jumps.get(BigInteger.valueOf(207))).hasSize(1);
        assertThat(ci.jumps.get(BigInteger.valueOf(207)).get(0)).isEqualTo(274);
    }
}