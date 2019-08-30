package secpriv.horst.translation.visitors;

public class ConstantFoldingRuleVisitor extends ClauseMappingRuleVisitor {
    public ConstantFoldingRuleVisitor() {
        super(new ConstantFoldingClauseVisitor());
    }
}
