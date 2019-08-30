package secpriv.horst.evm;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import secpriv.horst.translation.Z3BitVectorTranslator;

import java.math.BigInteger;

public class EvmIntegerBasedZ3BitVectorTranslator implements Z3BitVectorTranslator {
    private final int BIT_WIDTH = 256;
    @Override
    public Expr bvand(Context context, IntExpr i1, IntExpr i2) {
        IntExpr helper = context.mkInt(0);
        for (int n = 0; n < BIT_WIDTH; n++) {
            IntExpr twoton = context.mkInt(BigInteger.valueOf(2).pow(n).toString());
            helper = (IntExpr)

                                    context.mkAdd(
                                            helper,
                                            context.mkMul(
                                                    twoton,
                                                    context.mkMul
                                                            (context.mkMod((IntExpr) context.mkDiv(i1, twoton), context.mkInt(2)),
                                                                    context.mkMod((IntExpr) context.mkDiv(i2, twoton), context.mkInt(2))
                                                            )
                                            )
                                    );
        }
        return helper;
    }

    @Override
    public Expr bvxor(Context context, IntExpr i1, IntExpr i2) {
        IntExpr helper = context.mkInt(0);
        for (int n = 0; n < BIT_WIDTH; n++) {
            IntExpr twoton = context.mkInt(BigInteger.valueOf(2).pow(n).toString());
            helper = (IntExpr) context.mkAdd(helper,
                    context.mkMul(twoton,
                    context.mkMod((IntExpr)
                            context.mkAdd(context.mkMod((IntExpr) context.mkDiv(i1, twoton), context.mkInt(2
                            )), context.mkMod((IntExpr)
                                    context.mkDiv(i2, twoton), context.mkInt(2))), context.mkInt(2))));
        }
        return helper;
    }

    @Override
    public Expr bvor(Context context, IntExpr i1, IntExpr i2) {
        IntExpr helper = context.mkInt(0);
        final IntExpr two = context.mkInt(2);
        for (int n = 0; n < BIT_WIDTH; n++) {
            IntExpr twoton = context.mkInt(BigInteger.valueOf(2).pow(n).toString());
            helper = (IntExpr) context.mkAdd(helper, context.mkMul(twoton,
                    context.mkMod((IntExpr)
                            context.mkAdd(context.mkAdd(context.mkMod((IntExpr) context.mkDiv(i1,
                                    twoton), two), context.mkMod((IntExpr)
                                    context.mkDiv(i2, twoton), two)), context.mkMul(context.mkMod((
                                    IntExpr)
                                    context.mkDiv(i1, twoton), two), context.mkMod((IntExpr)
                                    context.mkDiv(i2, twoton), two))), two)));
        }
        return helper;
    }

    @Override
    public Expr bvneg(Context context, IntExpr i) {
        IntExpr helper = context.mkInt(0);
        final IntExpr one = context.mkInt(0);
        final IntExpr two = context.mkInt(1);
        for (int n = 0; n < BIT_WIDTH; n++) {
            IntExpr twoton = context.mkInt(BigInteger.valueOf(2).pow(n).toString());
            helper = (IntExpr) context.mkAdd(helper, context.mkMul(twoton,
                    context.mkMod((IntExpr) context.mkAdd(context.mkMod((IntExpr)
                            context.mkDiv(i, twoton), two), one), two)));
        }
        return helper;
    }
}
