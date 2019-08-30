package secpriv.horst.tools;

public class TupleCodeGenerator {
    private static final String TYPE_PARAM_PREFIX = "V";
    private static final String INDENT = "    ";
    private static final String VARIABLE_PREFIX = "v";
    private static final String CLASS_PREFIX = "Tuple";
    private static int indentLevel = 0;

    private StringBuilder sb = new StringBuilder();
    private int n;

    private TupleCodeGenerator(int n) {
        this.n = n;
    }

    public static void main(String[] args) {
        int n = 0;

        try {
            n = Integer.parseInt(args[0]);
        } catch (Exception e) {

        }

        System.out.println(generateTuple(n));
    }

    public static String generateTuple(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n has to be positive!");
        }

        TupleCodeGenerator generator = new TupleCodeGenerator(n);

        generator.packageDeclaration();
        generator.imports();
        generator.sb.append("\n");
        generator.sb.append("public class Tuple").append(n);
        generator.typeParameters();
        generator.sb.append(" ");
        generator.sb.append("implements Tuple ");

        generator.openBlock();
        generator.memberVariables();

        newMethod(generator.sb, generator::constructorDefinition);
        newMethod(generator.sb, generator::equalsDefinition);
        newMethod(generator.sb, generator::hashCodeDefinition);
        newMethod(generator.sb, generator::toStringDefinition);
        newMethod(generator.sb, generator::getParameterTypesDefinition);
        newMethod(generator.sb, generator::bindToNamesDefinition);

        generator.closeBlock();

        return generator.sb.toString();
    }

    private void bindToNamesDefinition() {
        indentedLinePrefix("public Map<String, BaseTypeValue> bindToNames(List<String> names)");
        openBlock();
        indentedLine("Map<String, BaseTypeValue> ret = new HashMap<>();");
        for (int i = 0; i < n; ++i) {
            indentedLinePrefix("ret.put(names.get(");
            sb.append(i).append("), BaseTypeValue.unsafeFromObject(");
            variable(i);
            sb.append("));\n");
        }
        indentedLine("return ret;");
        closeBlock();
    }

    private void getParameterTypesDefinition() {
        indentedLinePrefix("public List<Class> getParameterTypes()");
        openBlock();

        indentedLinePrefix("return ");
        sb.append("Arrays.asList(");
        variable(0);
        sb.append(".getClass()");
        for (int i = 1; i < n; ++i) {
            sb.append(", ");
            variable(i);
            sb.append(".getClass()");
        }
        sb.append(");\n");
        closeBlock();
    }

    private void toStringDefinition() {
        indentedLine("@Override");
        indentedLinePrefix("public String toString()");
        openBlock();
        indentedLinePrefix("return \"");
        className();
        sb.append("{\" + ");
        variable(0);
        for (int i = 1; i < n; ++i) {
            sb.append(" + \", \" + ");
            variable(i);
        }
        sb.append(" + \"}\";\n");
        closeBlock();
    }

    private void indentedLine(String s) {
        indentedLinePrefix(s);
        newLine();
    }

    private void newLine() {
        sb.append("\n");
    }

    private void indentedLinePrefix(String s) {
        indent();
        sb.append(s);
    }

    private static void newMethod(StringBuilder sb, Runnable r) {
        sb.append("\n");
        r.run();
    }

    private void hashCodeDefinition() {
        indentedLine("@Override");
        indentedLinePrefix("public int hashCode() ");
        openBlock();
        indentedLinePrefix("int result = ");
        hashCodeExpression(0);
        sb.append(";\n");

        for (int i = 1; i < n; ++i) {
            indentedLinePrefix("result = 31 * result + ");
            hashCodeExpression(i);
            sb.append(";\n");
        }
        indentedLine("return result;");
        closeBlock();
    }

    private void hashCodeExpression(int i) {
        variable(i);
        sb.append(".hashCode()");
    }

    private void openBlock() {
        ++indentLevel;
        sb.append("{").append("\n");
    }

    private void equalsDefinition() {
        indentedLine("@Override");
        indentedLinePrefix("public boolean equals(Object o) ");
        openBlock();
        indentedLine("if (this == o) return true;");
        indentedLine("if (o == null || getClass() != o.getClass()) return false;\n");

        indent();
        genericClassName();
        sb.append(" ");
        sb.append("t = (");
        genericClassName();
        sb.append(") o;\n\n");

        if (n > 1) {
            indentedLinePrefix("if ");
            notEqualsBranch(0);

            for (int i = 1; i < n - 1; ++i) {
                indentedLinePrefix("else if ");
                notEqualsBranch(i);
            }
        }
        indentedLinePrefix("return ");
        equalsExpression(n - 1);
        sb.append(";\n");
        closeBlock();
    }

    private void closeBlock() {
        --indentLevel;
        indent();
        sb.append("}\n");
    }

    private void notEqualsBranch(int i) {
        sb.append("(!");
        equalsExpression(i);
        sb.append(") return false;\n");
    }

    private void equalsExpression(int i) {
        variable(i);
        sb.append(".equals(t.");
        variable(i);
        sb.append(")");
    }

    private void genericClassName() {
        className();
        sb.append("<?");
        for (int i = 1; i < n; ++i) {
            sb.append(", ?");
        }
        sb.append(">");
    }

    private void className() {
        sb.append(CLASS_PREFIX).append(n);
    }

    private void packageDeclaration() {
        sb.append("package secpriv.horst.data.tuples;\n");
    }

    private void imports() {
        sb.append("import java.util.Arrays;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.Map;\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.Objects;\n");
        sb.append("import secpriv.horst.data.BaseTypeValue;\n");
    }

    private void constructorDefinition() {
        indent();
        sb.append("public ");
        className();
        constructorArguments();
        sb.append(" ");
        openBlock();
        for (int i = 0; i < n; ++i) {
            indentedLinePrefix("this.");
            variable(i);
            sb.append(" = ").append("Objects.requireNonNull(");
            variable(i);
            sb.append(", \"");
            variable(i);
            sb.append(" is null!\");\n");
        }
        closeBlock();
    }


    private void constructorArguments() {
        sb.append("(");
        variableDeclaration(0);
        for (int i = 1; i < n; ++i) {
            sb.append(", ");
            variableDeclaration(i);
        }
        sb.append(")");
    }

    private void variableDeclaration(int i) {
        typeParameter(i);
        sb.append(" ");
        variable(i);
    }

    private void memberVariables() {
        for (int i = 0; i < n; ++i) {
            indentedLinePrefix("public final ");
            variableDeclaration(i);
            sb.append(";\n");
        }
    }

    private void indent() {
        for (int i = 0; i < indentLevel; ++i) {
            sb.append(INDENT);
        }
    }

    private void variable(int i) {
        sb.append(VARIABLE_PREFIX).append(i);
    }

    private void typeParameters() {
        sb.append("<");
        typeParameter(0);
        for (int i = 1; i < n; ++i) {
            sb.append(", ");
            typeParameter(i);
        }
        sb.append(">");
    }

    private void typeParameter(int i) {
        sb.append(TYPE_PARAM_PREFIX).append(i);
    }
}
