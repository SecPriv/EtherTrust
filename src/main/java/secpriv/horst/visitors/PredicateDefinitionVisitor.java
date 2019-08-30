package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Predicate;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.OptionalInfo;
import secpriv.horst.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfListWithErrorReporting;

public class PredicateDefinitionVisitor extends ASBaseVisitor<Optional<Predicate>> {
    private VisitorState state;

    public PredicateDefinitionVisitor() {
        this(new VisitorState());
    }

    public PredicateDefinitionVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<Predicate> visitPredicateDeclaration(ASParser.PredicateDeclarationContext ctx) {
        String name = ctx.predID().getText();

        if(state.isPredicateDefined(name)) {
            return valueUndefined("Predicate", name, ctx);
        }

        List<Type> optParameterTypes = new ArrayList<>();

        if(ctx.baseTypes() != null) {
            OptionalInfo<Type> parameterTypesOptionalInfo = listOfOptionalToOptionalOfListWithErrorReporting(ctx.baseTypes().baseType().stream().map(p -> state.getType(p.getText())).collect(Collectors.toList()));
            if (!parameterTypesOptionalInfo.isSuccess()) {
                return valueUndefined("Parameter type", ctx.baseTypes().getText(), parameterTypesOptionalInfo.getPosition(), ctx);
            }
            optParameterTypes = parameterTypesOptionalInfo.getList();
        }

        OptionalInfo<Type> argumentTypesOptionalInfo = listOfOptionalToOptionalOfListWithErrorReporting(ctx.types().type().stream().map(p -> state.getType(p.getText())).collect(Collectors.toList()));
        if (!argumentTypesOptionalInfo.isSuccess()) {
            return valueUndefined("Argument type", ctx.types().getText(), argumentTypesOptionalInfo.getPosition(), ctx);
        }

        return Optional.of(new Predicate(name, optParameterTypes, argumentTypesOptionalInfo.getList()));
    }

    private Optional<Predicate> valueUndefined(String element, String name, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, ctx));
        return Optional.empty();
    }

    private Optional<Predicate> valueUndefined(String element, String name, int position, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, position, ctx));
        return Optional.empty();
    }
}
