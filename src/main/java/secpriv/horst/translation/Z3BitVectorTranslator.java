package secpriv.horst.translation;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;

public interface Z3BitVectorTranslator {
    Expr bvand(Context context, IntExpr i1, IntExpr i2);
    Expr bvxor(Context context, IntExpr i1, IntExpr i2);
    Expr bvor(Context context, IntExpr i1, IntExpr i2);
    Expr bvneg(Context context, IntExpr i);
}
