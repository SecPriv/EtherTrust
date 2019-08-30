package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.OptionalInfo;
import secpriv.horst.types.Type;

import java.util.*;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfNonEmptyListsToNonEmptyList;
import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfListWithErrorReporting;

public class ClauseVisitor extends ASBaseVisitor<Optional<Clause>> {
    private final VisitorState state;

    public ClauseVisitor() {
        this(new VisitorState());
    }

    public ClauseVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<Clause> visitClauseDef(ASParser.ClauseDefContext ctx) {
        Map<String, Type> freeVars = new HashMap<>();

        if (ctx.freeVars() != null) {

            OptionalInfo<Type> optionalInfo = listOfOptionalToOptionalOfListWithErrorReporting(ctx.freeVars().type().stream().map(RuleContext::getText).map(state::getType).collect(Collectors.toList()));
            if (!optionalInfo.isSuccess()) {
                return valueUndefined("free vars", ctx.freeVars().getText(), optionalInfo.getPosition(), ctx);
            }

            List<String> freeVarNames = ctx.freeVars().freeVar().stream().map(v -> v.VAR_ID().getText()).collect(Collectors.toList());

            Iterator<String> freeVarNameIterator = freeVarNames.iterator();

            for (Type type : optionalInfo.getList()) {
                freeVars.put(freeVarNameIterator.next(), type);
            }

            if (!checkIfSizeMatches(freeVars.keySet().size(), optionalInfo.getList().size(), "free variables", ctx)){
                return Optional.empty();
            }
        }

        VisitorState childState = new VisitorState(state);
        for (String k : freeVars.keySet()) {
            childState.defineFreeVar(k, freeVars.get(k));
        }

        PropositionVisitor propositionVisitor = new PropositionVisitor(childState);

        List<List<Proposition>> listOfLists = ctx.clause().prems().prop().stream().map(propositionVisitor::visit).collect(Collectors.toList());
        List<Proposition> premises = listOfNonEmptyListsToNonEmptyList(listOfLists);

        if (premises.isEmpty()) {
            return invalidPremises(ctx);
        }

        List<Proposition> optConclusion = propositionVisitor.visit(ctx.clause().conc());

        if (!checkIfSizeMatches(1, optConclusion.size(),"arguments in conclusion predicate",ctx)){
            return Optional.empty();
        }

        if (!(optConclusion.get(0) instanceof Proposition.PredicateProposition)) {
            // TODO: error reporting - report as type mismatch error when the method is refactored
            return Optional.empty();
        }

        Proposition.PredicateProposition conclusion = (Proposition.PredicateProposition) optConclusion.get(0);

        return Optional.of(new Clause(premises, conclusion, freeVars));
    }


    private Optional<Clause> valueUndefined(String element, String name, int position, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, position, ctx));
        return Optional.empty();
    }

    private boolean checkIfSizeMatches(int expectedSize, int actualSize, String location, ParserRuleContext ctx) {
        if (actualSize != expectedSize) {
            state.errorHandler.handleError(ErrorHelper.generateSizeDoesntMatchError(expectedSize, actualSize, location, ctx));
            return false;
        }
        return true;
    }

    private Optional<Clause> invalidPremises(ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateInvalidPremisesError(ctx));
        return Optional.empty();
    }

}
