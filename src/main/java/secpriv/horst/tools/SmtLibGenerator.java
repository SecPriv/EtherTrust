package secpriv.horst.tools;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Rule;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.translation.visitors.TranslateToSmtLibClauseVisitor;
import secpriv.horst.translation.visitors.TranslateToSmtLibVisitorState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SmtLibGenerator {
    static class ProgramCounterComparator implements Comparator<Clause> {
        @Override
        public int compare(Clause a, Clause b) {
            return getProgramCounter(a.conclusion.predicate) - getProgramCounter(b.conclusion.predicate);
        }

        private int getProgramCounter(Predicate a) {
            String[] parts = a.name.split("_");
            if (parts.length == 1) {
                return 0;
            }
            try {
                return Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    public static String generateSmtLib(List<Rule> rules, List<Rule> queryRules) {
        StringBuilder ruleStringBuilder = new StringBuilder();
        TranslateToSmtLibVisitorState state = new TranslateToSmtLibVisitorState();
        Map<Clause, Tuple2<Rule, Integer>> clauseToRuleMap = generateClauseToRuleMap(rules);


        TranslateToSmtLibClauseVisitor translateToSmtLibClauseVisitor = new TranslateToSmtLibClauseVisitor(state, ruleStringBuilder, clauseToRuleMap);

        List<Clause> clauses = rules.stream().flatMap(r -> r.clauses.stream()).sorted(new ProgramCounterComparator()).collect(Collectors.toList());

        clauses.forEach(c -> c.accept(translateToSmtLibClauseVisitor));

        StringBuilder sb = new StringBuilder();

        sb.append(state.getSmtLibVariableDeclarations());
        sb.append(state.getSmtLibPredicateDeclarations());
        sb.append(ruleStringBuilder.toString());

        sb.append("\n");

        for (Rule query : queryRules) {
            sb.append("(query ");
            sb.append(query.name);
            sb.append(")\n");
        }

        return sb.toString();
    }

    public static Map<Clause, Tuple2<Rule, Integer>> generateClauseToRuleMap(List<Rule> rules) {
        Map<Clause, Tuple2<Rule, Integer>> clauseToRuleMap = new HashMap<>();
        ClauseToRuleAndIndexMappingRuleVisitor clauseToRuleAndIndexMappingRuleVisitor = new ClauseToRuleAndIndexMappingRuleVisitor(clauseToRuleMap);
        rules.forEach(rule -> rule.accept(clauseToRuleAndIndexMappingRuleVisitor));
        return clauseToRuleMap;
    }

    public static void writeSmtLibToFile(String fileName, List<Rule> rules, List<Rule> queryRules) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.write(generateSmtLib(rules, queryRules));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeSmtLibToFile(File file, List<Rule> rules, List<Rule> queryRules) {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(generateSmtLib(rules, queryRules));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ClauseToRuleAndIndexMappingRuleVisitor implements Rule.Visitor<Void> {
        private final Map<Clause, Tuple2<Rule, Integer>> map;

        private ClauseToRuleAndIndexMappingRuleVisitor(Map<Clause, Tuple2<Rule, Integer>> map) {
            this.map = map;
        }

        @Override
        public Void visit(Rule rule) {
            for (int i = 0; i < rule.clauses.size(); ++i) {
                map.put(rule.clauses.get(i), new Tuple2<>(rule, i));
            }
            return null;
        }
    }
}
