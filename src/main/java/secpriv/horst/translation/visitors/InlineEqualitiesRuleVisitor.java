package secpriv.horst.translation.visitors;

public class InlineEqualitiesRuleVisitor extends ClauseMappingRuleVisitor {
    public InlineEqualitiesRuleVisitor() {
        super(new InlineEqualitiesClauseVisitor());
    }
}
