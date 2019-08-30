package secpriv.horst.internals.error.handling;

import org.antlr.v4.runtime.ParserRuleContext;
import secpriv.horst.internals.error.objects.Error;
import secpriv.horst.parser.ASParser;
import secpriv.horst.tools.ZipInfo;
import secpriv.horst.types.Type;

import java.util.List;

public class ErrorHelper {

    public static Error generateTypeMismatchError(Type actualType, Type expectedType, ParserRuleContext ctx) {
        return new Error.TypeMismatch(actualType, expectedType, ctx);
    }

    public static Error generateUndefinedValueError(String element, String name, Integer position, ParserRuleContext ctx){
        return new Error.UndefinedValue(element, name, position, ctx);
    }

    public static Error generateUndefinedValueError(String element, String name, ParserRuleContext ctx){
        return new Error.UndefinedValue(element, name, ctx);
    }

    public static Error generateSizeDoesntMatchError(int expectedSize, int actualSize, String element, ParserRuleContext ctx){
        return new Error.SizeDoesntMatch(expectedSize, actualSize, element, ctx);
    }

    public static Error generateIsNotInstanceOfError(Type type, Class<?> requiredClass, ParserRuleContext ctx) {
        return new Error.IsNotInstanceOf(type, requiredClass, ctx);
    }

    public static Error generateExpressionResultsTypesDontMatchError(Type type1, Type type2, ParserRuleContext ctx) {
        return new Error.ExpressionResultsTypesDontMatch(type1, type2, ctx);
    }

    public static Error generateMismatchInListError(String element, ZipInfo message, ParserRuleContext ctx){
        return new Error.MismatchInList(element, message, ctx);
    }

    public static Error generateElementAlreadyBoundError(String element, String className, Integer position, ParserRuleContext ctx) {
        return new Error.ElementAlreadyBound(element, className, position, ctx);
    }

    public static Error generateElementAlreadyBoundError(String element, String name, ParserRuleContext ctx) {
        return new Error.ElementAlreadyBound(element, name, ctx);
    }

    public static Error generateElementNotConstError(String element, Type expressionType, ParserRuleContext ctx) {
        return new Error.ElementNotConst(element, expressionType, ctx);
    }

    public static Error generatePatternTypeMismatchError(String patternName, String typeName, ParserRuleContext ctx){
        return new Error.PatternTypeMismatch(patternName, typeName, ctx);
    }

    public static Error generateSelectorFunctionMissingImplementationError(String selectorFunctionName, List<Type> parameterTypes, List<Type> returnTypes, ASParser.SelectorFunctionDeclarationContext ctx) {
        return new Error.SelectorFunctionMissingImplementation(selectorFunctionName,parameterTypes, returnTypes, ctx);
    }

    public static Error generateElementNotDefinedInMacroError(String simpleName, String macroName, int position, ParserRuleContext ctx) {
        return new Error.ElementNotDefinedInMacro(simpleName, macroName, position, ctx);
    }

    public static Error generateInvalidPremisesError(ParserRuleContext ctx) {
        return new Error.InvalidPremises(ctx);
    }

    public static Error generateInitRuleContainsPredicatePropositionError(ParserRuleContext ctx) {
        return new Error.InitRuleContainsPredicateProposition(ctx);
    }
}
