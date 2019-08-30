package secpriv.horst.data;

import secpriv.horst.internals.SelectorFunctionInvoker;
import secpriv.horst.tools.MapHelper;
import secpriv.horst.translation.layout.TypeLayouter;
import secpriv.horst.translation.visitors.AbstractExpressionVisitor;
import secpriv.horst.translation.visitors.InlineTypesExpressionVisitor;
import secpriv.horst.translation.visitors.InstantiateParametersExpressionVisitor;
import secpriv.horst.types.Type;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SumOperation {
    public final Expression startElement;

    private SumOperation(Expression startElement) {
        this.startElement = Objects.requireNonNull(startElement, "StartElement may not be null");
    }

    public abstract Expression apply(CompoundSelectorFunctionInvocation invocation, SelectorFunctionInvoker selectorFunctionInvoker, Map<String, BaseTypeValue> parameterMap, Expression body);

    public interface Visitor<T> {
        T visit(CustomSumOperation sumOperation);

        T visit(SimpleSumOperation sumOperation);

        T visit(InlinedCustomSumOperation sumOperation);
    }

    public abstract SumOperation mapSubExpressions(Function<Expression, Expression> function);

    public abstract Type getType();

    public abstract <T> T accept(Visitor<T> visitor);

    public static SumOperation ADD = new SimpleSumOperation((x, y) -> new Expression.BinaryIntExpression(x, y, Expression.IntOperation.ADD), Type.Integer, new Expression.IntConst(BigInteger.ZERO), "ADD");
    public static SumOperation MUL = new SimpleSumOperation((x, y) -> new Expression.BinaryIntExpression(x, y, Expression.IntOperation.MUL), Type.Integer, new Expression.IntConst(BigInteger.ONE), "MUL");
    public static SumOperation AND = new SimpleSumOperation((x, y) -> new Expression.BinaryBoolExpression(x, y, Expression.BoolOperation.AND), Type.Boolean, Expression.BoolConst.TRUE, "AND");
    public static SumOperation OR = new SimpleSumOperation((x, y) -> new Expression.BinaryBoolExpression(x, y, Expression.BoolOperation.OR), Type.Boolean, Expression.BoolConst.FALSE, "OR");

    public static class SimpleSumOperation extends SumOperation {
        private final BiFunction<Expression, Expression, Expression> combinator;
        private final Type type;
        private final String toString;

        private SimpleSumOperation(BiFunction<Expression, Expression, Expression> combinator, Type type, Expression startElement, String toString) {
            super(startElement);
            this.combinator = Objects.requireNonNull(combinator, "Combinator may not be null");
            this.type = Objects.requireNonNull(type, "Type may not be null!");
            this.toString = toString;
        }

        @Override
        public Expression apply(CompoundSelectorFunctionInvocation invocation, SelectorFunctionInvoker selectorFunctionInvoker, Map<String, BaseTypeValue> parameterMap, Expression body) {
            Expression ret = startElement;

            for (Map<String, BaseTypeValue> currentParameterMap : selectorFunctionInvoker.invoke(parameterMap, invocation)) {
                Expression newPartExpression = body.accept(new InstantiateParametersExpressionVisitor(MapHelper.joinDistinct(parameterMap, currentParameterMap), selectorFunctionInvoker));
                ret = combinator.apply(ret, newPartExpression);
            }
            return ret;
        }

        @Override
        public SumOperation mapSubExpressions(Function<Expression, Expression> function) {
            return this;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return toString;
        }
    }

    private static class CustomSumOperationFamilyCache {
        private final Set<InlinedCustomSumOperation> familyMembers = new HashSet<>();
        private final Map<Map<String, BaseTypeValue>, Map<InlinedCustomSumOperation, Expression>> invocationCache = new HashMap<>();

        private void addToFamily(InlinedCustomSumOperation sumOperation) {
            familyMembers.add(sumOperation);
        }

        public Expression apply(InlinedCustomSumOperation sumOperation, CompoundSelectorFunctionInvocation invocation, SelectorFunctionInvoker selectorFunctionInvoker, Map<String, BaseTypeValue> parameterMap) {
            if (invocationCache.get(parameterMap) == null) {
                fillInvocationCache(invocation, selectorFunctionInvoker, parameterMap);

            }
            return invocationCache.computeIfAbsent(parameterMap, s -> {
                throw new NoSuchElementException();
            }).computeIfAbsent(sumOperation, s -> {
                throw new NoSuchElementException();
            });
        }

        private void fillInvocationCache(CompoundSelectorFunctionInvocation invocation, SelectorFunctionInvoker selectorFunctionInvoker, Map<String, BaseTypeValue> parameterMap) {
            if (invocationCache.get(parameterMap) != null) {
                throw new IllegalStateException("Invocation cache can only be filled once!");
            }

            Map<InlinedCustomSumOperation, Expression> invocationCacheForParameterMap = new HashMap<>();
            Set<Expression.VarExpression> boundVariables = familyMembers.stream().map(o -> o.boundVariable).collect(Collectors.toSet());
            Map<Expression.VarExpression, InlinedCustomSumOperation> boundVariablesToOperations = new HashMap<>();

            for (InlinedCustomSumOperation o : familyMembers) {
                invocationCacheForParameterMap.put(o, o.startElement.accept(new InstantiateParametersExpressionVisitor(parameterMap, selectorFunctionInvoker)));
                boundVariablesToOperations.put(o.boundVariable, o);
            }

            class ReplacingExpressionVisitor extends AbstractExpressionVisitor {
                @Override
                public Expression visit(Expression.VarExpression expression) {
                    if (boundVariables.contains(expression)) {
                        return invocationCacheForParameterMap.computeIfAbsent(boundVariablesToOperations.get(expression), s -> {
                            throw new NoSuchElementException();
                        });
                    }
                    return expression;
                }
            }

            ReplacingExpressionVisitor replacingExpressionVisitor = new ReplacingExpressionVisitor();

            for (Map<String, BaseTypeValue> currentParameterMap : selectorFunctionInvoker.invoke(parameterMap, invocation)) {
                Map<InlinedCustomSumOperation, Expression> newPartExpressions = new HashMap<>();
                for (InlinedCustomSumOperation o : familyMembers) {
                    Expression newPartExpression = o.body.accept(new InstantiateParametersExpressionVisitor(MapHelper.joinDistinct(parameterMap, currentParameterMap), selectorFunctionInvoker));
                    newPartExpressions.put(o, newPartExpression.accept(replacingExpressionVisitor));
                }

                for (Map.Entry<InlinedCustomSumOperation, Expression> entry : newPartExpressions.entrySet()) {
                    invocationCacheForParameterMap.put(entry.getKey(), entry.getValue());
                }
            }

            invocationCache.put(parameterMap, invocationCacheForParameterMap);
        }
    }

    public static class InlinedCustomSumOperation extends SumOperation {
        public final Expression.VarExpression boundVariable;
        public final Expression body;
        private final CustomSumOperationFamilyCache cache;

        private InlinedCustomSumOperation(Expression.VarExpression boundVariable, Expression startElement, Expression body, CustomSumOperationFamilyCache cache) {
            super(startElement);
            this.boundVariable = boundVariable;
            this.body = body;
            this.cache = cache;
            cache.addToFamily(this);
        }

        @Override
        public Expression apply(CompoundSelectorFunctionInvocation invocation, SelectorFunctionInvoker selectorFunctionInvoker, Map<String, BaseTypeValue> parameterMap, Expression body) {
            return cache.apply(this, invocation, selectorFunctionInvoker, parameterMap);
        }

        @Override
        public SumOperation mapSubExpressions(Function<Expression, Expression> function) {
            throw new UnsupportedOperationException("I'm not sure if this has proper semantics. Throw error just in case!");
        }

        @Override
        public Type getType() {
            return boundVariable.type;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CustomSumOperation extends SumOperation {
        public final Expression.VarExpression boundVariable;

        public CustomSumOperation(Expression.VarExpression boundVariable, Expression startElement) {
            super(startElement);
            this.boundVariable = boundVariable;
        }

        @Override
        public Expression apply(CompoundSelectorFunctionInvocation invocation, SelectorFunctionInvoker selectorFunctionInvoker, Map<String, BaseTypeValue> parameterMap, Expression body) {
            Expression ret = startElement.accept(new InstantiateParametersExpressionVisitor(parameterMap, selectorFunctionInvoker));

            for (Map<String, BaseTypeValue> currentParameterMap : selectorFunctionInvoker.invoke(parameterMap, invocation)) {
                Expression newPartExpression = body.accept(new InstantiateParametersExpressionVisitor(MapHelper.joinDistinct(parameterMap, currentParameterMap), selectorFunctionInvoker));
                ret = combine(ret, newPartExpression);
            }
            return ret;
        }

        @Override
        public SumOperation mapSubExpressions(Function<Expression, Expression> function) {
            Expression mappedStartElement = function.apply(this.startElement);
            Expression.VarExpression mappedBoundVariable = (Expression.VarExpression) function.apply(this.boundVariable);

            return new CustomSumOperation(mappedBoundVariable, mappedStartElement);
        }

        @Override
        public Type getType() {
            return boundVariable.type;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        private Expression combine(Expression accumulator, Expression body) {
            class ReplacingExpressionVisitor extends AbstractExpressionVisitor {
                @Override
                public Expression visit(Expression.VarExpression expression) {
                    if (expression.equals(boundVariable)) {
                        return accumulator;
                    }
                    return expression;
                }
            }
            return body.accept(new ReplacingExpressionVisitor());
        }


        @Override
        public String toString() {
            return "CUSTOM(" + boundVariable.name + ":" + boundVariable.type + ")";
        }

        public List<InlinedCustomSumOperation> splitToFamilyOfOperations(TypeLayouter typeLayouter, Expression body, Map<String, List<Expression>> varBindings) {
            // First of all: sorry.
            // Second: match operations in custom sum operations are tricky, because we may need the primitive representation of one unfolded "expression branch"
            // in the iteration of another branch. Since, as things are right now, instantiation (which includes sum operation unrolling) has to be done after type inlining
            // we have to somehow connect the different primitive sum operations with each other. This is done via a shared cache, which is filled once for every
            // parameter mapping and then referred to, when the primitive sum operations are instantiated.

            int i = 0;
            List<Expression.VarExpression> boundVariables = new ArrayList<>();
            for (Type type : typeLayouter.unfoldToBaseTypes(getType())) {
                Expression.VarExpression boundVariable = new Expression.VarExpression(type, this.boundVariable.name + "&&s" + (i++));
                boundVariables.add(boundVariable);
            }

            HashMap<String, List<Expression>> childVarBindings = new HashMap<>(varBindings);
            childVarBindings.put(this.boundVariable.name, Collections.unmodifiableList(boundVariables));

            InlineTypesExpressionVisitor inlineTypesExpressionVisitor = new InlineTypesExpressionVisitor(typeLayouter, childVarBindings);
            Iterator<Expression> startElementIterator = startElement.accept(inlineTypesExpressionVisitor).iterator();
            Iterator<Expression> bodyIterator = body.accept(inlineTypesExpressionVisitor).iterator();

            List<SumOperation.InlinedCustomSumOperation> family = new ArrayList<>();
            CustomSumOperationFamilyCache cache = new CustomSumOperationFamilyCache();

            for (Expression.VarExpression boundVariable : boundVariables) {
                family.add(new SumOperation.InlinedCustomSumOperation(boundVariable, startElementIterator.next(), bodyIterator.next(), cache));
            }

            return family;
        }
    }
}
