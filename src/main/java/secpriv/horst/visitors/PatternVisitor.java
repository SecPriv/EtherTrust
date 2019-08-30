package secpriv.horst.visitors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import secpriv.horst.data.Pattern;
import secpriv.horst.internals.error.handling.ErrorHelper;
import secpriv.horst.parser.ASBaseVisitor;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class PatternVisitor extends ASBaseVisitor<Optional<Pattern>> {
    private VisitorState state;

    public PatternVisitor(VisitorState state) {
        this.state = state;
    }

    @Override
    public Optional<Pattern> visitConstructorPattern(ASParser.ConstructorPatternContext ctx) {
        String constructorName = ctx.elementID().getText();

        Optional<Constructor> optConstructor = state.getConstructorByName(constructorName);

        if (!optConstructor.isPresent()) {
            return valueUndefined("Constructor", constructorName, ctx);
        }

        Constructor constructor = optConstructor.get();

        List<Pattern> patterns = new ArrayList<>();

        for (ParseTree p : ctx.pattern()) {
            Optional<Pattern> optPattern = visit(p);
            if (!optPattern.isPresent()) {
                return Optional.empty();
            }
            patterns.add(optPattern.get());
        }

        if (!checkIfSizeMatches(patterns.size() , constructor.typeParameters.size(), "parameters in constructor", ctx)){
            return Optional.empty();
        }

        Iterator<Pattern> patternIterator = patterns.iterator();
        Iterator<Type> typeParameterIterator = constructor.typeParameters.iterator();

        while (patternIterator.hasNext()) {
            Pattern pattern = patternIterator.next();
            Type type = typeParameterIterator.next();

            if (!patternTypeMatches(type, pattern)) {
                String patternName = pattern.accept(new PatternDisplayStringVisitor());
                return patternTypeMismatch(patternName, type.name, ctx);
            }
        }

        return Optional.of(new Pattern.ValuePattern(constructor, patterns));
    }

    boolean patternTypeMatches(Type type, Pattern pattern) {
        //TODO maybe implement as Pattern::matchesType(Type), complicated because
        //TODO in ValuePattern we have no mapping between Constructor and Type
        class PatternTypeMatchVisitor implements Pattern.Visitor<Boolean> {
            @Override
            public Boolean visit(Pattern.ValuePattern pattern) {
                Type patternType = state.getTypeForConstructor(pattern.constructor.name).get();
                return patternType.equals(type);
            }

            @Override
            public Boolean visit(Pattern.WildcardPattern pattern) {
                return true;
            }
        }
        return pattern.accept(new PatternTypeMatchVisitor());
    }

    @Override
    public Optional<Pattern> visitWildCardPattern(ASParser.WildCardPatternContext ctx) {
        if (ctx.UNDERSCORE() != null) {
            return Optional.of(new Pattern.WildcardPattern("_"));
        } else if (ctx.var() != null) {
            return Optional.of(new Pattern.WildcardPattern(ctx.var().ID().getText()));
        }
        throw new RuntimeException("Unreachable code!");
    }

    @Override
    public Optional<Pattern> visitBaseConst(ASParser.BaseConstContext ctx) {
        if (ctx.boolConst() != null) {
            return Optional.of(new Pattern.ValuePattern(Type.Boolean.getConstructorByName(ctx.boolConst().getText()).get()));
        } else if (ctx.intConst() != null) {
            return Optional.of(new Pattern.ValuePattern(Type.Integer.getConstructorByName(ctx.intConst().getText()).get()));
        }
        throw new RuntimeException("Unreachable code!");
    }

    private Optional<Pattern> valueUndefined(String element, String name, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generateUndefinedValueError(element, name, ctx));
        return Optional.empty();
    }

    private Optional<Pattern> patternTypeMismatch(String patternName, String typeName, ParserRuleContext ctx){
        state.errorHandler.handleError(ErrorHelper.generatePatternTypeMismatchError(patternName, typeName, ctx));
        return Optional.empty();
    }

    private boolean checkIfSizeMatches(int expectedSize, int actualSize, String location, ParserRuleContext ctx) {
        if (actualSize != expectedSize) {
            state.errorHandler.handleError(ErrorHelper.generateSizeDoesntMatchError(expectedSize, actualSize, location, ctx));
            return false;
        }
        return true;
    }
}
