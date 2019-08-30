package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.data.Expression;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;

import java.util.Optional;

public class ConstDefinitionVisitor extends ASBaseVisitor<Optional<Expression.ConstExpression>> {
    private VisitorState state;

    public ConstDefinitionVisitor() {
        this(new VisitorState());
    }

    public ConstDefinitionVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<Expression.ConstExpression> visitConstDefinition(ASParser.ConstDefinitionContext ctx) {
        String constID = ctx.constID().getText();

        if(state.isConstantDefined(constID)) {
            return valueUndefined("Constant", constID, ctx);
        }

        ExpressionVisitor expressionVisitor = new ExpressionVisitor(new VisitorState(state));
        Optional<Expression> optExpression = expressionVisitor.visit(ctx.exp());

        if(!optExpression.isPresent()) {
            return Optional.empty();
        }

        ConstnessExpressionVisitor constnessVisitor = new ConstnessExpressionVisitor();
        if(!optExpression.get().accept(constnessVisitor)) {
            return argumentNotConst("Expression", optExpression.get(), ctx);
        }

        return Optional.of(new Expression.ConstExpression(constID, optExpression.get()));
    }

    private Optional<Expression.ConstExpression> valueUndefined(String element, String name, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, ctx));
        return Optional.empty();
    }

    private Optional<Expression.ConstExpression> argumentNotConst(String var, Expression expression, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateElementNotConstError(var, expression.getType(), ctx));
        return Optional.empty();
    }

}
