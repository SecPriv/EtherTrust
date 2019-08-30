package secpriv.horst.translation.visitors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;
import secpriv.horst.data.Rule;
import secpriv.horst.internals.IntervalProvider;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.SelectorFunctionInvoker;
import secpriv.horst.tools.TestBuilder;
import secpriv.horst.translation.TranslationPipeline;
import secpriv.horst.visitors.SExpressionClauseVisitor;
import secpriv.horst.visitors.VisitorState;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ClauseMergerTest {
    private ClauseMerger clauseMerger;
    private TestBuilder testBuilder;
    private TranslationPipeline pipeline;

    @BeforeEach
    public void setUp() {
        clauseMerger = new ClauseMerger();
        testBuilder = new TestBuilder(new VisitorState());
        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        selectorFunctionHelper.registerProvider(new IntervalProvider());
        testBuilder.setSelectorFunctionHelper(selectorFunctionHelper);

        pipeline = TranslationPipeline.builder()
                .addFlatMappingStep(new InstantiateParametersRuleVisitor(new SelectorFunctionInvoker(selectorFunctionHelper)))
                .addStep(new SimplifyPredicateArgumentsRuleVisitor())
                .addStep(new RenameFreeVariablesRuleVisitor()).build();
    }

    @AfterEach
    public void tearDown() {
        clauseMerger = null;
        testBuilder = null;
        pipeline = null;
    }

    @Test
    public void testSimpleMerge() {
        testBuilder.defineSelectorFunction("sel interval: int*int -> [int];");
        testBuilder.definePredicate("pred Oo{int}: int*int;");
        Rule rule = testBuilder.defineRule("rule testRule := for (!i:int) in interval(0,5) \n" +
                "clause [?x:int, ?y:int] Oo{!i}(?x,?y), ?x > !i, ?x > ?y => Oo{!i+1}(?x,?y);");

        List<Rule> rules = pipeline.apply(Collections.singletonList(rule));

        assertThat(rules).hasSize(5);
        List<Clause> clauses = rules.stream().map(r -> r.clauses.get(0)).collect(Collectors.toList());
        clauses.forEach(r -> System.out.println(r.accept(new SExpressionClauseVisitor(0))));

        Clause mergedClause = clauseMerger.merge(clauses);

        System.out.println(mergedClause.accept(new SExpressionClauseVisitor(0)));

        assertThat(mergedClause.freeVars).hasSize(2);
        assertThat(mergedClause.premises).hasSize(11);
        assertThat(mergedClause.premises.get(0)).isInstanceOfSatisfying(Proposition.PredicateProposition.class, p -> {
            assertThat(p.predicate.name).isEqualTo("Oo_0");
        });

        assertThat(mergedClause.conclusion.predicate.name).isEqualTo("Oo_5");
    }
}