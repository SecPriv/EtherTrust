package secpriv.horst.translation.visitors;

import secpriv.horst.data.BaseTypeValue;

public class ToObjectBaseTypeValueVisitor implements BaseTypeValue.Visitor<Object> {
    @Override
    public Object visit(BaseTypeValue.BaseTypeIntegerValue baseTypeValue) {
        return baseTypeValue.value;
    }

    @Override
    public Object visit(BaseTypeValue.BaseTypeBooleanValue baseTypeValue) {
        return baseTypeValue.value;
    }

    @Override
    public Object visit(BaseTypeValue.BaseTypeArrayValue baseTypeValue) {
        throw new UnsupportedOperationException();
    }
}
