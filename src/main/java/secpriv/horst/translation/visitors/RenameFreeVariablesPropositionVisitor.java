package secpriv.horst.translation.visitors;

import java.util.Map;

public class RenameFreeVariablesPropositionVisitor extends ExpressionMappingPropositionVisitor {
    public RenameFreeVariablesPropositionVisitor(Map<String, String> renamingMap) {
        super(new RenameFreeVariablesExpressionVisitor(renamingMap));
    }
}
