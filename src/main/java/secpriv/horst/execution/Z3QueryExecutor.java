package secpriv.horst.execution;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Fixedpoint;
import com.microsoft.z3.Status;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Rule;
import secpriv.horst.translation.TranslateToZ3VisitorState;
import secpriv.horst.translation.visitors.TranslateToZ3RuleVisitor;
import secpriv.horst.visitors.RuleTypeOracle;
import secpriv.horst.visitors.VisitorState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Z3QueryExecutor {
    public List<ExecutionResult> executeQueries(List<Rule> rules, RuleTypeOracle oracle) {
        List<ExecutionResult> results = new ArrayList<>();

        TranslateToZ3VisitorState z3TranslationState = TranslateToZ3VisitorState.withGeneralIntegers();
        List<BoolExpr> rulesForZ3 = new ArrayList<>();

        for (Rule rule : rules) {
            TranslateToZ3RuleVisitor translateToZ3RuleVisitor = new TranslateToZ3RuleVisitor(z3TranslationState);
            rulesForZ3.addAll(rule.accept(translateToZ3RuleVisitor));
        }

        for (Rule query : rules.stream().filter(oracle::isQueryOrTest).collect(Collectors.toList())) {
            Fixedpoint fixedpoint = z3TranslationState.context.mkFixedpoint();
            for (BoolExpr b : rulesForZ3) {
                fixedpoint.addRule(b, null);
            }
            z3TranslationState.registerRelations(fixedpoint, false);

            String queryId = query.name;
            long start = System.currentTimeMillis();

            Status result = null;
            Optional<String> info = Optional.empty();

            try {
                Predicate predicate = new Predicate(queryId, Collections.emptyList(), Collections.emptyList());
                BoolExpr z3query = (BoolExpr) z3TranslationState.getZ3PredicateDeclaration(predicate).apply();
                result = fixedpoint.query(z3query);
            } catch (Exception e) {
                result = Status.UNKNOWN;
                info = Optional.of(e.getMessage());
            } finally {
                long duration = System.currentTimeMillis() - start;
                result = result == null ? Status.UNKNOWN : result;
                if (oracle.isTest(query)) {
                    boolean success = result != Status.UNKNOWN && oracle.isExpectedTestResult(query, z3ToHorstResult(result));
                    results.add(new ExecutionResult.TestResult(queryId, result, duration, success, info));
                } else {
                    results.add(new ExecutionResult.QueryResult(queryId, result, duration, info));
                }
            }
        }

        return results;
    }

    private static VisitorState.TestResult z3ToHorstResult(Status result) {
        switch (result) {
            case SATISFIABLE:
                return VisitorState.TestResult.SAT;
            case UNSATISFIABLE:
                return VisitorState.TestResult.UNSAT;
        }
        throw new IllegalArgumentException("Provided " + result + " but only " + Status.SATISFIABLE + " and " + Status.UNSATISFIABLE + " are valid Arguments!");
    }
}
