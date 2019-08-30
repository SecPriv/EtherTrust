package secpriv.horst.internals;

// Adapted from https://gist.github.com/chrisvest/9873843

import secpriv.horst.data.SelectorFunction;
import secpriv.horst.data.tuples.Tuples;
import secpriv.horst.types.Type;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static secpriv.horst.tools.Zipper.zipPredicate;

public class SelectorFunctionHelper {
    private final Pattern extractClassNamePattern = Pattern.compile("^(?:.*[/\\\\])*([^/\\\\]+)\\.java$");

    private static final Set<String> ignoredNames = new HashSet<>(Stream.concat(Arrays.stream(Object.class.getMethods()).map(Method::getName), Stream.of("unit")).collect(Collectors.toList()));
    private static final Map<Type, Class> baseTypeMap = Collections.unmodifiableMap(generateTypeClassMap());

    private static Map<Type, Class> generateTypeClassMap() {
        HashMap<Type, Class> ret = new HashMap<>();
        ret.put(Type.Integer, BigInteger.class);
        ret.put(Type.Boolean, Boolean.class);
        return ret;
    }

    private Map<String, Method> methods = new HashMap<>();
    private Map<String, Object> providers = new HashMap<>();

    public SelectorFunctionHelper() {
        try {
            Method method = UnitProvider.class.getMethod("unit");
            methods.put("unit", method);
            providers.put("unit", UnitProvider.instance);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unreachable Code!");
        }
    }

    public void compileSelectorFunctionsProvider(String sourceCodeFileName, List<String> arguments) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        byte[] fileContents;
        File file = new File(sourceCodeFileName);
        try (FileInputStream inputStream = new FileInputStream(file)) {
            fileContents = new byte[(int) file.length()];
            if (inputStream.read(fileContents) != fileContents.length) {
                throw new IOException("Insufficient number of bytes read!");
            }
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        Matcher matcher = extractClassNamePattern.matcher(sourceCodeFileName);

        if (!matcher.matches()) {
            //TODO handle error
            throw new RuntimeException("Could not extract class name");
        }

        String className = matcher.group(1);

        JavaFileObject compilationUnit = new StringJavaFileObject(className, new String(fileContents));

        SimpleJavaFileManager fileManager =
                new SimpleJavaFileManager(compiler.getStandardFileManager(null, null, null));

        JavaCompiler.CompilationTask compilationTask = compiler.getTask(
                null, fileManager, null, null, null, Arrays.asList(compilationUnit));

        compilationTask.call();

        CompiledClassLoader classLoader =
                new CompiledClassLoader(fileManager.getGeneratedOutputFiles());

        Class c = Class.forName(className, true, classLoader);

        Object o = null;

        try {
            java.lang.reflect.Constructor constructor = c.getConstructor(List.class);
            o = constructor.newInstance(arguments);
        } catch (NoSuchMethodException ignored) {
        }

        if (o == null) {
            o = c.newInstance();
        }

        registerProvider(o);
    }

    public int registerProvider(Object o) {
        int methodCount = 0;
        for (Method method : o.getClass().getMethods()) {
            if (ignoredNames.contains(method.getName())) {
                continue;
            }
            if (methods.containsKey(method.getName())) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (!(method.getGenericReturnType() instanceof ParameterizedType)) {
                continue;
            }

            ParameterizedType iterableReturnType = (ParameterizedType) method.getGenericReturnType();
            java.lang.reflect.Type returnType = iterableReturnType.getActualTypeArguments()[0];

            if (iterableReturnType.getRawType() != Iterable.class) {
                continue;
            }

            if (!baseTypeMap.containsValue(returnType)) {
                if (!Tuples.isTupleType(returnType)) {
                    continue;
                }
            }

            if (Tuples.isTupleType(returnType)) {
                if (!(returnType instanceof ParameterizedType)) {
                    continue;
                }
                if (Arrays.stream(((ParameterizedType) returnType).getActualTypeArguments()).anyMatch(c -> !baseTypeMap.containsValue(c))) {
                    continue;
                }
            }

            if (Arrays.stream(method.getParameterTypes()).anyMatch(c -> !baseTypeMap.containsValue(c))) {
                continue;
            }

            ++methodCount;
            methods.put(method.getName(), method);
            providers.put(method.getName(), o);
        }
        return methodCount;
    }

    public Optional<Method> getMethod(String name, List<Type> parameterTypes, List<Type> returnTypes) {
        if (!methods.containsKey(name)) {
            //TODO handle error
            return Optional.empty();
        }

        Method method = methods.get(name);

        if (returnTypes.size() == 0) {
            throw new IllegalArgumentException("Selector function have to have at least one return type!");
        }

        if (!zipPredicate(Arrays.asList(method.getParameterTypes()), parameterTypes, (mt, st) -> mt.equals(baseTypeMap.get(st)))) {
            //TODO report error
            return Optional.empty();
        }

        ParameterizedType iterableReturnType = (ParameterizedType) method.getGenericReturnType();
        java.lang.reflect.Type returnType = iterableReturnType.getActualTypeArguments()[0];

        if (returnTypes.size() == 1) {
            if (!baseTypeMap.containsValue(returnType)) {
                //TODO handle error
                return Optional.empty();
            }
            if (!baseTypeMap.get(returnTypes.get(0)).equals(returnType)) {
                //TODO handle error
                return Optional.empty();
            }
            return Optional.of(method);
        }

        if (!(returnType instanceof ParameterizedType)) {
            //TODO report error
            return Optional.empty();
        }

        ParameterizedType parameterizedTupleType = (ParameterizedType) returnType;

        if (!parameterizedTupleType.getRawType().equals(Tuples.getClassForParameterCount(returnTypes.size()))) {
            //TODO report error
            return Optional.empty();
        }

        if (!zipPredicate(Arrays.asList(parameterizedTupleType.getActualTypeArguments()), returnTypes, (t, rt) -> baseTypeMap.get(rt).equals(t))) {
            //TODO report error
            return Optional.empty();
        }

        return Optional.of(method);
    }

    public Method getMethod(SelectorFunction selectorFunction) {
        if (selectorFunction == SelectorFunction.Unit) {
            return methods.get("unit");
        }
        return getMethod(selectorFunction.name, selectorFunction.parameterTypes, selectorFunction.returnTypes).get();
    }

    public Object getProvider(SelectorFunction selectorFunction) {
        return providers.get(selectorFunction.name);
    }

    private static class StringJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        StringJavaFileObject(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static class ClassJavaFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream outputStream;
        private final String className;

        ClassJavaFileObject(String className, Kind kind) {
            super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
            this.className = className;
            outputStream = new ByteArrayOutputStream();
        }

        @Override
        public OutputStream openOutputStream() {
            return outputStream;
        }

        byte[] getBytes() {
            return outputStream.toByteArray();
        }

        String getClassName() {
            return className;
        }
    }

    private static class SimpleJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final List<ClassJavaFileObject> outputFiles;

        SimpleJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
            outputFiles = new ArrayList<>();
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            ClassJavaFileObject file = new ClassJavaFileObject(className, kind);
            outputFiles.add(file);
            return file;
        }

        List<ClassJavaFileObject> getGeneratedOutputFiles() {
            return outputFiles;
        }
    }

    private static class CompiledClassLoader extends ClassLoader {
        private final List<ClassJavaFileObject> files;

        private CompiledClassLoader(List<ClassJavaFileObject> files) {
            this.files = files;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Iterator<ClassJavaFileObject> itr = files.iterator();
            while (itr.hasNext()) {
                ClassJavaFileObject file = itr.next();
                if (file.getClassName().equals(name)) {
                    itr.remove();
                    byte[] bytes = file.getBytes();
                    return super.defineClass(name, bytes, 0, bytes.length);
                }
            }
            return super.findClass(name);
        }
    }
}
