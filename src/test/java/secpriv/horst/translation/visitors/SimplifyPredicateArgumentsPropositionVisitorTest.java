package secpriv.horst.translation.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Predicate;
import secpriv.horst.data.Proposition;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.PredicateDefinitionVisitor;
import secpriv.horst.visitors.PropositionVisitor;
import secpriv.horst.visitors.VisitorState;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SimplifyPredicateArgumentsPropositionVisitorTest {

    private SimplifyPredicateArgumentsPropositionVisitor simplifyPredicateArgumentsPropositionVisitor;
    private VisitorState state;

    @BeforeEach
    public void setUp() {
        simplifyPredicateArgumentsPropositionVisitor = new SimplifyPredicateArgumentsPropositionVisitor();
        state = new VisitorState();
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
    }

    @AfterEach
    public void tearDown() {
        simplifyPredicateArgumentsPropositionVisitor = null;
        state = null;
    }

    @Test
    public void test1() {
        String p = "pred As{} : int;";
        String s = "As(123)";

        ASParser parser = getParserFromString(p);
        VisitorState state = new VisitorState();
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        Predicate predicate = optPred.get();
        state.definePredicate(predicate);

        parser = getParserFromString(s);

        PropositionVisitor propositionVisitor = new PropositionVisitor(state);
        List<Proposition> propositionList = propositionVisitor.visit(parser.prop());

        assertThat(propositionList.size()).isEqualTo(1);
        assertThat(propositionList.get(0)).isInstanceOf(Proposition.PredicateProposition.class);

        Proposition.PredicateProposition proposition = (Proposition.PredicateProposition) propositionList.get(0);

        assertThat(proposition.predicate).isEqualTo(predicate);
        assertThat(proposition.parameters).hasSize(0);
        assertThat(proposition.arguments).hasSize(1);

        SimplifyPredicatesData data = simplifyPredicateArgumentsPropositionVisitor.visit(proposition);

        assertThat(data.newFreeVars).hasSize(1);
        assertThat(data.simplifiedPropositions).hasSize(2);
        assertThat(data.simplifiedPropositions).filteredOnAssertions(
                pp -> assertThat(pp).isInstanceOfSatisfying(Proposition.PredicateProposition.class,
                        ppp -> assertThat(ppp.arguments).allSatisfy(a -> assertThat(a).isInstanceOf(Expression.FreeVarExpression.class))))
                .hasSize(1);

        assertThat(data.simplifiedPropositions).filteredOnAssertions(
                pp -> assertThat(pp).isInstanceOfSatisfying(Proposition.ExpressionProposition.class,
                        ppp -> assertThat(ppp.expression).isInstanceOf(Expression.ComparisonExpression.class)))
                .hasSize(1);
    }

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }
}