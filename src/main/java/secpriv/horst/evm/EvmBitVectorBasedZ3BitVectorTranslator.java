package secpriv.horst.evm;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import secpriv.horst.translation.Z3BitVectorTranslator;

public class EvmBitVectorBasedZ3BitVectorTranslator implements Z3BitVectorTranslator {
    private final int BIT_WIDTH = 256;

    @Override
    public Expr bvand(Context context, IntExpr i1, IntExpr i2) {
        BitVecExpr bvA = context.mkInt2BV(BIT_WIDTH, i1);
        BitVecExpr bvB = context.mkInt2BV(BIT_WIDTH, i2);
        BitVecExpr rez = context.mkBVAND(bvA, bvB);
        return context.mkBV2Int(rez, false);
    }

    @Override
    public Expr bvxor(Context context, IntExpr i1, IntExpr i2) {
        BitVecExpr bvA = context.mkInt2BV(BIT_WIDTH, i1);
        BitVecExpr bvB = context.mkInt2BV(BIT_WIDTH, i2);
        BitVecExpr rez = context.mkBVXOR(bvA, bvB);
        return context.mkBV2Int(rez, false);
    }

    @Override
    public Expr bvor(Context context, IntExpr i1, IntExpr i2) {

        BitVecExpr bvA = context.mkInt2BV(BIT_WIDTH, i1);
        BitVecExpr bvB = context.mkInt2BV(BIT_WIDTH, i2);
        BitVecExpr rez = context.mkBVOR(bvA, bvB);
        return context.mkBV2Int(rez, false);
    }

    @Override
    public Expr bvneg(Context context, IntExpr i) {
        BitVecExpr bvA = context.mkInt2BV(BIT_WIDTH, i);
        BitVecExpr not = context.mkBVNot(bvA);
        return context.mkBV2Int(not, false);
    }
}
