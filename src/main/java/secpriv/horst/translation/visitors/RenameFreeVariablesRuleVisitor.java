package secpriv.horst.translation.visitors;

public class RenameFreeVariablesRuleVisitor extends ClauseMappingRuleVisitor {
    public RenameFreeVariablesRuleVisitor() {
        super(new AlphaRenamingClauseVisitor());
    }
}
