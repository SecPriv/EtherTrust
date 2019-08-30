package secpriv.horst.visitors;

import secpriv.horst.translation.visitors.ClauseMappingRuleVisitor;

public class TypeCheckingRuleVisitor extends ClauseMappingRuleVisitor {
    public TypeCheckingRuleVisitor() {
        super(new TypeCheckingClauseVisitor());
    }
}
