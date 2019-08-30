package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import secpriv.horst.data.*;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfNonEmptyListsToNonEmptyList;

public class ProgramVisitor extends ASBaseVisitor<Optional<VisitorState>> {
    private VisitorState state = new VisitorState();

    public ProgramVisitor() {
        this(new VisitorState());
    }

    public ProgramVisitor(VisitorState state) {
        this.state = state;
        this.state.defineType(Type.Integer);
        this.state.defineType(Type.Boolean);
    }

    @Override
    public Optional<VisitorState> visitAbstractDomainDeclaration(ASParser.AbstractDomainDeclarationContext ctx) {
        TypeVisitor typeVisitor = new TypeVisitor(new VisitorState(state));

        Optional<Type.CustomType> optType = typeVisitor.visitAbstractDomainDeclaration(ctx);

        if (!optType.isPresent()) {
            //TODO überlegen was tun
            return Optional.empty();
        }
        if (!state.defineType(optType.get())) {
            return variableAlreadyBound("Type", optType.get().name, ctx);
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitOperationDefinition(ASParser.OperationDefinitionContext ctx) {
        OperationVisitor operationVisitor = new OperationVisitor(new VisitorState(state));

        Optional<Operation> optOperation = operationVisitor.visitOperationDefinition(ctx);

        if (!optOperation.isPresent()) {
            //TODO überlegen was tun
            return Optional.empty();
        }
        if (!state.defineOperation(optOperation.get())) {
            return variableAlreadyBound("Operation", optOperation.get().name, ctx);
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitPredicateDeclaration(ASParser.PredicateDeclarationContext ctx) {
        PredicateDefinitionVisitor predicateVisitor = new PredicateDefinitionVisitor(new VisitorState(state));

        Optional<Predicate> optPredicate = predicateVisitor.visitPredicateDeclaration(ctx);

        if (!optPredicate.isPresent()) {
            //TODO überlegen was tun
            return Optional.empty();
        }

        if (!state.definePredicate(optPredicate.get())) {
            return variableAlreadyBound("Predicate", optPredicate.get().name, ctx);
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitSelectorFunctionDeclaration(ASParser.SelectorFunctionDeclarationContext ctx) {
        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(new VisitorState(state));

        Optional<SelectorFunction> optSelectorFunction = selectorFunctionDefinitionVisitor.visitSelectorFunctionDeclaration(ctx);

        if (!optSelectorFunction.isPresent()) {
            //TODO überlegen was tun
            return Optional.empty();
        }

        if (!state.defineSelectorFunction(optSelectorFunction.get())) {
            return variableAlreadyBound("Selector function", optSelectorFunction.get().name, ctx);
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitGlobalMacroDefinition(ASParser.GlobalMacroDefinitionContext ctx) {
        MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(new VisitorState(state));

        Optional<VisitorState.MacroDefinition> optMacroDefinition = macroDefinitionVisitor.visitGlobalMacroDefinition(ctx);

        if (!optMacroDefinition.isPresent()) {
            //TODO überlegen was tun
            return Optional.empty();
        }

        if (!state.defineMacro(optMacroDefinition.get())) {
            return variableAlreadyBound("Global macro", optMacroDefinition.get().name, ctx);
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitConstDefinition(ASParser.ConstDefinitionContext ctx) {
        ConstDefinitionVisitor constDefinitionVisitor = new ConstDefinitionVisitor(new VisitorState(state));

        Optional<Expression.ConstExpression> optConstExpression = constDefinitionVisitor.visitConstDefinition(ctx);

        if (!optConstExpression.isPresent()) {
            //TODO überlegen was tun
            return Optional.empty();
        }

        if (!state.defineConstant(optConstExpression.get())) {
            return variableAlreadyBound("Constant", optConstExpression.get().name, ctx);
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitRuleDefinition(ASParser.RuleDefinitionContext ctx) {
        RuleVisitor ruleVisitor = new RuleVisitor(new VisitorState(state));

        Optional<Rule> optRule = ruleVisitor.visitRuleDefinition(ctx);

        if (!optRule.isPresent()) {
            //TODO überlegen was tun
            return Optional.empty();
        }

        if (!state.defineRule(optRule.get())) {
            return variableAlreadyBound("Rule", optRule.get().name, ctx);
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitAbstractProgram(ASParser.AbstractProgramContext ctx) {
        for (ParseTree p : ctx.children) {
            Optional<VisitorState> retState = visit(p);
            if (retState != null) {
                if (!retState.isPresent()) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(state);
    }

    private Optional<VisitorState> variableAlreadyBound(String element, String className, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementAlreadyBoundError(element, className, ctx));
        return Optional.empty();
    }

    private Optional<Rule> queryBodyToRule(ASParser.QueryBodyContext ctx, String id) {
        Map<String, Type> freeVars = Collections.emptyMap();

        if (ctx.freeVars() != null) {
            Optional<Map<String, Type>> optFreeVars = ctx.freeVars().accept(new FreeVarsVisitor(state));
            if (!optFreeVars.isPresent()) {
                return Optional.empty();
            } else {
                freeVars = optFreeVars.get();
            }
        }

        CompoundSelectorFunctionInvocation selectorFunctionInvocation = CompoundSelectorFunctionInvocation.UnitInvocation;
        VisitorState childState = new VisitorState(state);

        if (ctx.selectorExp() != null) {
            Optional<CompoundSelectorFunctionInvocation> optSelectorExp = ctx.selectorExp().accept(new CompoundSelectorFunctionInvocationVisitor(childState));

            if (!optSelectorExp.isPresent()) {
                return Optional.empty();
            }
            selectorFunctionInvocation = optSelectorExp.get();
        }

        for (String k : freeVars.keySet()) {
            childState.defineFreeVar(k, freeVars.get(k));
        }

        PropositionVisitor propositionVisitor = new PropositionVisitor(childState);

        List<List<Proposition>> listOfLists = ctx.prems().prop().stream().map(propositionVisitor::visit).collect(Collectors.toList());
        List<Proposition> premises = listOfNonEmptyListsToNonEmptyList(listOfLists);

        if (premises.isEmpty()) {
            //TODO handle error?
            return Optional.empty();
        }

        List<Type> parameterTypes = selectorFunctionInvocation.parameters().stream().map(Expression.VariableReferenceExpression::getType).collect(Collectors.toList());
        Predicate queryPredicate = new Predicate(id, parameterTypes, Collections.emptyList());
        if (!state.definePredicate(queryPredicate)) {
            //TODO report error
            return Optional.empty();
        }

        Clause queryAsClause = new Clause(premises, new Proposition.PredicateProposition(queryPredicate, Collections.unmodifiableList(selectorFunctionInvocation.parameters()), Collections.emptyList()), freeVars);
        return Optional.of(new Rule(id, selectorFunctionInvocation, Collections.singletonList(queryAsClause)));
    }

    @Override
    public Optional<VisitorState> visitQuery(ASParser.QueryContext ctx) {
        String queryId = ctx.queryID().ID().getText();

        Optional<Rule> optQueryAsRule = queryBodyToRule(ctx.queryBody(), queryId);

        if (!optQueryAsRule.isPresent()) {
            return Optional.empty();
        }

        if (!state.defineQuery(optQueryAsRule.get())) {
            //TODO handle error
            return Optional.empty();
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitTest(ASParser.TestContext ctx) {
        String testId = ctx.testID().ID().getText();

        Optional<Rule> optQueryAsRule = queryBodyToRule(ctx.queryBody(), testId);

        if (!optQueryAsRule.isPresent()) {
            return Optional.empty();
        }

        if (ctx.testResult().getText().equals("SAT")) {
            if (!state.defineSatisfiableTest(optQueryAsRule.get())) {
                //TODO handle error
                return Optional.empty();
            }
        } else if (ctx.testResult().getText().equals("UNSAT")) {
            if (!state.defineUnsatisfiableTest(optQueryAsRule.get())) {
                //TODO handle error
                return Optional.empty();
            }
        } else {
            throw new RuntimeException("Unreachable Code");
        }

        return Optional.of(state);
    }

    @Override
    public Optional<VisitorState> visitInitialization(ASParser.InitializationContext ctx) {
        RuleVisitor ruleVisitor = new RuleVisitor(new VisitorState(state));

        int i = 0;
        while(state.getRules().containsKey("init" + i)) {
            ++i;
        }

        Optional<Rule> optRule = ruleVisitor.visitInitialization("init"+i, ctx);

        if (!optRule.isPresent()) {
            //TODO überlegen was tun
            return Optional.empty();
        }

        if (!state.defineRule(optRule.get())) {
            return variableAlreadyBound("Rule", optRule.get().name, ctx);
        }

        return Optional.of(state);
    }
}
