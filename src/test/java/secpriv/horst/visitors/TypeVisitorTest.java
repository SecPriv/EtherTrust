package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TypeVisitorTest {

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @Test
    public void parseEqtypeDeclaration1() {
        String s = "eqtype AbsDom := @T | @V<int>;";

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ASParser parser = getParserFromString(s);
        TypeVisitor visitor = new TypeVisitor(state);

        Optional<Type.CustomType> optType = visitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();
        assertThat(optType.get()).isInstanceOfSatisfying(Type.CustomType.class, t -> {
            assertThat(t.name).isEqualTo("AbsDom");
            assertThat(t.constructors.stream().map(c -> c.name)).contains("@T");
            assertThat(t.constructors.stream().map(c -> c.name)).contains("@V");
            assertThat(t.constructors.stream().filter(c -> c.name.equals("@V")).findFirst().get().typeParameters).contains(Type.Integer);
        });
    }

    @Test
    public void parseEqtypeDeclarationAndDefineType() {
        String s = "eqtype AbsDom := @T | @V<int>;";

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ASParser parser = getParserFromString(s);
        TypeVisitor visitor = new TypeVisitor(state);

        Type.CustomType type = visitor.visit(parser.abstractDomainDeclaration()).get();
        assertThat(state.defineType(type)).isTrue();
        assertThat(state.defineType(type)).isFalse();
        assertThat(state.getTypeForConstructor("@T")).isPresent();
        assertThat(state.getTypeForConstructor("@V")).isPresent();
        assertThat(state.getTypeForConstructor("@T").get()).isEqualTo(type);
        assertThat(state.getTypeForConstructor("@V").get()).isEqualTo(type);
    }

    @Test
    public void parseDatatypeDeclaration1() {
        String s = "datatype AbsDom := @T | @V<int>;";

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ASParser parser = getParserFromString(s);
        TypeVisitor visitor = new TypeVisitor(state);

        Optional<Type.CustomType> optType = visitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();
        assertThat(optType.get()).isInstanceOfSatisfying(Type.CustomType.class, t -> {
            assertThat(t.name).isEqualTo("AbsDom");
            assertThat(t.constructors.stream().map(c -> c.name)).contains("@T");
            assertThat(t.constructors.stream().map(c -> c.name)).contains("@V");
            assertThat(t.constructors.stream().filter(c -> c.name.equals("@V")).findFirst().get().typeParameters).contains(Type.Integer);
        });
    }

    @Test
    public void parseDatatypeDeclaration2() {
        String s = "datatype AbsDom := @T | @V<array<int>>;";

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ASParser parser = getParserFromString(s);
        TypeVisitor visitor = new TypeVisitor(state);

        Optional<Type.CustomType> optType = visitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();
        assertThat(optType.get()).isInstanceOfSatisfying(Type.CustomType.class, t -> {
            assertThat(t.name).isEqualTo("AbsDom");
            assertThat(t.constructors.stream().map(c -> c.name)).contains("@T");
            assertThat(t.constructors.stream().map(c -> c.name)).contains("@V");
            assertThat(t.constructors.stream().filter(c -> c.name.equals("@V")).findFirst().get().typeParameters).contains(Type.Array.of(Type.Integer));
        });
    }

    @Test
    public void parseDatatypeDeclaration3() {
        String s = "datatype AbsDom := @T | @V<  array<   int   >     >;";

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        ASParser parser = getParserFromString(s);
        TypeVisitor visitor = new TypeVisitor(state);

        Optional<Type.CustomType> optType = visitor.visit(parser.abstractDomainDeclaration());

        assertThat(optType).isPresent();
        assertThat(optType.get()).isInstanceOfSatisfying(Type.CustomType.class, t -> {
            assertThat(t.name).isEqualTo("AbsDom");
            assertThat(t.constructors.stream().map(c -> c.name)).contains("@T");
            assertThat(t.constructors.stream().map(c -> c.name)).contains("@V");
            assertThat(t.constructors.stream().filter(c -> c.name.equals("@V")).findFirst().get().typeParameters).contains(Type.Array.of(Type.Integer));
        });
    }
}