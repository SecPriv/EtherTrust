package secpriv.horst.visitors;

import org.antlr.v4.runtime.RuleContext;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.*;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfList;

public class FreeVarsVisitor extends ASBaseVisitor<Optional<Map<String, Type>>> {
    private VisitorState state;

    public FreeVarsVisitor(VisitorState state) {
        this.state = Objects.requireNonNull(state, "State may not be null");
    }

    @Override
    public Optional<Map<String, Type>> visitFreeVars(ASParser.FreeVarsContext ctx) {
        Optional<List<Type>> optTypes = listOfOptionalToOptionalOfList(ctx.type().stream().map(RuleContext::getText).map(state::getType).collect(Collectors.toList()));
        if (!optTypes.isPresent()) {
            //TODO report error
            return Optional.empty();
        }
        List<String> freeVarNames = ctx.freeVar().stream().map(v -> v.VAR_ID().getText()).collect(Collectors.toList());

        Iterator<String> freeVarNameIterator = freeVarNames.iterator();

        Map<String, Type> freeVars = new HashMap<>();
        for (Type type : optTypes.get()) {
            freeVars.put(freeVarNameIterator.next(), type);
        }

        if (freeVars.keySet().size() != optTypes.get().size()) {
            //TODO report error
            return Optional.empty();
        }

        return Optional.of(freeVars);
    }
}
