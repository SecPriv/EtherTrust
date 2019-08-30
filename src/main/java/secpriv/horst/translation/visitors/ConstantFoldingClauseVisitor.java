package secpriv.horst.translation.visitors;

public class ConstantFoldingClauseVisitor extends PropositionMappingClauseVisitor {
    public ConstantFoldingClauseVisitor() {
        super(new ConstantFoldingPropositionVisitor());
    }
}
