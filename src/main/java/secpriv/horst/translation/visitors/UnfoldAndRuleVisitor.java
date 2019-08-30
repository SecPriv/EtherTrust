package secpriv.horst.translation.visitors;

public class UnfoldAndRuleVisitor extends ClauseMappingRuleVisitor {
    public UnfoldAndRuleVisitor() {
        super(new UnfoldAndClauseVisitor());
    }
}
