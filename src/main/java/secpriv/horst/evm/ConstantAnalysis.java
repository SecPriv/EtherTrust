package secpriv.horst.evm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.data.tuples.Tuple4;
import secpriv.horst.data.tuples.Tuple5;
import secpriv.horst.tools.CartesianHelper;
import secpriv.horst.tools.OptionalHelper;
import secpriv.horst.translation.BigStepClauseWalker;

import java.math.BigInteger;
import java.rmi.server.ExportException;
import java.util.*;

public class ConstantAnalysis implements EvmSelectorFunctionProviderTemplate {
    private static final Logger LOGGER = LogManager.getLogger(ConstantAnalysis.class);

    private HashMap<BigInteger, LinkedHashMap<Integer,LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>> blocksOfContracts;
    private HashMap<BigInteger, ControlFlowGraph> cfgsOfContracts;
    private final Map<BigInteger, ContractLexer.ContractInfo> contractInfos;
    private final EvmSelectorFunctionProvider evmSelectorFunctionProvider;

    public ConstantAnalysis(Map<BigInteger, ContractLexer.ContractInfo> contractInfos) {
        this.contractInfos = contractInfos;
        this.evmSelectorFunctionProvider = new EvmSelectorFunctionProvider(contractInfos);
        this.blocksOfContracts = new HashMap<BigInteger, LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>>();
        this.cfgsOfContracts = new HashMap<BigInteger, ControlFlowGraph>();
    }

    public void getBlocksFromBytecode() {
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : this.contractInfos.entrySet()) {
            Integer blockNum = 0;
            LinkedHashMap<Integer, ContractLexer.OpcodeInstance> currentBlock = new LinkedHashMap<>();
            LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blocks = new LinkedHashMap<>();

            for (Map.Entry<Integer, ContractLexer.OpcodeInstance> pcOpcode : entry.getValue().getPositionToOpcode().entrySet()) {
                Integer pc = pcOpcode.getKey();
                ContractLexer.OpcodeInstance opcodeInstance = pcOpcode.getValue();
                switch (opcodeInstance.opcode) {
                    case STOP: case JUMP: case JUMPI: case RETURN: case REVERT: case INVALID: case SUICIDE:
                        currentBlock.put(pc, opcodeInstance);
                        if (!currentBlock.isEmpty()){ blocks.put(blockNum, currentBlock); blockNum++; }
                        currentBlock = new LinkedHashMap<>();
                        break;
                    case JUMPDEST:
                        if (!currentBlock.isEmpty()){ blocks.put(blockNum,currentBlock); blockNum++; }
                        currentBlock = new LinkedHashMap<>();
                        currentBlock.put(pc, opcodeInstance);
                        break;
                    default:
                        currentBlock.put(pc, opcodeInstance);
                        break;
                }
            }
            blocks.put(blockNum, currentBlock);
            blocksOfContracts.put(entry.getKey(), blocks);
        }
    }
    public HashMap<BigInteger, LinkedHashMap<Integer,LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>> getBlocksOfContracts(){
        return blocksOfContracts;
    }

    public void printBlocks(){
        for (Map.Entry<BigInteger, LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>> entry : this.blocksOfContracts.entrySet()) {
            LOGGER.debug("Blocks of contract..." + entry.getKey().toString());
            for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : entry.getValue().entrySet()) {
                LOGGER.debug("-----Block " + blockInfo.getKey() + "-----");
                LinkedHashMap<Integer, ContractLexer.OpcodeInstance> blockInstructions = (LinkedHashMap<Integer, ContractLexer.OpcodeInstance>) blockInfo.getValue();
                for (Map.Entry<Integer, ContractLexer.OpcodeInstance> block: blockInstructions.entrySet()){
                    String args = "";
                    for (Optional<BigInteger> arg: block.getValue().args){
                        if (arg.isPresent()){
                            args += " " + arg.get();
                        }
                        else{
                            args += " _";
                        }
                    }
                    // _ means TOP or Not Present
                    String rez = "_";
                    Optional rezI = block.getValue().rez;
                    if (rezI.isPresent()){
                        rez = rezI.get().toString();
                    }
                    StringBuilder sb = new StringBuilder();
                    ContractLexer.ContractInfo ci = contractInfos.get(entry.getKey());
                    ci.jumps.getOrDefault(BigInteger.valueOf(block.getValue().position), Collections.emptyList()).forEach(t -> sb.append(t.toString() + " "));
                    String targets = sb.toString();
                    if (!targets.isEmpty()){
                        targets = " Target:" + targets;
                    }
                    LOGGER.debug("At pc=" + Integer.toHexString(block.getKey()) + "(" + block.getKey() + ")"  + " "
                            + block.getValue().opcode.toString() + args + " -> " + rez + targets);
                }
            }
        }
    }

    public void getCFGs() throws ExportException, org.jgrapht.io.ExportException {
        for (Map.Entry<BigInteger, LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>> entry : this.blocksOfContracts.entrySet()) {
                ControlFlowGraph cfg = new ControlFlowGraph(entry.getKey(), entry.getValue(), contractInfos);
                cfgsOfContracts.put(entry.getKey(), cfg);
        }
    }

    /*public void plugJumpTargets(){
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> ciEntry: contractInfos.entrySet()) {
            ContractLexer.ContractInfo ci = ciEntry.getValue();
            Map<Integer, ContractLexer.OpcodeInstance> positionToOpcode = ci.getPositionToOpcode();
            for (BlockData bd: ci.bdList){
                if (bd.jPc == null){
                    continue;
                }
                int pcJump = bd.jPc.intValue();
                ArrayList<BigInteger> invalidTargets = new ArrayList<>();
                for (BigInteger succ: bd.successors){
                    if (succ.equals(bd.jPc.subtract(BigInteger.ONE))){
                        continue;
                    }
                    else{
                        if (!bd.targets.contains(succ)){
                            invalidTargets.add(succ);
                        }
                    }
                }
                // TODO: we need to plug in jump targets into instructions and into special Jump datastructure, first is used for pretty printing, second is used in selector functions
                ci.jumps.put(pcJump, new Jump(bd.jPc, bd.hasUnresolvedJumps, bd.targets, invalidTargets));
                ContractLexer.OpcodeInstance instance = positionToOpcode.get(pcJump);
                ArrayList<Optional<BigInteger>> targets = new ArrayList<>();
                bd.targets.forEach(t -> targets.add(Optional.of(t)));
                instance.args.addAll(targets);
                if (bd.hasUnresolvedJumps){
                    instance.args.add(Optional.of(BigInteger.ONE.negate()));
                }
            }
        }
    }*/
    /*public Iterable<Tuple2<BigInteger, BigInteger>> getProgramCountersTargetsForJumps(BigInteger opcode) {
        Optional<ContractLexer.Opcode> rez = ContractLexer.Opcode.findByInt(opcode.intValue());
        if (rez.isPresent()) {
            ContractLexer.Opcode opcodeAsOpcode = rez.get();
            for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
                List<Tuple2<BigInteger, BigInteger>> ret = new ArrayList<>();
                for (ContractLexer.OpcodeInstance instance : entry.getValue().getInstancesForOpcode(opcodeAsOpcode)) {
                    Jump jump = entry.getValue().jumps.get(instance.position);
                    if (jump != null) {
                        CartesianHelper.product(Collections.singletonList(BigInteger.valueOf(instance.position)), jump.targets).forEach(ret::add);
                    } else {
                        LOGGER.error("Jump is not found where expected!");
                        throw new UnsupportedOperationException();
                    }
                }
                return ret;
            }
        }
        return Collections.emptyList();
    }*/

    /*public Iterable<Tuple2<BigInteger, BigInteger>> getProgramCounters1ArgumentForOpcode(BigInteger opcode) {
        return evmSelectorFunctionProvider.getProgramCounters1ArgumentForOpcode(opcode);
    }
    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> getProgramCounters2ArgumentsForOpcode(BigInteger opcode) {
        return evmSelectorFunctionProvider.getProgramCounters2ArgumentsForOpcode(opcode);
    }
    public Iterable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> getProgramCounters3ArgumentsForOpcode(BigInteger opcode) {
        return  evmSelectorFunctionProvider.getProgramCounters3ArgumentsForOpcode(opcode);
    }*/
    /*
    public void plugJumpTargets(){
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> ci: contractInfos.entrySet()) {
                LinkedHashMap<Integer,LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blocksOfContract = blocksOfContracts.get(ci.getKey());
                for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : blocksOfContract.entrySet()) {
                    for (Map.Entry<Integer, ContractLexer.OpcodeInstance> instruction: blockInfo.getValue().entrySet()) {
                        ContractLexer.OpcodeInstance inst = instruction.getValue();
                        if (inst.opcode == ContractLexer.Opcode.JUMP || inst.opcode == ContractLexer.Opcode.JUMPI){
                            for (BlockData bd: ci.getValue().bdList) {
                                if (bd.jPc != null){
                                if (bd.jPc.equals(BigInteger.valueOf(inst.position))) {
                                    BigInteger target;
                                    Optional<BigInteger> tOp;
                                    if (bd.successors.size() > 1) {
                                        BigInteger t1 = bd.successors.get(0);
                                        BigInteger t2 = bd.successors.get(1);
                                        if (t1.subtract(BigInteger.ONE).equals(BigInteger.valueOf(inst.position))){
                                            tOp = Optional.of(t2);
                                        }
                                        else{
                                            tOp = Optional.of(t1);
                                        }
                                    } else {
                                        if (bd.successors.size() > 0) {
                                            target = bd.successors.get(0);
                                            tOp = Optional.of(target);
                                        }
                                        else{
                                            tOp =Optional.empty();
                                        }
                                    }
                                    inst.rez = tOp;
                                }
                                }
                            }
                        }
                    }
                }
            }
        }
    */
    /*public void runExternalBlocks(){
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> ciEntry: contractInfos.entrySet()){
            ContractLexer.ContractInfo ci = ciEntry.getValue();
            BigInteger id = ciEntry.getKey();
            Map<Integer, ContractLexer.OpcodeInstance> positionToOpcode = ci.getPositionToOpcode();
            for (BlockData bd: ci.bdList){
                ConstantAnalysisState state = new ConstantAnalysisState();
                state.jumpDestsForID = jumpDestsForID(id);
                for (BigInteger pc: bd.opcodes){
                    ContractLexer.OpcodeInstance instance = positionToOpcode.get(pc.intValue());
                    state.modify(pc.intValue(), instance);
                }
            }
        }
    }
    public void runExternalBlocksTopologically(){
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> ciEntry: contractInfos.entrySet()){
            ContractLexer.ContractInfo ci = ciEntry.getValue();
            BigInteger id = ciEntry.getKey();
            ConstantAnalysisState state = new ConstantAnalysisState();
            state.jumpDestsForID = jumpDestsForID(id);
            Map<Integer, ContractLexer.OpcodeInstance> positionToOpcode = ci.getPositionToOpcode();
            BigInteger v;
            TopologicalOrderIterator<BigInteger, DefaultEdge> orderIterator;

            orderIterator =
                    new TopologicalOrderIterator<BigInteger, DefaultEdge>(ci.g);
            LOGGER.debug("\nTopological Ordering:");
            while (orderIterator.hasNext()) {
                v = orderIterator.next();
                LOGGER.debug("Exploring block" + v);
                //TODO: we can use a map here to avoid huge cycles
                for (BlockData bd: ci.bdList){
                    if (bd.id.equals(v)){
                        for (BigInteger pc: bd.opcodes){
                            ContractLexer.OpcodeInstance instance = positionToOpcode.get(pc.intValue());
                            if (instance == null){
                                //TODO: we do not delete suffix while generating the CFG, hence we might have pruned bytecodes, hence, we just skip them
                                continue;
                            }
                            state.modify(pc.intValue(), instance);
                        }
                        break;
                    }
                }
            }
        }
    }*/
    public void runBlocks() {
        for (Map.Entry<BigInteger, LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>> entry : this.blocksOfContracts.entrySet()) {
            for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : entry.getValue().entrySet()) {
                ConstantAnalysisState state = new ConstantAnalysisState(contractInfos.getOrDefault(entry.getKey(), new ContractLexer.ContractInfo()));
                for (Map.Entry<Integer, ContractLexer.OpcodeInstance> instruction: blockInfo.getValue().entrySet()) {
                    state.modify(instruction.getKey(), instruction.getValue());
                }
            }
        }
    }
    public void runBlocksTopologically(){
        for (Map.Entry<BigInteger, ControlFlowGraph> cfg: cfgsOfContracts.entrySet()) {
            ConstantAnalysisState state = new ConstantAnalysisState(contractInfos.getOrDefault(cfg.getKey(), new ContractLexer.ContractInfo()));
            LinkedHashMap<Integer, ContractLexer.OpcodeInstance> v;
            TopologicalOrderIterator<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge> orderIterator;
            orderIterator =
                    new TopologicalOrderIterator<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge>(cfg.getValue().getCfg());
            LOGGER.debug("\nTopological Ordering:");
            while (orderIterator.hasNext()) {
                v = orderIterator.next();
                for (Map.Entry<Integer, ContractLexer.OpcodeInstance> instruction: v.entrySet()) {
                    state.modify(instruction.getKey(), instruction.getValue());
                }
                //LOGGER.debug(v);
            }

        }
    }
    public boolean checkCycles(){
        boolean cycles = false;
        for (Map.Entry<BigInteger, ControlFlowGraph> cfgEntry: cfgsOfContracts.entrySet()){
            ControlFlowGraph cfg = cfgEntry.getValue();
            if (cfg.hasCycle()){
                return true;
            }
        }
        return cycles;
    }
    /*public void splitToStandardAndRichOpcodeToPosition(){
        for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : this.contractInfos.entrySet()) {
            entry.getValue().splitToStandardAndRichOpcodeToPosition();
        }
    }*/

    //@Override
    //public Iterable<BigInteger> interval(BigInteger a) {
    //    return evmSelectorFunctionProvider.interval(a);
    //}

    @Override
    public Iterable<BigInteger> interval(BigInteger a, BigInteger b) {
        return evmSelectorFunctionProvider.interval(a, b);
    }

    @Override
    public Iterable<Tuple2<BigInteger, BigInteger>> sizeAndOffSetForWordsize(BigInteger wordSize) {
        return evmSelectorFunctionProvider.sizeAndOffSetForWordsize(wordSize);
    }

    /*@Override
    public Iterable<Tuple2<BigInteger, BigInteger>> idsAndPcsForOpcode(BigInteger opcode) {
        List<Tuple2<BigInteger, BigInteger>> ret = new ArrayList<>();
        for(Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
            CartesianHelper.product(Collections.singletonList(entry.getKey()), entry.getValue().getProgramCountersForStandardOpcode(opcode)).forEach(ret::add);
        }
        return ret;
    }*/

    /*@Override
    public Iterable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> idsAndPcsAndValuesAndOffsetsForPush() {
        return evmSelectorFunctionProvider.idsAndPcsAndValuesAndOffsetsForPush();
    }

    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndValuesForTargetOpcode(BigInteger opcode) {
        List<Tuple3<BigInteger, BigInteger, BigInteger>> ret = new ArrayList<>();
        for(Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
            ArrayList<Tuple2<BigInteger,BigInteger>> pcsAndTargets = (ArrayList<Tuple2<BigInteger, BigInteger>>)
                    entry.getValue().getProgramCountersAndValuesForRichOpcode(opcode);
            for (Tuple2<BigInteger,BigInteger> pcAndTarget: pcsAndTargets){
                ret.add(new Tuple3<>(entry.getKey(), pcAndTarget.v0, pcAndTarget.v1));
            }
        }
        return ret;
    }

    @Override
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
        }
        else{
            return Collections.emptyList();
        }
    }*/

    //@Override
    //public Iterable<BigInteger> jumpDestsForID(BigInteger id) {
    //    return evmSelectorFunctionProvider.jumpDestsForID(id);
    //}

    /*@Override
    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndOffsetsForDup() {
        return evmSelectorFunctionProvider.idsAndPcsAndOffsetsForDup();
    }

    @Override
    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndOffsetsForSwap() {
        return evmSelectorFunctionProvider.idsAndPcsAndOffsetsForSwap();
    }

    public Iterable<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> idsAndPcsAndArgumentsForOpcode(BigInteger opcode) {
        Optional<ContractLexer.Opcode> rez = ContractLexer.Opcode.findByInt(opcode.intValue());
        if (rez.isPresent()) {
            ContractLexer.Opcode opcodeAsOpcode = rez.get();
            List<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> ret = new ArrayList<>();
            for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
                ArrayList<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> pcsAndArguments =
                        (ArrayList<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>)
                        entry.getValue().getProgramCountersArgumentsForOpcode(opcode);
                for (Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> pcAndArguments : pcsAndArguments) {
                    ret.add(new Tuple5<>(entry.getKey(), pcAndArguments.v0, pcAndArguments.v1, pcAndArguments.v2, pcAndArguments.v3));
                }
            }
            return ret;
        }
        else{
            return Collections.emptyList();
        }
    } */

    //@Override
    //public Iterable<BigInteger> idInit() {
    //    return evmSelectorFunctionProvider.idInit();
    //}

    //@Override
    //public Iterable<Tuple2<BigInteger, BigInteger>> idsAndLastPc() {
    //    return evmSelectorFunctionProvider.idsAndLastPc();
    //}

    //@Override
    //public Iterable<Tuple2<BigInteger, BigInteger>> allIdsAndPcs() {
    //    return evmSelectorFunctionProvider.allIdsAndPcs();
    //}

    @Override
    public Iterable<BigInteger> binOps() {
        return evmSelectorFunctionProvider.binOps();
    }

    @Override
    public Iterable<BigInteger> unOps() {
        return evmSelectorFunctionProvider.unOps();
    }

    @Override
    public Iterable<BigInteger> terOps() {
        return evmSelectorFunctionProvider.terOps();
    }

    @Override
    public Iterable<BigInteger> unitOps() {
        return evmSelectorFunctionProvider.unitOps();
    }

    @Override
    public Iterable<BigInteger> copyOps() {
        return evmSelectorFunctionProvider.copyOps();
    }

    /*public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndJumpTargetsForOpcode(BigInteger opcode) {
        Optional<ContractLexer.Opcode> rez = ContractLexer.Opcode.findByInt(opcode.intValue());
        if (rez.isPresent()) {
            ContractLexer.Opcode opcodeAsOpcode = rez.get();

            if (opcodeAsOpcode != ContractLexer.Opcode.JUMP && opcodeAsOpcode != ContractLexer.Opcode.JUMPI) {
                throw new IllegalArgumentException("Given integer has to correspond to JUMP or JUMPI!");
            }

            List<Tuple3<BigInteger, BigInteger, BigInteger>> ret = new ArrayList<>();
            for (Map.Entry<BigInteger, ContractLexer.ContractInfo> entry : contractInfos.entrySet()) {
                ArrayList<Tuple2<BigInteger, BigInteger>> pcsAndTargets = (ArrayList<Tuple2<BigInteger, BigInteger>>)
                        entry.getValue().getProgramCountersAndValuesForRichOpcode(opcode);
                for (Tuple2<BigInteger, BigInteger> pcAndTarget : pcsAndTargets) {
                    ret.add(new Tuple3<>(entry.getKey(), pcAndTarget.v0, pcAndTarget.v1));
                }
            }
            return ret;
        }
        else{
            return Collections.emptyList();
        }
    }*/
    public Iterable<BigInteger> ids () { return evmSelectorFunctionProvider.ids(); }
    public Iterable<BigInteger> pcsForIdAndOpcode (BigInteger id, BigInteger opcode) { return evmSelectorFunctionProvider.pcsForIdAndOpcode(id, opcode); }
    public Iterable<BigInteger> pcsForId (BigInteger id) { return evmSelectorFunctionProvider.pcsForId(id); }
    public Iterable<BigInteger> lastPcsForId (BigInteger id) { return evmSelectorFunctionProvider.lastPcsForId(id); }
    public Iterable<BigInteger> jumpDestsForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.jumpDestsForIdAndPc(id, pc); }
    public Iterable<BigInteger> argumentsZeroForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.argumentsZeroForIdAndPc(id, pc); }
    public Iterable<BigInteger> argumentsOneForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.argumentsOneForIdAndPc(id, pc); }
    public Iterable<Tuple2<BigInteger, BigInteger>> argumentsTwoForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.argumentsTwoForIdAndPc(id, pc);}
    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> argumentsThreeForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.argumentsThreeForIdAndPc(id, pc); }
    public Iterable<Tuple2<BigInteger, BigInteger>> resultsForIdAndPc (BigInteger id, BigInteger pc) {
        return evmSelectorFunctionProvider.resultsForIdAndPc(id, pc); }
    public Iterable<Boolean> jumpDestUniqueForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.jumpDestUniqueForIdAndPc(id, pc); }
}
