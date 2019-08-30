package secpriv.horst.translation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.Rule;
import secpriv.horst.evm.ContractLexer;

import java.util.*;

public class BigStepClauseWalker {
    private static final Logger LOGGER = LogManager.getLogger(BigStepClauseWalker.class);

    final private List<Rule> rules;
    final public List<Rule> result;
    private List<Rule> merge;
    private Set<List<Rule>> merges;
    final private Set<Rule> visited;
    final private Set<String> whitelist;
    boolean c_s_i = false, c_s_o = false, n_s_i = false, n_s_o = false;
    // Case distinction for algorithm:
    //      current_single_in (one clause that implies the current ->)
    //      c_s_o: current one ->
    //      c_s_i: current one <-
    //      n_s_o: next one ->
    //      n_s_i: next one <-
    public BigStepClauseWalker(List<Rule> rules){ ;
        this.rules = rules;
        this.result = new ArrayList<>();
        this.visited = new HashSet<>();
        this.merge = new ArrayList<>();
        this.merges = new HashSet<>();
        this.whitelist = new HashSet<>();
        whitelist.add("MState");
    }

    public Set<List<Rule>> getMerges(){
        return merges;
    }

    public void run() {
        Rule first = getFirstRule();
        if (first == null){
            LOGGER.error("First rule cannot be null!");
            return;
        }
        List<Rule> current = new ArrayList<>();
        current.add(first);
        process(current);
        //printMerges();
    }

    private void printMerges(){
        for (List<Rule> m: merges){
            if (m.size() > 1){
                for (Rule r: m){
                    LOGGER.debug(r.name + "+");
                }
                LOGGER.debug("");
            }
        }
    }

    private void merge(Rule current){
        // Merge start case: JUMP_DEST, beginning of the program
        if (c_s_o && !c_s_i && n_s_o && n_s_i){
            merge.add(current);
            List<Rule> oldMerge = new ArrayList<>(merge);
            merges.add(oldMerge);
            merge = new ArrayList<>();
            return;
        }
        // Merge continue case
        if (c_s_o && c_s_i && n_s_o && n_s_i){
            merge.add(current);
            return;
        }
        // Merge stop case
        if (c_s_o && c_s_i && !n_s_o && n_s_i){
            merge.add(current);
            List<Rule> oldMerge = new ArrayList<>(merge);
            merges.add(oldMerge);
            merge = new ArrayList<>();
            return;
        }
        if (c_s_o && c_s_i && n_s_o && !n_s_i){
            merge.add(current);
            List<Rule> oldMerge = new ArrayList<>(merge);
            merges.add(oldMerge);
            merge = new ArrayList<>();
            return;
        }
        // Merge: JUMPI
        if (!c_s_o){
        }
    }

    private void process(List<Rule> currents){
        // No successors: stop
        if (currents.size() == 0){
            List<Rule> oldMerge = new ArrayList<>(merge);
            merges.add(oldMerge);
            merge = new ArrayList<>();
            return;
        }
        // Simple cycle breaker
        for (Rule rule: currents){
            if (visited.contains(rule)){
                return;
            }
        }

        /*for (Rule rule: currents){
            LOGGER.debug(rule.name);
        }*/
        c_s_i = false; c_s_o = false; n_s_i = false; n_s_o = false;
        if (currents.size() == 1){
            // Singleton processing
            visited.addAll(currents); // we split on multiple successors, and revisit the rules again

            Rule current = currents.get(0);

            // Case when there are multiple clauses inside one rule
            int clausesToMerge = 0;
            for (Clause cl: current.clauses){
                if (nameIsOk(cl.conclusion.predicate.name)) {
                    ++clausesToMerge;
                }
            }
            if (clausesToMerge > 1) {
                List<Rule> oldMerge = new ArrayList<>(merge);
                merges.add(oldMerge);
                merge = new ArrayList<>();
                List<Rule> next = getNext(current);
                process(next);
                return;
            }

            List<Rule> next = getNext(current);
            List<Rule> prev = getPrev(current);
            getCase(prev, next);
            // Corner case: first clause
            if (prev.size() == 0){
            }
            else {
                merge(current);
            }
            //LOGGER.debug(Boolean.toString(c_s_o) + "/" + Boolean.toString(c_s_i) + "/" + Boolean.toString(n_s_o) + "/" + Boolean.toString(n_s_i));
            process(next);
        }
        else{
            // Multiple successor clauses processing: danger zone ahead
            for (Rule current: currents){
                List<Rule> next = getNext(current);
                List<Rule> prev = getPrev(current);
                getCase(prev, next);
                List<Rule> oldMerge = new ArrayList<>(merge);
                merges.add(oldMerge);
                merge = new ArrayList<>();
                merge(current);
                //LOGGER.debug(Boolean.toString(c_s_o) + "/" + Boolean.toString(c_s_i) + "/" + Boolean.toString(n_s_o) + "/" + Boolean.toString(n_s_i));
                process(next);
            }
        }
    }

    private void getCase(List<Rule> prev, List<Rule> next){
        c_s_o = false;
        n_s_i = false;
        n_s_o = false;
        n_s_i = false;
        if (next.size() == 1){
            c_s_o = true;
        }
        if (prev.size() < 2){
            c_s_i = true;
        }
        if (c_s_o) {
            List<Rule> next_next = getNext(next.get(0));
            if (next_next.size() < 2) {
                n_s_o = true;
            }
            List<Rule> next_prev = getPrev(next.get(0));
            if (next_prev.size() < 2) {
                n_s_i = true;
            }
        }
        // Corner case: last clause
        if (next.size() == 0){
                c_s_o = true;
                n_s_i = true;
                n_s_o = true;
                n_s_i = true;
        }
    }

    private boolean nameIsOk(String name){
        for (String wl: whitelist){
            if (name.contains(wl)){
                return true;
            }
        }
        return false;
    }

    private List<String> getPredicateNamesPremise(Rule rule){
        List<String> predicateNames = new ArrayList<>();
        for (Clause clause: rule.clauses) {
            for (Proposition prop : clause.premises) {
                if (prop instanceof Proposition.PredicateProposition) {
                    if (nameIsOk(((Proposition.PredicateProposition) prop).predicate.name)) {
                        predicateNames.add(((Proposition.PredicateProposition) prop).predicate.name);
                    }
                }
            }
        }
        return predicateNames;
    }

    private List<String> getPredicateNamesCause(Rule rule) {
        List<String> predicateNames = new ArrayList<>();
        for (Clause clause: rule.clauses) {
            if (nameIsOk(clause.conclusion.predicate.name)) {
                predicateNames.add(clause.conclusion.predicate.name);
            }
        }
        return predicateNames;
    }
    private List<Rule> getPrev(Rule current){
        List<Rule> prev = new ArrayList<>();
        List<String> currentPredicateNameCause = getPredicateNamesPremise(current);
        for (Rule rule: rules){
            if (rule.name.contains("opCall_")) continue; // we do not return CALL operation as a previous state for the first opcode
            List<String> nextPredicateNamesPremise = getPredicateNamesCause(rule);
            nextPredicateNamesPremise.retainAll(currentPredicateNameCause);
            if (nextPredicateNamesPremise.size() > 0){
                prev.add(rule);
            }
        }
        return prev;
    }
    private List<Rule> getNext(Rule current){
        List<Rule> next = new ArrayList<>();
        List<String> currentPredicateNameCause = getPredicateNamesCause(current);
        for (Rule rule: rules){
            List<String> nextPredicateNamesPremise = getPredicateNamesPremise(rule);
            nextPredicateNamesPremise.retainAll(currentPredicateNameCause);
            if (nextPredicateNamesPremise.size() > 0){
                next.add(rule);
            }
        }
        return next;
    }

    private Rule getFirstRule(){
        for (Rule rule: rules){
            boolean isFirst = true;
            for (Clause clause: rule.clauses){
                for (Proposition prop: clause.premises){
                    // Simple heuristic to find the first rule: no predicates in the premises
                    if (prop instanceof Proposition.PredicateProposition){
                        isFirst = false;
                        break;
                    }
                }
                if (!isFirst){
                    break;
                }
            }
            if (isFirst){
                return rule;
            }
        }
        return null;
    }
}
