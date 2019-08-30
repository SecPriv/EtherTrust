package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.data.Expression;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.OptionalInfo;
import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfList;
import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfListWithErrorReporting;

public class TypeVisitor extends ASBaseVisitor<Optional<Type.CustomType>> {
    public VisitorState state;

    public TypeVisitor(VisitorState state) {
        this.state = state;
    }

    public TypeVisitor() {
        this(new VisitorState());
    }

    private class ConstructorVisitor extends ASBaseVisitor<Optional<Constructor>> {
        @Override
        public Optional<Constructor> visitAbstractDomainElement(ASParser.AbstractDomainElementContext ctx) {
            String constructorName = ctx.elementID().getText();
            if(ctx.types() != null) {
                OptionalInfo<Type> optionalInfo = listOfOptionalToOptionalOfListWithErrorReporting(ctx.types().type().stream().map(p -> state.getType(p.getText())).collect(Collectors.toList()));
                if (!optionalInfo.isSuccess()) {
                    return valueUndefined("Data type", ctx.types().getText(), optionalInfo.getPosition(), ctx);
                }
                return Optional.of(new Constructor(constructorName, optionalInfo.getList()));
            }
            return Optional.of(new Constructor(constructorName));
        }
    }
    @Override
    public Optional<Type.CustomType> visitAbstractDomainDeclaration(ASParser.AbstractDomainDeclarationContext ctx) {
        String typeName = ctx.typeID().getText();

        ConstructorVisitor visitor = new ConstructorVisitor();

        Optional<List<Constructor>> optConstructors = listOfOptionalToOptionalOfList(ctx.abstractDomainElements().abstractDomainElement().stream().map(visitor::visit).collect(Collectors.toList()));

        return optConstructors.map(constructors -> new Type.CustomType(typeName, constructors));

    }

    private Optional<Constructor> valueUndefined(String element, String name, int position, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, position, ctx));
        return Optional.empty();
    }
}
