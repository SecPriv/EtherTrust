package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.OptionalInfo;
import secpriv.horst.types.Type;

import java.util.*;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfListWithErrorReporting;

public class MacroDefinitionVisitor extends ASBaseVisitor<Optional<VisitorState.MacroDefinition>> {
    private VisitorState state;

    public MacroDefinitionVisitor() {
        this(new VisitorState());
    }

    public MacroDefinitionVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<VisitorState.MacroDefinition> visitGlobalMacroDefinition(ASParser.GlobalMacroDefinitionContext ctx) {
        return visit(ctx.macroDef());
    }

    @Override
    public Optional<VisitorState.MacroDefinition> visitLocalMacroDef(ASParser.LocalMacroDefContext ctx) {
        return visit(ctx.macroDef());
    }

    @Override
    public Optional<VisitorState.MacroDefinition> visitMacroDef(ASParser.MacroDefContext ctx) {
        String name = ctx.macroSignature().macroID().getText();

        if(state.getMacro(name, Integer.MAX_VALUE).isPresent()) {
           return elementAlreadyBound("Macro definition", name, ctx);
        }

        List<String> arguments = new ArrayList<>();
        List<Type> argumentTypes = new ArrayList<>();

        if(ctx.macroSignature().macroDefArgs() != null) {
            if (!populateArgumentLists(arguments, argumentTypes, ctx.macroSignature())) {
                return Optional.empty();
            }
        }

        if (!checkIfSizeMatches(new HashSet<>(arguments).size(), arguments.size(), "macro argument list", ctx)){
            return Optional.empty();
        }

        Map<String, Type> freeVars = new HashMap<>();
        if(ctx.macroFreeVars() != null) {

            OptionalInfo<Type> optionalInfo = listOfOptionalToOptionalOfListWithErrorReporting(ctx.macroFreeVars().freeVars().type().stream().map(RuleContext::getText).map(state::getType).collect(Collectors.toList()));

            if (!optionalInfo.isSuccess()) {
                return typeNotDefinedInMacro("Free vars", name, optionalInfo.getPosition(), ctx);
            }

            List<String> freeVarIds = ctx.macroFreeVars().freeVars().freeVar().stream().map(RuleContext::getText).collect(Collectors.toList());

            for(int i = 0; i < freeVarIds.size(); ++i) {
                if(freeVars.containsKey(freeVarIds.get(i))) {
                    return elementAlreadyBound("Free var", freeVarIds.get(i), ctx);
                }
                freeVars.put(freeVarIds.get(i), optionalInfo.getList().get(i));
            }
        }

        //TODO check if macro contains unbound free variables

        return Optional.of(new VisitorState.MacroDefinition(name, arguments, argumentTypes, freeVars, ctx.macroBody()));
    }

    private boolean populateArgumentLists(List<String> arguments, List<Type> argumentTypes, ASParser.MacroSignatureContext ctx) {
        //TODO report error
        OptionalInfo<Type> optionalInfo = listOfOptionalToOptionalOfListWithErrorReporting(ctx.macroDefArgs().type().stream().map(s -> state.getType(s.getText())).collect(Collectors.toList()));
        if (!optionalInfo.isSuccess()) {
            return false;
        }

        ctx.macroDefArgs().MACRO_PAR_ID().forEach(i -> arguments.add(i.getText()));
        argumentTypes.addAll(optionalInfo.getList());
        return true;
    }

    private Optional<VisitorState.MacroDefinition> elementAlreadyBound(String element, String name, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementAlreadyBoundError(element, name, ctx));
        return Optional.empty();
    }

    private Optional<VisitorState.MacroDefinition> typeNotDefinedInMacro(String simpleName, String macroName, int position, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateElementNotDefinedInMacroError(simpleName, macroName, position, ctx));
        return Optional.empty();
    }

    private boolean checkIfSizeMatches(int expectedSize, int actualSize, String location, ParserRuleContext ctx) {
        if (actualSize != expectedSize) {
            state.errorHandler.handleError(ErrorHelper.generateSizeDoesntMatchError(expectedSize, actualSize, location, ctx));
            return false;
        }
        return true;
    }
}
