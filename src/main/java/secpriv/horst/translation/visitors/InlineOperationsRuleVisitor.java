package secpriv.horst.translation.visitors;

import secpriv.horst.data.Operation;

import java.util.List;

public class InlineOperationsRuleVisitor extends ClauseMappingRuleVisitor {
    public InlineOperationsRuleVisitor(List<Operation> operations) {
        super(new InlineOperationsClauseVisitor(operations));
    }
}
