package secpriv.horst.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Pattern;
import secpriv.horst.types.Type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static secpriv.horst.tools.Zipper.zipList;

public class CollectVariablesPatternVisitor implements Pattern.Visitor<Collection<Expression.VarExpression>> {
    private final Type expectedType;

    public CollectVariablesPatternVisitor(Type expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public Collection<Expression.VarExpression> visit(Pattern.ValuePattern pattern) {
        final List<Pattern> subPatterns = pattern.patterns;
        final List<Type> expectedTypes = pattern.constructor.typeParameters;

        return zipList(expectedTypes, subPatterns,
                (type, subPattern) -> subPattern.accept(new CollectVariablesPatternVisitor(type)))
                .stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public Collection<Expression.VarExpression> visit(Pattern.WildcardPattern pattern) {
        if (pattern.name.equals("_")) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(new Expression.VarExpression(expectedType, pattern.name));
        }
    }
}
