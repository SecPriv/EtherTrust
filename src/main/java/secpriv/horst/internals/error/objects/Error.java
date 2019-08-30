package secpriv.horst.internals.error.objects;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.tools.ZipInfo;
import secpriv.horst.types.Type;

import java.util.List;
import java.util.Objects;

public abstract class Error {
    final int lineNumber;
    final int column;

    Error(ParserRuleContext ctx) {
        this.lineNumber = ctx.start.getLine();
        this.column = ctx.start.getCharPositionInLine();
    }

    public interface Visitor<T> {
        T visit(TypeMismatch errorObject);

        T visit(UndefinedValue errorObject);

        T visit(SizeDoesntMatch errorObject);

        T visit(IsNotInstanceOf errorObject);

        T visit(ExpressionResultsTypesDontMatch errorObject);

        T visit(MismatchInList errorObject);

        T visit(ElementAlreadyBound errorObject);

        T visit(ElementNotConst errorObject);

        T visit(PatternTypeMismatch errorObject);

        T visit(SelectorFunctionMissingImplementation errorObject);

        T visit(ElementNotDefinedInMacro errorObject);

        T visit(InvalidPremises errorObject);

        T visit(InitRuleContainsPredicateProposition errorObject);
    }

    public abstract <T> T accept(Visitor<T> visitor);


    public static class TypeMismatch extends Error {
        private final Type expectedType;
        private final Type actualType;

        public TypeMismatch(Type actualType, Type expectedType, ParserRuleContext ctx) {
            super(ctx);
            this.expectedType = Objects.requireNonNull(expectedType, "Expected value may not be null!");
            this.actualType = Objects.requireNonNull(actualType, "Actual value may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: Expected type for expression is '" + this.expectedType.name  +
                    "', but received '" + this.actualType.name +
                    "'. Error found at position " + lineNumber + ":" + this.column;
        }
    }

    public static class UndefinedValue extends Error {
        private final String element;
        private final String name;
        private final Integer position;

        public UndefinedValue(String element, String name, ParserRuleContext ctx) {
            super(ctx);
            this.element = Objects.requireNonNull(element, "Variable name may not be null!");
            this.name = Objects.requireNonNull(name, "Name may not be null");
            this.position = -1;
        }

        public UndefinedValue(String element, String name, Integer position, ParserRuleContext ctx) {
            super(ctx);
            this.element = Objects.requireNonNull(element, "Variable name may not be null!");
            this.name = Objects.requireNonNull(name, "Name may not be null");
            this.position = Objects.requireNonNull(position, "Position may not be null");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: " + this.element + " '" + this.name +
                    (this.position < 0 ? "'" : "' at position '" + this.position + "'")
                    + " is undefined. " +
                    "Error found at position " + lineNumber + ":" + this.column;
        }
    }

    public static class SizeDoesntMatch extends Error {
        private final int expectedSize;
        private final int actualSize;
        private final String element;

        public SizeDoesntMatch(int expectedSize, int actualSize, String element, ParserRuleContext ctx) {
            super(ctx);
            this.expectedSize = expectedSize;
            this.actualSize = actualSize;
            this.element = Objects.requireNonNull(element, "Element may not be null!");
        }


        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: Expected size of " + this.element + " is '"
                    + this.expectedSize + "', but received '" +
                    this.actualSize + "'" +
                    ". Error found at position " + lineNumber +
                    ":" + column;

        }
    }

    public static class IsNotInstanceOf extends Error {
        private final Type type;
        private final String requiredCass;

        public IsNotInstanceOf(Type type, Class<?> requiredClass, ParserRuleContext ctx) {
            super(ctx);
            this.type = type;
            this.requiredCass = requiredClass.getSimpleName();
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: Expected type for expression is '" + this.requiredCass  +
                    "', but received '" + this.type.name +
                    "'. Error found at position " + lineNumber + ":" + this.column;
        }
    }

    public static class ExpressionResultsTypesDontMatch extends Error {
        private final Type type1;
        private final Type type2;

        public ExpressionResultsTypesDontMatch(Type type1, Type type2, ParserRuleContext ctx) {
            super(ctx);
            this.type1 = Objects.requireNonNull(type1, "Type may not be null!");
            this.type2 = Objects.requireNonNull(type2, "Required type may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: Expression results with types '" + this.type1.name +
                    "' and '" + this.type2.name + "' don't match. " +
                    "Error found at position " + lineNumber +
                    ":" + column;
        }
    }

    public static class MismatchInList extends Error {
        private final String type;
        private final ZipInfo zipInfo;

        public MismatchInList(String type, ZipInfo message, ParserRuleContext ctx) {
            super(ctx);
            this.zipInfo = Objects.requireNonNull(message, "Object may not be null!");
            this.type = Objects.requireNonNull(type, "Type may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: Mismatch in " + this.type + " list - " +
                    this.zipInfo.getMessage() +
                    " Error found at position " + lineNumber +
                    ":" + column;
        }
    }

    public static class ElementAlreadyBound extends Error {
        private final String element;
        private final String name;
        private final Integer position;

        public ElementAlreadyBound(String element, String name, Integer position, ParserRuleContext ctx) {
            super(ctx);
            this.element = Objects.requireNonNull(element, "Element may not be null!");
            this.name = Objects.requireNonNull(name, "Parameter may not be null!");
            this.position = Objects.requireNonNull(position, "Position may not be null!");
        }

        public ElementAlreadyBound(String element, String name, ParserRuleContext ctx) {
            super(ctx);
            this.element = Objects.requireNonNull(element, "Element may not be null!");
            this.name = Objects.requireNonNull(name, "Parameter may not be null!");
            this.position = -1;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: " + this.element + " '" + this.name +
                    (this.position < 0 ? "'" : "' at position " + this.position)  +
                    " already bound. " +
                    "Error found at position " + lineNumber +
                    ":" + column;
        }
    }

    public static class ElementNotConst extends Error {
        private final String var;
        private final Type type;

        public ElementNotConst(String var, Type type, ParserRuleContext ctx) {
            super(ctx);
            this.var = Objects.requireNonNull(var, "String with definition should not be null!");
            this.type = Objects.requireNonNull(type, "Type may not be null!");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: " +  this.var + " of type '" + this.type.name +
                    "' is not a constant but should be." +
                    " Error found at position " + lineNumber + ":" + column;
        }
    }

    public static class PatternTypeMismatch extends Error {
        private final String patternName;
        private final String typeName;

        public PatternTypeMismatch(String patternName, String typeName, ParserRuleContext ctx) {
            super(ctx);
            this.patternName = Objects.requireNonNull(patternName, "Pattern may not be null");
            this.typeName = Objects.requireNonNull(typeName, "Type may not be null");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: " + this.patternName +
                    " does't match type '" + this.typeName + "'. " +
                    "Error found at position " +
                    lineNumber + ":" + column;
        }
    }

    public static class SelectorFunctionMissingImplementation extends Error {
        private final String selectorFunctionName;
        private final List<Type> parameterTypes;
        private final List<Type> returnTypes;

        public SelectorFunctionMissingImplementation(String selectorFunctionName, List<Type> parameterTypes, List<Type> returnTypes, ParserRuleContext ctx) {
            super(ctx);
            this.selectorFunctionName = Objects.requireNonNull(selectorFunctionName, "Selector function name may not be null");
            this.parameterTypes = Objects.requireNonNull(parameterTypes, "Parameter types may not be null");
            this.returnTypes = Objects.requireNonNull(returnTypes, "Return types may not be null");
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: Selector function '" + this.selectorFunctionName +
                    "' with " + this.parameterTypes.size() + " parameters and " +
                    this.returnTypes + " return types doesn't have implementation." +
                    "Error found at position " + lineNumber + ":" + column;
        }
    }

    public static class ElementNotDefinedInMacro extends Error {
        private final String name;
        private final String macroName;
        private final int position;

        public ElementNotDefinedInMacro(String simpleName, String macroName, int position, ParserRuleContext ctx) {
            super(ctx);
            this.name = Objects.requireNonNull(simpleName, "Name may not be null");
            this.macroName = Objects.requireNonNull(macroName, "Macro name  may not be null");
            this.position = position;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: " + this.name +  " at position '" + this.position +
                    "' in macro '" + this.macroName + "' is not defined. " +
                    "Error found at position " + lineNumber +
                    ":" + column;
        }
    }

    public static class InvalidPremises extends Error {

        public InvalidPremises(ParserRuleContext ctx) {
            super(ctx);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "ERROR: Invalid premises of a clause!";
        }
    }

    public static class InitRuleContainsPredicateProposition extends Error {
        public InitRuleContainsPredicateProposition(ParserRuleContext ctx) {
            super(ctx);
        }

        @Override
        public String toString() {
            return "ERROR: " + "Init statements may not contain predicates in premises! " +
                    "Error found at position " + lineNumber +
                    ":" + column;
        }


        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
