package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import secpriv.horst.data.*;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.ZipInfo;
import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static secpriv.horst.tools.OptionalHelper.allPresent;
import static secpriv.horst.tools.OptionalHelper.listOfOptionalToOptionalOfList;
import static secpriv.horst.tools.Zipper.zipPredicateWithErrorReporting;

public class ExpressionVisitor extends ASBaseVisitor<Optional<Expression>> {
    private VisitorState state;


    public ExpressionVisitor() {
        state = new VisitorState();
    }

    public ExpressionVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<Expression> visitBoolConst(ASParser.BoolConstContext ctx) {
        if (ctx.TRUE() != null) {
            return Optional.of(Expression.BoolConst.TRUE);
        } else if (ctx.FALSE() != null) {
            return Optional.of(Expression.BoolConst.FALSE);
        }
        throw new RuntimeException("Unreachable code!");
    }

    @Override
    public Optional<Expression> visitVar(ASParser.VarContext ctx) {
        String varName = ctx.ID().getText();

        Optional<Type> optType = state.getTypeOfVar(varName);
        if (!optType.isPresent()) {
            return valueUndefined("Variable", varName, ctx);
        }
        return optType.map(t -> new Expression.VarExpression(t, varName));
    }

    @Override
    public Optional<Expression> visitIntConst(ASParser.IntConstContext ctx) {
        String intString = ctx.NUM().getText();

        if (intString.startsWith("~")) {
            intString = intString.replace('~', '-');
        }

        return Optional.of(new Expression.IntConst(new BigInteger(intString)));
    }

    @Override
    public Optional<Expression> visitArrayInitExp(ASParser.ArrayInitExpContext ctx) {
        Optional<Expression> optExpression = visit(ctx.exp());
        if (!optExpression.isPresent()) {
            return Optional.empty();
        }
        ConstnessExpressionVisitor constnessVisitor = new ConstnessExpressionVisitor();
        if (!optExpression.get().accept(constnessVisitor)) {
            return elementNotConst("Expression", optExpression.get(), ctx);
        }
        return Optional.of(new Expression.ArrayInitExpression(optExpression.get()));
    }

    @Override
    public Optional<Expression> visitSelectExp(ASParser.SelectExpContext ctx) {

        Optional<Expression> ret1 = checkIfExpressionTypeIsInstanceOf(visit(ctx.exp(0)), Type.ArrayType.class, ctx);
        Optional<Expression> ret2 = checkIfExpressionMatchesType(visit(ctx.exp(1)), Type.Integer, ctx);

        if (!allPresent(ret1, ret2)) {
            return Optional.empty();
        }
        return Optional.of(new Expression.SelectExpression(ret1.get(), ret2.get()));
    }

    @Override
    public Optional<Expression> visitCondExp(ASParser.CondExpContext ctx) {
        Optional<List<Expression>> optExpressions = listOfOptionalToOptionalOfList(ctx.exp().stream().map(this::visit).collect(Collectors.toList()));
        if (!optExpressions.isPresent()) {
            return Optional.empty();
        }
        List<Expression> expressions = optExpressions.get();

        Optional<Expression> ret1 = checkIfExpressionMatchesType(Optional.ofNullable(expressions.get(0)), Type.Boolean, ctx);
        Optional<Expression> ret2 = checkIfExpressionMatchesType(Optional.ofNullable(expressions.get(1)), expressions.get(2).getType(), ctx);

        if (!allPresent(ret1, ret2)) {
            return Optional.empty();
        }

        return Optional.of(new Expression.ConditionalExpression(ret1.get(), ret2.get(), expressions.get(2)));
    }

    @Override
    public Optional<Expression> visitStoreExp(ASParser.StoreExpContext ctx) {

        Optional<Expression> ret1 = checkIfExpressionTypeIsInstanceOf(visit(ctx.exp(0)), Type.ArrayType.class, ctx);
        Optional<Expression> ret2 = checkIfExpressionMatchesType(visit(ctx.exp(1)), Type.Integer, ctx);
        Optional<Expression> ret3 = Optional.empty();

        if (ret1.isPresent()) {
            ret3 = checkIfExpressionMatchesType(visit(ctx.exp(2)), ((Type.ArrayType) ret1.get().getType()).type, ctx);
        }

        if (!allPresent(ret1, ret2, ret3)) {
            return Optional.empty();
        }
        return Optional.of(new Expression.StoreExpression(ret1.get(), ret2.get(), ret3.get()));
    }

    @Override
    public Optional<Expression> visitParenExp(ASParser.ParenExpContext ctx) {
        Optional<Expression> ret = visit(ctx.exp());
        return ret;
    }

    @Override
    public Optional<Expression> visitParVar(ASParser.ParVarContext ctx) {
        String name = ctx.PAR_ID().getText();

        Optional<Type> optType = state.getTypeOfParameterVar(name);

        if (!optType.isPresent()) {
            return valueUndefined("Parameter", name, ctx);
        }
        return Optional.of(new Expression.ParVarExpression(optType.get(), name));
    }

    @Override
    public Optional<Expression> visitAppExp(ASParser.AppExpContext ctx) {
        String operationName = ctx.opID().getText();

        Optional<Operation> optOperation = state.getOperation(operationName);
        if (!optOperation.isPresent()) {
            return valueUndefined("Operation", operationName, ctx);
        }
        Operation operation = optOperation.get();

        List<Expression> parameters = new ArrayList<>();

        if (ctx.opPars() != null) {
            Optional<List<Expression>> optParameters = listOfOptionalToOptionalOfList(ctx.opPars().opPar().stream().map(this::visit).collect(Collectors.toList()));
            if (!optParameters.isPresent()) {
                return Optional.empty();
            }
            parameters = optParameters.get();
        }

        ConstnessExpressionVisitor constnessExpressionVisitor = new ConstnessExpressionVisitor();

        for (Expression parameter : parameters) {
            if (!parameter.accept(constnessExpressionVisitor)) {
                return elementNotConst("Parameter", parameter, ctx);
            }
        }

        ZipInfo zipInfo = zipPredicateWithErrorReporting(parameters, operation.parameters, (p, o) -> p.getType().equals(o.type));
        if (!zipInfo.isSuccess()) {
            return mismatchInList("operation parameter", zipInfo, ctx);
        }

        Optional<List<Expression>> optArguments = listOfOptionalToOptionalOfList(ctx.opArgs().opArg().stream().map(this::visit).collect(Collectors.toList()));
        if (!optArguments.isPresent()) {
            return Optional.empty();
        }

        List<Expression> arguments = optArguments.get();

        zipInfo = zipPredicateWithErrorReporting(arguments, operation.arguments, (p, o) -> p.getType().equals(o.type));
        if (!zipInfo.isSuccess()) {
            return mismatchInList("operation argument", zipInfo, ctx);
        }

        return Optional.of(new Expression.AppExpression(operation, parameters, arguments));
    }

    @Override
    public Optional<Expression> visitMatchExp(ASParser.MatchExpContext ctx) {
        Optional<List<Expression>> optMatchedExpressions = listOfOptionalToOptionalOfList(ctx.exps().exp().stream().map(this::visit).collect(Collectors.toList()));

        if (!optMatchedExpressions.isPresent()) {
            return Optional.empty();
        }

        List<Expression> matchedExpressions = optMatchedExpressions.get();

        PatternVisitor visitor = new PatternVisitor(state);
        List<List<Pattern>> branches = new ArrayList<>();

        for (ASParser.PatternsContext patternsTree : ctx.patterns()) {
            Optional<List<Pattern>> optPatterns = listOfOptionalToOptionalOfList(patternsTree.pattern().stream().map(visitor::visit).collect(Collectors.toList()));
            if (!optPatterns.isPresent()) {
                return Optional.empty();
            }
            List<Pattern> patterns = optPatterns.get();
            branches.add(patterns);
        }

        int patternCount = matchedExpressions.size();
        for (List<Pattern> branch : branches) {
            if (!checkIfSizeMatches(patternCount, branch.size(), "match expression", ctx)) {
                return Optional.empty();
            }
        }

        // As of writing the grammar prevents this case from appearing
        if (!checkIfSizeMatches(branches.size() + 1, ctx.exp().size(), "branches", ctx)) {
            return Optional.empty();
        }

        // Add the catch-all pattern
        branches.add(Collections.nCopies(patternCount, new Pattern.WildcardPattern("_")));

        List<Collection<Expression.VarExpression>> variableDefinitions = new ArrayList<>();

        for (List<Pattern> branch : branches) {
            Set<Expression.VarExpression> variableDefinition = new HashSet<>();
            Iterator<Expression> matchedExpressionIterator = matchedExpressions.iterator();
            for (Pattern pattern : branch) {
                CollectVariablesPatternVisitor variablesPatternVisitor = new CollectVariablesPatternVisitor(matchedExpressionIterator.next().getType());
                variableDefinition.addAll(pattern.accept(variablesPatternVisitor));
            }
            variableDefinitions.add(variableDefinition);
        }

        Iterator<Collection<Expression.VarExpression>> variableDefinitionIterator = variableDefinitions.iterator();

        List<Expression> resultExpressions = new ArrayList<>();

        Iterator<ASParser.ExpContext> resultExpressionParseTreeIterator = ctx.exp().iterator();

        for (List<Pattern> branch : branches) {
            // Check that type of matched expression matches type of pattern
            ZipInfo<Pattern, Expression> zipInfo = zipPredicateWithErrorReporting(branch, matchedExpressions, (p, e) -> visitor.patternTypeMatches(e.getType(), p));
            if (!zipInfo.isSuccess()) {
                return patternTypeMismatch(zipInfo, ctx);
            }

            VisitorState tmpState = state;
            state = new VisitorState(state);
            Collection<Expression.VarExpression> variableDefinition = variableDefinitionIterator.next();

            // Ensure that variables only bind once
            for (Expression.VarExpression expression : variableDefinition) {
                if (!state.defineVar(expression.name, expression.type)) {
                    return elementAlreadyBound("Variable", expression.name, ctx);
                }
            }

            Optional<Expression> optExpression = visit(resultExpressionParseTreeIterator.next());

            if (!optExpression.isPresent()) {
                return Optional.empty();
            }

            resultExpressions.add(optExpression.get());

            state = tmpState;
        }

        // Check that all result expressions have the same type
        if (!checkIfExpressionResultsHaveSameType(resultExpressions, ctx)) {
            return Optional.empty();
        }

        return Optional.of(new Expression.MatchExpression(branches, matchedExpressions, resultExpressions));
    }

    private Optional<Expression> expectTypedExpression(Optional<Expression> expression, Type type, Function<Expression, Expression> constructor, ParserRuleContext ctx) {
        if (!expression.isPresent()) {
            return Optional.empty();
        }

        Optional<Expression> ret = expression.filter(exp -> exp.getType().equals(type)).map(constructor);

        if (!ret.isPresent()) {
            state.errorHandler.handleError(ErrorHelper.generateTypeMismatchError(type, expression.get().getType(), ctx));
            return Optional.empty();
        }
        return ret;
    }

    @Override
    public Optional<Expression> visitConstID(ASParser.ConstIDContext ctx) {
        String constID = ctx.CONST_ID().getText();
        Optional<Expression.ConstExpression> optExp = state.getConstant(constID);

        if (!optExp.isPresent()) {
            return valueUndefined("Constant", constID, ctx);
        }
        return Optional.of(optExp.get());
    }

    @Override
    public Optional<Expression> visitExp(ASParser.ExpContext ctx) {
        if (isBinaryIntExpression(ctx)) {
            Expression.IntOperation operation = getIntOperationFromExpContext(ctx);
            return expectTypedExpression(visitExp(ctx.exp(0)), visitExp(ctx.exp(1)), Type.Integer, Type.Integer, (e1, e2) -> new Expression.BinaryIntExpression(e1, e2, operation), ctx);
        } else if (ctx.AND() != null || ctx.OR() != null) {
            Expression.BoolOperation operation = getBoolOperationFromExpContext(ctx);
            return expectTypedExpression(visitExp(ctx.exp(0)), visitExp(ctx.exp(1)), Type.Boolean, Type.Boolean, (e1, e2) -> new Expression.BinaryBoolExpression(e1, e2, operation), ctx);
        } else if (ctx.compOP() != null) {
            Expression.CompOperation operation = getCompOperationFromExpContext(ctx);
            if (operation == Expression.CompOperation.EQ || operation == Expression.CompOperation.NEQ) {
                Optional<Expression> optExp1 = visitExp(ctx.exp(0));
                Optional<Expression> optExp2 = visitExp(ctx.exp(1));

                if (!optExp2.isPresent()) {
                    return Optional.empty();
                }

                Optional<Expression> ret1 = checkIfExpressionMatchesType(optExp1, optExp2.get().getType(), ctx);

                return ret1.map(expression -> new Expression.ComparisonExpression(expression, optExp2.get(), operation));


            } else {
                return expectTypedExpression(visitExp(ctx.exp(0)), visitExp(ctx.exp(1)), Type.Integer, Type.Integer, (e1, e2) -> new Expression.ComparisonExpression(e1, e2, operation), ctx);
            }
        } else if (ctx.NEG() != null) {
            return expectTypedExpression(visit(ctx.exp(0)), Type.Boolean, Expression.NegationExpression::new, ctx);
        } else if (ctx.BVNEG() != null) {
            return expectTypedExpression(visit(ctx.exp(0)), Type.Integer, Expression.BitvectorNegationExpression::new, ctx);
        }
        return visitChildren(ctx);
    }

    private boolean isBinaryIntExpression(ASParser.ExpContext ctx) {
        return ctx.intOPPred1() != null || ctx.intOPPred2() != null || ctx.BVAND() != null || ctx.BVXOR() != null || ctx.BVOR() != null;
    }

    @Override
    public Optional<Expression> visitConstructorAppExp(ASParser.ConstructorAppExpContext ctx) {
        String constructorName = ctx.elementID().getText();
        Optional<Type.CustomType> optType = state.getTypeForConstructor(constructorName).filter(t -> t instanceof Type.CustomType).map(t -> (Type.CustomType) t);

        if (!optType.isPresent()) {
            return valueUndefined("Constructor", constructorName, ctx);
        }

        Optional<Constructor> optConstructor = optType.get().getConstructorByName(constructorName);

        if (!optConstructor.isPresent()) {
            return valueUndefined("Constructor", constructorName, ctx);
        }

        List<Expression> expressions = new ArrayList<>();

        for (ParseTree p : ctx.exp()) {
            Optional<Expression> optExpression = visit(p);
            if (!optExpression.isPresent()) {
                return Optional.empty();
            }

            expressions.add(optExpression.get());
        }

        Constructor constructor = optConstructor.get();

        if (!checkIfSizeMatches(expressions.size(), constructor.typeParameters.size(), "constructor", ctx)) {
            return Optional.empty();
        }

        Iterator<Type> typeParameterIterator = constructor.typeParameters.iterator();
        Iterator<Expression> expressionIterator = expressions.iterator();

        while (typeParameterIterator.hasNext()) {
            Optional<Expression> ret1 = checkIfExpressionMatchesType(Optional.ofNullable(expressionIterator.next()), typeParameterIterator.next(), ctx);

            if (!ret1.isPresent()) {
                return Optional.empty();
            }
        }

        return Optional.of(new Expression.ConstructorAppExpression(optConstructor.get(), optType.get(), expressions));
    }

    @Override
    public Optional<Expression> visitFreeVar(ASParser.FreeVarContext ctx) {
        String name = ctx.VAR_ID().getText();
        Optional<VisitorState.FreeVarLookupResult> optType = state.getTypeOfFreeVar(name);

        if (!optType.isPresent()) {
            return valueUndefined("Free variable", name, ctx);
        }

        return Optional.of(new Expression.FreeVarExpression(optType.get().type, optType.get().name));
    }

    @Override
    public Optional<Expression> visitMacroVar(ASParser.MacroVarContext ctx) {
        String name = ctx.MACRO_PAR_ID().getText();

        Optional<Expression> optExp = state.getMacroVarBinding(name);

        if (!optExp.isPresent()) {
            return valueUndefined("Macro variable", name, ctx);
        }

        return optExp;
    }

    @Override
    public Optional<Expression> visitSumExp(ASParser.SumExpContext ctx) {
        VisitorState childState = new VisitorState(state);
        Optional<CompoundSelectorFunctionInvocation> optSelectorFunctionInvocation = ctx.selectorExp().accept(new CompoundSelectorFunctionInvocationVisitor(childState));

        if (!optSelectorFunctionInvocation.isPresent()) {
            return Optional.empty();
        }

        CompoundSelectorFunctionInvocation selectorFunctionInvocation = optSelectorFunctionInvocation.get();

        SumOperation operation;
        Optional<Expression> optBody;

        if (ctx.simpleSumOperation() != null) {
            operation = getSumOperationFromExpContext(ctx.simpleSumOperation());
            optBody = ctx.simpleSumOperation().exp().accept(new ExpressionVisitor(childState));
        } else {
            String typeName = ctx.customSumOperation().type().getText();

            Optional<? extends Type> optType = state.getType(typeName);

            if (!optType.isPresent()) {
                return valueUndefined("Type", typeName, ctx);
            }

            Type type = optType.get();

            // This is the start value of the iteration, it neither can use the bound parameters, nor the variable introduced
            // by the custom operation. Therefore we use state instead of childState here
            Optional<Expression> optStartValue = ctx.customSumOperation().exp(1).accept(new ExpressionVisitor(state));
            optStartValue = checkIfExpressionMatchesType(optStartValue, type, ctx);

            if (!optStartValue.isPresent()) {
                return Optional.empty();
            }

            //This is the body, here we can use the bound parameters and the variable introduced by the binding
            Expression.VarExpression boundVariable = new Expression.VarExpression(type, ctx.customSumOperation().ID().getText());
            if (!childState.defineVar(boundVariable)) {
                return elementAlreadyBound("Variable bound in SumExpression", boundVariable.name, ctx);
            }
            optBody = ctx.customSumOperation().exp(0).accept(new ExpressionVisitor(childState));

            operation = new SumOperation.CustomSumOperation(boundVariable, optStartValue.get());
        }

        return checkIfExpressionMatchesType(optBody, operation.getType(), ctx).map(body -> new Expression.SumExpression(selectorFunctionInvocation, body, operation));
    }

    @Override
    public Optional<Expression> visitSimpleSumOperation(ASParser.SimpleSumOperationContext ctx) {
        return super.visitSimpleSumOperation(ctx);
    }

    private SumOperation getSumOperationFromExpContext(ASParser.SimpleSumOperationContext ctx) {
        if (ctx.AND() != null) {
            return SumOperation.AND;
        } else if (ctx.PLUS() != null) {
            return SumOperation.ADD;
        } else if (ctx.MUL() != null) {
            return SumOperation.MUL;
        } else if (ctx.OR() != null) {
            return SumOperation.OR;
        }
        throw new IllegalArgumentException("ExpContext has to be a binary SumExpression!");
    }

    private Expression.IntOperation getIntOperationFromExpContext(ASParser.ExpContext ctx) {
        if (ctx.intOPPred1() != null) {
            if (ctx.intOPPred1().DIV() != null) {
                return Expression.IntOperation.DIV;
            } else if (ctx.intOPPred1().MUL() != null) {
                return Expression.IntOperation.MUL;
            } else if (ctx.intOPPred1().MOD() != null) {
                return Expression.IntOperation.MOD;
            }
            throw new RuntimeException("Unreachable code!");
        } else if (ctx.intOPPred2() != null) {
            if (ctx.intOPPred2().PLUS() != null) {
                return Expression.IntOperation.ADD;
            } else if (ctx.intOPPred2().MINUS() != null) {
                return Expression.IntOperation.SUB;
            }
            throw new RuntimeException("Unreachable code!");
        } else if (ctx.BVAND() != null) {
            return Expression.IntOperation.BVAND;
        } else if (ctx.BVXOR() != null) {
            return Expression.IntOperation.BVXOR;
        } else if (ctx.BVOR() != null) {
            return Expression.IntOperation.BVOR;
        }
        throw new IllegalArgumentException("ExpContext has to be a binary IntegerExpression!");
    }

    private Expression.BoolOperation getBoolOperationFromExpContext(ASParser.ExpContext ctx) {
        if (ctx.AND() != null) {
            return Expression.BoolOperation.AND;
        } else if (ctx.OR() != null) {
            return Expression.BoolOperation.OR;
        }
        throw new IllegalArgumentException("ExpContext has to be a binary BooleanExpression!");
    }

    private Expression.CompOperation getCompOperationFromExpContext(ASParser.ExpContext ctx) {
        if (ctx.compOP().EQ() != null) {
            return Expression.CompOperation.EQ;
        } else if (ctx.compOP().NEQ() != null) {
            return Expression.CompOperation.NEQ;
        } else if (ctx.compOP().GE() != null) {
            return Expression.CompOperation.GE;
        } else if (ctx.compOP().LE() != null) {
            return Expression.CompOperation.LE;
        } else if (ctx.compOP().getText().equals(">")) {
            return Expression.CompOperation.GT;
        } else if (ctx.compOP().getText().equals("<")) {
            return Expression.CompOperation.LT;
        }
        throw new IllegalArgumentException("ExpContext has to be a binary ComparisonExpression!");
    }

    private Optional<Expression> expectTypedExpression(Optional<Expression> expression1, Optional<Expression> expression2, Type type1, Type type2, BiFunction<Expression, Expression, Expression> constructor, ParserRuleContext ctx) {

        Optional<Expression> ret1 = checkIfExpressionMatchesType(expression1, type1, ctx);
        Optional<Expression> ret2 = checkIfExpressionMatchesType(expression2, type2, ctx);

        if (!allPresent(ret1, ret2)) {
            return Optional.empty();
        }

        return Optional.of(constructor.apply(ret1.get(), ret2.get()));
    }

    private Optional<Expression> checkIfExpressionMatchesType(Optional<Expression> expression, Type expectedType, ParserRuleContext ctx) {
        if (!expression.isPresent()) {
            return Optional.empty();
        }

        Optional<Expression> ret = expression.filter(exp -> exp.getType().equals(expectedType));

        if (!ret.isPresent()) {
            reportTypeMismatch(expression.get().getType(), expectedType, ctx);
            return Optional.empty();
        }
        return ret;
    }

    private void reportTypeMismatch(Type actualType, Type expectedType, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateTypeMismatchError(actualType, expectedType, ctx));
    }

    private Optional<Expression> checkIfExpressionTypeIsInstanceOf(Optional<Expression> expression, Class<?> requiredClass, ParserRuleContext ctx) {
        if (!expression.isPresent()) {
            return Optional.empty();
        }

        Optional<Expression> ret = expression.filter(exp -> requiredClass.isInstance(exp.getType()));

        if (!ret.isPresent()) {
            // TODO: error reporting - report as type mismatch error when the method is refactored
            state.errorHandler.handleError(ErrorHelper.generateIsNotInstanceOfError(expression.get().getType(), requiredClass, ctx));
            return Optional.empty();
        }
        return ret;
    }

    private Optional<Expression> valueUndefined(String element, String name, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, ctx));
        return Optional.empty();
    }

    private Optional<Expression> mismatchInList(String type, ZipInfo zipInfo, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateMismatchInListError(type, zipInfo, ctx));
        return Optional.empty();
    }


    private Optional<Expression> elementAlreadyBound(String element, String name, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementAlreadyBoundError(element, name, ctx));
        return Optional.empty();
    }

    private Optional<Expression> elementNotConst(String var, Expression expression, ParserRuleContext ctx) {
        state.errorHandler.handleError(ErrorHelper.generateElementNotConstError(var, expression.getType(), ctx));
        return Optional.empty();
    }

    private boolean checkIfSizeMatches(int expectedSize, int actualSize, String location, ParserRuleContext ctx) {
        if (actualSize != expectedSize) {
            state.errorHandler.handleError(ErrorHelper.generateSizeDoesntMatchError(expectedSize, actualSize, location, ctx));
            return false;
        }
        return true;
    }

    private boolean checkIfExpressionResultsHaveSameType(List<Expression> expressions, ParserRuleContext ctx) {
        Type type = expressions.get(0).getType();

        for (Expression e : expressions) {
            if (!e.getType().equals(type)) {
                state.errorHandler.handleError(ErrorHelper.generateExpressionResultsTypesDontMatchError(e.getType(), type, ctx));
                return false;
            }
        }
        return true;
    }

    private Optional<Expression> patternTypeMismatch(ZipInfo<Pattern, Expression> zipInfo, ParserRuleContext ctx) {
        String patternName = zipInfo.getFirstElem().accept(new PatternDisplayStringVisitor());
        Type typeName = zipInfo.getSecondElem().getType();

        state.errorHandler.handleError(ErrorHelper.generatePatternTypeMismatchError(patternName, typeName.name, ctx));
        return Optional.empty();
    }

}
