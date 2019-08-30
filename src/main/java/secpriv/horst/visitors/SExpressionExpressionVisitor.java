package secpriv.horst.visitors;

import secpriv.horst.data.Expression;
import secpriv.horst.data.Pattern;
import secpriv.horst.data.SelectorFunctionInvocation;

import java.util.Iterator;
import java.util.List;

public class SExpressionExpressionVisitor implements Expression.Visitor<String> {
    private StringBuilder sb = new StringBuilder();
    private int indent;

    public SExpressionExpressionVisitor(int indent) {
        this.indent = indent;
    }

    @Override
    public String visit(Expression.IntConst expression) {
        return "(IntConst " + expression.value + ")";
    }

    @Override
    public String visit(Expression.BoolConst expression) {
        return "(BoolConst " + expression.value + ")";
    }

    @Override
    public String visit(Expression.ArrayInitExpression expression) {
        return "(ArrayInit " + expression.initializer.accept(this) + ")";
    }

    @Override
    public String visit(Expression.VarExpression expression) {
        return "(Var " + expression.name + " " + expression.type.name + ")";
    }

    @Override
    public String visit(Expression.FreeVarExpression expression) {
        return "(FreeVar " + expression.name + " " + expression.type.name + ")";
    }

    @Override
    public String visit(Expression.ParVarExpression expression) {
        return "(ParVar " + expression.name + " " + expression.type.name + ")";
    }

    private String visitBinaryExpression(String prefix, Expression.BinaryExpression expression) {
        StringBuilder sb = new StringBuilder();

        sb.append("(").append(prefix).append("\n");

        ++indent;
        indent(sb);
        sb.append(expression.expression1.accept(this)).append("\n");
        indent(sb);
        sb.append(expression.expression2.accept(this)).append(")");
        --indent;

        return sb.toString();
    }

    @Override
    public String visit(Expression.BinaryIntExpression expression) {
        return visitBinaryExpression("BinaryInt " + expression.operation, expression);
    }

    @Override
    public String visit(Expression.BinaryBoolExpression expression) {
        return visitBinaryExpression("BinaryBool " + expression.operation, expression);
    }

    @Override
    public String visit(Expression.SelectExpression expression) {
        return visitBinaryExpression("Select ", expression);
    }

    private String visitTernaryExpression(String prefix, Expression.TernaryExpression expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(prefix).append("\n" );
        ++indent;
        indent(sb).append(expression.expression1.accept(this)).append("\n");
        indent(sb).append(expression.expression2.accept(this)).append("\n");
        indent(sb).append(expression.expression3.accept(this)).append(")");
        --indent;

        return sb.toString();
    }

    @Override
    public String visit(Expression.StoreExpression expression) {
        return visitTernaryExpression("Store", expression);
    }

    private String visitVariadicExpression(String prefix, Expression.VariadicExpression expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(prefix);
        ++indent;
        for(Expression e : expression.expressions) {
            indent(sb.append("\n")).append(e.accept(this));
        }
        sb.append(")");
        --indent;

        return sb.toString();
    }

    @Override
    public String visit(Expression.AppExpression expression) {
        return visitVariadicExpression("App " + expression.operation.name, expression);
    }

    @Override
    public String visit(Expression.ConstructorAppExpression expression) {
        return visitVariadicExpression("ConstructorApp " + expression.constructor.name, expression);
    }

    @Override
    public String visit(Expression.MatchExpression expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("(Match \n");
        ++indent;
        indent(sb);
        sb.append("(matched-expressions");
        ++indent;
        for(Expression me : expression.matchedExpressions) {
            sb.append("\n");
            indent(sb);
            sb.append(me.accept(this));
        }
        --indent;
        sb.append(")\n");

        indent(sb);
        sb.append("(patterns ");
        Iterator<Expression> resultsExpressionIterator = expression.resultExpressions.iterator();
        ++indent;
        for(List<Pattern> patternTuple : expression.branchPatterns) {
            sb.append("\n");
            for(Pattern pattern : patternTuple) {
                SExpressionPatternVisitor patternVisitor = new SExpressionPatternVisitor(indent);
                indent(sb);
                sb.append(pattern.accept(patternVisitor));
                sb.append("\n");
            }
            indent(sb);
            sb.append(" => \n");
            ++indent;
            indent(sb);
            sb.append(resultsExpressionIterator.next().accept(this));
            --indent;
        }
        --indent;
        sb.append(")");
        --indent;
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String visit(Expression.NegationExpression expression) {
        return "(Neg " + expression.expression.accept(this) + ")";
    }

    @Override
    public String visit(Expression.BitvectorNegationExpression expression) {
        return "(BVNeg " + expression.expression.accept(this) + ")";
    }

    @Override
    public String visit(Expression.ConditionalExpression expression) {
        return visitTernaryExpression("Conditional", expression);
    }

    @Override
    public String visit(Expression.ComparisonExpression expression) {
        return visitBinaryExpression("Comparison " + expression.operation, expression);
    }

    @Override
    public String visit(Expression.ConstExpression expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("(Const ").append(expression.name).append("\n");
        ++indent;
        indent(sb);
        sb.append(expression.value.accept(this)).append(")");
        --indent;
        return sb.toString();
    }

    @Override
    public String visit(Expression.SumExpression expression) {
        StringBuilder sb = new StringBuilder();
        sb.append("(Sum ").append(expression.operation).append(" \n");
        ++indent;
        indent(sb);
        for (SelectorFunctionInvocation selectorFunctionInvocation : expression.selectorFunctionInvocation.selectorFunctionInvocations) {
            sb.append("(parameters ");
            for (Expression.ParVarExpression p : selectorFunctionInvocation.parameters) {
                sb.append(p.accept(this));
            }
            sb.append(")\n");
            --indent;
            ++indent;
            indent(sb);
            sb.append("(selector-function ").append(selectorFunctionInvocation.selectorFunction.name);
            ++indent;
            for (Expression a : selectorFunctionInvocation.arguments) {
                sb.append("\n");
                indent(sb);
                sb.append(a.accept(this));
            }
            --indent;
            ++indent;
            sb.append(")\n");
        }
        indent(sb);
        sb.append("(body\n");
        ++indent;
        indent(sb);
        sb.append(expression.body.accept(this));
        --indent;
        sb.append(")");
        --indent;
        sb.append(")");
        --indent;

        return sb.toString();
    }

    private StringBuilder indent(StringBuilder sb) {
        for(int i = 0; i < indent; ++i) {
            sb.append("  ");
        }
        return sb;
    }
}
