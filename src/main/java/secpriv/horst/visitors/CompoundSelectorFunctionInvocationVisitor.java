package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.data.CompoundSelectorFunctionInvocation;
import secpriv.horst.data.Expression;
import secpriv.horst.data.SelectorFunctionInvocation;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.visitors.SelectorFunctionInvocationVisitor;
import secpriv.horst.visitors.VisitorState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompoundSelectorFunctionInvocationVisitor extends ASBaseVisitor<Optional<CompoundSelectorFunctionInvocation>> {
    private final VisitorState state;

    public CompoundSelectorFunctionInvocationVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<CompoundSelectorFunctionInvocation> visitSelectorExp(ASParser.SelectorExpContext ctx) {
        List<SelectorFunctionInvocation> selectorFunctionInvocations = new ArrayList<>();

        for (ParserRuleContext invocationContext : ctx.selectorInvocation()) {
            Optional<SelectorFunctionInvocation> optSelectorFunctionInvocation = invocationContext.accept(new SelectorFunctionInvocationVisitor(state));
            if (!optSelectorFunctionInvocation.isPresent()) {
                return Optional.empty();
            }
            selectorFunctionInvocations.add(optSelectorFunctionInvocation.get());
            for (Expression.ParVarExpression parameter : optSelectorFunctionInvocation.get().parameters) {
                if (!state.defineParameterVar(parameter)) {
                    return variableAlreadyBound("Parameter", parameter.name, ctx);
                }
            }
        }

        return Optional.of(new CompoundSelectorFunctionInvocation(selectorFunctionInvocations));
    }

    private Optional<CompoundSelectorFunctionInvocation> variableAlreadyBound(String element, String name, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementAlreadyBoundError(element, name, ctx));
        return Optional.empty();
    }
}
