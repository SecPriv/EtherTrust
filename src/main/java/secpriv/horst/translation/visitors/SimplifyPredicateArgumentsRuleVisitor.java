package secpriv.horst.translation.visitors;

public class SimplifyPredicateArgumentsRuleVisitor extends ClauseMappingRuleVisitor {
    public SimplifyPredicateArgumentsRuleVisitor() {
        super(new SimplifyPredicateArgumentsClauseVisitor());
    }
}
