package secpriv.horst.translation.visitors;

import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.internals.SelectorFunctionInvoker;

import java.util.Map;

public class InstantiateParametersClauseVisitor extends PropositionMappingClauseVisitor {
    public InstantiateParametersClauseVisitor(Map<String, BaseTypeValue> parameterMap, SelectorFunctionInvoker selectorFunctionInvoker) {
        super(new InstantiateParametersPropositionVisitor(parameterMap, selectorFunctionInvoker));
    }
}
