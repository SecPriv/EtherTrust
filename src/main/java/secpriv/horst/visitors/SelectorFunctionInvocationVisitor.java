package secpriv.horst.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.SelectorFunction;
import secpriv.horst.data.SelectorFunctionInvocation;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.ZipInfo;
import secpriv.horst.types.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfList;
import static secpriv.horst.tools.Zipper.zipPredicateWithErrorReporting;

public class SelectorFunctionInvocationVisitor extends ASBaseVisitor<Optional<SelectorFunctionInvocation>> {
    private final VisitorState state;

    public SelectorFunctionInvocationVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<SelectorFunctionInvocation> visitSelectorInvocation(ASParser.SelectorInvocationContext ctx) {
        String selectorFunctionName = ctx.selectorApp().selectFunID().getText();
        Optional<SelectorFunction> optSelectorFunction = state.getSelectorFunction(selectorFunctionName);

        if (!optSelectorFunction.isPresent()) {
            return valueUndefined("Selector function", selectorFunctionName, ctx);
        }
        SelectorFunction selectorFunction = optSelectorFunction.get();
        List<Expression.ParVarExpression> parameters = new ArrayList<>();

        for (ASParser.ParameterContext p : ctx.parameters().parameter()) {
            String parName = p.paramID().getText();
            Optional<? extends Type> optParType = state.getType(p.baseType().getText());

            if (!optParType.isPresent()) {
                return valueUndefined("Parameter", parName, ctx);
            }

            parameters.add(new Expression.ParVarExpression(optParType.get(), parName));
        }


        ZipInfo zipInfo = zipPredicateWithErrorReporting(selectorFunction.returnTypes, parameters, (r, p) -> r.equals(p.getType()));
        if (!zipInfo.isSuccess()) {
            return mismatchInArgumentList("selector function return type", zipInfo, ctx);
        }

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(new VisitorState(state));

        Optional<List<Expression>> optSelectorFunctionArguments = Optional.of(Collections.emptyList());

        if (ctx.selectorApp().selectFunArgs() != null) {
            optSelectorFunctionArguments = listOfOptionalToOptionalOfList(ctx.selectorApp().selectFunArgs().exp().stream().map(expressionVisitor::visit).collect(Collectors.toList()));
        }

        if (!optSelectorFunctionArguments.isPresent()) {
            return Optional.empty();
        }
        List<Expression> arguments = optSelectorFunctionArguments.get();

        ConstnessExpressionVisitor constnessVisitor = new ConstnessExpressionVisitor();

        for (Expression argument : arguments) {
            if (!argument.accept(constnessVisitor)) {
                return argumentNotConst("Selector function argument", argument, ctx);
            }
        }

        zipInfo = zipPredicateWithErrorReporting(selectorFunction.parameterTypes, arguments, (t, a) -> t.equals(a.getType()));
        if (!zipInfo.isSuccess()) {
            return mismatchInArgumentList("selector function parameter type", zipInfo, ctx);
        }

        VisitorState childState = new VisitorState(state);
        for (Expression.ParVarExpression p : parameters) {
            if (!childState.defineParameterVar(p)) {
                return variableAlreadyBound("Parameter", p.name, ctx);
            }
        }

        return Optional.of(new SelectorFunctionInvocation(selectorFunction, parameters, arguments));
    }

    private Optional<SelectorFunctionInvocation> valueUndefined(String element, String name, ASParser.SelectorInvocationContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, ctx));
        return Optional.empty();
    }

    private Optional<SelectorFunctionInvocation> mismatchInArgumentList(String element, ZipInfo zipInfo, ASParser.SelectorInvocationContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateMismatchInListError(element, zipInfo, ctx));
        return Optional.empty();
    }

    private Optional<SelectorFunctionInvocation> argumentNotConst(String element, Expression expression, ASParser.SelectorInvocationContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementNotConstError(element, expression.getType(), ctx));
        return Optional.empty();
    }

    private Optional<SelectorFunctionInvocation> variableAlreadyBound(String element, String name, ASParser.SelectorInvocationContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementAlreadyBoundError(element, name, ctx));
        return Optional.empty();
    }
}
