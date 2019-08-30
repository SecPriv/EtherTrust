package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Pattern;
import secpriv.horst.translation.layout.TypeLayouter;
import secpriv.horst.types.Type;

import java.util.*;

public class GenerateBindingPatternVisitor implements Pattern.Visitor<Map<String, List<Expression>>> {
    private final List<Expression> matchedExpression;
    private final TypeLayouter typeLayouter;
    private final Type type;
    private static final String WILDCARD = "_";

    public GenerateBindingPatternVisitor(List<Expression> matchedExpression, Type type, TypeLayouter typeLayouter) {
        this.matchedExpression = matchedExpression;
        this.typeLayouter = typeLayouter;
        this.type = type;
    }

    @Override
    public Map<String, List<Expression>> visit(Pattern.ValuePattern pattern) {
        class PatternSubExpressionVisitor implements Type.Visitor<Map<String, List<Expression>>> {
            @Override
            public Map<String, List<Expression>> visit(Type.BooleanType type) {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, List<Expression>> visit(Type.IntegerType type) {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, List<Expression>> visit(Type.CustomType type) {
                List<List<Expression>> flattenedSubExpressions = typeLayouter.getFlattenedSubExpressions(type, pattern.constructor, matchedExpression);
                Iterator<Type> typeIterator = pattern.constructor.typeParameters.iterator();
                Iterator<Pattern> patternIterator = pattern.patterns.iterator();

                Map<String, List<Expression>> ret = new HashMap<>();
                for (List<Expression> subExpressions : flattenedSubExpressions) {
                    GenerateBindingPatternVisitor generateBindingPatternVisitor = new GenerateBindingPatternVisitor(subExpressions, typeIterator.next(), typeLayouter);
                    ret.putAll(patternIterator.next().accept(generateBindingPatternVisitor));
                }

                return ret;
            }

            @Override
            public  Map<String, List<Expression>> visit(Type.ArrayType type) {
                throw new UnsupportedOperationException("Cannot match on array types!");
            }
        }

        return type.accept(new PatternSubExpressionVisitor());
    }

    @Override
    public Map<String, List<Expression>> visit(Pattern.WildcardPattern pattern) {
        if (pattern.name.equals(WILDCARD)) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(pattern.name, matchedExpression);
    }
}
