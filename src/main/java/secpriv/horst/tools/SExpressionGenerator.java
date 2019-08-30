package secpriv.horst.tools;

import secpriv.horst.data.Rule;
import secpriv.horst.visitors.SExpressionRuleVisitor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class SExpressionGenerator {
    public static String generateSExpression(List<Rule> rules) {
        StringBuilder sb = new StringBuilder();

        for(Rule rule : rules) {
            SExpressionRuleVisitor ruleVisitor = new SExpressionRuleVisitor();
            sb.append(rule.accept(ruleVisitor));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static void writeSExpressionToFile(String fileName, List<Rule> rules) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.write(generateSExpression(rules));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
