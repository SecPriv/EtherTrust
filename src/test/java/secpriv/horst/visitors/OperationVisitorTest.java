package secpriv.horst.visitors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Operation;
import secpriv.horst.internals.error.handling.TestingErrorHandler;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationVisitorTest {

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);

        return parser;
    }

    @Test
    public void parseOperationDefinition1() {
        String s = "op add(a:int, b:int) : int := a + b;";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);

        OperationVisitor visitor = new OperationVisitor(state);
        Optional<Operation> optOperation = visitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();
        assertThat(optOperation.get().name).isEqualTo("add");
        assertThat(optOperation.get().parameters.size()).isEqualTo(0);
        assertThat(optOperation.get().arguments.size()).isEqualTo(2);
        assertThat(optOperation.get().arguments.get(0).name).isEqualTo("a");
        assertThat(optOperation.get().arguments.get(0).getType()).isEqualTo(Type.Integer);
        assertThat(optOperation.get().arguments.get(1).name).isEqualTo("b");
        assertThat(optOperation.get().arguments.get(1).getType()).isEqualTo(Type.Integer);
        assertThat(optOperation.get().body.getType()).isEqualTo(Type.Integer);

    }

    @Test
    public void parseOperationDefinition2() {
        String s = "op add{!x:int,!y:int}(a:int, b:int) : int := a + b;";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);

        OperationVisitor visitor = new OperationVisitor(state);
        Optional<Operation> optOperation = visitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();
        assertThat(optOperation.get().name).isEqualTo("add");
        assertThat(optOperation.get().parameters.size()).isEqualTo(2);
        assertThat(optOperation.get().parameters.get(0).name).isEqualTo("!x");
        assertThat(optOperation.get().parameters.get(0).type).isEqualTo(Type.Integer);
        assertThat(optOperation.get().parameters.get(1).name).isEqualTo("!y");
        assertThat(optOperation.get().parameters.get(1).type).isEqualTo(Type.Integer);
        assertThat(optOperation.get().arguments.size()).isEqualTo(2);
        assertThat(optOperation.get().arguments.get(0).name).isEqualTo("a");
        assertThat(optOperation.get().arguments.get(0).getType()).isEqualTo(Type.Integer);
        assertThat(optOperation.get().arguments.get(1).name).isEqualTo("b");
        assertThat(optOperation.get().arguments.get(1).getType()).isEqualTo(Type.Integer);
        assertThat(optOperation.get().body.getType()).isEqualTo(Type.Integer);

    }

    @Test
    public void parseOperationDefinitionContainingOperation1() {
        String o1 = "op inc(a:int): int :=  a+1;";
        String o2 = "op add(a:int, b:int): int := inc(inc(a) + inc(b));";
        ASParser parser = getParserFromString(o1);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        OperationVisitor operationVisitor = new OperationVisitor(state);
        Optional<Operation> optInc = operationVisitor.visit(parser.operationDefinition());

        assertThat(optInc).isPresent();
        state.defineOperation(optInc.get());

        parser = getParserFromString(o2);
        Optional<Operation> optAdd = operationVisitor.visit(parser.operationDefinition());

        assertThat(optAdd).isPresent();
        state.defineOperation(optAdd.get());

        assertThat(optAdd.get().name).isEqualTo("add");
        assertThat(optAdd.get().parameters.size()).isEqualTo(0);
        assertThat(optAdd.get().arguments.size()).isEqualTo(2);
        assertThat(optAdd.get().arguments.get(0).name).isEqualTo("a");
        assertThat(optAdd.get().arguments.get(0).getType()).isEqualTo(Type.Integer);
        assertThat(optAdd.get().arguments.get(1).name).isEqualTo("b");
        assertThat(optAdd.get().arguments.get(1).getType()).isEqualTo(Type.Integer);
        assertThat(optAdd.get().body.getType()).isEqualTo(Type.Integer);
        assertThat(optAdd.get().body).isInstanceOfSatisfying(Expression.AppExpression.class, e -> assertThat(e.operation).isEqualTo(optInc.get()));

    }

    @Test
    public void parseOperationDefinitionContainingOperation2() {
        String o2 = "op add(a:int, b:int): int := inc(inc(a) + inc(b));";

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        TestingErrorHandler testingHandler = (TestingErrorHandler)state.errorHandler;

        OperationVisitor operationVisitor = new OperationVisitor(state);

        ASParser parser = getParserFromString(o2);

        Optional<Operation> optAdd = operationVisitor.visit(parser.operationDefinition());
        assertThat(optAdd).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseOperationDefinition3() {
        String s = "op add{!x:int,!x:int}(a:int, b:int) : int := a + b;";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        TestingErrorHandler testingHandler = (TestingErrorHandler)state.errorHandler;

        OperationVisitor visitor = new OperationVisitor(state);
        Optional<Operation> optOperation = visitor.visit(parser.operationDefinition());

        assertThat(optOperation).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.UndefinedValue.class);
    }

    @Test
    public void parseOperationDefinition4() {
        String s = "op add{!x:int,!y:int}(a:int, a:int) : int := a + a;";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        TestingErrorHandler testingHandler = (TestingErrorHandler)state.errorHandler;

        OperationVisitor visitor = new OperationVisitor(state);
        Optional<Operation> optOperation = visitor.visit(parser.operationDefinition());

        assertThat(optOperation).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.ElementAlreadyBound.class);
    }

    @Test
    public void parseOperationDefinition5() {
        String s = "op add{!x:int,!y:int}(a:int, b:int) : int := false;";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());

        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        TestingErrorHandler testingHandler = (TestingErrorHandler)state.errorHandler;


        OperationVisitor visitor = new OperationVisitor(state);
        Optional<Operation> optOperation = visitor.visit(parser.operationDefinition());

        assertThat(optOperation).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseOperationDefinition6() {
        String s = "op add{!x:int,!y:int}(a:bool, b:int) : int := a + b;";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
        TestingErrorHandler testingHandler = (TestingErrorHandler)state.errorHandler;


        OperationVisitor visitor = new OperationVisitor(state);
        Optional<Operation> optOperation = visitor.visit(parser.operationDefinition());

        assertThat(optOperation).isNotPresent();
        assertEquals(1, testingHandler.errorObjects.size());
        assertThat(testingHandler.errorObjects.get(0)).isInstanceOf(Error.TypeMismatch.class);
    }

    @Test
    public void parseOperationDefinition7() {
        String s = "op add(a:array<int>, b:int) : int := (select a 2) + b;";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());

        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        OperationVisitor visitor = new OperationVisitor(state);
        Optional<Operation> optOperation = visitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();
        assertThat(optOperation.get().arguments.get(0).getType()).isEqualTo(Type.Array.of(Type.Integer));
    }

    @Test
    public void parseOperationDefinition8() {
        String s = "op add(a:array<array<int>>, b:int) : int := (select (select a 2) 3) + b;";
        ASParser parser = getParserFromString(s);

        VisitorState state = new VisitorState(new TestingErrorHandler());

        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);

        OperationVisitor visitor = new OperationVisitor(state);
        Optional<Operation> optOperation = visitor.visit(parser.operationDefinition());

        assertThat(optOperation).isPresent();
        assertThat(optOperation.get().arguments.get(0).getType()).isEqualTo(Type.Array.of(Type.Array.of(Type.Integer)));
    }
}