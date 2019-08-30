package secpriv.horst.translation.visitors;

public class ConstantFoldingPropositionVisitor extends ExpressionMappingPropositionVisitor {
    public ConstantFoldingPropositionVisitor() {
        super(new ConstantFoldingExpressionVisitor());
    }
}
