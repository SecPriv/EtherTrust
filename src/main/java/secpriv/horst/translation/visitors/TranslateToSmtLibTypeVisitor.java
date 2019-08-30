package secpriv.horst.translation.visitors;

import secpriv.horst.types.Type;

public class TranslateToSmtLibTypeVisitor implements Type.Visitor<String> {
    @Override
    public String visit(Type.BooleanType type) {
        return "Bool";
    }

    @Override
    public String visit(Type.IntegerType type) {
        return "Int";
    }

    @Override
    public String visit(Type.CustomType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String visit(Type.ArrayType type) {
        return "(Array Int " + type.type.accept(this) + ")";
    }
}
