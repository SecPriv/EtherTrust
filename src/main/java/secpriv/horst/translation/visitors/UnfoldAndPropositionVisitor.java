package secpriv.horst.translation.visitors;


import secpriv.horst.data.Proposition;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UnfoldAndPropositionVisitor implements Proposition.Visitor<List<Proposition>> {

    @Override
    public List<Proposition> visit(Proposition.PredicateProposition proposition) {
        return Collections.singletonList(proposition);
    }

    @Override
    public List<Proposition> visit(Proposition.ExpressionProposition proposition) {
        UnfoldAndExpressionVisitor expressionVisitor = new UnfoldAndExpressionVisitor();
        return proposition.expression.accept(expressionVisitor).stream().map(Proposition.ExpressionProposition::new).collect(Collectors.toList());
    }
}
