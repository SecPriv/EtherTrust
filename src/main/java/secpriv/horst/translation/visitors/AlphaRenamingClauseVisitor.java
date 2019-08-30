package secpriv.horst.translation.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;
import secpriv.horst.types.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlphaRenamingClauseVisitor implements Clause.Visitor<Clause> {
    private int renamedClauseCount = 0;

    @Override
    public Clause visit(Clause clause) {
        Map<String, String> renamedFreeVars = renameFreeVars(clause.freeVars);
        RenameFreeVariablesPropositionVisitor propositionVisitor = new RenameFreeVariablesPropositionVisitor(renamedFreeVars);

        ++renamedClauseCount;

        Map<String, Type> renamedFreeVarMap = new HashMap<>();

        for (Map.Entry<String, Type> entry : clause.freeVars.entrySet()) {
            renamedFreeVarMap.put(renamedFreeVars.get(entry.getKey()), entry.getValue());
        }

        List<Proposition> visitedPremises = clause.premises.stream().map(p -> p.accept(propositionVisitor)).collect(Collectors.toList());
        Proposition.PredicateProposition visitedConclusion = (Proposition.PredicateProposition) clause.conclusion.accept(propositionVisitor);

        return new Clause(visitedPremises, visitedConclusion, renamedFreeVarMap);

    }

    private Map<String, String> renameFreeVars(Map<String, Type> freeVars) {
        HashMap<String, String> ret = new HashMap<>();

        for (String key : freeVars.keySet()) {
            ret.put(key, renameVar(key));
        }

        return ret;
    }

    private String renameVar(String key) {
        return key + "?r" + renamedClauseCount;
    }
}
