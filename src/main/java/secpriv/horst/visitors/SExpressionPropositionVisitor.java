package secpriv.horst.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;

public class SExpressionPropositionVisitor implements Proposition.Visitor<String> {
    private int indent;

    public SExpressionPropositionVisitor(int indent) {
        this.indent = indent;
    }

    @Override
    public String visit(Proposition.PredicateProposition proposition) {
        StringBuilder sb = new StringBuilder();
        sb.append("(Predicate ");
        ++indent;
        sb.append(proposition.predicate.name).append(" \n");


        indent(sb);
        sb.append("(parameters");
        ++indent;
        SExpressionExpressionVisitor visitor = new SExpressionExpressionVisitor(indent);
        for (Expression expression : proposition.parameters) {
            sb.append("\n");
            indent(sb);
            sb.append(expression.accept(visitor));
        }
        --indent;
        sb.append(")\n");

        indent(sb);
        sb.append("(arguments");
        ++indent;
        visitor = new SExpressionExpressionVisitor(indent);
        for (Expression expression : proposition.arguments) {
            sb.append("\n");
            indent(sb);
            sb.append(expression.accept(visitor));
        }
        --indent;
        sb.append(")");
        --indent;
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String visit(Proposition.ExpressionProposition proposition) {
        StringBuilder sb = new StringBuilder();
        sb.append("(Expression ");
        sb.append("\n");
        ++indent;
        indent(sb);
        SExpressionExpressionVisitor visitor = new SExpressionExpressionVisitor(indent);
        sb.append(proposition.expression.accept(visitor));
        --indent;
        sb.append(")");
        return sb.toString();
    }

    private void indent(StringBuilder sb) {
        for (int i = 0; i < indent; ++i) {
            sb.append("  ");
        }
    }
}
