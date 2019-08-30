package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramVisitorTest {

    private VisitorState state;

    private ASParser getParserFromFileName(String s) {
        ASLexer lexer = null;
        try {
            lexer = new ASLexer(CharStreams.fromFileName(s));
        } catch (IOException e) {
        }
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @BeforeEach
    public void setUp() {
        state = new VisitorState(new TestingErrorHandler());
    }

    @AfterEach
    public void tearDown() {
        state = null;
    }

    @Test
    void parseProgram1() {
        SelectorFunctionHelper selectorFunctionHelper = new SelectorFunctionHelper();
        try {
            selectorFunctionHelper.compileSelectorFunctionsProvider(System.getProperty("user.dir") + "/grammar/WellTypedSelectorFunctionProvider.java", Collections.emptyList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        state.setSelectorFunctionHelper(selectorFunctionHelper);

        ASParser parser = getParserFromFileName(System.getProperty("user.dir") + "/grammar/test_welltyped.txt");

        ProgramVisitor visitor = new ProgramVisitor(state);
        Optional<VisitorState> state = visitor.visitAbstractProgram(parser.abstractProgram());

        assertThat(state).isPresent();
    }
}