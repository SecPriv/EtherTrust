package secpriv.horst.visitors;

import secpriv.horst.data.Pattern;

public class PatternDisplayStringVisitor implements Pattern.Visitor<String> {
    @Override
    public String visit(Pattern.ValuePattern pattern) {
        return "Constructor '" + pattern.constructor.name + "'";
    }

    @Override
    public String visit(Pattern.WildcardPattern pattern) {
        return "Wildcard '" + pattern.name + "'";
    }
}
