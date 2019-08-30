package secpriv.horst.data;

import secpriv.horst.types.Constructor;

import java.util.*;

public abstract class Pattern {
    public interface Visitor<T> {
        T visit(ValuePattern pattern);
        T visit(WildcardPattern pattern);
    }

    public static class ValuePattern extends Pattern {
        public final Constructor constructor;
        public final List<Pattern> patterns;

        public ValuePattern(Constructor constructor) {
            this(constructor, new ArrayList<>());
        }
        public ValuePattern(Constructor constructor, List<Pattern> patterns) {
            this.constructor = Objects.requireNonNull(constructor, "Constructor may not be null!");
            this.patterns = Collections.unmodifiableList(Objects.requireNonNull(patterns, "Patterns may not be null!"));
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class WildcardPattern extends Pattern {
        public final String name;

        public WildcardPattern(String name) {
            this.name = Objects.requireNonNull(name, "Name may not be null");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public abstract <T> T accept(Visitor<T> visitor);
}

