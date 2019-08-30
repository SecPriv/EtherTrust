package secpriv.horst.translation;

import com.microsoft.z3.*;
import secpriv.horst.data.BaseTypeValue;
import secpriv.horst.data.Expression;
import secpriv.horst.data.Predicate;
import secpriv.horst.translation.visitors.EvaluateExpressionVisitor;
import secpriv.horst.translation.visitors.ToConstExpressionBaseTypeValueVisitor;
import secpriv.horst.translation.visitors.TranslateToZ3ExpressionVisitor;
import secpriv.horst.translation.visitors.TranslateToZ3ExpressionVisitorWithBitVectorIntegers;
import secpriv.horst.types.Type;

import java.util.HashMap;
import java.util.Map;

public class TranslateToZ3VisitorState {
    public final Context context;
    private Map<Predicate, FuncDecl> z3Predicates = new HashMap<>();
    private Map<Expression.FreeVarExpression, Expr> z3FreeVars = new HashMap<>();
    private Map<String, BaseTypeValue> constantDefinitions = new HashMap<>();
    private final Expression.Visitor<Expr> expressionVisitor;

    private final static int BIT_WIDTH = 32;//64;//256;
    private int boundCount = 0;

    private final Type.Visitor<Sort> typeToSortVisitor;

    private TranslateToZ3VisitorState(boolean useBitVectorIntegers) {
        context = new Context();
        if(useBitVectorIntegers) {
            this.typeToSortVisitor = new TypeToSortVisitorWithBitVectorIntegers();
            this.expressionVisitor = new TranslateToZ3ExpressionVisitorWithBitVectorIntegers(this);
        } else {
            this.typeToSortVisitor = new TypeToSortVisitor();
            this.expressionVisitor = new TranslateToZ3ExpressionVisitor(this);
        }
    }

    public static TranslateToZ3VisitorState withBitVectorIntegers() {
        return new TranslateToZ3VisitorState(true);
    }

    public static TranslateToZ3VisitorState withGeneralIntegers() {
        return new TranslateToZ3VisitorState(false);
    }

    public Expression.Visitor<Expr> getExpressionVisitor() {
        return expressionVisitor;
    }

    private class TypeToSortVisitor implements Type.Visitor<Sort> {
        @Override
        public Sort visit(Type.BooleanType type) {
            return context.mkBoolSort();
        }

        @Override
        public Sort visit(Type.IntegerType type) {
            return context.mkIntSort();
        }

        @Override
        public Sort visit(Type.CustomType type) {
            throw new UnsupportedOperationException("Custom types cannot be translated to Z3!");
        }

        @Override
        public Sort visit(Type.ArrayType type) {
            return context.mkArraySort(context.mkIntSort(), typeToSort(type.type));
        }
    }

    private class TypeToSortVisitorWithBitVectorIntegers implements Type.Visitor<Sort> {
        @Override
        public Sort visit(Type.BooleanType type) {
            return context.mkBoolSort();
        }

        @Override
        public Sort visit(Type.IntegerType type) {
            return context.mkBitVecSort(BIT_WIDTH);
        }

        @Override
        public Sort visit(Type.CustomType type) {
            throw new UnsupportedOperationException("Custom types cannot be translated to Z3!");
        }

        @Override
        public Sort visit(Type.ArrayType type) {
            return context.mkArraySort(context.mkBitVecSort(BIT_WIDTH), typeToSort(type.type));
        }
    }

    public FuncDecl getZ3PredicateDeclaration(Predicate predicate) {
        return z3Predicates.computeIfAbsent(predicate, this::computeZ3PredicateDeclaration);
    }

    private FuncDecl computeZ3PredicateDeclaration(Predicate predicate) {
        String name = predicate.name;
        Sort[] arguments = predicate.argumentsTypes.stream().map(this::typeToSort).toArray(Sort[]::new);

        return context.mkFuncDecl(name, arguments, context.mkBoolSort());
    }

    private Sort typeToSort(Type type) {
        return type.accept(typeToSortVisitor);
    }

    public Expr getZ3FreeVar(Expression.FreeVarExpression expression) {
        return z3FreeVars.computeIfAbsent(expression, this::computeZ3FreeVarDeclaration);
    }

    private Expr computeZ3FreeVarDeclaration(Expression.FreeVarExpression expression) {
        return context.mkBound(boundCount++, typeToSort(expression.type));
    }

    public Expression getConstant(Expression.ConstExpression expression) {
        return constantDefinitions.computeIfAbsent(expression.name, s -> {
            EvaluateExpressionVisitor evaluateExpressionVisitor = new EvaluateExpressionVisitor();
            return expression.value.accept(evaluateExpressionVisitor);
        }).accept(new ToConstExpressionBaseTypeValueVisitor());
    }

    public void registerRelations(Fixedpoint fixedpoint, boolean settings) {
        if (settings) {
            for (FuncDecl predicate : z3Predicates.values()) {
                fixedpoint.registerRelation(predicate);
                Symbol[] symbols = new Symbol[]{context.mkSymbol("interval_relation"),
                        context.mkSymbol("bound_relation")};
                fixedpoint.setPredicateRepresentation(predicate, symbols);
            }
        }
        else{
            for(FuncDecl predicate : z3Predicates.values()) {
                fixedpoint.registerRelation(predicate);
            }
        }
    }
}
