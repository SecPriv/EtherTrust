package secpriv.horst.visitors;

import secpriv.horst.data.Pattern;

public class SExpressionPatternVisitor implements Pattern.Visitor<String> {
    private int indent;

    public SExpressionPatternVisitor(int indent) {
        this.indent = indent;
    }

    @Override
    public String visit(Pattern.ValuePattern pattern) {
        StringBuilder sb = new StringBuilder();

        sb.append("(Value ").append(pattern.constructor.name);
        ++indent;
        for(Pattern p : pattern.patterns) {
            sb.append("\n");
            indent(sb);
            sb.append(p.accept(this));
        }
        --indent;
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String visit(Pattern.WildcardPattern pattern) {
        return "(Wildcard " + pattern.name + ")";
    }

    private void indent(StringBuilder sb) {
        for(int i = 0; i < indent; ++i) {
            sb.append("  ");
        }
    }
}
