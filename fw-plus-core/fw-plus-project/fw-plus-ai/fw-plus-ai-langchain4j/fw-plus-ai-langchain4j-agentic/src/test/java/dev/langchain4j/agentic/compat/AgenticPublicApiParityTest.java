package dev.langchain4j.agentic.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.function.Consumer;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.RecordComponentVisitor;
import org.springframework.asm.TypePath;

/** Fixed-upstream API and Java 8 source compatibility contract for Agentic. */
class AgenticPublicApiParityTest {

    private static final String AGENTIC_PREFIX = "dev/langchain4j/agentic/";
    private static final Set<String> HELPERS = new LinkedHashSet<String>(Arrays.asList(
            "dev.langchain4j.agentic.internal.AgenticRecordComponentMetadata",
            "dev.langchain4j.agentic.internal.AgenticRecordComponentMetadata$ComponentMetadata",
            "dev.langchain4j.agentic.internal.AgenticRecordComponentMetadata$TypeMetadata",
            "dev.langchain4j.agentic.internal.CollectionHelper",
            "dev.langchain4j.agentic.internal.StringHelper"));
    private static final Set<String> RECORD_GENERATED_METHODS =
            new HashSet<String>(Arrays.asList("equals", "hashCode", "toString"));

    @Test
    void publicAndProtectedApiMatchesFixedUpstream() throws Exception {
        Path officialJar = requiredPropertyPath("agentic.official.jar");
        Path localClasses = requiredPropertyPath("agentic.local.classes");
        assertEquals("langchain4j-agentic-1.19.0-beta29.jar", officialJar.getFileName().toString());

        Map<String, ApiSnapshot> upstream = readJar(officialJar);
        Map<String, ApiSnapshot> local = readDirectory(localClasses);
        Set<String> missing = new TreeSet<String>(upstream.keySet());
        missing.removeAll(local.keySet());
        assertTrue(missing.isEmpty(), "missing Agentic named types: " + missing);

        Set<String> unexpected = new TreeSet<String>(local.keySet());
        unexpected.removeAll(upstream.keySet());
        assertEquals(HELPERS, unexpected, "unexpected Agentic named types");

        for (Map.Entry<String, ApiSnapshot> entry : upstream.entrySet()) {
            ApiSnapshot expected = entry.getValue();
            ApiSnapshot actual = local.get(entry.getKey());
            assertNotNull(actual, entry.getKey());
            assertApiEquivalent(expected, actual, entry.getKey());
        }
    }

    @Test
    void fixedSourcesContainExactlyTheManifestFiles() throws Exception {
        Path sourcesJar = requiredPropertyPath("agentic.official.sources");
        Path manifest = requiredPropertyPath("agentic.manifest");
        Path modifiedManifest = requiredPropertyPath("agentic.modified.manifest");
        Set<String> expected = new LinkedHashSet<String>();
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            if (!line.isEmpty() && line.charAt(0) != '#') {
                expected.add(line);
            }
        }
        Set<String> actual = new LinkedHashSet<String>();
        try (JarFile jar = new JarFile(sourcesJar.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(AGENTIC_PREFIX) && name.endsWith(".java")) {
                    actual.add(name);
                }
            }
        }
        assertEquals(153, expected.size());
        assertEquals(expected, actual);

        List<String> declaredModifiedList = manifestLines(modifiedManifest);
        assertEquals(new ArrayList<String>(new TreeSet<String>(declaredModifiedList)),
                declaredModifiedList, "modified manifest must be sorted and unique");
        Set<String> declaredModified = new LinkedHashSet<String>(declaredModifiedList);
        assertEquals(59, declaredModified.size(), "modified source count");
        assertTrue(expected.containsAll(declaredModified),
                "modified manifest contains paths outside the upstream manifest");

        Set<String> actualModified = new LinkedHashSet<String>();
        Path sourceRoot = requiredPropertyPath("agentic.source.root");
        try (JarFile jar = new JarFile(sourcesJar.toFile())) {
            for (String path : expected) {
                JarEntry entry = jar.getJarEntry(path);
                assertNotNull(entry, "fixed sources JAR entry " + path);
                byte[] official;
                try (InputStream input = jar.getInputStream(entry)) {
                    official = readAll(input);
                }
                byte[] local = Files.readAllBytes(sourceRoot.resolve(path));
                if (!Arrays.equals(official, local)) {
                    actualModified.add(path);
                }
            }
        }
        assertEquals(declaredModified, actualModified,
                "modified manifest differs from the fixed sources JAR byte comparison");
    }

    private static List<String> manifestLines(Path manifest) throws IOException {
        List<String> lines = new ArrayList<String>();
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            if (!line.isEmpty() && line.charAt(0) != '#') {
                lines.add(line);
            }
        }
        return lines;
    }

    @Test
    void comparatorRejectsClassFieldAndMethodAccessDrift() {
        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(
                        snapshot(classWithAccess(Opcodes.ACC_PUBLIC)),
                        snapshot(classWithAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT)),
                        "synthetic.AccessApi"));
        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(
                        snapshot(classWithFieldAccess(Opcodes.ACC_PUBLIC)),
                        snapshot(classWithFieldAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE)),
                        "synthetic.FieldAccessApi"));
        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(
                        snapshot(classWithMethodAccess(Opcodes.ACC_PUBLIC)),
                        snapshot(classWithMethodAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_VARARGS)),
                        "synthetic.MethodAccessApi"));
    }

    @Test
    void comparatorRejectsClassGenericSignatureDrift() {
        ApiSnapshot expected = snapshot(classWithSignature(
                "Ljava/lang/Object;Ljava/util/function/Supplier<Ljava/lang/String;>;"));
        ApiSnapshot actual = snapshot(classWithSignature(
                "Ljava/lang/Object;Ljava/util/function/Supplier<Ljava/lang/Integer;>;"));

        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(expected, actual, "synthetic.GenericApi"));
    }

    @Test
    void comparatorRejectsMethodThrowsDrift() {
        ApiSnapshot expected = snapshot(classWithMethodException("java/io/IOException"));
        ApiSnapshot actual = snapshot(classWithMethodException("java/sql/SQLException"));

        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(expected, actual, "synthetic.ThrowsApi"));
    }

    @Test
    void comparatorRejectsFieldConstantDrift() {
        ApiSnapshot expected = snapshot(classWithConstant(1));
        ApiSnapshot actual = snapshot(classWithConstant(2));

        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(expected, actual, "synthetic.ConstantApi"));
    }

    @Test
    void comparatorRejectsFieldAnnotationDrift() {
        ApiSnapshot expected = snapshot(classWithFieldAnnotation("expected"));
        ApiSnapshot actual = snapshot(classWithFieldAnnotation("actual"));

        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(expected, actual, "synthetic.FieldAnnotatedApi"));
    }

    @Test
    void comparatorRejectsAnnotationValueAndDefaultDrift() {
        ApiSnapshot expectedValue = snapshot(classWithAnnotationValue("expected"));
        ApiSnapshot actualValue = snapshot(classWithAnnotationValue("actual"));
        ApiSnapshot expectedDefault = snapshot(annotationWithDefault("expected"));
        ApiSnapshot actualDefault = snapshot(annotationWithDefault("actual"));

        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(expectedValue, actualValue, "synthetic.AnnotatedApi"));
        assertThrows(AssertionError.class,
                () -> assertApiEquivalent(expectedDefault, actualDefault, "synthetic.DefaultedAnnotation"));
    }

    @Test
    void majorAgenticEntryPointsCompileWithJava8() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK8 JavaCompiler is required");
        Path root = Files.createTempDirectory("agentic-java8-example");
        Path source = root.resolve("Example.java");
        Files.write(source, exampleSource().getBytes(StandardCharsets.UTF_8));
        Path output = root.resolve("classes");
        Files.createDirectories(output);
        StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        try {
            manager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(output.toFile()));
            Iterable<? extends JavaFileObject> units = manager.getJavaFileObjects(source.toFile());
            List<String> options = Arrays.asList("-source", "8", "-target", "8", "-proc:none",
                    "-classpath", System.getProperty("java.class.path"));
            Boolean compiled = compiler.getTask(null, manager, null, options, null, units).call();
            assertTrue(Boolean.TRUE.equals(compiled), "Agentic Java 8 example did not compile");
        } finally {
            manager.close();
            deleteTree(root);
        }
    }

    private static void assertApiEquivalent(ApiSnapshot expected, ApiSnapshot actual,
                                            String binaryName) {
        assertEquals(expected.kind, actual.kind, binaryName);
        assertEquals(expected.classAccessFor(expected.record),
                actual.classAccessFor(expected.record), binaryName);
        assertEquals(expected.superName, actual.superName, binaryName);
        assertEquals(expected.classSignatureFor(expected.record),
                actual.classSignatureFor(expected.record), binaryName);
        assertEquals(expected.interfaces, actual.interfaces, binaryName);
        assertEquals(expected.fields, actual.fields, binaryName);
        assertEquals(expected.methodsFor(expected.record), actual.methodsFor(expected.record), binaryName);
        assertEquals(expected.annotationsForJava8(binaryName),
                actual.annotationsForJava8(binaryName), binaryName);
        if (expected.record) {
            assertTrue(actual.finalClass, binaryName);
            assertFalse(actual.record, binaryName);
            assertRecordComponentAccessors(expected, actual, binaryName);
        } else {
            assertEquals(expected.record, actual.record, binaryName);
        }
    }

    private static void assertRecordComponentAccessors(ApiSnapshot expected, ApiSnapshot actual,
                                                       String binaryName) {
        assertFalse(expected.recordComponents.isEmpty(), binaryName + " has no upstream record components");
        for (String component : expected.recordComponents) {
            String[] parts = component.split("\\|", -1);
            assertEquals(3, parts.length, binaryName + " malformed record component: " + component);
            String methodSignature = parts[2].isEmpty() ? "" : "()" + parts[2];
            String accessor = parts[0] + "|()" + parts[1] + "|" + methodSignature + "|public|";
            assertTrue(expected.methods.contains(accessor),
                    binaryName + " upstream record accessor missing: " + accessor);
            assertTrue(actual.methods.contains(accessor),
                    binaryName + " Java 8 record accessor mismatch: " + accessor);
        }
    }

    private static ApiSnapshot snapshot(byte[] bytes) {
        ApiSnapshot snapshot = new ApiSnapshot();
        new ClassReader(bytes).accept(snapshot,
                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return snapshot;
    }

    private static byte[] classWithAccess(int access) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, access, "synthetic/AccessApi", null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithFieldAccess(int access) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "synthetic/FieldAccessApi", null,
                "java/lang/Object", null);
        writer.visitField(access, "value", "I", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithMethodAccess(int access) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "synthetic/MethodAccessApi", null,
                "java/lang/Object", null);
        writer.visitMethod(access, "call", "([Ljava/lang/String;)V", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithSignature(String signature) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "synthetic/GenericApi", signature,
                "java/lang/Object", new String[] {"java/util/function/Supplier"});
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithMethodException(String exception) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "synthetic/ThrowsApi", null,
                "java/lang/Object", null);
        writer.visitMethod(Opcodes.ACC_PUBLIC, "call", "()V", null,
                new String[] {exception}).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithConstant(int value) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "synthetic/ConstantApi", null,
                "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "VALUE", "I", null, Integer.valueOf(value)).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithFieldAnnotation(String value) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "synthetic/FieldAnnotatedApi", null,
                "java/lang/Object", null);
        FieldVisitor field = writer.visitField(Opcodes.ACC_PUBLIC, "value", "Ljava/lang/String;", null, null);
        AnnotationVisitor annotation = field.visitAnnotation("Lsynthetic/Marker;", true);
        annotation.visit("value", value);
        annotation.visitEnd();
        field.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] classWithAnnotationValue(String value) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "synthetic/AnnotatedApi", null,
                "java/lang/Object", null);
        AnnotationVisitor annotation = writer.visitAnnotation("Lsynthetic/Marker;", true);
        annotation.visit("value", value);
        annotation.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] annotationWithDefault(String value) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION,
                "synthetic/DefaultedAnnotation", null, "java/lang/Object",
                new String[] {"java/lang/annotation/Annotation"});
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "value", "()Ljava/lang/String;", null, null);
        AnnotationVisitor defaultValue = method.visitAnnotationDefault();
        defaultValue.visit(null, value);
        defaultValue.visitEnd();
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static String exampleSource() {
        return "import dev.langchain4j.agentic.AgenticServices;\n"
                + "import dev.langchain4j.agentic.observability.AgentMonitor;\n"
                + "public class Example {\n"
                + "  public static void compileEntryPoints() {\n"
                + "    AgenticServices.agentBuilder();\n"
                + "    AgenticServices.sequenceBuilder();\n"
                + "    AgenticServices.parallelBuilder();\n"
                + "    AgenticServices.parallelMapperBuilder();\n"
                + "    AgenticServices.loopBuilder();\n"
                + "    AgenticServices.conditionalBuilder();\n"
                + "    AgenticServices.plannerBuilder();\n"
                + "    AgenticServices.supervisorBuilder();\n"
                + "    AgenticServices.humanInTheLoopBuilder();\n"
                + "    new AgentMonitor();\n"
                + "  }\n"
                + "}\n";
    }

    private static Path requiredPropertyPath(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, "missing required property: " + name);
        Path path = Paths.get(value).toAbsolutePath().normalize();
        assertTrue(Files.exists(path), "missing required path " + name + ": " + path);
        return path;
    }

    private static Map<String, ApiSnapshot> readJar(Path jarPath) throws IOException {
        Map<String, ApiSnapshot> result = new LinkedHashMap<String, ApiSnapshot>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!namedAgenticClass(entry.getName())) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    put(result, new ClassReader(readAll(input)));
                }
            }
        }
        return result;
    }

    private static Map<String, ApiSnapshot> readDirectory(Path root) throws IOException {
        Map<String, ApiSnapshot> result = new LinkedHashMap<String, ApiSnapshot>();
        List<Path> files = new ArrayList<Path>();
        Files.walk(root).forEach(path -> {
            if (Files.isRegularFile(path) && path.toString().endsWith(".class")) {
                files.add(path);
            }
        });
        for (Path file : files) {
            String entry = root.relativize(file).toString().replace(File.separatorChar, '/');
            if (namedAgenticClass(entry)) {
                put(result, new ClassReader(Files.readAllBytes(file)));
            }
        }
        return result;
    }

    private static boolean namedAgenticClass(String entry) {
        if (!entry.startsWith(AGENTIC_PREFIX) || !entry.endsWith(".class")) {
            return false;
        }
        String binary = entry.substring(0, entry.length() - ".class".length());
        return !binary.matches(".*\\$[0-9]+(?:$|\\$.*)") && !binary.contains("$$Lambda$");
    }

    private static void put(Map<String, ApiSnapshot> result, ClassReader reader) {
        ApiSnapshot snapshot = new ApiSnapshot();
        reader.accept(snapshot, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        assertNullDuplicate(result, snapshot.name);
        result.put(snapshot.name, snapshot);
    }

    private static void assertNullDuplicate(Map<String, ApiSnapshot> result, String name) {
        assertFalse(result.containsKey(name), "duplicate named class: " + name);
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void deleteTree(Path root) throws IOException {
        List<Path> paths = new ArrayList<Path>();
        Files.walk(root).forEach(paths::add);
        Collections.sort(paths, (left, right) -> right.toString().length() - left.toString().length());
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    private static final class ApiSnapshot extends ClassVisitor {
        private String name;
        private String kind;
        private String superName;
        private String classSignature;
        private int classAccess;
        private boolean record;
        private boolean finalClass;
        private final Set<String> interfaces = new TreeSet<String>();
        private final Set<String> fields = new TreeSet<String>();
        private final Set<String> methods = new TreeSet<String>();
        private final Set<String> annotations = new TreeSet<String>();
        private final List<String> recordComponents = new ArrayList<String>();

        private ApiSnapshot() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.name = name.replace('/', '.');
            this.classSignature = nullToEmpty(signature);
            this.classAccess = access;
            this.record = (access & Opcodes.ACC_RECORD) != 0;
            this.finalClass = (access & Opcodes.ACC_FINAL) != 0;
            this.kind = (access & Opcodes.ACC_ANNOTATION) != 0 ? "annotation"
                    : (access & Opcodes.ACC_ENUM) != 0 ? "enum"
                    : (access & Opcodes.ACC_INTERFACE) != 0 ? "interface" : "class";
            this.superName = this.record ? "java/lang/Object" : superName;
            if (interfaces != null) {
                this.interfaces.addAll(Arrays.asList(interfaces));
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return annotationVisitor("class|" + descriptor + "|" + visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                                                       String descriptor, boolean visible) {
            return annotationVisitor(
                    "class-type|" + typeRef + "|" + descriptor + "|" + visible);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            if (!visible(access)) {
                return null;
            }
            final String prefix = name + "|" + descriptor + "|" + nullToEmpty(signature) + "|"
                    + fieldAccessShape(access) + "|" + constantValue(value);
            fields.add(prefix);
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                    return annotationVisitor(
                            "field|" + prefix + "|" + annotation + "|" + visible);
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                                                               String annotation, boolean visible) {
                    return annotationVisitor(
                            "field-type|" + prefix + "|" + typeRef + "|" + annotation + "|" + visible);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if (!visible(access)) {
                return null;
            }
            final String prefix = name + "|" + descriptor + "|" + nullToEmpty(signature) + "|"
                    + methodAccessShape(access) + "|" + exceptionList(exceptions);
            methods.add(prefix);
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                    return annotationVisitor(
                            "method|" + prefix + "|" + annotation + "|" + visible);
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                                                               String annotation, boolean visible) {
                    return annotationVisitor(
                            "method-type|" + prefix + "|" + typeRef + "|" + annotation + "|" + visible);
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter,
                                                                   String annotation, boolean visible) {
                    return annotationVisitor(
                            "parameter|" + prefix + "|" + parameter + "|" + annotation + "|" + visible);
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return annotationVisitor("method-default|" + prefix);
                }
            };
        }

        private AnnotationVisitor annotationVisitor(final String prefix) {
            return new AnnotationValueCollector(value -> annotations.add(prefix + "|" + value));
        }

        private static final class AnnotationValueCollector extends AnnotationVisitor {
            private final Consumer<String> sink;
            private final Map<String, String> namedValues = new LinkedHashMap<String, String>();
            private final List<String> arrayValues = new ArrayList<String>();

            private AnnotationValueCollector(Consumer<String> sink) {
                super(Opcodes.ASM9);
                this.sink = sink;
            }

            @Override
            public void visit(String name, Object value) {
                add(name, annotationValue(value));
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                add(name, "enum(" + descriptor + ":" + value + ")");
            }

            @Override
            public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
                return new AnnotationValueCollector(value ->
                        add(name, "annotation(" + descriptor + ":" + value + ")"));
            }

            @Override
            public AnnotationVisitor visitArray(final String name) {
                return new AnnotationValueCollector(value -> add(name, "array" + value));
            }

            @Override
            public void visitEnd() {
                if (!arrayValues.isEmpty()) {
                    sink.accept(arrayValues.toString());
                    return;
                }
                List<String> keys = new ArrayList<String>(namedValues.keySet());
                Collections.sort(keys);
                List<String> values = new ArrayList<String>();
                for (String key : keys) {
                    values.add(key + "=" + namedValues.get(key));
                }
                sink.accept(values.toString());
            }

            private void add(String name, String value) {
                if (name == null) {
                    arrayValues.add(value);
                } else {
                    namedValues.put(name, value);
                }
            }
        }

        private static String annotationValue(Object value) {
            if (value == null) {
                return "null";
            }
            if (value.getClass().isArray()) {
                if (value instanceof Object[]) {
                    return Arrays.deepToString((Object[]) value);
                }
                if (value instanceof int[]) return Arrays.toString((int[]) value);
                if (value instanceof long[]) return Arrays.toString((long[]) value);
                if (value instanceof short[]) return Arrays.toString((short[]) value);
                if (value instanceof byte[]) return Arrays.toString((byte[]) value);
                if (value instanceof char[]) return Arrays.toString((char[]) value);
                if (value instanceof boolean[]) return Arrays.toString((boolean[]) value);
                if (value instanceof float[]) return Arrays.toString((float[]) value);
                if (value instanceof double[]) return Arrays.toString((double[]) value);
            }
            return value.getClass().getName() + ":" + value;
        }

        @Override
        public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
            recordComponents.add(name + "|" + descriptor + "|" + nullToEmpty(signature));
            return null;
        }

        private int classAccessFor(boolean normalizeRecord) {
            int mask = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL
                    | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION
                    | Opcodes.ACC_ENUM | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_RECORD;
            int access = classAccess & mask;
            return normalizeRecord ? access & ~Opcodes.ACC_RECORD : access;
        }

        private Set<String> annotationsForJava8(String binaryName) {
            if (!"dev.langchain4j.agentic.agent.AgentBuilder".equals(binaryName)) {
                return annotations;
            }
            Set<String> normalized = new TreeSet<String>();
            for (String annotation : annotations) {
                if (annotation.startsWith(
                        "method|maxSequentialToolsInvocations|(I)Ldev/langchain4j/agentic/agent/AgentBuilder;|")
                        && annotation.contains("|Ljava/lang/Deprecated;|true|")) {
                    int values = annotation.lastIndexOf('|');
                    annotation = annotation.substring(0, values + 1) + "[]";
                }
                normalized.add(annotation);
            }
            return normalized;
        }

        private String classSignatureFor(boolean normalizeRecordSuperclass) {
            if (!normalizeRecordSuperclass) {
                return classSignature;
            }
            return classSignature.replace("Ljava/lang/Record;", "Ljava/lang/Object;");
        }

        private Set<String> methodsFor(boolean normalizeRecordGenerated) {
            if (!normalizeRecordGenerated) {
                return methods;
            }
            Set<String> normalized = new TreeSet<String>();
            for (String method : methods) {
                String[] parts = method.split("\\|", -1);
                if (parts.length >= 4 && RECORD_GENERATED_METHODS.contains(parts[0])) {
                    parts[3] = "record-generated";
                    method = join(parts);
                }
                normalized.add(method);
            }
            return normalized;
        }

        private static String join(String[] values) {
            StringBuilder result = new StringBuilder();
            for (String value : values) {
                if (result.length() > 0) {
                    result.append('|');
                }
                result.append(value);
            }
            return result.toString();
        }

        private static boolean visible(int access) {
            return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
        }

        private static String fieldAccessShape(int access) {
            return visibility(access)
                    + flag(access, Opcodes.ACC_STATIC, "static")
                    + flag(access, Opcodes.ACC_FINAL, "final")
                    + flag(access, Opcodes.ACC_VOLATILE, "volatile")
                    + flag(access, Opcodes.ACC_TRANSIENT, "transient")
                    + flag(access, Opcodes.ACC_ENUM, "enum")
                    + flag(access, Opcodes.ACC_SYNTHETIC, "synthetic");
        }

        private static String methodAccessShape(int access) {
            return visibility(access)
                    + flag(access, Opcodes.ACC_STATIC, "static")
                    + flag(access, Opcodes.ACC_FINAL, "final")
                    + flag(access, Opcodes.ACC_ABSTRACT, "abstract")
                    + flag(access, Opcodes.ACC_SYNCHRONIZED, "synchronized")
                    + flag(access, Opcodes.ACC_BRIDGE, "bridge")
                    + flag(access, Opcodes.ACC_VARARGS, "varargs")
                    + flag(access, Opcodes.ACC_NATIVE, "native")
                    + flag(access, Opcodes.ACC_STRICT, "strict")
                    + flag(access, Opcodes.ACC_SYNTHETIC, "synthetic");
        }

        private static String visibility(int access) {
            return (access & Opcodes.ACC_PUBLIC) != 0 ? "public" : "protected";
        }

        private static String flag(int access, int flag, String name) {
            return (access & flag) != 0 ? "," + name : "";
        }

        private static String exceptionList(String[] exceptions) {
            if (exceptions == null || exceptions.length == 0) {
                return "";
            }
            Set<String> sorted = new TreeSet<String>(Arrays.asList(exceptions));
            return sorted.toString();
        }

        private static String constantValue(Object value) {
            return value == null ? "<none>" : value.getClass().getName() + ":" + value;
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
