package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Proposition;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.ZipInfo;
import secpriv.horst.types.Type;

import java.util.*;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfNonEmptyListsToNonEmptyList;
import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfList;
import static secpriv.horst.tools.Zipper.zipPredicateWithErrorReporting;

//A valid returned value is a list of exactly one Proposition, or, in case of a macro instantiation,
//a list of many Propositions. An empty List indicates a failure.
public class PropositionVisitor extends ASBaseVisitor<List<Proposition>> {
    private VisitorState state;

    public PropositionVisitor() {
        this(new VisitorState());
    }

    public PropositionVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public List<Proposition> visitPredApp(ASParser.PredAppContext ctx) {
        String name = ctx.predID().getText();

        Optional<Predicate> optPredicate = state.getPredicate(name);
        if (!optPredicate.isPresent()) {
            return valueUndefined(Predicate.class.getSimpleName(), name, ctx);
        }
        Predicate predicate = optPredicate.get();

        ExpressionVisitor visitor = new ExpressionVisitor(state);

        Optional<List<Expression>> optParameters = Optional.of(Collections.emptyList());

        if (ctx.predParams() != null) {
            optParameters = listOfOptionalToOptionalOfList(ctx.predParams().predParam().stream().map(visitor::visit).collect(Collectors.toList()));
        }

        if (!optParameters.isPresent()) {
            return Collections.emptyList();
        }

        ConstnessExpressionVisitor constnessExpressionVisitor = new ConstnessExpressionVisitor();
        for (Expression parameter : optParameters.get()) {
            if (!parameter.accept(constnessExpressionVisitor)){
                return valueNotConst("Parameter", parameter, ctx);
            }
        }

        ZipInfo zipInfo = zipPredicateWithErrorReporting(predicate.parameterTypes, optParameters.get(), (t, p) -> t.equals(p.getType()));
        if(!zipInfo.isSuccess()) {
            return mismatchInPredicate("parameter", zipInfo, ctx);
        }

        Optional<List<Expression>> optArguments = listOfOptionalToOptionalOfList(ctx.predArgs().predArg().stream().map(visitor::visit).collect(Collectors.toList()));

        if (!optArguments.isPresent()) {
            return Collections.emptyList();
        }

        zipInfo = zipPredicateWithErrorReporting(predicate.argumentsTypes, optArguments.get(), (t, a) -> t.equals(a.getType()));
        if(!zipInfo.isSuccess()) {
            return mismatchInPredicate("argument", zipInfo, ctx);
        }

        return Collections.singletonList(new Proposition.PredicateProposition(predicate, optParameters.get(), optArguments.get()));
    }

    @Override
    public List<Proposition> visitProp(ASParser.PropContext ctx) {
        if (ctx.exp() != null) {
            ExpressionVisitor visitor = new ExpressionVisitor(state);

            Optional<Expression> ret = checkIfExpressionMatchesType(visitor.visit(ctx.exp()), Type.Boolean, ctx);

            if (!ret.isPresent()) {
                return Collections.emptyList();
            }

            return Collections.singletonList(new Proposition.ExpressionProposition(ret.get()));
        } else {
            return visitChildren(ctx);
        }
    }

    @Override
    public List<Proposition> visitMacroApp(ASParser.MacroAppContext ctx) {
        String macroName = ctx.macroID().getText();
        Optional<VisitorState.MacroDefinition> optMacro = state.getMacro(macroName, state.getMacroLevel());

        if (!optMacro.isPresent()) {
            return valueUndefined("Macro", macroName, ctx);
        }

        VisitorState.MacroDefinition macro = optMacro.get();

        Optional<List<Expression>> optMacroArgs = Optional.of(Collections.emptyList());

        if (ctx.macroArgs() != null) {
            ExpressionVisitor expressionVisitor = new ExpressionVisitor(state);
            optMacroArgs = listOfOptionalToOptionalOfList(ctx.macroArgs().macroArg().stream().map(ma -> expressionVisitor.visit(ma.exp())).collect(Collectors.toList()));
        }

        if (!optMacroArgs.isPresent()) {
            return Collections.emptyList();
        }

        List<Expression> macroArgs = optMacroArgs.get();

        ZipInfo zipInfo = zipPredicateWithErrorReporting(macro.argumentTypes, macroArgs, (a, ma) -> a.equals(ma.getType()));
        if(!zipInfo.isSuccess()) {
            return mismatchInPredicate("macro argument type", zipInfo, ctx);
        }

        VisitorState tmpState = state;
        state = new VisitorState(state);
        state.setMacroLevel(macro.macroLevel);

        Iterator<String> macroArgNameIterator = macro.arguments.iterator();
        for (Expression macroArg : macroArgs) {
            state.defineMacroVarBinding(macroArgNameIterator.next(), macroArg);
        }

        for (String k : macro.freeVars.keySet()) {
            state.overrideFreeVar(k, macro.name.replace('#', '&') + k, macro.freeVars.get(k));
        }

        List<List<Proposition>> listOfLists = macro.body.prems().prop().stream().map(this::visit).collect(Collectors.toList());
        List<Proposition> ret = listOfNonEmptyListsToNonEmptyList(listOfLists);
        state = tmpState;
        return ret;
    }

    private List<Proposition> valueUndefined(String element, String name, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, ctx));
        return Collections.emptyList();
    }

    private List<Proposition> valueNotConst(String element, Expression expression, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateElementNotConstError(element, expression.getType(), ctx));
        return Collections.emptyList();
    }

    private List<Proposition> mismatchInPredicate(String type, ZipInfo zipInfo, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateMismatchInListError(type, zipInfo, ctx));
        return Collections.emptyList();
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
