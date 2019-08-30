package secpriv.horst.translation.visitors;

import secpriv.horst.data.Operation;

import java.util.List;

public class InlineOperationsPropositionVisitor extends ExpressionMappingPropositionVisitor {
    public InlineOperationsPropositionVisitor(List<Operation> operations) {
        super(new InlineOperationsExpressionVisitor(operations));
    }
}
