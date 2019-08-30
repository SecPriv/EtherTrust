package secpriv.horst.translation.layout;

import secpriv.horst.data.Expression;
import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;

import java.util.List;

public interface TypeLayouter {
    List<Type> unfoldToBaseTypes(Type type);

    List<Expression> layoutExpression(Expression.ConstructorAppExpression e, List<List<Expression>> laidOutSubExpression);

    List<List<Expression>> getFlattenedSubExpressions(Type.CustomType type, Constructor constructor, List<Expression> matchedExpression);

    Expression getSelectExpression(Type type, Constructor constructor, List<Expression> matchedExpression);

    List<Expression> translateFreeVars(Expression.FreeVarExpression expression);
}
