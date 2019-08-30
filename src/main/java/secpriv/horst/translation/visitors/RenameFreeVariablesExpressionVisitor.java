package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;

import java.util.Collections;
import java.util.Map;

public class RenameFreeVariablesExpressionVisitor extends AbstractExpressionVisitor {
    final private Map<String, String> renamingMap;

    public RenameFreeVariablesExpressionVisitor(Map<String, String> renamingMap) {
        this.renamingMap = Collections.unmodifiableMap(renamingMap);
    }

    @Override
    public Expression visit(Expression.FreeVarExpression expression) {
        String newName = renamingMap.getOrDefault(expression.name, expression.name);
        return new Expression.FreeVarExpression(expression.type, newName);
    }
}
