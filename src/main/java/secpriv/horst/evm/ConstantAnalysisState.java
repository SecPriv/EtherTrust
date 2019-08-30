package secpriv.horst.evm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Hash;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class ConstantAnalysisState {
    private static final Logger LOGGER = LogManager.getLogger(ConstantAnalysisState.class);

    BigInteger max = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639936");
    private final ContractLexer.ContractInfo ci;
    public Stack<AbstractValue> stack;
    public HashMap<BigInteger, AbstractValue> memory;
    public HashMap<BigInteger, AbstractValue> storage;
    AbstractValue top, result, val, val2;
    EvmTypes evt;

    //private final BitVectorArithmeticEvaluator bitVectorArithmeticEvaluator = new EvmBitVectorArithmeticEvaluator();

    ConstantAnalysisState(ContractLexer.ContractInfo ci) {
        this.ci = ci;
        this.evt = new EvmTypes();
        this.stack = new Stack<>();
        this.memory = new HashMap<>();
        this.storage = new HashMap<>();
    }

    private Optional<BigInteger> fromUIntOptToBigIntOpt(Optional<EvmTypes.UInt256> v) {
        if (v.isPresent()) {
            return Optional.of(v.get().getValue());
        } else {
            return Optional.empty();
        }
    }

    private final static Function<EvmTypes.UInt256, AbstractValue> returnTop = x -> AbstractValue.Top;

    // op extractByteL{!n:int}(w:int): int := extractByteR{31-!n}(w)
    private BigInteger extractByteL(BigInteger n, BigInteger w) {
        return extractByteR(BigInteger.valueOf(31).subtract(n), w);
    }

    // op extractByteR{!n:int}(w: int): int := (w / pow{!n}(256)) mod 256;
    private BigInteger extractByteR(BigInteger n, BigInteger w) {
        return w.divide(BigInteger.valueOf(256).pow((n.intValue()))).mod(BigInteger.valueOf(256));
    }

    /**
     * Transforms a stack of the form
     * <p>
     * ... Y X
     * <p>
     * to a stack of the form
     * <p>
     * ... Z
     * <p>
     * where Z equals
     * <p>
     * * the result of functionForBothConcrete(X,Y) is X and Y are concrete
     * * the result of functionForSecondConcrete(X) if X is concrete
     * * the result of functionForFirstConcrete(Y) if Y is concrete
     * * Top, if both X and Y are abstract
     * <p>
     * the argument-field of instruction is set to Z.value
     * <p>
     * //@param functionForBothConcrete
     * //@param functionForSecondConcrete
     * //@param functionForFirstConcrete
     *
     * @param instruction
     */

    private void applyToStackIfConcrete(BiFunction<EvmTypes.UInt256, EvmTypes.UInt256, EvmTypes.UInt256> functionForBothConcrete, Function<EvmTypes.UInt256, AbstractValue> functionForSecondConcrete, Function<EvmTypes.UInt256, AbstractValue> functionForFirstConcrete, ContractLexer.OpcodeInstance instruction) {
        AbstractValue x = safePop();
        AbstractValue y = safePop();

        AbstractValue result = AbstractValue.Top;

        if (x.isConcrete() && y.isConcrete()) {
            instruction.args.set(0, fromUIntOptToBigIntOpt(x.value));
            instruction.args.set(1, fromUIntOptToBigIntOpt(y.value));
            result = new AbstractValue(functionForBothConcrete.apply(x.get(), y.get()));
        } else if (x.isConcrete()) {
            instruction.args.set(0, fromUIntOptToBigIntOpt(x.value));
            result = functionForSecondConcrete.apply(x.get());
        } else if (y.isConcrete()) {
            instruction.args.set(1, fromUIntOptToBigIntOpt(y.value));
            result = functionForFirstConcrete.apply(y.get());
        }

        stack.push(result);
        instruction.rez = fromUIntOptToBigIntOpt(result.value);
    }

    private void applyToStackIfConcrete(BiFunction<EvmTypes.UInt256, EvmTypes.UInt256, EvmTypes.UInt256> function, ContractLexer.OpcodeInstance instruction) {
        applyToStackIfConcrete(function, returnTop,
                returnTop, instruction);
    }

    private void applyToStackIfConcrete(Function<EvmTypes.UInt256, EvmTypes.UInt256> function, ContractLexer.OpcodeInstance instruction) {
        AbstractValue x = safePop();

        if (x.isAbstract()) {
            stack.push(AbstractValue.Top);
        } else {
            instruction.args.set(0, fromUIntOptToBigIntOpt(x.value));
            AbstractValue result = new AbstractValue(function.apply(x.get()));
            instruction.rez = fromUIntOptToBigIntOpt(result.value);
            stack.push(result);
        }
    }

    private void applyPredicateToStackIfConcrete(BiPredicate<EvmTypes.UInt256, EvmTypes.UInt256> predicate, ContractLexer.OpcodeInstance instruction) {
        AbstractValue x = safePop();
        AbstractValue y = safePop();

        if (x.isAbstract() || y.isAbstract()) {
            if (!x.isAbstract()) {
                instruction.args.set(0, fromUIntOptToBigIntOpt(x.value));
            }
            if (!y.isAbstract()) {
                instruction.args.set(1, fromUIntOptToBigIntOpt(y.value));
            }
            stack.push(AbstractValue.Top);
        } else {
            AbstractValue result = boolToAbstract(predicate.test(x.get(), y.get()));
            instruction.rez = fromUIntOptToBigIntOpt(result.value);
            stack.push(result);
        }
    }

    private void applyPredicateToStackIfConcrete(Predicate<EvmTypes.UInt256> predicate, ContractLexer.OpcodeInstance instruction) {
        AbstractValue x = safePop();

        if (x.isAbstract()) {
            stack.push(AbstractValue.Top);
        } else {
            instruction.args.set(0, fromUIntOptToBigIntOpt(x.value));
            AbstractValue result = boolToAbstract(predicate.test(x.get()));
            instruction.rez = fromUIntOptToBigIntOpt(result.value);
            stack.push(result);
        }
    }

    private boolean checkJumpDest(BigInteger dest) {
        for (BigInteger jumpDest : ci.getJumpDestinations()) {
            if (dest.equals(jumpDest)) {
                return true;
            }
        }
        return false;
    }

    private static AbstractValue boolToAbstract(boolean b) {
        return b ? new AbstractValue(new EvmTypes.UInt256(BigInteger.ONE)) : new AbstractValue(EvmTypes.UZERO);
    }

    private void defineUniqueTarget(Integer pc, BigInteger target) {
        ArrayList<BigInteger> singleton = new ArrayList<>();
        singleton.add(target);
        ci.jumps.put(BigInteger.valueOf(pc), singleton);
    }

    public void modify(Integer pc, ContractLexer.OpcodeInstance instruction) {
        /*System.out.println("Stack before" + instruction.opcode);
        for (AbstractValue v: stack){
            if (v.isConcrete()){
            System.out.println(v.get().getValue());
            }
            else {
                System.out.println("_");
            }
        }*/

        ContractLexer.Opcode opcode = instruction.opcode;

        if (ContractLexer.Opcode.isPush(opcode)) {
            if (instruction.args.get(0).isPresent()) {
                stack.push(new AbstractValue(new EvmTypes.UInt256(instruction.args.get(0).get())));
            } else {
                LOGGER.error("No PUSH value at pc =" + pc);
            }
        } else if (ContractLexer.Opcode.isDup(opcode)) {
            int dupParameter = ContractLexer.Opcode.getDupParameter(opcode);
            val = safeGet(stack.size() - dupParameter);
            instruction.rez = fromUIntOptToBigIntOpt(val.value);
            stack.push(val);
        } else if (ContractLexer.Opcode.isSwap(opcode)) {
            int swapParameter = ContractLexer.Opcode.getSwapParameter(opcode);
            top = safeGet(stack.size() - 1);
            val = safeGet(stack.size() - swapParameter - 1);
            instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
            instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
            safeSet(stack.size() - 1, val);
            safeSet(stack.size() - swapParameter - 1, top);
        } else if (ContractLexer.Opcode.isLog(opcode)) {
            int logParameter = ContractLexer.Opcode.getLogParameter(opcode);
            popElements(logParameter + 2);
        } else {
            switch (instruction.opcode) {
                case POP:
                    safePop();
                    break;

                case JUMPDEST:
                    break;

                case JUMPI:
                    top = safePop();
                    val = safePop();
                    if (top.isConcrete()) {
                        if (checkJumpDest(top.get().getValue())) {
                            defineUniqueTarget(pc, top.value.get().getValue());
                        } else {
                            // jump target is invalid, so just eliminate all
                            ci.jumps.put(BigInteger.valueOf(pc), Collections.emptyList());
                        }
                    }
                    instruction.args.set(0, fromUIntOptToBigIntOpt(val.value));
                    break;

                case JUMP:
                    top = safePop();
                    if (top.isConcrete()) {
                        if (checkJumpDest(top.get().getValue())) {
                            defineUniqueTarget(pc, top.value.get().getValue());
                        } else {
                            // jump target is invalid, so just eliminate all
                            ci.jumps.put(BigInteger.valueOf(pc), Collections.emptyList());
                        }
                    }
                    break;

                case ADD:
                    applyToStackIfConcrete((x, y) -> x.add(y), instruction);
                    break;

                case MUL:
                    applyToStackIfConcrete(
                            (x, y) -> x.mul(y),
                            x -> x.isZeroBool() ? new AbstractValue(EvmTypes.UZERO) : AbstractValue.Top,
                            y -> y.isZeroBool() ? new AbstractValue(EvmTypes.UZERO) : AbstractValue.Top,
                            instruction);
                    break;

                case SUB:
                    applyToStackIfConcrete((x, y) -> x.sub(y), instruction);
                    break;

                case DIV:
                    applyToStackIfConcrete(
                            (x, y) -> x.div(y), // this is not exactly the EVM-Semantics (there x/0 == 0)
                            returnTop, // I think we could do the same thing as in the line below
                            x -> x.equals(BigInteger.ZERO) ? new AbstractValue(EvmTypes.UZERO) : AbstractValue.Top,
                            instruction);
                    break;

                case SDIV:
                    applyToStackIfConcrete(
                            (x, y) -> x.sdiv(y), // this is not exactly the EVM-Semantics (there x/0 == 0)
                            returnTop, // I think we could do the same thing as in the line below
                            x -> x.equals(BigInteger.ZERO) ? new AbstractValue(EvmTypes.UZERO) : AbstractValue.Top,
                            instruction);
                    break;

                case SMOD:
                    applyToStackIfConcrete((x, y) -> x.smod(y),
                            returnTop,
                            x -> x.equals(BigInteger.ZERO) ? new AbstractValue(EvmTypes.UZERO) : AbstractValue.Top,
                            instruction);
                    break;
                case SIGNEXTEND:
                    top = safePop();
                    val = safePop();
                    try {
                        applyToStackIfConcrete((x, y) -> y.signExtend(x.getValue().intValueExact()), instruction);
                    } catch (java.lang.ArithmeticException e) {
                        result = AbstractValue.Top;
                        stack.push(val);
                    }
                    break;
                case SGT:
                    applyToStackIfConcrete((x, y) -> x.sgt(y), instruction);
                    break;
                case SLT:
                    applyToStackIfConcrete((x, y) -> x.slt(y), instruction);
                    break;
                case BYTE:
                    top = safePop();
                    val = safePop();
                    try {
                        applyToStackIfConcrete((x, y) -> y.byteExtract(x.getValue().intValueExact()), instruction);
                    } catch (java.lang.ArithmeticException e) {
                        result = AbstractValue.Top;
                        stack.push(val);
                    }
                    break;
                case SHL:
                    applyToStackIfConcrete((x, y) -> y.shl(x.getValue().intValueExact()), instruction);
                    break;
                case SHR:
                    applyToStackIfConcrete((x, y) -> y.shr(x.getValue().intValueExact()), instruction);
                    break;
                case SAR:
                    applyToStackIfConcrete((x, y) -> y.sar(x.getValue().intValueExact()), instruction);
                    break;
                //case ROL:
                //   applyToStackIfConcrete((x, y) -> y.rol(x.getValue().intValueExact()), instruction);
                //    break;
                //case ROR:
                //    applyToStackIfConcrete((x, y) -> y.ror(x.getValue().intValueExact()), instruction);
                //    break;
                case MOD:
                    applyToStackIfConcrete((x, y) -> x.mod(y),
                            returnTop,
                            x -> x.equals(BigInteger.ZERO) ? new AbstractValue(EvmTypes.UZERO) : AbstractValue.Top,
                            instruction);
                    break;

                case ADDMOD:
                    top = safePop();
                    val = safePop();
                    val2 = safePop();
                    if (top.value.isPresent() && val.value.isPresent() && val2.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                        instruction.args.set(2, fromUIntOptToBigIntOpt(val2.value));
                        result = new AbstractValue(top.get().addmod(val.get(), val2.get()));
                        instruction.rez = fromUIntOptToBigIntOpt(result.value);
                    } else {
                        if (top.value.isPresent()) {
                            instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        }
                        if (val.value.isPresent()) {
                            instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                        }
                        if (val2.value.isPresent()) {
                            instruction.args.set(2, fromUIntOptToBigIntOpt(val2.value));
                        }
                        result = AbstractValue.Top;
                    }
                    stack.push(result);
                    break;

                case MULMOD:
                    top = safePop();
                    val = safePop();
                    val2 = safePop();
                    result = AbstractValue.Top;
                    if (top.value.isPresent() && val.value.isPresent() && val2.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                        instruction.args.set(2, fromUIntOptToBigIntOpt(val2.value));
                        result = new AbstractValue(top.get().mulmod(val.get(), val2.get()));
                        instruction.rez = fromUIntOptToBigIntOpt(result.value);
                    } else {
                        if (top.value.isPresent()) {
                            instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                            if (top.get().isZeroBool()) {
                                result = new AbstractValue(EvmTypes.UZERO);
                                instruction.rez = fromUIntOptToBigIntOpt(result.value);
                            }
                        }
                        if (val.value.isPresent()) {
                            instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                            if (val.get().isZeroBool()) {
                                result = new AbstractValue(EvmTypes.UZERO);
                                instruction.rez = fromUIntOptToBigIntOpt(result.value);
                            }
                        }
                        if (val2.value.isPresent()) {
                            instruction.args.set(2, fromUIntOptToBigIntOpt(val2.value));
                        }
                    }
                    stack.push(result);
                    break;

                //TODO: check the computation, dangerous cast to int
                case EXP:
                    top = safePop();
                    val = safePop();
                    result = AbstractValue.Top;
                    if (top.value.isPresent() && val.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                        result = new AbstractValue(top.get().pow(val.get()));
                        instruction.rez = fromUIntOptToBigIntOpt(result.value);
                    } else {
                        if (top.value.isPresent()) {
                            instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                            if (top.get().isZeroBool()) {
                                result = new AbstractValue(EvmTypes.UZERO);
                                instruction.rez = fromUIntOptToBigIntOpt(result.value);
                            }
                            if (top.get().isOneBool()) {
                                result = new AbstractValue(EvmTypes.UONE);
                                instruction.rez = fromUIntOptToBigIntOpt(result.value);
                            }
                        }
                        if (val.value.isPresent()) {
                            instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                            if (val.get().isZeroBool()) {
                                result = new AbstractValue(EvmTypes.UONE);
                                instruction.rez = fromUIntOptToBigIntOpt(result.value);
                            }
                        }
                    }
                    stack.push(result);
                    break;

                case AND:
                    applyToStackIfConcrete((x, y) -> x.and(y), instruction);
                    break;

                case OR:
                    applyToStackIfConcrete((x, y) -> x.or(y), instruction);
                    break;

                case XOR:
                    applyToStackIfConcrete((x, y) -> x.xor(y), instruction);
                    break;

                case NOT:
                    applyToStackIfConcrete(x -> x.neg(), instruction);
                    break;

                case LT:
                    applyToStackIfConcrete((x, y) -> x.lt(y), instruction);
                    break;
                case GT:
                    applyToStackIfConcrete((x, y) -> x.gt(y), instruction);
                    break;
                case EQ:
                    applyToStackIfConcrete((x, y) -> x.eq(y), instruction);
                    break;

                case ISZERO:
                    applyToStackIfConcrete(x -> x.isZero(), instruction);
                    break;

                case SHA3:
                    AbstractValue offset = safePop();
                    AbstractValue length = safePop();
                    if (offset.value.isPresent() && length.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(offset.value));
                        instruction.args.set(1, fromUIntOptToBigIntOpt(length.value));
                        ArrayList<Byte> byteList = new ArrayList<>();
                        int posInt = offset.get().getValue().intValue();
                        int sInt = length.get().getValue().intValue();
                        for (int ind = posInt; ind < posInt + sInt; ++ind) {
                            AbstractValue val = memory.getOrDefault(ind, AbstractValue.Top);
                            //AbstractValue val = memory.getOrDefault(ind, new AbstractValue(new EvmTypes.UInt256(BigInteger.ZERO)));
                            if (val != null) {
                                if (val.value.isPresent()) {
                                    byte[] valAsByteArray = val.get().getValue().toByteArray();
                                    for (byte b : valAsByteArray) {
                                        byteList.add(b);
                                    }
                                } else {
                                    byteList = new ArrayList<>();
                                    break;
                                }
                            } else {
                                byteList = new ArrayList<>();
                                break;
                            }
                        }
                        BigInteger rez;
                        if (byteList.size() > 0) {
                            byte[] byteArray = new byte[byteList.size()];
                            for (int i = 0; i < byteList.size(); i++) {
                                byteArray[i] = byteList.get(i).byteValue();
                            }
                            byte[] rezByte = Hash.sha3(byteArray);
                            rez = new BigInteger(1, rezByte).mod(max);
                            val = new AbstractValue(new EvmTypes.UInt256(rez));
                        } else {
                            val = AbstractValue.Top;
                        }
                        instruction.rez = fromUIntOptToBigIntOpt(val.value);
                    } else {
                        val = AbstractValue.Top;
                    }
                    stack.push(val);
                    break;

                case MSTORE:
                    // for (!a : int) in interval(32): x : array<AbsDom> -> store x (o + !a) (absExtractByteL{!a}(v)), mem;
                    top = safePop();
                    val = safePop();
                    if (val.value.isPresent()) {
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                    }
                    // we know the location
                    if (top.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        for (int i = 0; i < 32; ++i) {
                            BigInteger location = top.value.get().getValue().add(BigInteger.valueOf(i));
                            if (val.isConcrete()) {
                                BigInteger v = val.get().getValue();
                                memory.put(location, new AbstractValue(new EvmTypes.UInt256(extractByteL(BigInteger.valueOf(i), v))));
                            } else {
                                memory.put(location, AbstractValue.Top);
                            }
                        }
                    } else {
                        memory = new HashMap<>();
                    }
                    break;

                case MSTORE8:
                    top = safePop();
                    val = safePop();
                    if (val.value.isPresent()) {
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                    }
                    // we know the location
                    if (top.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        BigInteger location = top.value.get().getValue();
                        if (val.value.isPresent()) {
                            memory.put(location, new AbstractValue(val.get().mod(new EvmTypes.UInt256(BigInteger.valueOf(256)))));
                        } else {
                            memory.put(location, AbstractValue.Top);
                        }
                    } else {
                        memory = new HashMap<>();
                    }
                    break;

                case MLOAD:
                    top = safePop();
                    // ?v = for (!i: int) in interval (0, 32): x : AbsDom ->
                    //    			absadd(x, absmul(select ?mem (?p + !i), @V(pow{31-!i}(256)))), @V(0)
                    if (top.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        val = new AbstractValue(new EvmTypes.UInt256(BigInteger.valueOf(0)));
                        for (int i = 0; i < 32; ++i) {
                            BigInteger location = top.value.get().getValue().add(BigInteger.valueOf(i));
                            AbstractValue v = memory.getOrDefault(location, AbstractValue.Top);
                            if (v.isAbstract()) {
                                val = AbstractValue.Top;
                                break;
                            } else {
                                BigInteger bi256 = BigInteger.valueOf(256).pow(31 - i);
                                val = new AbstractValue(val.value.get().add(v.get().mul(new EvmTypes.UInt256(bi256))));
                            }
                        }
                        //if (top.get().mod(BigInteger.valueOf(32)).equals(BigInteger.ZERO)){
                        //val = memory.getOrDefault(top.get(), AbstractValue.Top);
                        if (val.value.isPresent()) {
                            instruction.rez = fromUIntOptToBigIntOpt(val.value);
                        }
                    } else {
                        val = AbstractValue.Top;
                    }
                    stack.push(val);
                    break;

                case SSTORE:
                    top = safePop();
                    val = safePop();
                    if (val.value.isPresent()) {
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                    }
                    if (top.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        // we know the location
                        BigInteger location = top.get().getValue();
                        storage.put(location, val);
                    } else {
                        storage = new HashMap<>();
                    }
                    break;

                case SLOAD:
                    top = safePop();
                    if (top.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        BigInteger location = top.get().getValue();
                        val = storage.getOrDefault(location, AbstractValue.Top);
                        if (val.value.isPresent()) {
                            instruction.rez = fromUIntOptToBigIntOpt(val.value);
                        }
                    } else {
                        val = AbstractValue.Top;
                    }
                    stack.push(val);
                    break;

                // gas	addr	value	argsOffset	argsLength	retOffset	retLength
                case CALL:
                    top = safeGet(stack.size() - 6);
                    val = safeGet(stack.size() - 7);
                    if (top.value.isPresent() && val.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                    }
                    overApproximate(7); // success flag over-approximated
                    storage = new HashMap<>();
                    break;
                case CALLCODE:
                case DELEGATECALL:
                    // TODO: we should never be here, error is thrown by lexer if we are not generating statistics
                    break;

                case CREATE:
                    overApproximate(3); // address over-approximated
                    storage = new HashMap<>();
                    break;
                case CREATE2:
                    overApproximate(4);
                    storage = new HashMap<>();
                    break;
                // gas	addr	argsOffset	argsLength	retOffset	retLength
                case STATICCALL:
                    top = safeGet(stack.size() - 5);
                    val = safeGet(stack.size() - 6);
                    if (top.value.isPresent() && val.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                    }
                    overApproximate(6); // success flag over-approximated
                    storage = new HashMap<>();
                    break;

                case ADDRESS:
                case ORIGIN:
                case CALLER:
                case CALLVALUE:
                case CALLDATASIZE:
                case CODESIZE:
                case GASPRICE:
                case RETURNDATASIZE:
                case PC:
                case MSIZE:
                case GAS:
                    overApproximate(0);
                    break;

                case BALANCE:
                case CALLDATALOAD:
                case EXTCODESIZE:
                case BLOCKHASH:
                case EXTCODEHASH:
                    overApproximate(1);
                    break;

                case CALLDATACOPY:
                case CODECOPY:
                case RETURNDATACOPY:
                    // destOffset	offset	length
                    top = safePop();
                    val = safeGet(stack.size() - 3);
                    if (top.value.isPresent() && val.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                    }
                    safePop();
                    safePop();
                    this.memory = new HashMap<>();
                    break;

                case EXTCODECOPY:
                    // addr	destOffset	offset	length
                    top = safeGet(stack.size() - 2);
                    val = safeGet(stack.size() - 4);
                    if (top.value.isPresent() && val.value.isPresent()) {
                        instruction.args.set(0, fromUIntOptToBigIntOpt(top.value));
                        instruction.args.set(1, fromUIntOptToBigIntOpt(val.value));
                    }
                    safePop();
                    safePop();
                    safePop();
                    safePop();
                    this.memory = new HashMap<>();
                    break;

                //TODO we can get this info from blockchain
                case COINBASE:
                case TIMESTAMP:
                case NUMBER:
                case DIFFICULTY:
                case GASLIMIT:
                    overApproximate(0);
                    break;
            }
        }
    }

    /*private int getCompareToResult(ContractLexer.Opcode opcode) {
        switch (opcode) {
            case LT:
                return -1;
            case EQ:
                return 0;
            case GT:
                return 1;
        }
        throw new IllegalArgumentException("Argument has to be one of: LT, EQ, GT but was " + opcode);
    } */

    private void popElements(int argumentCount) {
        for (int i = 0; i < argumentCount; ++i) {
            safePop();
        }
    }

    private void overApproximate(int argumentCount) {
        popElements(argumentCount);
        stack.push(AbstractValue.Top);
    }

    /*private AbstractValue safeGetLast() {
        Vector<AbstractValue> stack_vector = stack;
        AbstractValue result = AbstractValue.Top;
        if (!stack_vector.isEmpty()) {
            result = stack_vector.lastElement();
            if (result == null) {
                result = AbstractValue.Top;
            }
        }
        return result;
    } */

    private AbstractValue safeGet(int index) {
        if (index < 0) {
            return AbstractValue.Top;
        }
        Vector<AbstractValue> stack_vector = stack;
        AbstractValue result = AbstractValue.Top;
        if (!stack_vector.isEmpty()) {
            result = stack_vector.get(index);
            if (result == null) {
                result = AbstractValue.Top;
            }
        }
        return result;
    }

    private void safeSet(int index, AbstractValue val) {
        try {
            stack.set(index, val);
        } catch (java.lang.IndexOutOfBoundsException e) {
            //TODO: at the moment do nothingapplyToStackIfConcrete
        }
    }

    private AbstractValue safePop() {
        AbstractValue result;
        try {
            result = stack.pop();
        } catch (java.util.EmptyStackException e) {
            result = AbstractValue.Top;
            //TODO: at the moment do nothing
        }
        return result;
    }

    /*private void safePop() {
        try {
            safePopValue();
        } catch (java.util.EmptyStackException e) {
            //TODO: at the moment do nothing
        }
    }*/
}