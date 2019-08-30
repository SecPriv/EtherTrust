package secpriv.horst.visitors;

import secpriv.horst.data.Clause;
import secpriv.horst.data.Proposition;
import secpriv.horst.types.Type;

import java.util.Map;

public class SExpressionClauseVisitor implements Clause.Visitor<String> {
    private StringBuilder sb = new StringBuilder();
    private int indent;

    public SExpressionClauseVisitor(int indent) {
        this.indent = indent;
    }

    @Override
    public String visit(Clause clause) {
        sb.append("(Clause ");
        ++indent;
        indent();
        sb.append('\n');
        indent();
        sb.append("(freevars ");
        ++indent;
        for (Map.Entry<String, Type> e : clause.freeVars.entrySet()) {
            sb.append("\n");
            indent();
            sb.append("(fv ").append(e.getKey()).append(" ").append(e.getValue().name).append(")");
        }
        --indent;
        sb.append(")\n");


        indent();
        sb.append("(premises ");
        ++indent;
        for (Proposition premise : clause.premises) {
            SExpressionPropositionVisitor visitor = new SExpressionPropositionVisitor(indent);
            sb.append("\n");
            indent();
            sb.append(premise.accept(visitor));
        }
        --indent;
        sb.append(")\n");

        indent();
        sb.append("(conclusion ");
        ++indent;
        SExpressionPropositionVisitor visitor = new SExpressionPropositionVisitor(indent);
        sb.append("\n");
        indent();
        sb.append(clause.conclusion.accept(visitor));
        --indent;
        sb.append(")");


        --indent;
        sb.append(")");


        return sb.toString();
    }

    private void indent() {
        for(int i = 0; i < indent; ++i) {
            sb.append("  ");
        }
    }
}
