package secpriv.horst.translation.visitors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.Rule;
import secpriv.horst.tools.TestBuilder;
import secpriv.horst.visitors.VisitorState;

import static org.assertj.core.api.Assertions.assertThat;

class RemoveTruePremiseRuleVisitorTest {
    private TestBuilder testBuilder;
    private RemoveTruePremiseRuleVisitor ruleVisitor;

    @BeforeEach
    public void setUp() {
        VisitorState state = new VisitorState();
        testBuilder = new TestBuilder(state);
        testBuilder.definePredicate("pred FunnyFun{} : int;)");
        ruleVisitor = new RemoveTruePremiseRuleVisitor();
    }

    @AfterEach
    public void tearDown() {
        testBuilder = null;
    }

    @Test
    public void testTruePremisesGetFiltered1() {
        String r = "rule unitRule :=   \n" +
                "clause [?i : int]  \n" +
                "true,              \n" +
                "true,              \n" +
                "true,              \n" +
                "FunnyFun(?i),      \n" +
                "true,              \n" +
                "true,              \n" +
                "true               \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        Rule rule = testBuilder.defineRule(r);

        assertThat(rule.clauses).hasSize(1);
        Rule visitedRule = rule.accept(ruleVisitor);
        assertThat(visitedRule.clauses).hasSize(1);
        assertThat(visitedRule.clauses.get(0).premises).hasSize(1);
    }

    @Test
    public void testTruePremisesGetFiltered2() {
        String r = "rule unitRule :=   \n" +
                "clause [?i : int]  \n" +
                "true,              \n" +
                "true,              \n" +
                "true,              \n" +
                "true,              \n" +
                "true,              \n" +
                "true               \n" +
                "=> FunnyFun(?i+1)  \n" +
                ";";

        Rule rule = testBuilder.defineRule(r);

        assertThat(rule.clauses).hasSize(1);
        Rule visitedRule = rule.accept(ruleVisitor);
        assertThat(visitedRule.clauses).hasSize(1);
        assertThat(visitedRule.clauses.get(0).premises).hasSize(1);
        assertThat(visitedRule.clauses.get(0).premises.get(0)).isInstanceOfSatisfying(Proposition.ExpressionProposition.class, p -> assertThat(p.expression).isEqualTo(Expression.BoolConst.TRUE));
    }

}