package secpriv.horst.translation;

import secpriv.horst.data.*;
import secpriv.horst.translation.visitors.ClauseMerger;
import secpriv.horst.translation.visitors.FilterClauseRuleVisitor;
import secpriv.horst.visitors.RuleTypeOracle;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MediumStepTransformer {
    static class RetrievePredicatesPropositionVisitor implements Proposition.Visitor<Optional<Predicate>> {
        @Override
        public Optional<Predicate> visit(Proposition.PredicateProposition proposition) {
            return Optional.of(proposition.predicate);
        }

        @Override
        public Optional<Predicate> visit(Proposition.ExpressionProposition proposition) {
            return Optional.empty();
        }
    }

    private static class RetrievePredicatesClauseVisitor implements Clause.Visitor<List<Predicate>> {
        @Override
        public List<Predicate> visit(Clause clause) {
            RetrievePredicatesPropositionVisitor propositionVisitor = new RetrievePredicatesPropositionVisitor();
            return Stream.concat(clause.premises.stream().map(p -> p.accept(propositionVisitor)).filter(Optional::isPresent).map(Optional::get), Stream.of(clause.conclusion.predicate)).collect(Collectors.toList());
        }
    }

    static class MapPredicateToClauseClauseVisitor implements Clause.Visitor<Void> {
        private final Map<Predicate, List<Clause>> occurrencesAsPremise;
        private final Map<Predicate, List<Clause>> occurrencesAsConclusion;

        MapPredicateToClauseClauseVisitor(Map<Predicate, List<Clause>> occurrencesAsPremise, Map<Predicate, List<Clause>> occurrencesAsConclusion) {
            this.occurrencesAsPremise = occurrencesAsPremise;
            this.occurrencesAsConclusion = occurrencesAsConclusion;
        }

        @Override
        public Void visit(Clause clause) {
            RetrievePredicatesPropositionVisitor propositionVisitor = new RetrievePredicatesPropositionVisitor();
            clause.premises.stream().map(p -> p.accept(propositionVisitor)).filter(Optional::isPresent).map(Optional::get).forEach(p -> occurrencesAsPremise.compute(p, (ignored, list) -> addIfPresent(clause, list)));
            occurrencesAsConclusion.compute(clause.conclusion.predicate, (ignored, list) -> addIfPresent(clause, list));
            return null;
        }

        private List<Clause> addIfPresent(Clause clause, List<Clause> list) {
            if (list == null) {
                ArrayList<Clause> newList = new ArrayList<>();
                newList.add(clause);
                return newList;
            } else {
                list.add(clause);
                return list;
            }
        }
    }


    public static List<Rule> foldToMediumSteps(List<Rule> allRules, RuleTypeOracle oracle) {
        allRules = deleteAllUnreachableRules(allRules, oracle);
        allRules = foldLinearClausesRules(allRules);
        return allRules;
    }

    private static List<Rule> deleteAllUnreachableRules(List<Rule> allRules, RuleTypeOracle oracle) {
        Set<Predicate> queriedPredicates = allRules.stream().filter(oracle::isQueryOrTest).flatMap(r -> r.clauses.stream()).map(c -> c.conclusion.predicate).collect(Collectors.toSet());

        while (true) {
            List<Rule> resultRules = deleteLeaveRules(allRules, queriedPredicates);
            if (allRules == resultRules) {
                break;
            }
            allRules = resultRules;
        }
        return allRules;
    }

    private static List<Rule> deleteLeaveRules(List<Rule> allRules, Set<Predicate> queriedPredicates) {
        Map<Predicate, List<Clause>> occurrencesAsPremise = new HashMap<>();
        Map<Predicate, List<Clause>> occurrencesAsConclusion = new HashMap<>();

        List<Clause> allClauses = allRules.stream().flatMap(r -> r.clauses.stream()).collect(Collectors.toList());
        MapPredicateToClauseClauseVisitor mapPredicateToClauseClauseVisitor = new MapPredicateToClauseClauseVisitor(occurrencesAsPremise, occurrencesAsConclusion);
        allClauses.forEach(c -> c.accept(mapPredicateToClauseClauseVisitor));

        RetrievePredicatesClauseVisitor retrievePredicatesClauseVisitor = new RetrievePredicatesClauseVisitor();
        Set<Predicate> allPredicates = allRules.stream().flatMap(r -> r.clauses.stream()).flatMap(c -> c.accept(retrievePredicatesClauseVisitor).stream()).collect(Collectors.toSet());

        Set<Clause> deletionCandidates = new HashSet<>();

        for (Predicate p : allPredicates) {
            List<Clause> occurrenceInConclusion = occurrencesAsConclusion.getOrDefault(p, Collections.emptyList());
            List<Clause> occurrenceInPremise = occurrencesAsPremise.getOrDefault(p, Collections.emptyList());

            if (occurrenceInPremise.isEmpty() && !queriedPredicates.contains(p)) {
                deletionCandidates.addAll(occurrenceInConclusion);
            }
        }

        if (deletionCandidates.isEmpty()) {
            return allRules;
        }

        FilterClauseRuleVisitor filterClauseRuleVisitor = new FilterClauseRuleVisitor(deletionCandidates);
        return allRules.stream().map(r -> r.accept(filterClauseRuleVisitor)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    private static List<Rule> foldLinearClausesRules(List<Rule> allRules) {
        Map<Predicate, List<Clause>> occurrencesAsPremise = new HashMap<>();
        Map<Predicate, List<Clause>> occurrencesAsConclusion = new HashMap<>();

        List<Clause> allClauses = allRules.stream().flatMap(r -> r.clauses.stream()).collect(Collectors.toList());
        MapPredicateToClauseClauseVisitor mapPredicateToClauseClauseVisitor = new MapPredicateToClauseClauseVisitor(occurrencesAsPremise, occurrencesAsConclusion);
        allClauses.forEach(c -> c.accept(mapPredicateToClauseClauseVisitor));

        RetrievePredicatesClauseVisitor retrievePredicatesClauseVisitor = new RetrievePredicatesClauseVisitor();
        Set<Predicate> allPredicates = allRules.stream().flatMap(r -> r.clauses.stream()).flatMap(c -> c.accept(retrievePredicatesClauseVisitor).stream()).collect(Collectors.toSet());

        Set<Clause> foldedClauses = new HashSet<>();
        Set<Predicate> deletedPredicates = new HashSet<>();

        for (Predicate p : allPredicates.stream().sorted(Comparator.comparing(p -> ((Predicate) p).name.length()).thenComparing(o -> ((Predicate) o).name)).collect(Collectors.toList())) {
            List<Clause> occurrenceInConclusion = occurrencesAsConclusion.getOrDefault(p, Collections.emptyList());
            List<Clause> occurrenceInPremise = occurrencesAsPremise.getOrDefault(p, Collections.emptyList());

            if (occurrenceInPremise.size() == 1 && occurrenceInConclusion.size() == 1) {
                foldedClauses.addAll(occurrenceInConclusion);
                foldedClauses.addAll(occurrenceInPremise);
                deletedPredicates.add(p);
            }
        }

        List<Set<Predicate>> groupedDeletedPredicates = groupDeletedPredicates(deletedPredicates, occurrencesAsPremise);

        FilterClauseRuleVisitor deleteFoldedClausesVisitor = new FilterClauseRuleVisitor(foldedClauses);
        List<Rule> foldedRules = allRules.stream().map(r -> r.accept(deleteFoldedClausesVisitor)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        int i = 0;
        for (Set<Predicate> deletionGroup : groupedDeletedPredicates) {
            Clause mergedClause = mergeDeletionGroup(occurrencesAsPremise, occurrencesAsConclusion, deletionGroup);
            foldedRules.add(new Rule("merge" + (++i), CompoundSelectorFunctionInvocation.UnitInvocation, Collections.singletonList(mergedClause)));
        }

        return foldedRules;
    }

    private static Clause mergeDeletionGroup(Map<Predicate, List<Clause>> occurrencesAsPremise, Map<Predicate, List<Clause>> occurrencesAsConclusion, Set<Predicate> deletionGroup) {
        Set<Clause> unsortedImplicationChain = new HashSet<>();

        for (Predicate p : deletionGroup) {
            unsortedImplicationChain.addAll(occurrencesAsConclusion.get(p));
            unsortedImplicationChain.addAll(occurrencesAsPremise.get(p));
        }

        Clause start = getStartOfImplicationChain(unsortedImplicationChain, deletionGroup);
        Clause end = getEndOfImplicationChain(unsortedImplicationChain, deletionGroup);

        List<Clause> sortedImplicationChain = new ArrayList<>();

        while (start != end) {
            sortedImplicationChain.add(start);
            start = occurrencesAsPremise.get(start.conclusion.predicate).get(0);
        }
        sortedImplicationChain.add(end);

        ClauseMerger clauseMerger = new ClauseMerger();
        return clauseMerger.merge(sortedImplicationChain);
    }

    private static Clause getStartOfImplicationChain(Set<Clause> implicationChain, Set<Predicate> internalPredicates) {
        for (Clause c : implicationChain) {
            if (internalPredicates.stream().noneMatch(p -> implies(p, c))) {
                return c;
            }
        }
        throw new IllegalArgumentException("There must be one clause in whose premises do not contain any of internalPredicates!");
    }

    private static boolean implies(Predicate premise, Clause clause) {
        class ImplicationCheckPropositionVisitor implements Proposition.Visitor<Boolean> {
            @Override
            public Boolean visit(Proposition.PredicateProposition proposition) {
                return proposition.predicate.equals(premise);
            }

            @Override
            public Boolean visit(Proposition.ExpressionProposition proposition) {
                return false;
            }
        }

        ImplicationCheckPropositionVisitor propositionVisitor = new ImplicationCheckPropositionVisitor();
        return clause.premises.stream().anyMatch(p -> p.accept(propositionVisitor));
    }

    private static Clause getEndOfImplicationChain(Set<Clause> implicationChain, Set<Predicate> internalPredicates) {
        for (Clause c : implicationChain) {
            if (!internalPredicates.contains(c.conclusion.predicate)) {
                return c;
            }
        }
        throw new IllegalArgumentException("There must be one clause in whose conclusion is not in internalPredicates!");
    }

    private static List<Set<Predicate>> groupDeletedPredicates(Set<Predicate> deletedPredicates, Map<Predicate, List<Clause>> occurrencesAsPremise) {
        Map<Predicate, Integer> representatives = new HashMap<>();

        int i = 0;
        for (Predicate p : deletedPredicates) {
            representatives.put(p, i++);
        }

        // The representative r of a predicate p is r in deletedPredicates, such that r is transitively implied by
        // p and r does not imply any other p' in deletedPredicates. This is basically union-find.
        for (Predicate premise : deletedPredicates) {
            Predicate conclusion = occurrencesAsPremise.get(premise).get(0).conclusion.predicate;

            while (deletedPredicates.contains(conclusion)) {
                Predicate newConclusion = occurrencesAsPremise.get(conclusion).get(0).conclusion.predicate;

                if (deletedPredicates.contains(newConclusion)) {
                    conclusion = newConclusion;
                } else {
                    representatives.put(premise, representatives.get(conclusion));
                    break;
                }
            }
        }

        return deletedPredicates.stream().collect(Collectors.groupingBy(representatives::get)).values().stream().map(HashSet::new).collect(Collectors.toList());
    }
}
