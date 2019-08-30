package secpriv.horst.translation.visitors;

public abstract class DefaultValueExpressionVisitor<E> extends FunctionMappingExpressionVisitor<E> {
    public DefaultValueExpressionVisitor(E defaultValue) {
        super(x -> defaultValue);
    }
}
