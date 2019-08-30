package secpriv.horst.evm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.web3j.abi.datatypes.Int;
import secpriv.horst.data.tuples.Tuple;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.data.tuples.Tuple4;
import secpriv.horst.tools.CartesianHelper;


import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContractLexer {
    private static final Logger LOGGER = LogManager.getLogger(ContractLexer.class);

    private static class TokenIterator implements Iterator<String> {
        private String remainingString;

        public TokenIterator(String s) {
            remainingString = s.trim();
            if (remainingString.length() % 2 == 1) {
                throw new IllegalArgumentException("Input string has to be of even length!");
            }
        }

        public boolean hasNext() {
            return !remainingString.isEmpty();
        }

        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String ret = remainingString.substring(0, 2);
            remainingString = remainingString.substring(2);
            return ret;
        }
    }

    public static class OpcodeInstance {
        public final int position;
        public final Opcode opcode;
        // size of the arguments is equal to the arguments taken by the opcode
        // in case of JUMP/JUMPI can contain an unbounded number of jump targets
        public ArrayList<Optional<BigInteger>> args = new ArrayList<>();
        public Optional<BigInteger> rez;

        public OpcodeInstance(int position, Opcode opcode, int argSize) {
            this.position = position;
            this.opcode = Objects.requireNonNull(opcode, "Opcode may not be null!");
            for (int i = 0; i < argSize; i++) {
                this.args.add(Optional.empty());
            }
            this.rez = Optional.empty();
        }
    }

    public static class ContractInfo {
        // TODO: this value is changed only if we know that there no cycles in the contract
        public boolean hasCycles = true;
        public Graph<BigInteger, DefaultEdge> g;
        public Map<BigInteger, List<BigInteger>> jumps = new HashMap<>();
        private Map<Integer, OpcodeInstance> positionToOpcode = new LinkedHashMap<>();
        private Map<Opcode, List<OpcodeInstance>> opcodeToPosition = new LinkedHashMap<>();

        private List<Integer> jumpDestinations = new ArrayList<>();

        public Integer getSize() {
            return positionToOpcode.size();
        }

        public Map<Integer, OpcodeInstance> getPositionToOpcode() {
            return this.positionToOpcode;
        }

        public List<OpcodeInstance> getInstancesForOpcode(Opcode opcode){
            return opcodeToPosition.getOrDefault(opcode, new ArrayList<>());
        }

        public void addJumpDestination(int jumpDestination) {
            jumpDestinations.add(jumpDestination);
        }

        public void addOpcodeInstance(OpcodeInstance opcodeInstance) {
            positionToOpcode.put(opcodeInstance.position, opcodeInstance);
            opcodeToPosition.computeIfAbsent(opcodeInstance.opcode, s -> new ArrayList<>()).add(opcodeInstance);
        }

        public Iterable<BigInteger> getJumpDestinations() {
            return jumpDestinations.stream().map(BigInteger::valueOf).collect(Collectors.toList());
        }

        public Iterable<BigInteger> getAllProgramCounters() {
            return positionToOpcode.keySet().stream().map(BigInteger::valueOf).collect(Collectors.toList());
        }
        public void print(){
            for (Map.Entry<Integer, OpcodeInstance> instance: positionToOpcode.entrySet()){
                String opcodeAndArgs = instance.getValue().opcode.toString();
                if (instance.getValue().opcode == Opcode.JUMP || instance.getValue().opcode == Opcode.JUMPI){
                    LOGGER.debug("-------");
                }
                for (Optional<BigInteger> arg: instance.getValue().args){
                    if (arg.isPresent()){
                        opcodeAndArgs += " " + arg.get();
                    }
                    else{
                        opcodeAndArgs += " _";
                    }
                }
                LOGGER.debug("At pc=" + instance.getKey() + " " + opcodeAndArgs + " result:" + instance.getValue().rez);
            }
        }


        public OpcodeInstance getOpcodeInstance(Integer pc) {
            return positionToOpcode.get(pc);
        }

        /*public String toCodeRepresentation() {
            TreeSet<Integer> programCounters = new TreeSet<>();

            programCounters.addAll(jumpDestinations);
            programCounters.addAll(positionToOpcode.keySet());

            StringBuilder sb = new StringBuilder();

            for (Integer pc : programCounters) {
                if (jumpDestinations.contains(pc)) {
                    sb.append(String.format("[%1$7s: JUMPDEST]", pc));
                } else {
                    sb.append(String.format("%1$8s: ", pc));
                    sb.append(positionToOpcode.get(pc).opcode);
                    positionToOpcode.get(pc).rez.ifPresent(s -> sb.append(" ").append(s));
                }
                sb.append("\n");
            }

            return sb.toString();
        }*/

        public BigInteger getMaxProgramCounter() {
            return BigInteger.valueOf(Collections.max(positionToOpcode.keySet()));
        }
    }

    private TokenIterator opIterator;
    private int byteCounter;
    private final boolean stat;

    private ContractLexer(String source, boolean stat) {
        opIterator = new TokenIterator(source);
        this.stat = stat;
        byteCounter = 0;
    }

    public static ContractInfo generateContractInfo(String source, boolean stat) {
        source = removeMetaData(source);
        return (new ContractLexer(source, stat)).parseContract();
    }

    private static String removeMetaData(String source) {
        // This only works for "swarm version 0" as described here:
        // https://solidity.readthedocs.io/en/develop/metadata.html
        final String META_DATA_PREFIX = "a165627a7a72305820";

        int startOfMetaData = source.lastIndexOf(META_DATA_PREFIX);

        if (startOfMetaData < 0) {
            //TODO handle warning properly
            LOGGER.warn("No meta data found! Maybe contract has a malformed suffix!");
            return source;
        }

        return source.substring(0, startOfMetaData);
    }

    private ContractInfo parseContract() {
        ContractInfo contractInfo = new ContractInfo();

        while (opIterator.hasNext()) {
            Optional<Opcode> rez = null;
            rez = Opcode.findByInt(Integer.parseInt(opIterator.next(), 16));

            if (rez.isPresent()) {
                Opcode opcode = rez.get();

                if (opcode == Opcode.JUMPDEST) {
                    contractInfo.addJumpDestination(byteCounter);
                }

                if (Opcode.isDelegateCallOrCallCode(opcode)) {
                    if (stat){
                        LOGGER.error("DELEGATECALL and CALLCODE operations are not allowed (soundness)");
                    }
                    else{
                        throw new UnsupportedOperationException("DELEGATECALL");
                    }
                }

                contractInfo.addOpcodeInstance(parseOpcodeInstance(opcode));
                byteCounter++;
            }
            else{
                LOGGER.warn("Encountered an unknown bytecode, skipping the subsequent after the byte..." + byteCounter);
                // if we do know the bytecode the layout is broken, hence, we stop lexing from that point on
                break;
            }
        }

        // We populate the jumps with all possible jump targets in the beginning
        ArrayList<BigInteger> jumpDest = new ArrayList<>();
        contractInfo.getJumpDestinations().forEach(jumpDest::add);
        contractInfo.opcodeToPosition.getOrDefault(Opcode.JUMP, Collections.emptyList()).forEach(inst -> contractInfo.jumps.put(BigInteger.valueOf(inst.position), jumpDest));
        contractInfo.opcodeToPosition.getOrDefault(Opcode.JUMPI, Collections.emptyList()).forEach(inst -> contractInfo.jumps.put(BigInteger.valueOf(inst.position), jumpDest));
        return contractInfo;
    }

    private OpcodeInstance parseOpcodeInstance(Opcode opcode) {
        if (Opcode.isPush(opcode)) {
            return parsePushWithArguments(opcode);
        } else {
            return new OpcodeInstance(byteCounter, opcode, argSize(opcode));
        }
    }

    private OpcodeInstance parsePushWithArguments(Opcode opcode) {
        int numberOfBytes = Opcode.getNumberOfPushedBytes(opcode);
        String argument = parsePushArgument(numberOfBytes);
        OpcodeInstance ret = new OpcodeInstance(byteCounter, opcode, argSize(opcode));
        ret.args.set(0, Optional.of(new BigInteger(argument, 16)));
        byteCounter += numberOfBytes;
        return ret;
    }

    private String parsePushArgument(int numberOfBytes) {
        StringBuilder argument = new StringBuilder();

        for (int j = 0; j < numberOfBytes; ++j) {
            //if there is an argument underflow, we pad the remaining bytes with zeroes
            argument.append(opIterator.hasNext() ? opIterator.next() : "00");
        }

        return argument.toString();
    }

    public static int argSize(Opcode opcode){
        switch (opcode){
            case INVALID:
            case JUMP:
            case STOP:
            case ADDRESS:
            case ORIGIN:
            case CALLER:
            case CALLVALUE:
            case CALLDATASIZE:
            case CODESIZE:
            case GASPRICE:
            case RETURNDATASIZE:
            case COINBASE:
            case TIMESTAMP:
            case NUMBER:
            case DIFFICULTY:
            case GASLIMIT:
            case POP:
            case PC:
            case MSIZE:
            case GAS:
            case JUMPDEST:
            case DUP1:
            case DUP2:
            case DUP3:
            case DUP4:
            case DUP5:
            case DUP6:
            case DUP7:
            case DUP8:
            case DUP9:
            case DUP10:
            case DUP11:
            case DUP12:
            case DUP13:
            case DUP14:
            case DUP15:
            case DUP16:
            case LOG0:
            case LOG1:
            case LOG2:
            case LOG3:
            case LOG4:
            case CREATE:
            case CREATE2:
                return 0;

            case PUSH1:
            case PUSH2:
            case PUSH3:
            case PUSH4:
            case PUSH5:
            case PUSH6:
            case PUSH7:
            case PUSH8:
            case PUSH9:
            case PUSH10:
            case PUSH11:
            case PUSH12:
            case PUSH13:
            case PUSH14:
            case PUSH15:
            case PUSH16:
            case PUSH17:
            case PUSH18:
            case PUSH19:
            case PUSH20:
            case PUSH21:
            case PUSH22:
            case PUSH23:
            case PUSH24:
            case PUSH25:
            case PUSH26:
            case PUSH27:
            case PUSH28:
            case PUSH29:
            case PUSH30:
            case PUSH31:
            case PUSH32:
            case ISZERO:
            case NOT:
            case BALANCE:
            case CALLDATALOAD:
            case EXTCODESIZE:
            case BLOCKHASH:
            case MLOAD:
            case SLOAD:
            case SUICIDE:
            case JUMPI:
                return 1;

            case ADD:
            case MUL:
            case SUB:
            case DIV:
            case SDIV:
            case MOD:
            case SMOD:
            case EXP:
            case SIGNEXTEND:
            case LT:
            case GT:
            case SLT:
            case SGT:
            case EQ:
            case AND:
            case OR:
            case XOR:
            case BYTE:
            case SHL:
            case SHR:
            case SAR:
            case SHA3:
            case MSTORE:
            case MSTORE8:
            case SSTORE:
            case RETURN:
            case REVERT:
            case SWAP1:
            case SWAP2:
            case SWAP3:
            case SWAP4:
            case SWAP5:
            case SWAP6:
            case SWAP7:
            case SWAP8:
            case SWAP9:
            case SWAP10:
            case SWAP11:
            case SWAP12:
            case SWAP13:
            case SWAP14:
            case SWAP15:
            case SWAP16:
            case CALL:
            case CALLCODE:
            case DELEGATECALL:
            case STATICCALL:
            case EXTCODECOPY:
            case CALLDATACOPY:
            case CODECOPY:
            case RETURNDATACOPY:
                return 2;

            case ADDMOD:
            case MULMOD:
                return 3;
            default:
                return 0;
        }
    }

    public static int popSize(Opcode opcode){
        switch (opcode){
            case INVALID:
            case STOP:
                case ADDRESS:
            case ORIGIN:
            case CALLER:
            case CALLVALUE:
            case CALLDATASIZE:
            case CODESIZE:
            case GASPRICE:
            case RETURNDATASIZE:
            case COINBASE:
            case TIMESTAMP:
            case NUMBER:
            case DIFFICULTY:
            case GASLIMIT:
            case POP:
            case PC:
            case MSIZE:
            case GAS:
            case JUMPDEST:
            case DUP1:
            case DUP2:
            case DUP3:
            case DUP4:
            case DUP5:
            case DUP6:
            case DUP7:
            case DUP8:
            case DUP9:
            case DUP10:
            case DUP11:
            case DUP12:
            case DUP13:
            case DUP14:
            case DUP15:
            case DUP16:
            case PUSH1:
            case PUSH2:
            case PUSH3:
            case PUSH4:
            case PUSH5:
            case PUSH6:
            case PUSH7:
            case PUSH8:
            case PUSH9:
            case PUSH10:
            case PUSH11:
            case PUSH12:
            case PUSH13:
            case PUSH14:
            case PUSH15:
            case PUSH16:
            case PUSH17:
            case PUSH18:
            case PUSH19:
            case PUSH20:
            case PUSH21:
            case PUSH22:
            case PUSH23:
            case PUSH24:
            case PUSH25:
            case PUSH26:
            case PUSH27:
            case PUSH28:
            case PUSH29:
            case PUSH30:
            case PUSH31:
            case PUSH32:
            case SWAP1:
            case SWAP2:
            case SWAP3:
            case SWAP4:
            case SWAP5:
            case SWAP6:
            case SWAP7:
            case SWAP8:
            case SWAP9:
            case SWAP10:
            case SWAP11:
            case SWAP12:
            case SWAP13:
            case SWAP14:
            case SWAP15:
            case SWAP16:
                return 0;

            case JUMP:
            case ISZERO:
            case NOT:
            case BALANCE:
            case CALLDATALOAD:
            case EXTCODESIZE:
            case BLOCKHASH:
            case MLOAD:
            case SLOAD:
            case SUICIDE:

                return 1;

            case JUMPI:
            case ADD:
            case MUL:
            case SUB:
            case DIV:
            case SDIV:
            case MOD:
            case SMOD:
                case EXP:
                case SIGNEXTEND:
            case LT:
            case GT:
            case SLT:
            case SGT:
            case EQ:
            case AND:
            case OR:
            case XOR:
            case BYTE:
            case SHL:
            case SHR:
            case SAR:
            case SHA3:
            case MSTORE:
            case MSTORE8:
            case SSTORE:
            case RETURN:
            case REVERT:
            case LOG0:
                return 2;

            case ADDMOD:
            case MULMOD:
            case CALLDATACOPY:
            case CODECOPY:
            case RETURNDATACOPY:
            case CREATE:
            case LOG1:
                return 3;

            case EXTCODECOPY:
            case LOG2:
            case CREATE2:
                return 4;

            case LOG3:
                return 5;

            case DELEGATECALL:
            case STATICCALL:
            case LOG4:
                return 6;

            case CALL:
            case CALLCODE:
                return 7;

                default:
                    return 0;

        }
    }

    public enum Opcode {
        STOP(0x00),
        ADD(0x01),
        MUL(0x02),
        SUB(0x03),
        DIV(0x04),
        SDIV(0x05),
        MOD(0x06),
        SMOD(0x07),
        ADDMOD(0x08),
        MULMOD(0x09),
        EXP(0x0a),
        SIGNEXTEND(0x0b),

        LT(0x10),
        GT(0x11),
        SLT(0x12),
        SGT(0x13),
        EQ(0x14),
        ISZERO(0x15),
        AND(0x16),
        OR(0x17),
        XOR(0x18),
        NOT(0x19),
        BYTE(0x1a),

        SHL(0x1b),
        SHR(0x1c),
        SAR(0x1d),
        //ROL(0x1e),
        //ROR(0x1f),

        SHA3(0x20),
        ADDRESS(0x30),
        BALANCE(0x31),
        ORIGIN(0x32),
        CALLER(0x33),
        CALLVALUE(0x34),
        CALLDATALOAD(0x35),
        CALLDATASIZE(0x36),
        CALLDATACOPY(0x37),
        CODESIZE(0x38),
        CODECOPY(0x39),
        GASPRICE(0x3a),
        EXTCODESIZE(0x3b),
        EXTCODECOPY(0x3c),
        RETURNDATASIZE(0x3d),
        RETURNDATACOPY(0x3e),
        EXTCODEHASH(0x3f),
        BLOCKHASH(0x40),
        COINBASE(0x41),
        TIMESTAMP(0x42),
        NUMBER(0x43),
        DIFFICULTY(0x44),
        GASLIMIT(0x45),
        POP(0x50),
        MLOAD(0x51),
        MSTORE(0x52),
        MSTORE8(0x53),
        SLOAD(0x54),
        SSTORE(0x55),
        JUMP(0x56),
        JUMPI(0x57),
        PC(0x58),
        MSIZE(0x59),
        GAS(0x5a),
        JUMPDEST(0x5b),
        PUSH1(0x60),
        PUSH2(0x61),
        PUSH3(0x62),
        PUSH4(0x63),
        PUSH5(0x64),
        PUSH6(0x65),
        PUSH7(0x66),
        PUSH8(0x67),
        PUSH9(0x68),
        PUSH10(0x69),
        PUSH11(0x6a),
        PUSH12(0x6b),
        PUSH13(0x6c),
        PUSH14(0x6d),
        PUSH15(0x6e),
        PUSH16(0x6f),
        PUSH17(0x70),
        PUSH18(0x71),
        PUSH19(0x72),
        PUSH20(0x73),
        PUSH21(0x74),
        PUSH22(0x75),
        PUSH23(0x76),
        PUSH24(0x77),
        PUSH25(0x78),
        PUSH26(0x79),
        PUSH27(0x7a),
        PUSH28(0x7b),
        PUSH29(0x7c),
        PUSH30(0x7d),
        PUSH31(0x7e),
        PUSH32(0x7f),
        DUP1(0x80),
        DUP2(0x81),
        DUP3(0x82),
        DUP4(0x83),
        DUP5(0x84),
        DUP6(0x85),
        DUP7(0x86),
        DUP8(0x87),
        DUP9(0x88),
        DUP10(0x89),
        DUP11(0x8a),
        DUP12(0x8b),
        DUP13(0x8c),
        DUP14(0x8d),
        DUP15(0x8e),
        DUP16(0x8f),
        SWAP1(0x90),
        SWAP2(0x91),
        SWAP3(0x92),
        SWAP4(0x93),
        SWAP5(0x94),
        SWAP6(0x95),
        SWAP7(0x96),
        SWAP8(0x97),
        SWAP9(0x98),
        SWAP10(0x99),
        SWAP11(0x9a),
        SWAP12(0x9b),
        SWAP13(0x9c),
        SWAP14(0x9d),
        SWAP15(0x9e),
        SWAP16(0x9f),
        LOG0(0xa0),
        LOG1(0xa1),
        LOG2(0xa2),
        LOG3(0xa3),
        LOG4(0xa4),
        CREATE(0xf0),
        CALL(0xf1),
        CALLCODE(0xf2),
        RETURN(0xf3),
        DELEGATECALL(0xf4),
        CREATE2(0xf5),
        STATICCALL(0xfa),
        REVERT(0xfd),
        INVALID(0xfe),
        SUICIDE(0xff);

        private static final Map<Integer, Opcode> intToOpcode;

        static {
            intToOpcode = new HashMap<>();
            for (Opcode v : Opcode.values()) {
                intToOpcode.put(v.opcode, v);
            }
        }

        public final int opcode;

        Opcode(int opcode) {
            this.opcode = opcode;
        }

        public static Optional<Opcode> findByInt(int i) {
            Opcode ret = intToOpcode.get(i);
            if (ret == null) {
                LOGGER.warn("Integer " + i + " does not correspond to opcode!");
                return Optional.empty();
            }
            return Optional.of(ret);
        }

        public static boolean isDelegateCallOrCallCode(Opcode opcode) {
            return opcode.opcode == DELEGATECALL.opcode || opcode.opcode == CALLCODE.opcode;
        }

        public static boolean isPush(Opcode opcode) {
            return opcode.opcode >= PUSH1.opcode && opcode.opcode <= PUSH32.opcode;
        }

        public static int getDupParameter(Opcode opcode) {
            if (isDup(opcode)) {
                return opcode.opcode - DUP1.opcode + 1;
            }
            throw new IllegalArgumentException("Given argument " + opcode + " is not a dup operation!");
        }

        public static int getSwapParameter(Opcode opcode) {
            if (isSwap(opcode)) {
                return opcode.opcode - SWAP1.opcode + 1;
            }
            throw new IllegalArgumentException("Given argument " + opcode + " is not a swap operation!");
        }

        public static int getLogParameter(Opcode opcode) {
            if (isLog(opcode)) {
                return opcode.opcode - LOG0.opcode;
            }
            throw new IllegalArgumentException("Given argument " + opcode + " is not a log operation!");
        }

        public static int getNumberOfPushedBytes(Opcode opcode) {
            if (isPush(opcode)) {
                return (opcode.opcode - PUSH1.opcode) + 1;
            }
            throw new IllegalArgumentException("Given argument " + opcode + " is not a push operation!");
        }

        public static boolean isDup(Opcode opcode) {
            return opcode.opcode >= DUP1.opcode && opcode.opcode <= DUP16.opcode;
        }

        public static boolean isSwap(Opcode opcode) {
            return opcode.opcode >= SWAP1.opcode && opcode.opcode <= SWAP16.opcode;
        }

        public static boolean isLog(Opcode opcode) {
            return opcode.opcode >= LOG0.opcode && opcode.opcode <= LOG4.opcode;
        }
    }
}
