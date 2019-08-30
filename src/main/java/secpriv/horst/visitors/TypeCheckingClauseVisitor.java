package secpriv.horst.visitors;

import secpriv.horst.translation.visitors.PropositionMappingClauseVisitor;

public class TypeCheckingClauseVisitor extends PropositionMappingClauseVisitor {
    public TypeCheckingClauseVisitor() {
        super(new TypeCheckingPropositionVisitor());
    }
}
