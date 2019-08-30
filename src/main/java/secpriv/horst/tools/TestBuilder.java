package secpriv.horst.tools;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import secpriv.horst.data.*;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.parser.ASLexer;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Type;
import secpriv.horst.visitors.*;

public class TestBuilder {
    private final VisitorState state;

    public TestBuilder(VisitorState state) {
        this.state = state;
        state.defineType(Type.Integer);
        state.defineType(Type.Boolean);
    }

    public Type.CustomType defineType(String code) {
        TypeVisitor typeVisitor = new TypeVisitor(new VisitorState(state));
        Type.CustomType type = typeVisitor.visitAbstractDomainDeclaration(getParserFromString(code).abstractDomainDeclaration()).get();

        if (!state.defineType(type)) {
            throw new RuntimeException("Type already defined!");
        }

        return type;
    }

    public SelectorFunction defineSelectorFunction(String code) {
        SelectorFunctionDefinitionVisitor selectorFunctionDefinitionVisitor = new SelectorFunctionDefinitionVisitor(new VisitorState(state));
        SelectorFunction selectorFunction = selectorFunctionDefinitionVisitor.visitSelectorFunctionDeclaration(getParserFromString(code).selectorFunctionDeclaration()).get();

        if(!state.defineSelectorFunction(selectorFunction)) {
            throw new RuntimeException("Selector function is already defined!");
        }

        return selectorFunction;
    }

    public Operation defineOperation(String code) {
        OperationVisitor operationVisitor = new OperationVisitor(new VisitorState(state));
        Operation operation = operationVisitor.visitOperationDefinition(getParserFromString(code).operationDefinition()).get();

        if(!state.defineOperation(operation)) {
            throw new RuntimeException("Operation is already defined!");
        }

        return operation;
    }

    public Rule defineRule(String code) {
        RuleVisitor ruleVisitor = new RuleVisitor(new VisitorState(state));
        Rule rule = ruleVisitor.visitRuleDefinition(getParserFromString(code).ruleDefinition()).get();

        if(!state.defineRule(rule)) {
            throw new RuntimeException("Rule already defined!");
        }

        return rule;
    }

    public Predicate definePredicate(String code) {
        PredicateDefinitionVisitor predicateDefinitionVisitor = new PredicateDefinitionVisitor(new VisitorState(state));
        Predicate predicate = predicateDefinitionVisitor.visitPredicateDeclaration(getParserFromString(code).predicateDeclaration()).get();

        if(!state.definePredicate(predicate)) {
            throw new RuntimeException("Predicate already defined!");
        }

        return predicate;
    }

    private ASParser getParserFromString(String s) {
        ASLexer lexer = new ASLexer(CharStreams.fromString(s));
        lexer.removeErrorListeners();
        lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ASParser parser = new ASParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        return parser;
    }

    public Expression parseExpression(String code) {
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(new VisitorState(state));
        return expressionVisitor.visitExp(getParserFromString(code).exp()).get();
    }

    public void setSelectorFunctionHelper(SelectorFunctionHelper selectorFunctionHelper) {
        state.setSelectorFunctionHelper(selectorFunctionHelper);
    }
}
