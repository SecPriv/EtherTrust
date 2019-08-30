package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.VisitorState.MacroDefinition;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MacroDefinitionVisitorTest {
    private VisitorState state;
    private TestingErrorHandler testingHandler;

    @BeforeEach
    public void setUp() {
        state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;
    }

    @AfterEach
    public void tearDown() {
        state = null;
    }

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }


    @Test
    public void parseGlobalMacroDefinition1() {
        String s = "g-macro #Hallo := Pa(1123,123,443);";

        ASParser parser = getParserFromString(s);

        MacroDefinitionVisitor visitor = new MacroDefinitionVisitor(state);

        Optional<MacroDefinition> optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isPresent();
        assertThat(optMacroDefinition.get().name).isEqualTo("#Hallo");
        assertThat(optMacroDefinition.get().arguments).isEmpty();
        assertThat(optMacroDefinition.get().argumentTypes).isEmpty();
        assertThat(optMacroDefinition.get().freeVars).isEmpty();
    }

    @Test
    public void parseGlobalMacroDefinition2() {
        String s = "g-macro #Hallo($x:int,$y:bool) := Pa(1123,123,443);";

        ASParser parser = getParserFromString(s);

        MacroDefinitionVisitor visitor = new MacroDefinitionVisitor(state);

        Optional<MacroDefinition> optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isPresent();
        assertThat(optMacroDefinition.get().name).isEqualTo("#Hallo");
        assertThat(optMacroDefinition.get().arguments).hasSize(2);
        assertThat(optMacroDefinition.get().arguments.get(0)).isEqualTo("$x");
        assertThat(optMacroDefinition.get().arguments.get(1)).isEqualTo("$y");
        assertThat(optMacroDefinition.get().argumentTypes).hasSize(2);
        assertThat(optMacroDefinition.get().argumentTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optMacroDefinition.get().argumentTypes.get(1)).isEqualTo(Type.Boolean);
        assertThat(optMacroDefinition.get().freeVars).isEmpty();
    }

    @Test
    public void parseGlobalMacroDefinition3() {
        String s = "g-macro #Hallo($x:int,$y:bool) := Pa(1123,123,443) free [?a : int, ?b : bool];";

        ASParser parser = getParserFromString(s);

        MacroDefinitionVisitor visitor = new MacroDefinitionVisitor(state);

        Optional<MacroDefinition> optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isPresent();
        assertThat(optMacroDefinition.get().name).isEqualTo("#Hallo");
        assertThat(optMacroDefinition.get().arguments).hasSize(2);
        assertThat(optMacroDefinition.get().arguments.get(0)).isEqualTo("$x");
        assertThat(optMacroDefinition.get().arguments.get(1)).isEqualTo("$y");
        assertThat(optMacroDefinition.get().argumentTypes).hasSize(2);
        assertThat(optMacroDefinition.get().argumentTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optMacroDefinition.get().argumentTypes.get(1)).isEqualTo(Type.Boolean);
        assertThat(optMacroDefinition.get().freeVars).hasEntrySatisfying("?a", e -> assertThat(e).isEqualTo(Type.Integer));
        assertThat(optMacroDefinition.get().freeVars).hasEntrySatisfying("?b", e -> assertThat(e).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseGlobalMacroDefinition4() {
        String s1 = "g-macro #Hallo($x:int,$y:bool) := Pa(1123,123,443) free [?a : int, ?b : bool];";
        String s2 = "g-macro #Hallo2($x:int,$y:bool) := Pa(1123,123,443), #Hallo(12,false) free [?a : int, ?b : bool];";

        ASParser parser = getParserFromString(s1);

        MacroDefinitionVisitor visitor = new MacroDefinitionVisitor(state);

        Optional<MacroDefinition> optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isPresent();
        assertThat(optMacroDefinition.get().name).isEqualTo("#Hallo");
        assertThat(optMacroDefinition.get().arguments).hasSize(2);
        assertThat(optMacroDefinition.get().arguments.get(0)).isEqualTo("$x");
        assertThat(optMacroDefinition.get().arguments.get(1)).isEqualTo("$y");
        assertThat(optMacroDefinition.get().argumentTypes).hasSize(2);
        assertThat(optMacroDefinition.get().argumentTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optMacroDefinition.get().argumentTypes.get(1)).isEqualTo(Type.Boolean);
        assertThat(optMacroDefinition.get().freeVars).hasEntrySatisfying("?a", e -> assertThat(e).isEqualTo(Type.Integer));
        assertThat(optMacroDefinition.get().freeVars).hasEntrySatisfying("?b", e -> assertThat(e).isEqualTo(Type.Boolean));

        state.defineMacro(optMacroDefinition.get());
        parser = getParserFromString(s2);
        optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isPresent();
        assertThat(optMacroDefinition.get().name).isEqualTo("#Hallo2");
        assertThat(optMacroDefinition.get().arguments).hasSize(2);
        assertThat(optMacroDefinition.get().arguments.get(0)).isEqualTo("$x");
        assertThat(optMacroDefinition.get().arguments.get(1)).isEqualTo("$y");
        assertThat(optMacroDefinition.get().argumentTypes).hasSize(2);
        assertThat(optMacroDefinition.get().argumentTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optMacroDefinition.get().argumentTypes.get(1)).isEqualTo(Type.Boolean);
        assertThat(optMacroDefinition.get().freeVars).hasEntrySatisfying("?a", e -> assertThat(e).isEqualTo(Type.Integer));
        assertThat(optMacroDefinition.get().freeVars).hasEntrySatisfying("?b", e -> assertThat(e).isEqualTo(Type.Boolean));
    }

    @Test
    public void parseGlobalMacroDoubleDefinition() {
        String s1 = "g-macro #Hallo($x:int,$y:bool) := Pa(1123,123,443) free [?a : int, ?b : bool];";
        String s2 = "g-macro #Hallo($x:int,$y:bool) := Pa(1123,123,443), #Hallo(12,false) free [?a : int, ?b : bool];";

        ASParser parser = getParserFromString(s1);

        MacroDefinitionVisitor visitor = new MacroDefinitionVisitor(state);

        Optional<MacroDefinition> optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isPresent();
        assertThat(optMacroDefinition.get().name).isEqualTo("#Hallo");
        assertThat(optMacroDefinition.get().arguments).hasSize(2);
        assertThat(optMacroDefinition.get().arguments.get(0)).isEqualTo("$x");
        assertThat(optMacroDefinition.get().arguments.get(1)).isEqualTo("$y");
        assertThat(optMacroDefinition.get().argumentTypes).hasSize(2);
        assertThat(optMacroDefinition.get().argumentTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optMacroDefinition.get().argumentTypes.get(1)).isEqualTo(Type.Boolean);
        assertThat(optMacroDefinition.get().freeVars).hasEntrySatisfying("?a", e -> assertThat(e).isEqualTo(Type.Integer));
        assertThat(optMacroDefinition.get().freeVars).hasEntrySatisfying("?b", e -> assertThat(e).isEqualTo(Type.Boolean));

        state.defineMacro(optMacroDefinition.get());
        parser = getParserFromString(s2);
        optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }


    @Test
    public void parseGlobalMacroDefinitionDoubleParameterName() {
        String s = "g-macro #Hallo($x:int,$x:bool) := Pa(1123,123,443);";

        ASParser parser = getParserFromString(s);

        MacroDefinitionVisitor visitor = new MacroDefinitionVisitor(state);

        Optional<MacroDefinition> optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.SizeDoesntMatch.class);

    }

    @Test
    public void parseGlobalMacroDefinitionDoubleFreeVarName() {
        String s = "g-macro #Hallo($x:int,$y:bool) := Pa(1123,123,443) free [?a :int, ?a:int];";

        ASParser parser = getParserFromString(s);

        MacroDefinitionVisitor visitor = new MacroDefinitionVisitor(state);

        Optional<MacroDefinition> optMacroDefinition = visitor.visit(parser.globalMacroDefinition());

        assertThat(optMacroDefinition).isNotPresent();
        assertThat(optMacroDefinition).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }
}