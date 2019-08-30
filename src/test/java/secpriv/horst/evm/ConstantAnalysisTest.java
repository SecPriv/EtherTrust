package secpriv.horst.evm;

import org.jgrapht.io.ExportException;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple5;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class ConstantAnalysisTest {

    @Test // test for and_test.txt
    public void testIdsAndPcsAndArgumentsForOpcodeAND1() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/and_test.txt"), false);
        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 593606337;
        Set<Tuple2<BigInteger, BigInteger>> set = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> rez = new HashSet<>();
        Set<BigInteger> pcs = new HashSet<>();
        // This performs all preprocessing steps
        ca.getBlocksFromBytecode();
        ca.runBlocks();
        ca.printBlocks();
        ca.getCFGs();
        ca.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(22)).forEach(pc -> pcs.add(pc));
        pcs.forEach
                (pc -> ca.argumentsTwoForIdAndPc(BigInteger.valueOf(id), pc).forEach(set::add));
        pcs.forEach
                (pc -> ca.resultsForIdAndPc(BigInteger.valueOf(id), pc).forEach(rez::add));
        assertThat(pcs).hasSize(1);
        assertThat(set).hasSize(1);
        assertThat(rez).hasSize(0);
        assertThat(pcs).contains(BigInteger.valueOf(17));
        assertThat(set).contains(new Tuple2<>(BigInteger.valueOf(255), BigInteger.valueOf(-1)));
    }

    // test for the AND occurrences of 1f
    @Test
    public void testIdsAndPcsAndArgumentsForOpcodeAND2() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/bench/1f.txt"), false);
        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 1383431771; //TODO: to be adapted
        Map<BigInteger, Tuple2<BigInteger, BigInteger>> map = new HashMap<>();
        Map<BigInteger, Tuple2<BigInteger, BigInteger>> rez = new HashMap<>();
        Set<BigInteger> pcs = new HashSet<>();

        // This performs all preprocessing steps
        ca.getBlocksFromBytecode();
        ca.runBlocks();
        ca.printBlocks();
        ca.getCFGs();

        ca.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(22)).forEach(pc -> pcs.add(pc));
        pcs.forEach
                (pc -> ca.argumentsTwoForIdAndPc(BigInteger.valueOf(id), pc).forEach(arg -> map.put(pc, arg)));
        pcs.forEach
                (pc -> ca.resultsForIdAndPc(BigInteger.valueOf(id), pc).forEach(r -> rez.put(pc, r)));
        assertThat(pcs).hasSize(5);
        assertThat(map).hasSize(5);
        assertThat(rez).hasSize(0);
        assertThat(pcs).contains(BigInteger.valueOf(52));
        assertThat(pcs).contains(BigInteger.valueOf(107));
        assertThat(pcs).contains(BigInteger.valueOf(137));
        assertThat(pcs).contains(BigInteger.valueOf(159));
        assertThat(pcs).contains(BigInteger.valueOf(191));
        assertThat(map).containsEntry(BigInteger.valueOf(52), new Tuple2<>(new BigInteger("4294967295"), BigInteger.valueOf(-1)));
        assertThat(map).containsEntry(BigInteger.valueOf(107), new Tuple2<>(new BigInteger("1461501637330902918203684832716283019655932542975"), BigInteger.valueOf(-1)));
        assertThat(map).containsEntry(BigInteger.valueOf(137), new Tuple2<>(new BigInteger("255"), BigInteger.valueOf(-1)));
        assertThat(map).containsEntry(BigInteger.valueOf(159), new Tuple2<>(new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639680"), BigInteger.valueOf(-1)));
        assertThat(map).containsEntry(BigInteger.valueOf(191), new Tuple2<>(new BigInteger("1461501637330902918203684832716283019655932542975"), BigInteger.valueOf(-1)));

        /*assertThat(set).hasSize(5);
        assertThat(set).contains(new Tuple5<>(BigInteger.valueOf(id), BigInteger.valueOf(52), new BigInteger("4294967295"), BigInteger.valueOf(-1), BigInteger.valueOf(-1)));
        assertThat(set).contains(new Tuple5<>(BigInteger.valueOf(id), BigInteger.valueOf(107), new BigInteger("1461501637330902918203684832716283019655932542975"), BigInteger.valueOf(-1), BigInteger.valueOf(-1)));
        assertThat(set).contains(new Tuple5<>(BigInteger.valueOf(id), BigInteger.valueOf(137), new BigInteger("255"), BigInteger.valueOf(-1), BigInteger.valueOf(-1)));
        assertThat(set).contains(new Tuple5<>(BigInteger.valueOf(id), BigInteger.valueOf(159), new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639680"), BigInteger.valueOf(-1), BigInteger.valueOf(-1)));
        assertThat(set).contains(new Tuple5<>(BigInteger.valueOf(id), BigInteger.valueOf(191), new BigInteger("1461501637330902918203684832716283019655932542975"), BigInteger.valueOf(-1), BigInteger.valueOf(-1)));*/
    }

    // test for the OR occurrences of 1f
    @Test
    public void testIdsAndPcsAndArgumentsForOpcodeOR2() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/bench/1f.txt"), false);
        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 1383431771; //TODO: to be adapted
        Set<Tuple2<BigInteger, BigInteger>> set = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> rez = new HashSet<>();
        Set<BigInteger> pcs = new HashSet<>();
        // This performs all preprocessing steps
        ca.getBlocksFromBytecode();
        ca.runBlocks();
        ca.printBlocks();
        ca.getCFGs();
        //ca.splitToStandardAndRichOpcodeToPosition();
        //ca.idsAndPcsAndArgumentsForOpcode(BigInteger.valueOf(23)).forEach(set::add);
        ca.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(23)).forEach(pc -> pcs.add(pc));
        pcs.forEach
                (pc -> ca.argumentsTwoForIdAndPc(BigInteger.valueOf(id), pc).forEach(set::add));
        pcs.forEach
                (pc -> ca.resultsForIdAndPc(BigInteger.valueOf(id), pc).forEach(rez::add));
        assertThat(pcs).hasSize(1);
        assertThat(set).hasSize(1);
        assertThat(rez).hasSize(0);
        assertThat(pcs).contains(BigInteger.valueOf(165));
        assertThat(set).contains(new Tuple2<>(BigInteger.valueOf(1), BigInteger.valueOf(-1)));
        //assertThat(set).hasSize(1);
        //assertThat(set).contains(new Tuple5<>(BigInteger.valueOf(id), BigInteger.valueOf(165), new BigInteger("1"), BigInteger.valueOf(-1), BigInteger.valueOf(-1)));
    }

    @Test
    public void testJumpDestsForIdAndPc() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
        ca.getBlocksFromBytecode();
        ca.runBlocks();
        int id = 583913548;
        Set<BigInteger> set = new HashSet<>();
        ca.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(9)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(21));
        set.clear();
        ca.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(20)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(4));
    }
    @Test
    public void testJumpTargets() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/bench/1f.txt"), false);
        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
        ca.getBlocksFromBytecode();
        ca.runBlocks();
        int id = 1383431771;
        Set<BigInteger> set = new HashSet<>();
        ca.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(11)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(63));
        set.clear();
        ca.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(62)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(68));
        set.clear();
        ca.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(73)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(78));
        set.clear();
        ca.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(119)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(122));
        set.clear();
        ca.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(142)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(215));
        //217 -> tricky
        set.clear();
        ca.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(217)).forEach(set::add);
        assertThat(set).hasSize(6);
        assertThat(set).contains(BigInteger.valueOf(63));
        assertThat(set).contains(BigInteger.valueOf(68));
        assertThat(set).contains(BigInteger.valueOf(78));
        assertThat(set).contains(BigInteger.valueOf(120));
        assertThat(set).contains(BigInteger.valueOf(122));
        assertThat(set).contains(BigInteger.valueOf(215));
    }

}