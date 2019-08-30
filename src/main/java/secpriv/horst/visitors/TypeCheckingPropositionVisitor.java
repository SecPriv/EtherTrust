package secpriv.horst.visitors;

import secpriv.horst.translation.visitors.ExpressionMappingPropositionVisitor;

public class TypeCheckingPropositionVisitor extends ExpressionMappingPropositionVisitor {
    public TypeCheckingPropositionVisitor() {
        super(new TypeCheckingExpressionVisitor());
    }
}
