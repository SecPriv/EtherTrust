package secpriv.horst.translation.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Proposition;

public class TranslateToSmtLibPropositionVisitor implements Proposition.Visitor<Void> {
    private final StringBuilder sb;
    private final TranslateToSmtLibExpressionVisitor expressionVisitor;
    private final TranslateToSmtLibVisitorState state;
    private final static String indent = "      ";

    public TranslateToSmtLibPropositionVisitor(StringBuilder sb, TranslateToSmtLibVisitorState state) {
        this.sb = sb;
        this.state = state;
        this.expressionVisitor = new TranslateToSmtLibExpressionVisitor(sb, state);
    }

    @Override
    public Void visit(Proposition.PredicateProposition proposition) {
        state.registerPredicate(proposition.predicate);
        sb.append("\n");
        sb.append(indent);

        if(proposition.arguments.isEmpty()) {
            sb.append(proposition.predicate.name);
        } else {
            sb.append("(");
            sb.append(proposition.predicate.name);
            for (Expression argument : proposition.arguments) {
                sb.append(" ");
                argument.accept(expressionVisitor);
            }
            sb.append(")");
        }
        return null;
    }

    @Override
    public Void visit(Proposition.ExpressionProposition proposition) {
        sb.append("\n");
        sb.append(indent);
        proposition.expression.accept(expressionVisitor);
        return null;
    }
}
