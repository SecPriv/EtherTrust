package secpriv.horst.internals;

import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.CompoundSelectorFunctionInvocation;
import secpriv.horst.data.SelectorFunctionInvocation;
import secpriv.horst.data.tuples.Tuple;
import secpriv.horst.tools.CartesianHelper;
import secpriv.horst.tools.MapHelper;
import secpriv.horst.tools.MappingIterator;
import secpriv.horst.translation.visitors.EvaluateExpressionVisitor;
import secpriv.horst.translation.visitors.ToObjectBaseTypeValueVisitor;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class SelectorFunctionInvoker {
    private final SelectorFunctionHelper selectorFunctionHelper;

    public SelectorFunctionInvoker(SelectorFunctionHelper selectorFunctionHelper) {
        this.selectorFunctionHelper = Objects.requireNonNull(selectorFunctionHelper, "SelectorFunctionHelper may not be null!");
    }

    public Iterable<Map<String, BaseTypeValue>> invoke(CompoundSelectorFunctionInvocation invocation) {
        return invoke(Collections.emptyMap(), selectorFunctionHelper, invocation.selectorFunctionInvocations);
    }

    public Iterable<Map<String, BaseTypeValue>> invoke(Map<String, BaseTypeValue> parameterMap, CompoundSelectorFunctionInvocation invocation) {
        return invoke(parameterMap, selectorFunctionHelper, invocation.selectorFunctionInvocations);
    }

    private static Iterable<Map<String, BaseTypeValue>> invoke(Map<String, BaseTypeValue> parameterMap, SelectorFunctionHelper selectorFunctionHelper, List<SelectorFunctionInvocation> invocations) {
        if (invocations.isEmpty()) {
            return Collections.singletonList(Collections.emptyMap());
        }

        return () -> new MappingIterator<>(
                CartesianHelper.dependentProduct(
                        invoke(parameterMap, selectorFunctionHelper, invocations.get(0)),
                        x -> invoke(MapHelper.joinDistinct(parameterMap, x), selectorFunctionHelper, invocations.subList(1, invocations.size()))
                ).iterator(),
                x -> MapHelper.joinDistinct(x.v0, x.v1));
    }

    private static Iterable<Map<String, BaseTypeValue>> invoke(Map<String, BaseTypeValue> parameterMap, SelectorFunctionHelper selectorFunctionHelper, SelectorFunctionInvocation invocation) {
        try {
            Method method = selectorFunctionHelper.getMethod(invocation.selectorFunction);
            Object provider = selectorFunctionHelper.getProvider(invocation.selectorFunction);

            EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor(parameterMap);
            Object o = method.invoke(provider, invocation.arguments.stream().map(e -> e.accept(evaluateExpressionVisitor).accept(new ToObjectBaseTypeValueVisitor())).toArray());
            List<String> names = invocation.parameters.stream().map(p -> p.name).collect(Collectors.toList());

            return resultToBindings(o, names);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Iterable<Map<String, BaseTypeValue>> resultToBindings(Object result, List<String> names) {
        List<String> unmodifiableNames = Collections.unmodifiableList(new ArrayList<>(names));
        return () -> new MappingIterator<>(((Iterable<Object>) result).iterator(), o -> bindToNames(o, unmodifiableNames));
    }

    private static Map<String, BaseTypeValue> bindToNames(Object o, List<String> names) {
        if (names.size() == 1) {
            if (!(o instanceof Boolean || o instanceof BigInteger)) {
                throw new IllegalArgumentException("When binding to one name, the result type has to be either Boolean or BigInteger!");
            }
            return Collections.singletonMap(names.get(0), BaseTypeValue.unsafeFromObject(o));
        } else {
            if (!(o instanceof Tuple)) {
                throw new IllegalArgumentException("When binding to more than one name (or not binding any names), the result type has to be an implementation of Tuple!");
            }
            return ((Tuple) o).bindToNames(names);
        }
    }
}
