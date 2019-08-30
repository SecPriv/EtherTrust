package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.SelectorFunction;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectorFunctionDefinitionVisitorTest {

    private VisitorState state;
    private TestingErrorHandler testingHandler;
    private SelectorFunctionHelper selectorFunctionHelper;

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @BeforeEach
    public void setUp() {
        state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        testingHandler = (TestingErrorHandler)state.errorHandler;
        selectorFunctionHelper = new SelectorFunctionHelper();
        state.setSelectorFunctionHelper(selectorFunctionHelper);
    }

    @AfterEach
    public void tearDown() {
        state = null;
        selectorFunctionHelper = null;
    }

    @Test
    public void parseSelectorFunctionDeclaration1() {
        String s = "sel interval: int -> [int];";
        ASParser parser = getParserFromString(s);

        selectorFunctionHelper.registerProvider(new Object() {
            public Iterable<BigInteger> interval(BigInteger i) {
                return null;
            }
        });

        SelectorFunctionDefinitionVisitor visitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = visitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        assertThat(optSelectorFunction.get().name).isEqualTo("interval");
        assertThat(optSelectorFunction.get().parameterTypes.size()).isEqualTo(1);
        assertThat(optSelectorFunction.get().returnTypes.size()).isEqualTo(1);
        assertThat(optSelectorFunction.get().parameterTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optSelectorFunction.get().returnTypes.get(0)).isEqualTo(Type.Integer);

    }

    @Test
    public void parseSelectorFunctionDeclaration2() {
        String s = "sel interval: int*int -> [bool*int*bool];";
        ASParser parser = getParserFromString(s);

        selectorFunctionHelper.registerProvider(new Object() {
            public Iterable<Tuple3<Boolean, BigInteger, Boolean>> interval(BigInteger a, BigInteger b) {
                return null;
            }
        });

        SelectorFunctionDefinitionVisitor visitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = visitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        assertThat(optSelectorFunction.get().name).isEqualTo("interval");
        assertThat(optSelectorFunction.get().parameterTypes.size()).isEqualTo(2);
        assertThat(optSelectorFunction.get().returnTypes.size()).isEqualTo(3);
        assertThat(optSelectorFunction.get().parameterTypes.get(0)).isEqualTo(Type.Integer);
        assertThat(optSelectorFunction.get().parameterTypes.get(1)).isEqualTo(Type.Integer);
        assertThat(optSelectorFunction.get().returnTypes.get(0)).isEqualTo(Type.Boolean);
        assertThat(optSelectorFunction.get().returnTypes.get(1)).isEqualTo(Type.Integer);
        assertThat(optSelectorFunction.get().returnTypes.get(2)).isEqualTo(Type.Boolean);
    }

    @Test
    public void parseSelectorFunctionDeclaration3() {
        String s = "sel interval: unit -> [bool*int*bool];";
        ASParser parser = getParserFromString(s);

        selectorFunctionHelper.registerProvider(new Object() {
            public Iterable<Tuple3<Boolean, BigInteger, Boolean>> interval() {
                return null;
            }
        });

        SelectorFunctionDefinitionVisitor visitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optSelectorFunction = visitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optSelectorFunction).isPresent();
        assertThat(optSelectorFunction.get().name).isEqualTo("interval");
        assertThat(optSelectorFunction.get().parameterTypes.size()).isEqualTo(0);
        assertThat(optSelectorFunction.get().returnTypes.size()).isEqualTo(3);
        assertThat(optSelectorFunction.get().returnTypes.get(0)).isEqualTo(Type.Boolean);
        assertThat(optSelectorFunction.get().returnTypes.get(1)).isEqualTo(Type.Integer);
        assertThat(optSelectorFunction.get().returnTypes.get(2)).isEqualTo(Type.Boolean);

    }

    @Test
    public void parseSelectorFunctionDeclarationDoubleName() {
        String s1 = "sel interval: bool -> [bool*int*bool];";
        String s2 = "sel interval: int -> [int];";
        ASParser parser = getParserFromString(s1);

        selectorFunctionHelper.registerProvider(new Object() {
            public Iterable<Tuple3<Boolean, BigInteger, Boolean>> interval(Boolean b) {
                return null;
            }
        });

        SelectorFunctionDefinitionVisitor visitor = new SelectorFunctionDefinitionVisitor(state);
        Optional<SelectorFunction> optInterval1 = visitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optInterval1).isPresent();
        state.defineSelectorFunction(optInterval1.get());

        parser = getParserFromString(s2);
        Optional<SelectorFunction> optInterval2 = visitor.visit(parser.selectorFunctionDeclaration());

        assertThat(optInterval2).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }
}
