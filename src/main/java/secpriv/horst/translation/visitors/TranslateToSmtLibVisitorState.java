package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Predicate;
import secpriv.horst.types.Type;

import java.util.*;

public class TranslateToSmtLibVisitorState {
    private static class ScopedVariableIndex {
        private final Type type;
        private final int index;

        private ScopedVariableIndex(Type type, int index) {
            this.type = type;
            this.index = index;
        }

        private ScopedVariableIndex inc() {
            return new ScopedVariableIndex(type, index + 1);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ScopedVariableIndex that = (ScopedVariableIndex) o;
            return index == that.index &&
                    Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, index);
        }
    }

    private final Map<ScopedVariableIndex, String> freeVars = new HashMap<>();
    private Map<Expression.FreeVarExpression, ScopedVariableIndex> freeVarsInScope;
    private Map<Type, ScopedVariableIndex> scope;
    private LinkedHashSet<Predicate> predicates = new LinkedHashSet<>();

    void registerPredicate(Predicate predicate) {
        predicates.add(predicate);
    }


    String getFreeVar(Expression.FreeVarExpression var) {
        return freeVars.computeIfAbsent(freeVarsInScope.computeIfAbsent(var, this::computeFreeVarScopedIndex), this::computeSemanticFreeVarName);
    }

    private String computeSemanticFreeVarName(ScopedVariableIndex variableIndex) {
        class SemanticNameTypeVisitor implements Type.Visitor<String> {

            @Override
            public String visit(Type.BooleanType type) {
                return "B";
            }

            @Override
            public String visit(Type.IntegerType type) {
                return "I";
            }

            @Override
            public String visit(Type.CustomType type) {
                throw new UnsupportedOperationException("Custom Types have to be inlined at this stage!");
            }

            @Override
            public String visit(Type.ArrayType type) {
                return "AI" + type.type.accept(this);
            }
        }

        return variableIndex.type.accept(new SemanticNameTypeVisitor()) + variableIndex.index;
    }

    private ScopedVariableIndex computeFreeVarScopedIndex(Expression.FreeVarExpression expression) {
        return scope.merge(expression.type, new ScopedVariableIndex(expression.type, 0), (o, n) -> o.inc());
    }

    public String getSmtLibPredicateDeclarations() {
        StringBuilder sb = new StringBuilder();
        TranslateToSmtLibTypeVisitor typeVisitor = new TranslateToSmtLibTypeVisitor();

        List<Predicate> sortedPredicates = new ArrayList<>(predicates);

        class ProgramCounterComparator implements Comparator<Predicate> {
            @Override
            public int compare(Predicate a, Predicate b) {
                return getProgramCounter(a) - getProgramCounter(b);
            }

            private int getProgramCounter(Predicate a) {
                String[] parts = a.name.split("_");
                if(parts.length == 1) {
                    return 0;
                }
                try {
                    return Integer.parseInt(parts[parts.length - 1]);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }

        sortedPredicates.sort(new ProgramCounterComparator());

        for (Predicate predicate : sortedPredicates) {
            sb.append("(declare-rel ");
            sb.append(predicate.name);
            sb.append(" ");
            StringJoiner sj = new StringJoiner(" ", "(", ")");
            for (Type parameterType : predicate.argumentsTypes) {
                sj.add(parameterType.accept(typeVisitor));
            }
            sb.append(sj);
            sb.append(")\n");
        }

        return sb.toString();
    }

    public String getSmtLibVariableDeclarations() {
        StringBuilder sb = new StringBuilder();
        TranslateToSmtLibTypeVisitor typeVisitor = new TranslateToSmtLibTypeVisitor();
        List<Map.Entry<ScopedVariableIndex, String>> sortedFreeVars = new ArrayList<>(freeVars.entrySet());
        Comparator<Map.Entry<ScopedVariableIndex, String>> comparator1 = Comparator.comparing(x -> x.getValue().length());
        Comparator<Map.Entry<ScopedVariableIndex, String>> comparator2 = Comparator.comparing(Map.Entry::getValue);

        sortedFreeVars.sort(comparator1.thenComparing(comparator2));
        for (Map.Entry<ScopedVariableIndex, String> entry : sortedFreeVars) {
            sb.append("(declare-var ");
            sb.append(entry.getValue());
            sb.append(" ");
            sb.append(entry.getKey().type.accept(typeVisitor));
            sb.append(")\n");
        }

        return sb.toString();
    }

    void newScope() {
        scope = new HashMap<>();
        freeVarsInScope = new HashMap<>();
    }
}
