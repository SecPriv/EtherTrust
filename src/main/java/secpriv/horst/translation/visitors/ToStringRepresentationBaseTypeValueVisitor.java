package secpriv.horst.translation.visitors;

import secpriv.horst.data.BaseTypeValue;

public class ToStringRepresentationBaseTypeValueVisitor implements BaseTypeValue.Visitor<String> {
    @Override
    public String visit(BaseTypeValue.BaseTypeIntegerValue baseTypeValue) {
        return baseTypeValue.value.toString();
    }

    @Override
    public String visit(BaseTypeValue.BaseTypeBooleanValue baseTypeValue) {
        return baseTypeValue.value.toString();
    }

    @Override
    public String visit(BaseTypeValue.BaseTypeArrayValue baseTypeValue) {
        throw new UnsupportedOperationException();
    }
}
