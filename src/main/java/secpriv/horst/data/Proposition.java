package secpriv.horst.data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class Proposition {
    public interface Visitor<T> {
        T visit(PredicateProposition proposition);

        T visit(ExpressionProposition proposition);
    }

    public abstract <T> T accept(Visitor<T> visitor);

    public static class PredicateProposition extends Proposition {

        public final Predicate predicate;
        public final List<Expression> parameters;
        public final List<Expression> arguments;


        public PredicateProposition(Predicate predicate, List<Expression> parameters, List<Expression> arguments) {
            this.predicate = Objects.requireNonNull(predicate, "Predicate may not be null!");
            this.parameters = Collections.unmodifiableList(Objects.requireNonNull(parameters, "Parameters may not be null!"));
            this.arguments = Collections.unmodifiableList(Objects.requireNonNull(arguments, "Arguments may not be null!"));
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class ExpressionProposition extends Proposition {
        public final Expression expression;

        public ExpressionProposition(Expression expression) {
            this.expression = Objects.requireNonNull(expression, "Expression may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

    }
}
