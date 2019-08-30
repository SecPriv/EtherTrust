package secpriv.horst.visitors;

import secpriv.horst.data.Expression;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterExpressionVisitor implements Expression.Visitor<List<Expression>> {
    private final Predicate<Expression> predicate;

    public FilterExpressionVisitor(Predicate<Expression> predicate) {
        this.predicate = predicate;
    }

    @Override
    public List<Expression> visit(Expression.IntConst expression) {
        if(predicate.test(expression)) {
            return Collections.singletonList(expression);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Expression> visit(Expression.BoolConst expression) {
        if(predicate.test(expression)) {
            return Collections.singletonList(expression);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Expression> visit(Expression.ArrayInitExpression expression) {
        List<Expression> ret = Collections.emptyList();
        if(predicate.test(expression)) {
            ret = Collections.singletonList(expression);
        }

        return Stream.concat(ret.stream(), expression.initializer.accept(this).stream()).collect(Collectors.toList());
    }

    @Override
    public List<Expression> visit(Expression.VarExpression expression) {
        if(predicate.test(expression)) {
            return Collections.singletonList(expression);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Expression> visit(Expression.FreeVarExpression expression) {
        if(predicate.test(expression)) {
            return Collections.singletonList(expression);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Expression> visit(Expression.ParVarExpression expression) {
        if(predicate.test(expression)) {
            return Collections.singletonList(expression);
        }
        return Collections.emptyList();
    }

    private List<Expression> visitBinaryExpression(Expression.BinaryExpression expression) {
        List<Expression> ret = Collections.emptyList();
        if(predicate.test(expression)) {
            ret = Collections.singletonList(expression);
        }

        return Stream.concat(ret.stream(), Stream.concat(expression.expression1.accept(this).stream(), expression.expression2.accept(this).stream())).collect(Collectors.toList());
    }

    private List<Expression> visitTernaryExpression(Expression.TernaryExpression expression) {
        List<Expression> ret = Collections.emptyList();
        if(predicate.test(expression)) {
            ret = Collections.singletonList(expression);
        }

        return Stream.concat(ret.stream(), Stream.concat(expression.expression1.accept(this).stream(), Stream.concat(expression.expression2.accept(this).stream(), expression.expression3.accept(this).stream()))).collect(Collectors.toList());
    }
    @Override
    public List<Expression> visit(Expression.BinaryIntExpression expression) {
        return visitBinaryExpression(expression);
    }

    @Override
    public List<Expression> visit(Expression.BinaryBoolExpression expression) {
        return visitBinaryExpression(expression);
    }

    @Override
    public List<Expression> visit(Expression.SelectExpression expression) {
        return visitBinaryExpression(expression);
    }

    @Override
    public List<Expression> visit(Expression.StoreExpression expression) {
        return visitTernaryExpression(expression);
    }

    private List<Expression> visitVariadicExpression(Expression.VariadicExpression expression) {
        List<Expression> ret = Collections.emptyList();
        if(predicate.test(expression)) {
            ret = Collections.singletonList(expression);
        }
        Stream<Expression> s = ret.stream();
        for(Expression e : expression.expressions) {
            s = Stream.concat(s, e.accept(this).stream());
        }

        return s.collect(Collectors.toList());
    }

    @Override
    public List<Expression> visit(Expression.AppExpression expression) {
        return visitVariadicExpression(expression);
    }

    @Override
    public List<Expression> visit(Expression.ConstructorAppExpression expression) {
        return visitVariadicExpression(expression);
    }

    @Override
    public List<Expression> visit(Expression.MatchExpression expression) {
        Stream<Expression> s = Stream.empty();
        if(predicate.test(expression)) {
            s = Stream.concat(s, Stream.of(expression));
        }
        for(Expression e : expression.matchedExpressions) {
            s = Stream.concat(s, e.accept(this).stream());
        }
        for(Expression e : expression.resultExpressions) {
            s = Stream.concat(s, e.accept(this).stream());
        }

        return s.collect(Collectors.toList());
    }

    @Override
    public List<Expression> visit(Expression.NegationExpression expression) {
        if(predicate.test(expression)) {
            return Collections.singletonList(expression);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Expression> visit(Expression.BitvectorNegationExpression expression) {
        if(predicate.test(expression)) {
            return Collections.singletonList(expression);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Expression> visit(Expression.ConditionalExpression expression) {
        return visitTernaryExpression(expression);
    }

    @Override
    public List<Expression> visit(Expression.ComparisonExpression expression) {
        return visitBinaryExpression(expression);
    }

    @Override
    public List<Expression> visit(Expression.ConstExpression expression) {
        if(predicate.test(expression)) {
            return Collections.singletonList(expression);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Expression> visit(Expression.SumExpression expression) {
        Stream<Expression> s = Stream.empty();
        if(predicate.test(expression)) {
            s = Stream.concat(s, Stream.of(expression));
        }
        for(Expression e : expression.selectorFunctionInvocation.parameters()) {
            s = Stream.concat(s, e.accept(this).stream());
        }
        for(Expression e : expression.selectorFunctionInvocation.arguments()) {
            s = Stream.concat(s, e.accept(this).stream());
        }
        s = Stream.concat(s, expression.body.accept(this).stream());

        return s.collect(Collectors.toList());
    }
}
