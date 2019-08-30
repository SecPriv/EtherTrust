package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.data.Expression;
import secpriv.horst.data.SelectorFunction;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.OptionalInfo;
import secpriv.horst.types.Type;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfListWithErrorReporting;

public class SelectorFunctionDefinitionVisitor extends ASBaseVisitor<Optional<SelectorFunction>> {
    VisitorState state;

    public SelectorFunctionDefinitionVisitor() {
        this(new VisitorState());
    }

    public SelectorFunctionDefinitionVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<SelectorFunction> visitSelectorFunctionDeclaration(ASParser.SelectorFunctionDeclarationContext ctx) {
        String selectorFunctionName = ctx.selectFunID().getText();

        if(state.isSelectorFunctionDefined(selectorFunctionName)) {
            return valueUndefined("Selector function", selectorFunctionName, ctx);
        }

        List<Type> optParameterTypeList = Collections.emptyList();

        if(ctx.selectorFunctionArgTypes().baseTypes() != null) {

            OptionalInfo<Type> optionalInfo = listOfOptionalToOptionalOfListWithErrorReporting(ctx.selectorFunctionArgTypes().baseTypes().baseType().stream().map(s -> state.getType(s.getText())).collect(Collectors.toList()));
            if (!optionalInfo.isSuccess()) {
                return valueUndefined("selector function argument type", selectorFunctionName, optionalInfo.getPosition(), ctx);
            }
            optParameterTypeList = optionalInfo.getList();
        }

        OptionalInfo<Type> optionalInfo = listOfOptionalToOptionalOfListWithErrorReporting(ctx.baseTypes().baseType().stream().map(s -> state.getType(s.getText())).collect(Collectors.toList()));
        if (!optionalInfo.isSuccess()) {
            return valueUndefined("selector function return type", selectorFunctionName, optionalInfo.getPosition(), ctx);
        }

        if(!state.hasSelectorFunctionImplementation(selectorFunctionName, optParameterTypeList, optionalInfo.getList())) {
            return selectorFunctionMissingImplementation(selectorFunctionName, optParameterTypeList, optionalInfo.getList(), ctx);
        }

        return Optional.of(new SelectorFunction(selectorFunctionName, optParameterTypeList, optionalInfo.getList()));

    }

    private Optional<SelectorFunction> valueUndefined(String element, String name, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, ctx));
        return Optional.empty();
    }

    private Optional<SelectorFunction> valueUndefined(String element, String name, int position, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, position, ctx));
        return Optional.empty();
    }

    private Optional<SelectorFunction> selectorFunctionMissingImplementation(String selectorFunctionName, List<Type> parameterTypes, List<Type> returnTypes, ASParser.SelectorFunctionDeclarationContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateSelectorFunctionMissingImplementationError(selectorFunctionName, parameterTypes, returnTypes, ctx));
        return Optional.empty();
    }

}
