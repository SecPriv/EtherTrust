package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.data.*;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;

import secpriv.horst.internals.error.handling.ErrorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfList;

public class RuleVisitor extends ASBaseVisitor<Optional<Rule>> {
    private final VisitorState state;

    public RuleVisitor() {
        this(new VisitorState());
    }

    public RuleVisitor(VisitorState state) {
        this.state = state;
    }

    public Optional<Rule> visitRuleBodyWithRuleName(String ruleName, ASParser.RuleBodyContext ctx) {
        CompoundSelectorFunctionInvocation selectorFunctionInvocation = CompoundSelectorFunctionInvocation.UnitInvocation;
        VisitorState childState = new VisitorState(state);
        if (ctx.selectorExp() != null) {
            Optional<CompoundSelectorFunctionInvocation> optSelectorExp = ctx.selectorExp().accept(new CompoundSelectorFunctionInvocationVisitor((childState)));

            if (!optSelectorExp.isPresent()) {
                return Optional.empty();
            }

            selectorFunctionInvocation = optSelectorExp.get();
        }

        if (ctx.macros() != null) {
            MacroDefinitionVisitor macroDefinitionVisitor = new MacroDefinitionVisitor(childState);

            Optional<List<VisitorState.MacroDefinition>> optMacroDefinitions = listOfOptionalToOptionalOfList(ctx.macros().localMacroDef().stream().map(macroDefinitionVisitor::visit).collect(Collectors.toList()));

            if (!optMacroDefinitions.isPresent()) {
                return Optional.empty();
            }

            for (VisitorState.MacroDefinition macro : optMacroDefinitions.get()) {
                if (!childState.defineMacro(macro)) {
                    return variableAlreadyBound("Macro definition", macro.name, ctx);
                }
            }
        }

        List<Clause> clauses = new ArrayList<>();

        for (ASParser.ClauseDefContext p : ctx.clauses().clauseDef()) {
            ClauseVisitor clauseVisitor = new ClauseVisitor(childState);
            Optional<Clause> optClause = clauseVisitor.visit(p);

            if (!optClause.isPresent()) {
                return Optional.empty();
            }
            clauses.add(optClause.get());
        }
        return Optional.of(new Rule(ruleName, selectorFunctionInvocation, clauses));
    }

    @Override
    public Optional<Rule> visitRuleDefinition(ASParser.RuleDefinitionContext ctx) {
        String ruleName = ctx.ruleID().getText();
        return visitRuleBodyWithRuleName(ruleName, ctx.ruleBody());
    }

    public Optional<Rule> visitInitialization(String ruleName, ASParser.InitializationContext ctx) {
        Optional<Rule> optRule = visitRuleBodyWithRuleName(ruleName, ctx.ruleBody());

        if(!optRule.isPresent()) {
            return Optional.empty();
        }

        if(optRule.get().clauses.stream().anyMatch(c -> c.premises.stream().anyMatch(p -> p instanceof Proposition.PredicateProposition))) {
            state.errorHandler.handleError(ErrorHelper.generateInitRuleContainsPredicatePropositionError(ctx));
            return Optional.empty();
        }

        return optRule;
    }

    private Optional<Rule> variableAlreadyBound(String element, String name, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementAlreadyBoundError(element, name, ctx));
        return Optional.empty();
    }
}
