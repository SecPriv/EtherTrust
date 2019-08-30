package secpriv.horst.translation.visitors;

import secpriv.horst.data.*;
import secpriv.horst.types.Type;

import java.util.*;
import java.util.stream.Collectors;

public class ClauseMerger {
    private class FilterConclusionVisitor implements Proposition.Visitor<Boolean> {
        final String filteredName;

        private FilterConclusionVisitor(String filteredName) {
            this.filteredName = filteredName;
        }

        @Override
        public Boolean visit(Proposition.PredicateProposition proposition) {
            return !proposition.predicate.name.equals(filteredName);
        }

        @Override
        public Boolean visit(Proposition.ExpressionProposition proposition) {
            return true;
        }
    }

    private String createName(List<Rule> rules){
        String name = "";
        for (Rule r: rules){
            name += r.name;
        }
        return name;
    }

    private List<Clause> project(List<Rule> rules, String name){
        List<Clause> stackClauses = new ArrayList<>();
        for (Rule rule: rules){
            for (Clause clause: rule.clauses){
                if (clause.conclusion.predicate.name.contains(name)){
                    stackClauses.add(clause);
                }
            }
        }
        return stackClauses;
    }

    public Rule projectAndMerge(List<Rule> rules){
        String name = createName(rules);
        List<Clause> result = new ArrayList<>();
        result.add(merge(project(rules, "MState")));
        //result.add(merge(project(rules, "Mem")));
        // TODO: uncomment when rules for storage are there
        //result.add(merge(project(rules, "Stor")));
        // TODO: deal with folded exceptions
        //result.add(merge(project(rules, "Exc")));
        return new Rule(name, CompoundSelectorFunctionInvocation.UnitInvocation, result);
    }

    public Clause merge(List<Clause> clauses){
        Clause currentClause = clauses.get(0);
        List<Proposition> currentPremises = new ArrayList<>(currentClause.premises);
        Map<String, Type> currentFreeVars = new HashMap<>(currentClause.freeVars);
        Map<String, String> renames = new HashMap<>();

        for(int i = 1; i < clauses.size(); ++i) {
            Clause clauseToMerge = clauses.get(i);

            final Clause currentClauseForLambda = currentClause;

            Proposition.PredicateProposition filteredPredicate =
                    (Proposition.PredicateProposition) clauseToMerge.premises.stream().filter(p ->
                    !p.accept(new FilterConclusionVisitor(currentClauseForLambda.conclusion.predicate.name))).findFirst().get();
            // creating rename map
            for(int j = 0; j < currentClause.conclusion.arguments.size(); ++j) {
                String name1 = ((Expression.FreeVarExpression) filteredPredicate.arguments.get(j)).name;
                String name2 = ((Expression.FreeVarExpression) currentClause.conclusion.arguments.get(j)).name;
                renames.put(name1, name2);
            }
            // renaming variables in the premise expressions
            clauseToMerge.premises.stream().filter(p ->
                    p.accept(new FilterConclusionVisitor(currentClauseForLambda.conclusion.predicate.name))).forEach(currentPremises::add);

            currentPremises =
                    currentPremises.stream().map(p ->
                            p.accept(new RenameFreeVariablesPropositionVisitor(renames))).collect(Collectors.toList());
            //renaming variables in conclusion
            Proposition.PredicateProposition currentConclusion =
                    (Proposition.PredicateProposition) clauseToMerge.conclusion.accept(new RenameFreeVariablesPropositionVisitor(renames));
            // renaming free variables
            Map<String, Type> clauseToMergeFreeVars = new HashMap<>(clauseToMerge.freeVars);
            Map<String, Type> filteredToMergeFreeVars = new HashMap<>();
            for (Map.Entry<String, Type> freeVar: clauseToMergeFreeVars.entrySet()){
                if (!renames.containsKey(freeVar.getKey())){
                    filteredToMergeFreeVars.put(freeVar.getKey(), freeVar.getValue());
                }
            }
            currentFreeVars.putAll(filteredToMergeFreeVars);

            currentClause = new Clause(currentPremises, currentConclusion, currentFreeVars);
        }
        return currentClause;
    }
}
