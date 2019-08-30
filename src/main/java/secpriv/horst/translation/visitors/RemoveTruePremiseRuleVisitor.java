package secpriv.horst.translation.visitors;

public class RemoveTruePremiseRuleVisitor extends ClauseMappingRuleVisitor {
    public RemoveTruePremiseRuleVisitor() {
        super(new RemoveTruePremiseClauseVisitor());
    }
}
