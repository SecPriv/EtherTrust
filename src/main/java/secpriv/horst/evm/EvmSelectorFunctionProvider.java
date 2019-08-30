package secpriv.horst.evm;

import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.sun.org.apache.bcel.internal.generic.BIPUSH;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.data.tuples.Tuple4;
import secpriv.horst.data.tuples.Tuple5;
import secpriv.horst.internals.IntervalProvider;
import secpriv.horst.tools.CartesianHelper;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EvmSelectorFunctionProvider implements EvmSelectorFunctionProviderTemplate {
    private static final Logger LOGGER = LogManager.getLogger(EvmSelectorFunctionProvider.class);

    private final Map<BigInteger, ContractLexer.ContractInfo> contractInfos;
    private final IntervalProvider intervalProvider = new IntervalProvider();

    //public Iterable<BigInteger> interval(BigInteger a) {
    //    return intervalProvider.interval(BigInteger.ZERO, a);
    //}

    @Override
    public Iterable<BigInteger> interval(BigInteger a, BigInteger b) {
        return intervalProvider.interval(a, b);
    }

    public Iterable<Tuple2<BigInteger, BigInteger>> sizeAndOffSetForWordsize(BigInteger a) {
        List<Tuple2<BigInteger, BigInteger>> ret = new ArrayList<>();
        Iterable<BigInteger> sizes = intervalProvider.interval(BigInteger.ZERO, a.add(BigInteger.valueOf(1))); // size might range between 0 and a
        for (BigInteger s : sizes) {
            //TODO adjust this:
            Iterable<BigInteger> offsets = intervalProvider.interval(BigInteger.ZERO, a); // offset might range between 0 and a-1
            for (BigInteger o : offsets) {
                if (o.add(s).compareTo(a) <= 0) { //offset + size may at most add up to a
                    ret.add(new Tuple2<>(s, o));
                }
            }
        }
        return ret;
    }

    @Override
    public Iterable<BigInteger> binOps() {
        List<BigInteger> ret = new ArrayList<>();
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.ADD.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.MUL.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SUB.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.DIV.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SDIV.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.MOD.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SMOD.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.LT.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.GT.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SLT.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SGT.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.EQ.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.XOR.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.BYTE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SIGNEXTEND.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SHL.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SHR.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SAR.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.EXP.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.SHA3.opcode));
        return ret;
    }

    @Override
    public Iterable<BigInteger> unOps() {
        List<BigInteger> ret = new ArrayList<>();
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.NOT.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.ISZERO.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.BALANCE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.CALLDATALOAD.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.EXTCODESIZE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.BLOCKHASH.opcode));
        return ret;
    }

    @Override
    public Iterable<BigInteger> terOps() {
        List<BigInteger> ret = new ArrayList<>();
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.ADDMOD.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.MULMOD.opcode));
        return ret;
    }

    @Override
    public Iterable<BigInteger> unitOps() {
        List<BigInteger> ret = new ArrayList<>();
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.ADDRESS.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.ORIGIN.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.CALLDATASIZE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.CALLVALUE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.CALLER.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.CODESIZE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.GASPRICE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.RETURNDATASIZE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.COINBASE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.TIMESTAMP.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.NUMBER.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.DIFFICULTY.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.GASLIMIT.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.MSIZE.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.GAS.opcode));
        return ret;
    }

    public Iterable<BigInteger> copyOps() {
        List<BigInteger> ret = new ArrayList<>();
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.RETURNDATACOPY.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.CALLDATACOPY.opcode));
        ret.add(BigInteger.valueOf(ContractLexer.Opcode.CODECOPY.opcode));
        return ret;
    }



    public EvmSelectorFunctionProvider(Map<BigInteger, ContractLexer.ContractInfo> contractInfos) {
        this.contractInfos = contractInfos;
    }

    /* public Iterable<Tuple2<BigInteger, BigInteger>> idsAndPcsForOpcode(BigInteger opcode) {
        List<Tuple2<BigInteger, BigInteger>> ret = new ArrayList<>();
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
            CartesianHelper.product(Collections.singletonList(entry.getKey()), entry.getValue().getProgramCountersForStandardOpcode(opcode)).forEach(ret::add);
        }
        return ret;
    }

    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndJumpDestsForOpcode(BigInteger opcode) {
        Optional<ContractLexer.Opcode> rez = ContractLexer.Opcode.findByInt(opcode.intValue());
        if (rez.isPresent()) {
            ContractLexer.Opcode opcodeAsOpcode = rez.get();

            if (opcodeAsOpcode != ContractLexer.Opcode.JUMP && opcodeAsOpcode != ContractLexer.Opcode.JUMPI) {
                throw new IllegalArgumentException("Given integer has to correspond to JUMP or JUMPI!");
            }

            List<Tuple3<BigInteger, BigInteger, BigInteger>> ret = new ArrayList<>();
            for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
                CartesianHelper.product(Collections.singletonList(entry.getKey()),
                        entry.getValue().getProgramCountersForStandardOpcode(opcode), entry.getValue().getJumpDestinations()).forEach(ret::add);
            }
            return ret;
        } else {
            return Collections.emptyList();
        }
    } */

    // public Iterable<BigInteger> jumpDestsForID(BigInteger id) {
    //    return contractInfos.get(id).getJumpDestinations();
    //}

    /*public Iterable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> idsAndPcsAndValuesAndOffsetsForPush() {
        List<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> ret = new ArrayList<>();
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
            // TODO: error handling (opcode instance might not have an argument)
            for (ContractLexer.Opcode opcode : Arrays.stream(ContractLexer.Opcode.values()).filter(ContractLexer.Opcode::isPush).collect(Collectors.toList())) {
                for (BigInteger pc : entry.getValue().getProgramCountersForStandardOpcode(BigInteger.valueOf(opcode.opcode))) {
                    ret.add(new Tuple4<>(entry.getKey(), pc, BigInteger.valueOf(ContractLexer.Opcode.getNumberOfPushedBytes(opcode)), entry.getValue().getOpcodeInstance(pc.intValue()).rez.get()));
                }
            }
        }
        return ret;
    }

    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndOffsetsForDup() {
        List<Tuple3<BigInteger, BigInteger, BigInteger>> ret = new ArrayList<>();
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
            for (ContractLexer.Opcode opcode : Arrays.stream(ContractLexer.Opcode.values()).filter(ContractLexer.Opcode::isDup).collect(Collectors.toList())) {
                for (BigInteger pc : entry.getValue().getProgramCountersForStandardOpcode(BigInteger.valueOf(opcode.opcode))) {
                    ret.add(new Tuple3<>(entry.getKey(), pc, BigInteger.valueOf(ContractLexer.Opcode.getDupParameter(opcode))));
                }
            }
        }
        return ret;
    }

    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndOffsetsForSwap() {
        List<Tuple3<BigInteger, BigInteger, BigInteger>> ret = new ArrayList<>();
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
            for (ContractLexer.Opcode opcode : Arrays.stream(ContractLexer.Opcode.values()).filter(ContractLexer.Opcode::isSwap).collect(Collectors.toList())) {
                for (BigInteger pc : entry.getValue().getProgramCountersForStandardOpcode(BigInteger.valueOf(opcode.opcode))) {
                    ret.add(new Tuple3<>(entry.getKey(), pc, BigInteger.valueOf(ContractLexer.Opcode.getSwapParameter(opcode))));
                }
            }
        }
        return ret;
    } */

    /*@Override
    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndValuesForTargetOpcode(BigInteger opcode) {
        return Collections.emptyList();
    } */

    /*@Override
    public Iterable<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> idsAndPcsAndArgumentsForOpcode(BigInteger opcode) {
        List<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> ret = new ArrayList<>();
        BigInteger minusOne = BigInteger.ONE.negate();

        for(Tuple2<BigInteger,BigInteger> t : idsAndPcsForOpcode (opcode)) {
            ret.add(new Tuple5<>(t.v0, t.v1, minusOne, minusOne, minusOne));
        }

        return ret;
    } */

    //@Override
    //public Iterable<BigInteger> idInit() {
    //    return Collections.singletonList(contractInfos.keySet().iterator().next());
    //}

    /*public Iterable<Tuple2<BigInteger, BigInteger>> allIdsAndPcs() {
        List<Tuple2<BigInteger, BigInteger>> ret = new ArrayList<>();
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
            CartesianHelper.product(Collections.singletonList(entry.getKey()), entry.getValue().getAllProgramCounters()).forEach(ret::add);
        }
        return ret;
    }*/

    /*@Override
    public Iterable<Tuple2<BigInteger, BigInteger>> idsAndLastPc() {
        List<Tuple2<BigInteger, BigInteger>> ret = new ArrayList<>();

        for (BigInteger id : contractInfos.keySet()) {
            BigInteger max = contractInfos.get(id).getMaxProgramCounter().add(BigInteger.ONE);
            ret.add(new Tuple2<>(id, max));
        }

        return ret;
    }*/
    private static BigInteger listToSingletonZeroArg(List<Optional<BigInteger>> list, ContractLexer.Opcode opcode) {
        if(list.size() != 0) {
            String message = "Abstract semantics expected 0 arguments for opcode " + opcode + " got " + ContractLexer.argSize(opcode);
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }
        return BigInteger.valueOf(-1);
    }
    private static BigInteger listToSingleton(List<Optional<BigInteger>> list, ContractLexer.Opcode opcode) {
        if(list.size() != 1) {
            String message = "Abstract semantics expected 1 argument for opcode " + opcode + " got " + ContractLexer.argSize(opcode);
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }
        return list.get(0).orElse(BigInteger.valueOf(-1));
    }
    private static Tuple2<BigInteger, BigInteger> listToTuple2(List<Optional<BigInteger>> list, ContractLexer.Opcode opcode) {
        if(list.size() != 2) {
            String message = "Abstract semantics expected 2 arguments for opcode " + opcode + " got " + ContractLexer.argSize(opcode);
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }
        return new Tuple2<>(list.get(0).orElse(BigInteger.valueOf(-1)), list.get(1).orElse(BigInteger.valueOf(-1)));
    }
    private static Tuple3<BigInteger, BigInteger, BigInteger> listToTuple3(List<Optional<BigInteger>> list, ContractLexer.Opcode opcode) {
        String message = "Abstract semantics expected 3 arguments for opcode " + opcode + " got " + ContractLexer.argSize(opcode);
        if(list.size() != 3) {
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }
        return new Tuple3<>(list.get(0).orElse(BigInteger.valueOf(-1)), list.get(1).orElse(BigInteger.valueOf(-1)), list.get(2).orElse(BigInteger.valueOf(-1)));
    }
    private <E> Iterable<E> getArgumentsForIdAndPc(BigInteger id, BigInteger pc, BiFunction<List<Optional<BigInteger>>, ContractLexer.Opcode, E> constructor) {
        ContractLexer.ContractInfo ci = contractInfos.getOrDefault(id, new ContractLexer.ContractInfo());
        ContractLexer.OpcodeInstance inst = ci.getOpcodeInstance(pc.intValue());
        return Collections.singletonList(constructor.apply(inst.args, inst.opcode));
    }
    public Iterable<BigInteger> ids () { return Collections.singletonList(contractInfos.keySet().iterator().next()); }
    public Iterable<BigInteger> pcsForId (BigInteger id) {
        ContractLexer.ContractInfo ci = contractInfos.getOrDefault(id, new ContractLexer.ContractInfo());
        return ci.getAllProgramCounters();
    }
    public Iterable<BigInteger> pcsForIdAndOpcode (BigInteger id, BigInteger opcode) {
        ArrayList<BigInteger> ret = new ArrayList<>();
        Optional<ContractLexer.Opcode> rez = ContractLexer.Opcode.findByInt(opcode.intValue());
        if (rez.isPresent()) {
            ContractLexer.ContractInfo ci = contractInfos.getOrDefault(id, new ContractLexer.ContractInfo());
            ci.getInstancesForOpcode(rez.get()).forEach(i -> ret.add(BigInteger.valueOf(i.position)));
            return ret;
        }
        return ret;
    }
    private boolean resultsExist(BigInteger id, BigInteger pc){
        List<Tuple2<BigInteger, BigInteger>> results = new ArrayList<>();
        resultsForIdAndPc(id, pc).forEach(results::add);
        return(!results.isEmpty());
    }
    public Iterable<BigInteger> argumentsZeroForIdAndPc (BigInteger id, BigInteger pc) {
        if (resultsExist(id, pc)){
            return Collections.emptyList();
        }
        return getArgumentsForIdAndPc(id, pc, EvmSelectorFunctionProvider::listToSingletonZeroArg);
    }
    public Iterable<BigInteger> argumentsOneForIdAndPc (BigInteger id, BigInteger pc) {
        if (resultsExist(id, pc)){
            return Collections.emptyList();
        }
        return getArgumentsForIdAndPc(id, pc, EvmSelectorFunctionProvider::listToSingleton);
    }
    public Iterable<Tuple2<BigInteger, BigInteger>> argumentsTwoForIdAndPc (BigInteger id, BigInteger pc) {
        if (resultsExist(id, pc)){
            return Collections.emptyList();
        }
        return getArgumentsForIdAndPc(id, pc, EvmSelectorFunctionProvider::listToTuple2);
    }
    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> argumentsThreeForIdAndPc (BigInteger id, BigInteger pc) {
        if (resultsExist(id, pc)){
            return Collections.emptyList();
        }
        return getArgumentsForIdAndPc(id, pc, EvmSelectorFunctionProvider::listToTuple3);
    }
    public Iterable<Tuple2<BigInteger, BigInteger>> resultsForIdAndPc (BigInteger id, BigInteger pc) {
        ContractLexer.ContractInfo ci = contractInfos.getOrDefault(id, new ContractLexer.ContractInfo());
        ContractLexer.OpcodeInstance inst = ci.getOpcodeInstance(pc.intValue());
        if (inst.rez.isPresent()){
            BigInteger result = inst.rez.get();
            BigInteger pops = BigInteger.valueOf(ContractLexer.popSize(inst.opcode));
            return Collections.singletonList(new Tuple2<>(result, pops));
        }
        else{
            return Collections.emptyList();
        }
    }
    public Iterable<BigInteger> jumpDestsForIdAndPc (BigInteger id, BigInteger pc) {
        ContractLexer.ContractInfo ci = contractInfos.getOrDefault(id, new ContractLexer.ContractInfo());
        return ci.jumps.getOrDefault(pc, new ArrayList<>());
    }
    public Iterable<Boolean> jumpDestUniqueForIdAndPc (BigInteger id, BigInteger pc) {
        List<BigInteger> targets = new ArrayList<>();
        jumpDestsForIdAndPc(id, pc).forEach(targets::add);
        if (targets.size() == 1){
            return Collections.singletonList(true);
        }
        else{
            return Collections.singletonList(false);
        }
    }
    public Iterable<BigInteger> lastPcsForId (BigInteger id) {
        List<Tuple2<BigInteger, BigInteger>> ret = new ArrayList<>();
        ContractLexer.ContractInfo ci = contractInfos.getOrDefault(id, new ContractLexer.ContractInfo());
        BigInteger max = ci.getMaxProgramCounter();
        ContractLexer.OpcodeInstance lastOpcode = ci.getOpcodeInstance(max.intValue());
        if (ContractLexer.Opcode.isPush(lastOpcode.opcode)){
            return Collections.singletonList(max.add(BigInteger.valueOf(ContractLexer.Opcode.getNumberOfPushedBytes(lastOpcode.opcode))).add(BigInteger.ONE));
        }
        else{
            return Collections.singletonList(max.add(BigInteger.ONE));
        }
    }
}
