package secpriv.horst.translation.visitors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Rule;
import secpriv.horst.tools.TestBuilder;
import secpriv.horst.visitors.RuleTypeOracle;
import secpriv.horst.visitors.VisitorState;

import static org.assertj.core.api.Assertions.*;

class FilterUnapplicableClausesRuleVisitorTest {
    private TestBuilder testBuilder;
    private FilterUnapplicableClausesRuleVisitor ruleVisitor;

    @BeforeEach
    public void setUp() {
        VisitorState state = new VisitorState();
        testBuilder = new TestBuilder(state);
        testBuilder.definePredicate("pred FunnyFun{} : int;)");
        ruleVisitor = new FilterUnapplicableClausesRuleVisitor(new RuleTypeOracle(state));
    }

    @AfterEach
    public void tearDown() {
        testBuilder = null;
    }

    @Test
    public void testImpossibleClauseGetFiltered1() {
        String r = "rule unitRule :=   \n" +
                "clause [?i : int]  \n" +
                "false,             \n" +
                "FunnyFun(?i)       \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        Rule rule = testBuilder.defineRule(r);

        assertThat(rule.clauses).hasSize(1);
        Rule visitedRule = rule.accept(ruleVisitor);
        assertThat(visitedRule.clauses).isEmpty();
    }

    @Test
    public void testImpossibleClauseGetFiltered2() {
        String r = "rule unitRule :=   \n" +
                "clause [?i : int]  \n" +
                "FunnyFun(?i),      \n" +
                "false              \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        Rule rule = testBuilder.defineRule(r);

        assertThat(rule.clauses).hasSize(1);
        Rule visitedRule = rule.accept(ruleVisitor);
        assertThat(visitedRule.clauses).isEmpty();
    }
}