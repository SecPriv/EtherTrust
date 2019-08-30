package secpriv.horst.translation.layout;

import secpriv.horst.data.Expression;
import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// This Layouter layouts a type as follows:
//      An Integer is mapped to an Integer
//      A Boolean is mapped to a Boolean
//      A custom type is mapped to an Integer (which signifies the constructor of the expression) and a all the
//        flattened (i.e. laid out with this Layouter) type parameters for each constructor, concatenated.
//
// For example:
//
// data Union = @BoolCell bool | @IntCell int;
//
// becomes
//
// [No. of Constructor] [type parameter of @BoolCell] [type parameter of @IntCell]
// Integer              Boolean                       Integer
//
// data UnionSmallSet = @Empty | @OneElement Union | @TwoElement Union Union;
//
// becomes
//
// [No. of Constructor] [type parameter of @OneElement] [first type parameter of @TwoElement] [second type parameter of @TwoElement]
// Integer              Integer Boolean Integer         Integer Boolean Integer               Integer Boolean Integer
//
public class FlatTypeLayouter implements TypeLayouter {
    public List<Type> unfoldToBaseTypes(Type type) {
        class UnfoldToBaseTypesVisitor implements Type.Visitor<List<Type>> {
            @Override
            public List<Type> visit(Type.BooleanType type) {
                return Collections.singletonList(type);
            }

            @Override
            public List<Type> visit(Type.IntegerType type) {
                return Collections.singletonList(type);
            }

            @Override
            public List<Type> visit(Type.CustomType type) {
                return Stream.concat(Stream.of(Type.Integer), //select constructor
                        type.constructors.stream().flatMap(c -> c.typeParameters.stream().flatMap(t -> unfoldToBaseTypes(t).stream()))).collect(Collectors.toList());
            }

            @Override
            public List<Type> visit(Type.ArrayType type) {
                return unfoldToBaseTypes(type.type).stream().map(Type.Array::of).collect(Collectors.toList());
            }
        }

        return type.accept(new UnfoldToBaseTypesVisitor());
    }

    @Override
    public List<Expression> layoutExpression(Expression.ConstructorAppExpression expression, List<List<Expression>> laidOutSubExpressions) {
        Type.CustomType type = (Type.CustomType) expression.getType();
        Constructor constructor = expression.constructor;
        List<Expression> subExpressions = laidOutSubExpressions.stream().flatMap(Collection::stream).collect(Collectors.toList());

        Expression.IntConst selector = new Expression.IntConst(BigInteger.valueOf(getConstructorIndex(type, constructor)));
        int offset = getConstructorOffset(type, constructor);
        int baseTypeParameterCount = getConstructorBaseTypeParameterCount(constructor);

        List<Expression> dummy = getDummyForType(type);

        Iterator<Expression> subExpressionIterator = subExpressions.iterator();

        dummy.set(0, selector);
        for (int i = offset; i < offset + baseTypeParameterCount; ++i) {
            dummy.set(i, subExpressionIterator.next());
        }

        return dummy;
    }

    @Override
    public List<List<Expression>> getFlattenedSubExpressions(Type.CustomType type, Constructor constructor, List<Expression> matchedExpression) {
        if (constructor.typeParameters.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Expression>> ret = new ArrayList<>();
        int offset = getConstructorOffset(type, constructor);
        for (Type subType : constructor.typeParameters) {
            int baseTypeCount = getDummyForType(subType).size();
            ret.add(matchedExpression.subList(offset, offset + baseTypeCount));
            offset += baseTypeCount;
        }

        return ret;
    }

    @Override
    public Expression getSelectExpression(Type type, Constructor constructor, List<Expression> matchedExpression) {
        class SelectExpressionVisitor implements Type.Visitor<Expression> {
            @Override
            public Expression visit(Type.BooleanType type) {
                switch (constructor.name) {
                    case "true":
                        return matchedExpression.get(0);
                    case "false":
                        return new Expression.NegationExpression(matchedExpression.get(0));
                    default:
                        throw new IllegalArgumentException("Constructor " + constructor + " does not match type Boolean!");
                }
            }

            @Override
            public Expression visit(Type.IntegerType type) {
                return new Expression.ComparisonExpression(new Expression.IntConst(new BigInteger(constructor.name)), matchedExpression.get(0), Expression.CompOperation.EQ);
            }

            @Override
            public Expression visit(Type.CustomType type) {
                int constructorIndex = getConstructorIndex(type, constructor);
                return new Expression.ComparisonExpression(new Expression.IntConst(BigInteger.valueOf(constructorIndex)), matchedExpression.get(0), Expression.CompOperation.EQ);
            }

            @Override
            public Expression visit(Type.ArrayType type) {
                throw new UnsupportedOperationException("Cannot generate select expression for array types!");
            }
        }

        return type.accept(new SelectExpressionVisitor());
    }

    @Override
    public List<Expression> translateFreeVars(Expression.FreeVarExpression expression) {
        Type type = expression.type;
        List<Type> baseTypes = this.unfoldToBaseTypes(type);

        if (baseTypes.size() == 1) {
            return Collections.singletonList(expression);
        }

        int i = 0;
        List<Expression> ret = new ArrayList<>();
        for (Type t : baseTypes) {
            ret.add(new Expression.FreeVarExpression(t, expression.name + "?" + (i++)));
        }
        return ret;
    }

    private int getConstructorIndex(Type.CustomType type, Constructor constructor) {
        int i = 0;
        for (Constructor c : type.constructors) {
            if (constructor.equals(c)) {
                return i;
            }
            ++i;
        }
        throw new IllegalArgumentException("Type " + type + " has no constructor " + constructor + "!");
    }

    private int getConstructorOffset(Type.CustomType type, Constructor constructor) {
        int i = 1;
        for (Constructor c : type.constructors) {
            if (constructor.equals(c)) {
                return i;
            }
            i += getConstructorBaseTypeParameterCount(c);
        }
        throw new IllegalArgumentException("Type " + type + " has no constructor " + constructor + "!");
    }

    private List<Expression> getDummyForType(Type type) {
        class DummyVisitor implements Type.Visitor<List<Expression>> {
            @Override
            public List<Expression> visit(Type.BooleanType type) {
                return Collections.singletonList(Expression.BoolConst.FALSE);
            }

            @Override
            public List<Expression> visit(Type.IntegerType type) {
                return Collections.singletonList(new Expression.IntConst(BigInteger.valueOf(0)));
            }

            @Override
            public List<Expression> visit(Type.CustomType type) {
                return unfoldToBaseTypes(type).stream().flatMap(t -> t.accept(this).stream()).collect(Collectors.toList());
            }

            @Override
            public List<Expression> visit(Type.ArrayType type) {
                return type.type.accept(this).stream().map(Expression.ArrayInitExpression::new).collect(Collectors.toList());
            }
        }

        return type.accept(new DummyVisitor());
    }

    private int getConstructorBaseTypeParameterCount(Constructor constructor) {
        int i = 0;
        for (Type t : constructor.typeParameters) {
            i += unfoldToBaseTypes(t).size();
        }
        return i;
    }
}
