package secpriv.horst.evm;

import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.data.tuples.Tuple4;
import secpriv.horst.data.tuples.Tuple5;

import java.math.BigInteger;

public interface EvmSelectorFunctionProviderTemplate {
    //Iterable<BigInteger> interval(BigInteger a);

    Iterable<BigInteger> interval(BigInteger a, BigInteger b);

    Iterable<Tuple2<BigInteger, BigInteger>> sizeAndOffSetForWordsize(BigInteger wordSize);

    //Iterable<Tuple2<BigInteger, BigInteger>> idsAndPcsForOpcode(BigInteger opcode);

    //Iterable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> idsAndPcsAndValuesAndOffsetsForPush();

    // Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndJumpDestsForOpcode(BigInteger opcode);

    // Iterable<BigInteger> jumpDestsForID(BigInteger id);

    // Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndOffsetsForDup();

    // Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndOffsetsForSwap();

    // Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndValuesForTargetOpcode(BigInteger opcode);

    // Iterable<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> idsAndPcsAndArgumentsForOpcode(BigInteger opcode);

    //Iterable<BigInteger> idInit();

    //Iterable<Tuple2<BigInteger, BigInteger>> idsAndLastPc();

    //Iterable<Tuple2<BigInteger, BigInteger>> allIdsAndPcs();

    Iterable<BigInteger> binOps();

    Iterable<BigInteger> unOps();

    Iterable<BigInteger> terOps();

    Iterable<BigInteger> unitOps();

    Iterable<BigInteger> copyOps();




    Iterable<BigInteger> ids();
    Iterable<BigInteger> pcsForIdAndOpcode(BigInteger id, BigInteger opcode);
    Iterable<Tuple2<BigInteger, BigInteger>> resultsForIdAndPc(BigInteger id, BigInteger pc);
    Iterable<BigInteger> argumentsZeroForIdAndPc(BigInteger id, BigInteger pc);
    Iterable<BigInteger> argumentsOneForIdAndPc(BigInteger id, BigInteger pc);
    Iterable<Tuple2<BigInteger, BigInteger>> argumentsTwoForIdAndPc(BigInteger id, BigInteger pc);
    Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> argumentsThreeForIdAndPc(BigInteger id, BigInteger pc);
    Iterable<BigInteger> jumpDestsForIdAndPc(BigInteger id, BigInteger pc);
    Iterable<Boolean> jumpDestUniqueForIdAndPc(BigInteger id, BigInteger pc);
    Iterable<BigInteger> pcsForId(BigInteger id);
    Iterable<BigInteger> lastPcsForId(BigInteger id);
}
