package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Operation;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OperationVisitor extends ASBaseVisitor<Optional<Operation>> {
    private VisitorState state;

    public OperationVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<Operation> visitOperationDefinition(ASParser.OperationDefinitionContext ctx) {
        String operationName = ctx.opID().getText();
        String returnTypeName = ctx.type().getText();

        if(!state.getType(returnTypeName).isPresent()) {
            return valueUndefined("Return type", returnTypeName, ctx);
        }

        Type returnType = state.getType(returnTypeName).get();

        List<Expression.ParVarExpression> parameters = new ArrayList<>();
        List<Expression.VarExpression> arguments = new ArrayList<>();

        if(ctx.parameters() != null) {
            for (ASParser.ParameterContext pc : ctx.parameters().parameter()) {
                String parName = pc.paramID().getText();
                Type parType = state.getType(pc.baseType().getText()).get();

                parameters.add(new Expression.ParVarExpression(parType, parName));
            }
        }

        for (ASParser.ArgumentContext ac : ctx.arguments().argument()) {
            String argName = ac.ID().getText();
            Optional<? extends Type> optArgType = state.getType(ac.type().getText());

            if (!optArgType.isPresent()) {
                return valueUndefined("Argument type", argName, ctx);
            }
            arguments.add(new Expression.VarExpression(optArgType.get(), argName));
        }

        VisitorState childState = new VisitorState(state);

        for (Integer count = 0; count < arguments.size(); count++){
            Expression.VarExpression param = arguments.get(count);
            if (!childState.defineVar(param)) {
                return variableAlreadyBound("Argument", param.name, count, ctx);
            }
        }

        for (Integer count = 0; count < parameters.size(); count++){
            Expression.ParVarExpression param = parameters.get(count);
            if (!childState.defineParameterVar(param)) {
                return valueUndefined("Parameter", param.name, count, ctx);
            }
        }

        ExpressionVisitor visitor = new ExpressionVisitor(childState);

        Optional<Expression> optBody = checkIfExpressionMatchesType(visitor.visit(ctx.exp()), returnType, ctx);

        return optBody.map(expression -> new Operation(operationName, expression, parameters, arguments));

    }

    private Optional<Operation> valueUndefined(String element, String name, ParserRuleContext ctx){
        return valueUndefined(element, name, -1, ctx);
    }

    private Optional<Operation> valueUndefined(String element, String name, int count, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, count, ctx));
        return Optional.empty();
    }

    private Optional<Operation> variableAlreadyBound(String element, String className, Integer position, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementAlreadyBoundError(element, className, position, ctx));
        return Optional.empty();
    }

    private Optional<Expression> checkIfExpressionMatchesType(Optional<Expression> expression, Type expectedType, ParserRuleContext ctx) {
        if (!expression.isPresent()) {
            return Optional.empty();
        }

        Optional<Expression> ret = expression.filter(exp -> exp.getType().equals(expectedType));

        if (!ret.isPresent()) {
            state.errorHandler.handleError(ErrorHelper.generateTypeMismatchError(expression.get().getType(), expectedType, ctx));
            return Optional.empty();
        }
        return ret;
    }
}
