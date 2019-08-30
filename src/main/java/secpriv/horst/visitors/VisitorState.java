package secpriv.horst.visitors;

import secpriv.horst.data.*;
import secpriv.horst.internals.SelectorFunctionHelper;
import secpriv.horst.internals.error.handling.ErrorHandler;
import secpriv.horst.internals.error.handling.ErrorOutputHandler;
import secpriv.horst.parser.ASParser;
import secpriv.horst.types.Constructor;
import secpriv.horst.types.Type;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

public class VisitorState {
    private Map<String, Type> freeVars = new HashMap<>();
    private Map<String, Type> parameterVars = new HashMap<>();
    private Map<String, Type> vars = new HashMap<>();

    private Map<String, Expression.ConstExpression> constants = new HashMap<>();
    private Map<String, SelectorFunction> selectorFunctions = new HashMap<>();
    private Map<String, Predicate> predicates = new HashMap<>();

    private Map<String, String> freeVarOverrides = new HashMap<>();

    private Map<String, MacroDefinition> macros = new HashMap<>();
    private Map<String, Expression> macroVarBindings = new HashMap<>();

    private Map<String, Rule> rules = new HashMap<>();
    private Map<String, Type.NonParameterizedType> types = new LinkedHashMap<>();
    private LinkedHashMap<String, Operation> operations = new LinkedHashMap<>();
    private Map<SelectorFunction, Method> selectorFunctionImplementations = new HashMap<>();
    private SelectorFunctionHelper selectorFunctionHelper;

    private List<String> queryIds = new ArrayList<>();
    private List<String> testIds = new ArrayList<>();
    private Map<String, TestResult> testResults = new HashMap<>();

    private static final String ARRAY_START_MARKER = "array<";
    private static final String ARRAY_END_MARKER = ">";

    private int macroLevel = Integer.MAX_VALUE;

    public ErrorHandler errorHandler;

    public int getMacroLevel() {
        return macroLevel;
    }

    public void setMacroLevel(int macroLevel) {
        this.macroLevel = macroLevel;
    }

    public boolean defineRule(Rule rule) {
        return defineStringMapping(rule.name, rule, rules);
    }

    public void setSelectorFunctionHelper(SelectorFunctionHelper compiler) {
        selectorFunctionHelper = compiler;
    }

    public boolean hasSelectorFunctionImplementation(String selectorFunctionName, List<Type> parameterTypes, List<Type> returnTypes) {
        Optional<Method> optMethod = selectorFunctionHelper.getMethod(selectorFunctionName, parameterTypes, returnTypes);
        return optMethod.isPresent();
    }

    public Map<String, Operation> getOperations() {
        return Collections.unmodifiableMap(operations);
    }

    public Map<String, Rule> getRules() {
        return rules;
    }

    public boolean defineQuery(Rule queryAsRule) {
        queryIds.add(queryAsRule.name);
        return defineRule(queryAsRule);
    }

    boolean isQueryOrTest(Rule rule) {
        return Stream.concat(queryIds.stream(), testIds.stream()).anyMatch(s -> rule.name.startsWith(s + "_") || s.equals(rule.name));
    }

    boolean isTest(Rule test) {
        return testIds.stream().anyMatch(s -> test.name.startsWith(s + "_") || s.equals(test.name));
    }

    public boolean isExpectedTestResult(Rule test, TestResult result) {
        Optional<TestResult> optResult = testResults.entrySet().stream().filter(s -> test.name.startsWith(s.getKey() + "_") || s.getKey().equals(test.name)).map(Map.Entry::getValue).findFirst();
        return optResult.filter(testResult -> testResult == result).isPresent();

    }

    public enum TestResult {
        SAT, UNSAT
    }

    private boolean defineTest(Rule testAsRule, TestResult testResult) {
        testIds.add(testAsRule.name);
        testResults.put(testAsRule.name, testResult);
        return defineRule(testAsRule);
    }

    public boolean defineSatisfiableTest(Rule testAsRule) {
        return defineTest(testAsRule, TestResult.SAT);
    }

    public boolean defineUnsatisfiableTest(Rule testAsRule) {
        return defineTest(testAsRule, TestResult.UNSAT);
    }

    public static class MacroDefinition {
        public final String name;
        public final List<String> arguments;
        public final List<Type> argumentTypes;
        public final Map<String, Type> freeVars;
        public final int macroLevel;
        public final ASParser.MacroBodyContext body;

        private static int macroLevelCounter = 0;


        public MacroDefinition(String name, List<String> arguments, List<Type> argumentTypes, Map<String, Type> freeVars, ASParser.MacroBodyContext body) {
            this.name = Objects.requireNonNull(name, "Name may not be null!");
            this.arguments = Collections.unmodifiableList(Objects.requireNonNull(arguments, "Arguments may not be null!"));
            this.argumentTypes = Collections.unmodifiableList(Objects.requireNonNull(argumentTypes, "ArgumentTypes may not be null!"));
            this.freeVars = Collections.unmodifiableMap(Objects.requireNonNull(freeVars, "FreeVars may not be null!"));
            this.body = Objects.requireNonNull(body, "Body may not be null!");

            macroLevel = macroLevelCounter++;
        }
    }

    public VisitorState() {
        this.errorHandler = new ErrorOutputHandler();
    }

    public VisitorState(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public VisitorState(VisitorState state) {
        freeVars = new HashMap<>(state.freeVars);
        parameterVars = new HashMap<>(state.parameterVars);
        vars = new HashMap<>(state.vars);
        constants = new HashMap<>(state.constants);
        selectorFunctions = new HashMap<>(state.selectorFunctions);

        freeVarOverrides = new HashMap<>(state.freeVarOverrides);
        macroVarBindings = new HashMap<>(state.macroVarBindings);
        macros = new HashMap<>(state.macros);
        selectorFunctionImplementations = new HashMap<>(state.selectorFunctionImplementations);

        types = new LinkedHashMap<>(state.types);
        operations = new LinkedHashMap<>(state.operations);

        predicates = new HashMap<>(state.predicates);
        macroLevel = state.macroLevel;
        rules = new HashMap<>(rules);

        selectorFunctionHelper = state.selectorFunctionHelper;
        this.errorHandler = state.errorHandler;
        queryIds = new ArrayList<>(state.queryIds);
        testIds = new ArrayList<>(state.testIds);
        testResults = new HashMap<>(state.testResults);
    }

    public Optional<Type> getTypeOfVar(String name) {
        return lookUpNullable(name, vars);
    }

    public boolean defineVar(String name, Type type) {
        return defineStringMapping(name, type, vars);
    }

    public boolean defineFreeVar(String name, Type type) {
        return defineStringMapping(name, type, freeVars);
    }

    public boolean defineVar(Expression.VarExpression exp) {
        return defineStringMapping(exp.name, exp.getType(), vars);
    }

    public boolean defineParameterVar(String name, Type type) {
        return defineStringMapping(name, type, parameterVars);
    }

    public boolean defineParameterVar(Expression.ParVarExpression exp) {
        return defineStringMapping(exp.name, exp.getType(), parameterVars);
    }

    public boolean defineConstant(Expression.ConstExpression constant) {
        return defineStringMapping(constant.name, constant, constants);
    }

    public boolean defineSelectorFunction(SelectorFunction selectorFunction) {
        Optional<Method> optMethod = selectorFunctionHelper.getMethod(selectorFunction.name, selectorFunction.parameterTypes, selectorFunction.returnTypes);
        selectorFunctionImplementations.put(selectorFunction, optMethod.get());
        return defineStringMapping(selectorFunction.name, selectorFunction, selectorFunctions);
    }

    public Optional<Type> getTypeOfParameterVar(String name) {
        return lookUpNullable(name, parameterVars);
    }

    public boolean defineMacro(MacroDefinition macro) {
        return defineStringMapping(macro.name, macro, macros);
    }

    public Optional<MacroDefinition> getMacro(String name, int macroLevel) {
        return lookUpNullable(name, macros).filter(m -> m.macroLevel < macroLevel);
    }

    public boolean definePredicate(Predicate predicate) {
        return defineStringMapping(predicate.name, predicate, predicates);
    }

    public boolean defineMacroVarBinding(String name, Expression expression) {
        return defineStringMapping(name, expression, macroVarBindings);
    }

    public Optional<Expression> getMacroVarBinding(String name) {
        return lookUpNullable(name, macroVarBindings);
    }

    private <T> boolean defineStringMapping(String name, T t, Map<String, T> map) {
        if (map.containsKey(name)) {
            return false;
        } else {
            map.put(name, t);
            return true;
        }
    }

    public <T> Optional<T> lookUpNullable(String name, Map<String, T> map) {
        return Optional.ofNullable(map.get(name));
    }

    public boolean defineType(Type.NonParameterizedType type) {
        String name = type.name;
        if (types.containsKey(name)) {
            return false;
        } else {
            class CheckConstructorAlreadyDefinedVisitor implements Type.Visitor<Boolean> {
                @Override
                public Boolean visit(Type.BooleanType type) {
                    return false;
                }

                @Override
                public Boolean visit(Type.IntegerType type) {
                    return false;
                }

                @Override
                public Boolean visit(Type.CustomType type) {
                    return type.constructors.stream().anyMatch(k -> VisitorState.this.types.values().stream().anyMatch(t -> t.hasConstructor(k.name)));
                }

                @Override
                public Boolean visit(Type.ArrayType type) {
                    throw new UnsupportedOperationException("Cannot define array type in VisitorState!");
                }
            }
            if (type.accept(new CheckConstructorAlreadyDefinedVisitor())) {
                //TODO report error
                return false;
            }
            types.put(name, type);
            return true;
        }
    }

    public Optional<? extends Type> getType(String name) {
        if (name.startsWith(ARRAY_START_MARKER)) {
            if (name.endsWith(ARRAY_END_MARKER)) {
                String innerName = name.substring(ARRAY_START_MARKER.length());
                innerName = innerName.substring(0, innerName.length() - ARRAY_END_MARKER.length());

                return getType(innerName).map(Type.Array::of);
            } else {
                throw new RuntimeException("Malformed ArrayType Definition!");
            }
        }

        return lookUpNullable(name, types);
    }

    public Optional<? extends Type> getTypeForConstructor(String constructorName) {
        return types.values().stream().filter(t -> t.hasConstructor(constructorName)).findFirst();
    }

    public Optional<Constructor> getConstructorByName(String constructorName) {
        return types.values().stream().map(t -> t.getConstructorByName(constructorName)).filter(Optional::isPresent).findFirst().map(Optional::get);
    }

    public Optional<Operation> getOperation(String name) {
        return lookUpNullable(name, operations);
    }

    public boolean defineOperation(Operation operation) {
        return defineStringMapping(operation.name, operation, operations);
    }

    public boolean isConstantDefined(String constID) {
        return constants.containsKey(constID);
    }

    public Optional<Expression.ConstExpression> getConstant(String constID) {
        return lookUpNullable(constID, constants);
    }

    public boolean isSelectorFunctionDefined(String name) {
        return selectorFunctions.containsKey(name);
    }

    public Optional<SelectorFunction> getSelectorFunction(String name) {
        return lookUpNullable(name, selectorFunctions);
    }

    public void overrideFreeVar(String oldName, String newName, Type type) {
        freeVars.put(newName, type);
        freeVarOverrides.put(oldName, newName);
    }

    public static class FreeVarLookupResult {
        public final Type type;
        public final String name;

        public FreeVarLookupResult(Type type) {
            this(type, null);
        }

        public FreeVarLookupResult(Type type, String name) {
            this.type = type;
            this.name = name;
        }

        public boolean isOverridden() {
            return name != null;
        }
    }

    public Optional<FreeVarLookupResult> getTypeOfFreeVar(String name) {
        String override = freeVarOverrides.get(name);
        String newName = override != null ? override : name;
        return lookUpNullable(newName, freeVars).map(t -> new FreeVarLookupResult(t, newName));
    }

    public boolean isPredicateDefined(String name) {
        return predicates.containsKey(name);
    }

    public Optional<Predicate> getPredicate(String name) {
        return lookUpNullable(name, predicates);
    }
}
