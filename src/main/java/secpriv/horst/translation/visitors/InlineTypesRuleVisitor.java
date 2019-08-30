package secpriv.horst.translation.visitors;

public class InlineTypesRuleVisitor extends ClauseMappingRuleVisitor {
    public InlineTypesRuleVisitor(InlineTypesExpressionVisitor expressionVisitor) {
        super(new InlineTypesClauseVisitor(expressionVisitor));
    }
}
