package secpriv.horst.translation.visitors;

import secpriv.horst.data.Operation;

import java.util.List;

public class InlineOperationsClauseVisitor extends PropositionMappingClauseVisitor {
    public InlineOperationsClauseVisitor(List<Operation> operations) {
        super(new InlineOperationsPropositionVisitor(operations));
    }
}
