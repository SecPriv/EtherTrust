package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Pattern;
import secpriv.horst.translation.layout.TypeLayouter;
import secpriv.horst.types.Type;

import java.util.Iterator;
import java.util.List;

public class GenerateMatchConditionVisitor implements Pattern.Visitor<Expression> {
    private final List<Expression> matchedExpression;
    private final TypeLayouter typeLayouter;
    private final Type type;

    public GenerateMatchConditionVisitor(List<Expression> matchedExpression, Type type, TypeLayouter typeLayouter) {
        this.matchedExpression = matchedExpression;
        this.typeLayouter = typeLayouter;
        this.type = type;
    }

    @Override
    public Expression visit(Pattern.ValuePattern pattern) {
        class PatternSubExpressionVisitor implements Type.Visitor<Expression> {
            @Override
            public Expression visit(Type.BooleanType type) {
                return typeLayouter.getSelectExpression(type, pattern.constructor, matchedExpression);
            }

            @Override
            public Expression visit(Type.IntegerType type) {
                return typeLayouter.getSelectExpression(type, pattern.constructor, matchedExpression);
            }

            @Override
            public Expression visit(Type.CustomType type) {
                List<List<Expression>> flattenedSubExpressions = typeLayouter.getFlattenedSubExpressions(type, pattern.constructor, matchedExpression);
                Iterator<Type> typeIterator = pattern.constructor.typeParameters.iterator();
                Iterator<Pattern> patternIterator = pattern.patterns.iterator();

                Expression ret = typeLayouter.getSelectExpression(type, pattern.constructor, matchedExpression);

                for (List<Expression> subExpressions : flattenedSubExpressions) {
                    GenerateMatchConditionVisitor generateMatchConditionVisitor = new GenerateMatchConditionVisitor(subExpressions, typeIterator.next(), typeLayouter);
                    ret = new Expression.BinaryBoolExpression(ret, patternIterator.next().accept(generateMatchConditionVisitor), Expression.BoolOperation.AND);
                }

                return ret;
            }

            @Override
            public Expression visit(Type.ArrayType type) {
                throw new UnsupportedOperationException("Cannot match array types!");
            }
        }

        return type.accept(new PatternSubExpressionVisitor());
    }

    @Override
    public Expression visit(Pattern.WildcardPattern pattern) {
        return Expression.BoolConst.TRUE;
    }
}
