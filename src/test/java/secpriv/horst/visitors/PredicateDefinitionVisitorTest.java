package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Predicate;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class PredicateDefinitionVisitorTest {

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @Test
    public void parsePredicateDefinition1() {
        String s = "pred As{} : int;";

        ASParser parser = getParserFromString(s);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);


        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        assertThat(optPred.get().parameterTypes).isEmpty();
        assertThat(optPred.get().argumentsTypes.size()).isEqualTo(1);
        assertThat(optPred.get().argumentsTypes.get(0)).isEqualTo(Type.Integer);
    }

    @Test
    public void parsePredicateDefinition2() {
        String s = "pred As{} : int*array<bool>;";

        ASParser parser = getParserFromString(s);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);


        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        assertThat(optPred.get().parameterTypes).isEmpty();
        assertThat(optPred.get().argumentsTypes.size()).isEqualTo(2);
        assertThat(optPred.get().argumentsTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optPred.get().argumentsTypes.get(1)).isEqualTo(Type.Array.of(Type.Boolean));
    }

    @Test
    public void parsePredicateDefinition3() {
        String s = "pred As{int*bool} : int;";

        ASParser parser = getParserFromString(s);
        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);


        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        assertThat(optPred.get().parameterTypes.size()).isEqualTo(2);
        assertThat(optPred.get().parameterTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optPred.get().parameterTypes.get(1)).isEqualTo(Type.Boolean);
        assertThat(optPred.get().argumentsTypes.size()).isEqualTo(1);
        assertThat(optPred.get().argumentsTypes.get(0)).isEqualTo(Type.Integer);
    }

    @Test
    public void parsePredicateDefinition4() {
        String t = "datatype MyColor := @R | @G | @B;";
        String s = "pred As{int*bool} : int*MyColor;";

        ASParser parser = getParserFromString(t);

        TypeVisitor typeVisitor = new TypeVisitor();
        Optional<Type.CustomType> optType = typeVisitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        state.defineType(optType.get());


        parser = getParserFromString(s);

        PredicateDefinitionVisitor visitor = new PredicateDefinitionVisitor(state);
        Optional<Predicate> optPred = visitor.visit(parser.predicateDeclaration());

        assertThat(optPred).isPresent();
        assertThat(optPred.get().parameterTypes.size()).isEqualTo(2);
        assertThat(optPred.get().parameterTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optPred.get().parameterTypes.get(1)).isEqualTo(Type.Boolean);
        assertThat(optPred.get().argumentsTypes.size()).isEqualTo(2);
        assertThat(optPred.get().argumentsTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optPred.get().argumentsTypes.get(1)).isEqualTo(optType.get());
    }

}