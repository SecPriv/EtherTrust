package secpriv.horst.evm;

import com.sun.org.apache.bcel.internal.generic.PUSH;
import org.jgrapht.io.ExportException;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.tuples.Tuple4;
import secpriv.horst.data.tuples.Tuple2;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class EVMSelectorFunctionProviderTest {

    @Test
    public void testJumpDestsForIdAndPcUniqueWithTargetsFromJson() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/info_reader_test.json"), false);
        BigInteger id = new BigInteger("156693190222160447294390249645274463400205975050");
        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
        final Set<BigInteger> pcsJump = new HashSet<>();
        Map<BigInteger, BigInteger> map = new HashMap<>();
        Map<BigInteger, Boolean> mapUnique = new HashMap<>();
        ca.pcsForIdAndOpcode(id, BigInteger.valueOf(86)).forEach(pc -> pcsJump.add(pc));
        assertThat(pcsJump).hasSize(5);
        pcsJump.forEach(pc -> ca.jumpDestsForIdAndPc(id, pc).forEach(t -> map.put(pc, t)));
        pcsJump.forEach(pc -> ca.jumpDestUniqueForIdAndPc(id, pc).forEach(t -> mapUnique.put(pc, t)));
        final Set<BigInteger> pcsJumpi = new HashSet<>();
        ca.pcsForIdAndOpcode(id, BigInteger.valueOf(87)).forEach(pc -> pcsJumpi.add(pc));
        pcsJumpi.forEach(pc -> ca.jumpDestsForIdAndPc(id, pc).forEach(t -> map.put(pc, t)));
        pcsJumpi.forEach(pc -> ca.jumpDestUniqueForIdAndPc(id, pc).forEach(t -> mapUnique.put(pc, t)));
        assertThat(pcsJumpi).hasSize(7);

        assertThat(map).hasSize(11);
        assertThat(map).doesNotContainKey(BigInteger.valueOf(115));
        assertThat(map).containsEntry(BigInteger.valueOf(65), BigInteger.valueOf(143));
        assertThat(map).containsEntry(BigInteger.valueOf(52), BigInteger.valueOf(134));
        assertThat(map).containsEntry(BigInteger.valueOf(100), BigInteger.valueOf(106));
        assertThat(map).containsEntry(BigInteger.valueOf(133), BigInteger.valueOf(163));
        assertThat(map).containsEntry(BigInteger.valueOf(279), BigInteger.valueOf(145));
        assertThat(map).containsEntry(BigInteger.valueOf(41), BigInteger.valueOf(116));
        assertThat(map).containsEntry(BigInteger.valueOf(10), BigInteger.valueOf(53));
        assertThat(map).containsEntry(BigInteger.valueOf(142), BigInteger.valueOf(145));
        assertThat(map).containsEntry(BigInteger.valueOf(30), BigInteger.valueOf(66));
        assertThat(map).containsEntry(BigInteger.valueOf(111), BigInteger.valueOf(173));
        assertThat(map).containsEntry(BigInteger.valueOf(207), BigInteger.valueOf(274));

        assertThat(mapUnique).hasSize(12);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(115), false);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(65), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(52), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(100), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(133), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(279), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(41), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(10), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(142), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(30), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(111), true);
        assertThat(mapUnique).containsEntry(BigInteger.valueOf(207), true);
    }
    @Test
    public void testArgumentsOneForIdAndPcsForPush() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"),
                false);
        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 583913548;
        Map<BigInteger, BigInteger> map = new HashMap<>();
        Map<BigInteger, Tuple2<BigInteger, BigInteger>> rez = new HashMap<>();
        Set<BigInteger> pcs = new HashSet<>();

        // This performs all preprocessing steps
        ca.getBlocksFromBytecode();
        ca.runBlocks();
        ca.printBlocks();
        ca.getCFGs();

        ca.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(96)).forEach(pc -> pcs.add(pc));
        pcs.forEach
                (pc -> ca.argumentsOneForIdAndPc(BigInteger.valueOf(id), pc).forEach(arg -> map.put(pc, arg)));
        pcs.forEach
                (pc -> ca.resultsForIdAndPc(BigInteger.valueOf(id), pc).forEach(r -> rez.put(pc, r)));
        assertThat(pcs).hasSize(6);
        assertThat(map).hasSize(6);
        assertThat(rez).hasSize(0);
        assertThat(map).containsEntry(BigInteger.valueOf(0), BigInteger.valueOf(0));
        assertThat(map).containsEntry(BigInteger.valueOf(2), BigInteger.valueOf(10));
        assertThat(map).containsEntry(BigInteger.valueOf(7), BigInteger.valueOf(21));
        assertThat(map).containsEntry(BigInteger.valueOf(14), BigInteger.valueOf(1));
        assertThat(map).containsEntry(BigInteger.valueOf(18), BigInteger.valueOf(4));
        assertThat(map).containsEntry(BigInteger.valueOf(23), BigInteger.valueOf(0));
    }


    @Test
    public void testPcsForIdAndOpcode() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        ConstantAnalysis ca = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 583913548;
        Set<BigInteger> pcs = new HashSet<>();

        // This performs all preprocessing steps
        ca.getBlocksFromBytecode();
        ca.runBlocks();
        ca.printBlocks();
        ca.getCFGs();

        ca.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(91)).forEach(pc -> pcs.add(pc));
        assertThat(pcs).hasSize(2);
        assertThat(pcs).contains(BigInteger.valueOf(4));
        assertThat(pcs).contains(BigInteger.valueOf(21));
    }

    @Test
    public void testJumpDestsForIdAndPc() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        int id = 583913548;
        Set<BigInteger> set = new HashSet<>();
        provider.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(9)).forEach(set::add);
        assertThat(set).hasSize(2);
        assertThat(set).contains(BigInteger.valueOf(4));
        assertThat(set).contains(BigInteger.valueOf(21));
        set.clear();
        provider.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(20)).forEach(set::add);
        assertThat(set).hasSize(2);
        assertThat(set).contains(BigInteger.valueOf(4));
        assertThat(set).contains(BigInteger.valueOf(21));
    }

    @Test
    public void testInterval_starts_with_0() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        Set<BigInteger> set = new HashSet<>();
        provider.interval(new BigInteger("0"), new BigInteger("10")).forEach(set::add);
        assertThat(set).hasSize(10);
        assertThat(set).contains(BigInteger.valueOf(0));
        assertThat(set).contains(BigInteger.valueOf(1));
        assertThat(set).contains(BigInteger.valueOf(2));
        assertThat(set).contains(BigInteger.valueOf(3));
        assertThat(set).contains(BigInteger.valueOf(4));
        assertThat(set).contains(BigInteger.valueOf(5));
        assertThat(set).contains(BigInteger.valueOf(6));
        assertThat(set).contains(BigInteger.valueOf(7));
        assertThat(set).contains(BigInteger.valueOf(8));
        assertThat(set).contains(BigInteger.valueOf(9));
    }

    @Test
    public void testInterval() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        Set<BigInteger> set = new HashSet<>();
        provider.interval(new BigInteger("0"), new BigInteger("0")).forEach(set::add);
        assertThat(set).hasSize(0);
    }

    //TODO: Discuss with Ilya whether these functions are used at the moment (I (Clara) don't think so)
    /*
    @Test
    public void testIdsAndPcsAndJumpDestsForOpcode1()throws IOException {
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(Arrays.asList("jumpApproximation=true","data/sum_run.txt"));
        int id = 583913548;
        Set<Tuple3<BigInteger, BigInteger, BigInteger>> set = new HashSet<>();
        provider.idsAndPcsAndJumpDestsForOpcode(BigInteger.valueOf(ContractLexer.Opcode.JUMPI.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(new Tuple3<>(BigInteger.valueOf(id), BigInteger.valueOf(9), BigInteger.valueOf(21)));
    }

    @Test
    public void testIdsAndPcsAndJumpDestsForOpcode2()throws IOException {
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(Arrays.asList("jumpApproximation=true","data/sum_run.txt"));
        int id = 583913548;
        Set<Tuple3<BigInteger, BigInteger, BigInteger>> set = new HashSet<>();
        provider.idsAndPcsAndJumpDestsForOpcode(BigInteger.valueOf(ContractLexer.Opcode.JUMP.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(new Tuple3<>(BigInteger.valueOf(id), BigInteger.valueOf(20), BigInteger.valueOf(4)));
    } */

    /*
    @Test
    public void testIdsAndPcsAndJumpDestsForOpcode3()throws IOException {
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(Arrays.asList("data/sum_run.txt"));
        int id = 583913548;
        Set<Tuple3<BigInteger, BigInteger, BigInteger>> set = new HashSet<>();
        provider.idsAndPcsAndJumpDestsForOpcode(BigInteger.valueOf(ContractLexer.Opcode.JUMPI.opcode)).forEach(set::add);
        assertThat(set).hasSize(2);
        assertThat(set).contains(new Tuple3<>(BigInteger.valueOf(id), BigInteger.valueOf(9), BigInteger.valueOf(21)));
        assertThat(set).contains(new Tuple3<>(BigInteger.valueOf(id), BigInteger.valueOf(9), BigInteger.valueOf(4)));
    }

    @Test
    public void testIdsAndPcsAndJumpDestsForOpcode4()throws IOException {
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(Arrays.asList("data/sum_run.txt"));
        int id = 583913548;
        Set<Tuple3<BigInteger, BigInteger, BigInteger>> set = new HashSet<>();
        provider.idsAndPcsAndJumpDestsForOpcode(BigInteger.valueOf(ContractLexer.Opcode.JUMP.opcode)).forEach(set::add);
        assertThat(set).hasSize(2);
        assertThat(set).contains(new Tuple3<>(BigInteger.valueOf(id), BigInteger.valueOf(20), BigInteger.valueOf(4)));
        assertThat(set).contains(new Tuple3<>(BigInteger.valueOf(id), BigInteger.valueOf(20), BigInteger.valueOf(21)));
    } */

    @Test
    public void sizeAndOffSetForWordsize1() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        Set<Tuple2<BigInteger, BigInteger>> set = new HashSet<>();
        provider.sizeAndOffSetForWordsize(new BigInteger("1")).forEach(set::add);
        assertThat(set).hasSize(2);
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(0)));

    }

    @Test
    public void sizeAndOffSetForWordsize2() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        Set<Tuple2<BigInteger, BigInteger>> set = new HashSet<>();
        provider.sizeAndOffSetForWordsize(new BigInteger("2")).forEach(set::add);
        assertThat(set).hasSize(5);
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(2), BigInteger.valueOf(0)));
    }

    @Test
    public void sizeAndOffSetForWordsize3() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        Set<Tuple2<BigInteger, BigInteger>> set = new HashSet<>();
        provider.sizeAndOffSetForWordsize(new BigInteger("3")).forEach(set::add);
        assertThat(set).hasSize(9);
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(2)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(2), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(3), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(2)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(2), BigInteger.valueOf(1)));
    }

    @Test
    public void sizeAndOffSetForWordsize4() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        Set<Tuple2<BigInteger, BigInteger>> set = new HashSet<>();
        provider.sizeAndOffSetForWordsize(new BigInteger("4")).forEach(set::add);
        assertThat(set).hasSize(14);
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(2)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(2), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(3), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(3)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(2)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(2), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(4), BigInteger.valueOf(0)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(3)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(3), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(2), BigInteger.valueOf(2)));
    }

    @Test
    public void sizeAndOffSetForWordsize5() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        Set<Tuple2<BigInteger, BigInteger>> set = new HashSet<>();
        provider.sizeAndOffSetForWordsize(new BigInteger("32")).forEach(set::add);
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(31), BigInteger.valueOf(1)));
        assertThat(set).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(32), BigInteger.valueOf(0)));
    }

    @Test
    public void idsTest1() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/bench/1f.txt"), false);
        EvmSelectorFunctionProvider ca = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        int id = 1383431771;
        Set<BigInteger> set = new HashSet<>();
        ca.ids().forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(id));
    }

    @Test
    public void pcsForIdAndOpcodeTest1() throws IOException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        EvmSelectorFunctionProvider provider = new EvmSelectorFunctionProvider(contractInfoReader.getContractInfos());
        int id = 583913548;
        Set<BigInteger> set = new HashSet<>();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.PUSH1.opcode)).forEach(set::add);
        assertThat(set).hasSize(6);
        assertThat(set).contains(BigInteger.valueOf(0));
        assertThat(set).contains(BigInteger.valueOf(2));
        assertThat(set).contains(BigInteger.valueOf(7));
        assertThat(set).contains(BigInteger.valueOf(14));
        assertThat(set).contains(BigInteger.valueOf(18));
        assertThat(set).contains(BigInteger.valueOf(23));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.JUMPDEST.opcode)).forEach(set::add);
        assertThat(set).hasSize(2);
        assertThat(set).contains(BigInteger.valueOf(4));
        assertThat(set).contains(BigInteger.valueOf(21));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.DUP1.opcode)).forEach(set::add);
        assertThat(set).hasSize(2);
        assertThat(set).contains(BigInteger.valueOf(5));
        assertThat(set).contains(BigInteger.valueOf(10));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.ISZERO.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(6));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.JUMPI.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(9));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.SWAP2.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(11));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.ADD.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(12));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.SWAP1.opcode)).forEach(set::add);
        assertThat(set).hasSize(2);
        assertThat(set).contains(BigInteger.valueOf(13));
        assertThat(set).contains(BigInteger.valueOf(16));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.SUB.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(17));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.JUMP.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(20));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.POP.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(22));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.SSTORE.opcode)).forEach(set::add);
        assertThat(set).hasSize(1);
        assertThat(set).contains(BigInteger.valueOf(25));
        set.clear();
        provider.pcsForIdAndOpcode(BigInteger.valueOf(id), BigInteger.valueOf(ContractLexer.Opcode.CALL.opcode)).forEach(set::add);
        assertThat(set).hasSize(0);
    }

    @Test
    public void resultsForIdAndPcTest1() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 583913548;
        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<Tuple2<BigInteger, BigInteger>> set = new HashSet<>();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(0)).forEach(set::add);
        assertThat(set).hasSize(0); // it seems that the tuple is inserted even though it does not have a result

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(2)).forEach(set::add);
        assertThat(set).hasSize(0); // it seems that the tuple is inserted even though it does not have a result

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(4)).forEach(set::add);
        assertThat(set).hasSize(0); // it seems that the tuple is inserted even though it does not have a result

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(5)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(6)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(7)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(9)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(10)).forEach(set::add); // the DUP1 should not have a precomputed argument as it is at the beginning of a block
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(11)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(12)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(13)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(14)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(16)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(17)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(18)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(20)).forEach(set::add); // JUMP targets are not computed as arguments
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(21)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(22)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(23)).forEach(set::add);
        assertThat(set).hasSize(0);

        set.clear();
        provider.resultsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(23)).forEach(set::add);
        assertThat(set).hasSize(0);
    }

    // CUSTOM result tests for the opcodes
    @Test
    public void resultsForIdAndPcADDTest1() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("6001600201", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(3), BigInteger.valueOf(2)));
        set2.clear();

        provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.ADD.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(4));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [3] PUSH1 0x02
    [4] ADD
     */

    @Test
    public void resultsForIdAndPcADDTest2() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("60014201", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(3)).forEach(set2::add);
        assertThat(set2).hasSize(0);

        provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(3)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(-1), BigInteger.valueOf(1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(3))).isInstanceOf(IllegalArgumentException.class);

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.ADD.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(3));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [2] TIMESTAMP
    [3] ADD
     */

    @Test
    public void resultsForIdAndPcMULTest1() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("6001600202", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(2), BigInteger.valueOf(2)));
        set2.clear();

        provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.MUL.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(4));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [3] PUSH1 0x02
    [4] MUL
     */

    @Test
    public void resultsForIdAndPcMULTest2() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("60014202", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(3)).forEach(set2::add);
        assertThat(set2).hasSize(0);

        provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(3)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(-1), BigInteger.valueOf(1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(3))).isInstanceOf(IllegalArgumentException.class);

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.MUL.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(3));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [2] TIMESTAMP
    [3] MUL
     */

    @Test
    public void resultsForIdAndPcMLOADTest1() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("600051", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(2)).forEach(set2::add); //TODO: @Ilya: If we access the relation for a position that does not exist, we through a null pointer exception. Probably we want to have better error-reporting here
        assertThat(set2).hasSize(0);

        provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(2)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(0));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(2))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(2))).isInstanceOf(IllegalArgumentException.class);

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.MLOAD.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(2));
        set1.clear();
    }
    /*
    [1] PUSH1 0x00
    [2] MLOAD
     */

    @Test
    public void resultsForIdAndPcMLOADTest2() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("4251", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(1)).forEach(set2::add);
        assertThat(set2).hasSize(0);

        provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(1)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(-1));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(1))).isInstanceOf(IllegalArgumentException.class);

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.MLOAD.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(1));
        set1.clear();
    }
    /*
    [0] TIMESTAMP
    [1] MLOAD
     */

    @Test
    public void resultsForIdAndPcSWAPTest1() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("6001600290", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(2), BigInteger.valueOf(1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.SWAP1.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(4));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [3] PUSH1 0x02
    [4] SWAP1
     */

    @Test
    public void resultsForIdAndPcSWAPTest2() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("60014290", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(3)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(3)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(-1), BigInteger.valueOf(1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(3))).isInstanceOf(IllegalArgumentException.class);

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.SWAP1.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(3));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [2] TIMESTAMP
    [3] SWAP1
     */

    @Test
    public void resultsForIdAndPcSWAPTest3() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("600160026003600460056006600760086009600a600b600c600d600e600f601060119f", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(34)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(34)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(17), BigInteger.valueOf(1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(34))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(34))).isInstanceOf(IllegalArgumentException.class);

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.SWAP16.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(34));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [3] PUSH1 0x02
    [5] PUSH1 0x03
    [7] PUSH1 0x04
    [9] PUSH1 0x05
    [11] PUSH1 0x06
    [13] PUSH1 0x07
    [15] PUSH1 0x08
    [17] PUSH1 0x09
    [19] PUSH1 0x0a
    [21] PUSH1 0x0b
    [23] PUSH1 0x0c
    [25] PUSH1 0x0d
    [27] PUSH1 0x0e
    [29] PUSH1 0x0f
    [31] PUSH1 0x10
    [33] PUSH1 0x11
    [34] SWAP16
     */

    @Test
    public void resultsForIdAndPcDUPTest1() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("600180", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(2)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(1), BigInteger.valueOf(0)));
        set2.clear();

        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.DUP1.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(2));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [2] DUP1
     */

    @Test
    public void resultsForIdAndPcDUPTest2() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("600160026003600460056006600760086009600a600b600c600d600e600f60108f", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(32)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<>(BigInteger.valueOf(1), BigInteger.valueOf(0)));
        set2.clear();
        provider.pcsForIdAndOpcode(id, BigInteger.valueOf(ContractLexer.Opcode.DUP16.opcode)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(32));
        set1.clear();
    }
    /*
    [1] PUSH1 0x01
    [3] PUSH1 0x02
    [5] PUSH1 0x03
    [7] PUSH1 0x04
    [9] PUSH1 0x05
    [11] PUSH1 0x06
    [13] PUSH1 0x07
    [15] PUSH1 0x08
    [17] PUSH1 0x09
    [19] PUSH1 0x0a
    [21] PUSH1 0x0b
    [23] PUSH1 0x0c
    [25] PUSH1 0x0d
    [27] PUSH1 0x0e
    [29] PUSH1 0x0f
    [31] PUSH1 0x10
    [32] DUP16
     */

    @Test
    public void resultsForIdAndPcJUMPITest1() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("6001600957", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(4)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(1));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);

        provider.jumpDestsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set1::add);
        assertThat(set1).hasSize(0); // as the jump destination is invalid
        set1.clear();
    }

    /*
    [1] PUSH1 0x01
    [3] PUSH1 0x09
    [4] JUMPI
     */

    @Test
    public void resultsForIdAndPcJUMPITest2() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("6000600957", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(4)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(0));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);

        provider.jumpDestsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set1::add);
        assertThat(set1).hasSize(0); // as the jump destination is invalid
        set1.clear();
    }

    /*
    [1] PUSH1 0x00
    [3] PUSH1 0x09
    [4] JUMPI
     */

    @Test
    public void resultsForIdAndPcJUMPITest3() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("5b6001600057", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Boolean> setb = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(5)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(5)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(1));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(5))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(5))).isInstanceOf(IllegalArgumentException.class);

        provider.jumpDestsForIdAndPc(id, BigInteger.valueOf(5)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(0));
        set1.clear();

        provider.jumpDestUniqueForIdAndPc(id, BigInteger.valueOf(5)).forEach(setb::add);
        assertThat(setb).hasSize(1);
        assertThat(setb).contains(true);
        setb.clear();
    }

    /*
    [0] JUMPDEST
    [2] PUSH1 0x01
    [4] PUSH1 0x00
    [5] JUMPI
     */

    @Test
    public void resultsForIdAndPcJUMPITest4() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("42600957", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(3)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(3)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(-1));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(3))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(3))).isInstanceOf(IllegalArgumentException.class);

        provider.jumpDestsForIdAndPc(id, BigInteger.valueOf(3)).forEach(set1::add);
        assertThat(set1).hasSize(0); // as the jump destination is invalid
        set1.clear();
    }

    /*
    [0] TIMESTAMP
    [2] PUSH1 0x09
    [3] JUMPI
     */

    @Test
    public void resultsForIdAndPcJUMPITest5() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("5b42600057", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Boolean> setb = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.resultsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set2::add);
        assertThat(set2).hasSize(0);
        set2.clear();

        provider.argumentsOneForIdAndPc(id, BigInteger.valueOf(4)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(-1));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(id, BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsThreeForIdAndPc(id, BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);

        provider.jumpDestsForIdAndPc(id, BigInteger.valueOf(4)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(0));
        set1.clear();

        provider.jumpDestUniqueForIdAndPc(id, BigInteger.valueOf(4)).forEach(setb::add);
        assertThat(setb).hasSize(1);
        assertThat(setb).contains(true);
        setb.clear();
    }

    /*
    [0] JUMPDEST
    [1] TIMESTAMP
    [3] PUSH1 0x00
    [4] JUMPI
     */


    @Test
    public void argumentsForIdAndPcTest1() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 583913548;
        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(0)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(0));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(0))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(2)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(10));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(2))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(4))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(5))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(5))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(6)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(-1));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(6))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(7)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(21));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(7))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(9)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(-1));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(9))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(10))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(10))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(11)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(-1), BigInteger.valueOf(-1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(11))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(12)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(-1), BigInteger.valueOf(-1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(12))).isInstanceOf(IllegalArgumentException.class);


        provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(13)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(-1), BigInteger.valueOf(-1)));
        set2.clear();

        assertThatThrownBy(() -> provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(13))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(14)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(1));
        set1.clear();

        provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(16)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(1), BigInteger.valueOf(-1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(16))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(17)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(-1), BigInteger.valueOf(1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(17))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(18)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(4));
        set1.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(20))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(20))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(21))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(21))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(22))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(22))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(23)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(0));
        set1.clear();
        assertThatThrownBy(() ->  provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(23))).isInstanceOf(IllegalArgumentException.class);

        provider.argumentsTwoForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(25)).forEach(set2::add);
        assertThat(set2).hasSize(1);
        assertThat(set2).contains(new Tuple2<BigInteger, BigInteger>(BigInteger.valueOf(0), BigInteger.valueOf(-1)));
        set2.clear();

        assertThatThrownBy(() ->  provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(25))).isInstanceOf(IllegalArgumentException.class);

        //TODO: @Ilya: Exceptions are thrown inconsistently! Either the argument relation for the wrong number of arguments should be always empty or should always throw some informative exception!
    }

    @Test
    public void jumpDestsForIdAndPcTest1() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 583913548;
        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Boolean> setb = new HashSet<>();

        provider.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(9)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(21));
        set1.clear();

        provider.jumpDestUniqueForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(9)).forEach(setb::add);
        assertThat(setb).hasSize(1);
        assertThat(setb).contains(true);
        setb.clear();

        provider.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(20)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(4));
        set1.clear();

        provider.jumpDestUniqueForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(20)).forEach(setb::add);
        assertThat(setb).hasSize(1);
        assertThat(setb).contains(true);
        setb.clear();
    }

    @Test
    public void pcsForIdTest1() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 583913548;
        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();

        provider.pcsForId(BigInteger.valueOf(id)).forEach(set1::add);
        assertThat(set1).hasSize(20);
        assertThat(set1).contains(BigInteger.valueOf(0));
        assertThat(set1).contains(BigInteger.valueOf(2));
        assertThat(set1).contains(BigInteger.valueOf(4));
        assertThat(set1).contains(BigInteger.valueOf(5));
        assertThat(set1).contains(BigInteger.valueOf(6));
        assertThat(set1).contains(BigInteger.valueOf(7));
        assertThat(set1).contains(BigInteger.valueOf(9));
        assertThat(set1).contains(BigInteger.valueOf(10));
        assertThat(set1).contains(BigInteger.valueOf(11));
        assertThat(set1).contains(BigInteger.valueOf(12));
        assertThat(set1).contains(BigInteger.valueOf(13));
        assertThat(set1).contains(BigInteger.valueOf(14));
        assertThat(set1).contains(BigInteger.valueOf(16));
        assertThat(set1).contains(BigInteger.valueOf(17));
        assertThat(set1).contains(BigInteger.valueOf(18));
        assertThat(set1).contains(BigInteger.valueOf(20));
        assertThat(set1).contains(BigInteger.valueOf(21));
        assertThat(set1).contains(BigInteger.valueOf(22));
        assertThat(set1).contains(BigInteger.valueOf(23));
        assertThat(set1).contains(BigInteger.valueOf(25));
    }


    @Test
    public void lastPcsForIdTest1() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/sum_run.txt"), false);
        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 583913548;
        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();

        provider.lastPcsForId(BigInteger.valueOf(id)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(26)); //TODO: @Ilya: we should rename the selector function to outOfBoundsPc, and make sure that Pushes are taken into account
    }

    @Test
    public void lastPcsForIdTest2() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("6000", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.lastPcsForId(id).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(2));
    }

    @Test
    public void lastPcsForIdTest3() throws IOException, ExportException {

        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo("7f0000000000000000000000000000000000000000000000000000000000000000", false);
        ContractInfoReader contractInfoReader = new ContractInfoReader();

        BigInteger id = BigInteger.valueOf(0);
        contractInfoReader.contractInfos.put(id, ci);

        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());

        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.lastPcsForId(id).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(33));
    }


// sum.txt
/*
      0: PUSH1 00 {0}
      2: PUSH1 0a {10:0}
[      4: JUMPDEST] {10::0}
      5: DUP1 {10::10::0}
      6: ISZERO {0::10::0}
      7: PUSH1 15 {21::0::10::0}
      9: JUMPI {}
     10: DUP1
     11: SWAP2
     12: ADD
     13: SWAP1
     14: PUSH1 01
     16: SWAP1
     17: SUB
     18: PUSH1 04
     20: JUMP
[     21: JUMPDEST]
     22: POP
     23: PUSH1 00
     25: SSTORE
 */

/* Tests for Contract 1f */

    @Test
    public void Contract1fTest1() throws IOException, ExportException {
        ContractInfoReader contractInfoReader = new ContractInfoReader(Collections.singletonList("data/bench/1f.txt"), false);
        ConstantAnalysis provider = new ConstantAnalysis(contractInfoReader.getContractInfos());
        int id = 1383431771;
        provider.getBlocksFromBytecode();
        provider.runBlocks();
        provider.printBlocks();
        provider.getCFGs();

        Set<BigInteger> set1 = new HashSet<>();
        Set<Boolean> setb = new HashSet<>();
        Set<Tuple2<BigInteger, BigInteger>> set2 = new HashSet<>();

        provider.jumpDestsForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(62)).forEach(set1::add);
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(68));
        set1.clear();

        provider.jumpDestUniqueForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(62)).forEach(setb::add);
        assertThat(setb).hasSize(1);
        assertThat(setb).contains(true);
        setb.clear();

        provider.argumentsOneForIdAndPc(BigInteger.valueOf(id), BigInteger.valueOf(62)).forEach((set1::add));
        assertThat(set1).hasSize(1);
        assertThat(set1).contains(BigInteger.valueOf(-1));
        set1.clear();
    }


}