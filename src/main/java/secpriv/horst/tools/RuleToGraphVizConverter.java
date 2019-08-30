package secpriv.horst.tools;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.Rule;
import secpriv.horst.data.tuples.Tuple2;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class RuleToGraphVizConverter {
    public static String generateGraphViz(List<Rule> rules, Map<Predicate, List<String>> styles) {
        StringJoiner sj = new StringJoiner("\n", "digraph G {\n", "}");

        Map<Clause, Tuple2<Rule, Integer>> clauseToRuleMap = SmtLibGenerator.generateClauseToRuleMap(rules);

        rules.stream().map(rule -> ruleToString(rule, clauseToRuleMap)).forEach(sj::add);
        styles.entrySet().stream().map(p -> styleToPredicate(p.getKey(), styles)).forEach(sj::add);

        return sj.toString();
    }

    private static String styleToPredicate(Predicate predicate, Map<Predicate, List<String>> styles) {
        StringJoiner sj = new StringJoiner("\n", predicate.name + " [", "];");
        styles.get(predicate).forEach(sj::add);
        return sj.toString();
    }

    private static String ruleToString(Rule rule, Map<Clause, Tuple2<Rule, Integer>> clauseToRuleMap) {
        StringBuilder sb = new StringBuilder();

        for (Clause clause : rule.clauses) {
            for (Proposition proposition : clause.premises) {
                //TODO beautify
                if (proposition instanceof Proposition.PredicateProposition) {
                    sb.append(((Proposition.PredicateProposition) proposition).predicate.name);
                    sb.append(" -> ");
                    sb.append(clause.conclusion.predicate.name);

                    if(clauseToRuleMap.containsKey(clause)) {
                        sb.append(" [label=\"");
                        sb.append(clauseToRuleMap.get(clause).v0.name);
                        sb.append(" ");
                        sb.append(clauseToRuleMap.get(clause).v1);
                        sb.append("\"]");
                    }

                    sb.append(";\n");
                }
            }
        }
        return sb.toString();
    }

    public static void writeGraphVizToFile(String fileName, List<Rule> rules, Map<Predicate, List<String>> styles) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.write(generateGraphViz(rules, styles));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
