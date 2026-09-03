package dev.langchain4j.agentic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Opcodes;

/**
 * Source contract for the explicit record-component metadata registry.
 *
 * <p>The expected table is copied from the fixed upstream public API/source contract. It is
 * intentionally independent from the migrated classes' fields and accessors.</p>
 */
public final class Task2a3MetadataContractTest {

    private static final String SOURCE_ROOT = "src/main/java";
    private static final String METADATA_SOURCE =
            "dev/langchain4j/agentic/internal/AgenticRecordComponentMetadata.java";
    private static final String SUPERVISOR_SOURCE =
            "dev/langchain4j/agentic/supervisor/SupervisorPlanner.java";

    private static final Map<String, ComponentSpec[]> EXPECTED =
            new LinkedHashMap<String, ComponentSpec[]>();

    static {
        put("dev.langchain4j.agentic.AgenticServices$DefaultDeclarativeAgentCreationContext",
                c("agentServiceClass", "java.lang.Class", "java.lang.Class<T>", "agentServiceClass"),
                c("agentBuilder", "dev.langchain4j.agentic.agent.AgentBuilder",
                        "dev.langchain4j.agentic.agent.AgentBuilder<T, ?>", "agentBuilder"));
        put("dev.langchain4j.agentic.AgenticServices$AgentConfigurator",
                c("configurator", "java.util.function.Consumer",
                        "java.util.function.Consumer<dev.langchain4j.agentic.AgenticServices$DeclarativeAgentCreationContext<?>>",
                        "configurator"),
                c("subAgentResolver", "java.util.function.Function",
                        "java.util.function.Function<java.lang.Class<?>, java.lang.Object>", "subAgentResolver"),
                c("agentInstanceFactory", "java.util.function.Function",
                        "java.util.function.Function<dev.langchain4j.agentic.internal.InternalAgent, java.lang.Object>",
                        "agentInstanceFactory"),
                c("defaultMemoryIdSupplier", "java.util.function.Supplier",
                        "java.util.function.Supplier<java.lang.Object>", "defaultMemoryIdSupplier"));
        put("dev.langchain4j.agentic.observability.BeforeAgentToolExecution",
                c("agentInstance", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agentInstance"),
                c("toolExecution", "dev.langchain4j.service.tool.BeforeToolExecution",
                        "dev.langchain4j.service.tool.BeforeToolExecution", "toolExecution"));
        put("dev.langchain4j.agentic.agent.ErrorContext",
                c("agentName", "java.lang.String", "java.lang.String", "agentName"),
                c("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope"),
                c("exception", "dev.langchain4j.agentic.agent.AgentInvocationException",
                        "dev.langchain4j.agentic.agent.AgentInvocationException", "exception"));
        put("dev.langchain4j.agentic.scope.AgentInvocation",
                c("agentType", "java.lang.Class", "java.lang.Class<?>", "agentType"),
                c("agentName", "java.lang.String", "java.lang.String", "agentName"),
                c("agentId", "java.lang.String", "java.lang.String", "agentId"),
                c("input", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "input"),
                c("output", "java.lang.Object", "java.lang.Object", "output"));
        put("dev.langchain4j.agentic.planner.InitPlanningContext",
                c("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope"),
                c("plannerAgent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "plannerAgent"),
                c("subagents", "java.util.List",
                        "java.util.List<dev.langchain4j.agentic.planner.AgentInstance>", "subagents"));
        put("dev.langchain4j.agentic.workflow.HumanInTheLoop",
                c("outputKey", "java.lang.String", "java.lang.String", "outputKey"),
                c("description", "java.lang.String", "java.lang.String", "description"),
                c("async", "boolean", "boolean", "async"),
                c("responseProvider", "java.util.function.Function",
                        "java.util.function.Function<dev.langchain4j.agentic.scope.AgenticScope, ?>", "responseProvider"),
                c("listener", "dev.langchain4j.agentic.observability.AgentListener",
                        "dev.langchain4j.agentic.observability.AgentListener", "listener"),
                c("arguments", "java.util.List",
                        "java.util.List<dev.langchain4j.agentic.planner.AgentArgument>", "arguments"));
        put("dev.langchain4j.agentic.observability.AgentRequest",
                c("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope"),
                c("agent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agent"),
                c("inputs", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "inputs"));
        put("dev.langchain4j.agentic.observability.AgentInvocationError",
                c("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope"),
                c("agent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agent"),
                c("inputs", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "inputs"),
                c("error", "java.lang.Throwable", "java.lang.Throwable", "error"));
        put("dev.langchain4j.agentic.observability.AgentResponse",
                c("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope"),
                c("agent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agent"),
                c("inputs", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "inputs"),
                c("output", "java.lang.Object", "java.lang.Object", "output"),
                c("chatRequest", "dev.langchain4j.model.chat.request.ChatRequest",
                        "dev.langchain4j.model.chat.request.ChatRequest", "chatRequest"),
                c("chatResponse", "dev.langchain4j.model.chat.response.ChatResponse",
                        "dev.langchain4j.model.chat.response.ChatResponse", "chatResponse"));
        put("dev.langchain4j.agentic.planner.AgentArgument",
                c("type", "java.lang.reflect.Type", "java.lang.reflect.Type", "type"),
                c("name", "java.lang.String", "java.lang.String", "name"),
                c("defaultValue", "java.lang.Object", "java.lang.Object", "defaultValue"),
                c("isOptional", "boolean", "boolean", "isOptional"));
        put("dev.langchain4j.agentic.agent.ErrorRecoveryResult",
                c("type", "dev.langchain4j.agentic.agent.ErrorRecoveryResult$Type",
                        "dev.langchain4j.agentic.agent.ErrorRecoveryResult$Type", "type"),
                c("result", "java.lang.Object", "java.lang.Object", "result"));
        put("dev.langchain4j.agentic.internal.AgentInvocationArguments",
                c("namedArgs", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "namedArgs"),
                c("positionalArgs", "java.lang.Object[]", "java.lang.Object[]", "positionalArgs"));
        put("dev.langchain4j.agentic.observability.HtmlReportGenerator",
                c("monitor", "dev.langchain4j.agentic.observability.AgentMonitor",
                        "dev.langchain4j.agentic.observability.AgentMonitor", "monitor"),
                c("rootAgent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "rootAgent"),
                c("memoryId", "java.lang.Object", "java.lang.Object", "memoryId"));
        put("dev.langchain4j.agentic.internal.AgentExecutor",
                c("agentInvoker", "dev.langchain4j.agentic.internal.AgentInvoker",
                        "dev.langchain4j.agentic.internal.AgentInvoker", "agentInvoker"),
                c("agent", "java.lang.Object", "java.lang.Object", "agent"));
        put("dev.langchain4j.agentic.scope.DefaultAgenticScope$AgentMessage",
                c("agentName", "java.lang.String", "java.lang.String", "agentName"),
                c("agentId", "java.lang.String", "java.lang.String", "agentId"),
                c("message", "dev.langchain4j.data.message.ChatMessage",
                        "dev.langchain4j.data.message.ChatMessage", "message"));
        put("dev.langchain4j.agentic.scope.DefaultAgenticScope$CompensableExecution",
                c("toolExecution", "dev.langchain4j.service.tool.ToolExecution",
                        "dev.langchain4j.service.tool.ToolExecution", "toolExecution"),
                c("compensatingAction", "java.util.function.Consumer",
                        "java.util.function.Consumer<dev.langchain4j.service.tool.ToolExecution>", "compensatingAction"));
        put("dev.langchain4j.agentic.scope.AgenticScopeKey",
                c("agentId", "java.lang.String", "java.lang.String", "agentId"),
                c("memoryId", "java.lang.Object", "java.lang.Object", "memoryId"));
        put("dev.langchain4j.agentic.observability.AfterAgentToolExecution",
                c("agentInstance", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agentInstance"),
                c("toolExecution", "dev.langchain4j.service.tool.ToolExecution",
                        "dev.langchain4j.service.tool.ToolExecution", "toolExecution"));
        put("dev.langchain4j.agentic.declarative.DeclarativeUtil$DefaultSupplierParameterResolverContext",
                c("declaringAgentClass", "java.lang.Class", "java.lang.Class<?>", "declaringAgentClass"),
                c("supplierMethod", "java.lang.reflect.Method", "java.lang.reflect.Method", "supplierMethod"),
                c("parameter", "java.lang.reflect.Parameter", "java.lang.reflect.Parameter", "parameter"));
        put("dev.langchain4j.agentic.workflow.ConditionalAgent",
                c("condition", "java.lang.String", "java.lang.String", "condition"),
                c("predicate", "java.util.function.Predicate",
                        "java.util.function.Predicate<dev.langchain4j.agentic.scope.AgenticScope>", "predicate"),
                c("agentInstances", "java.util.List",
                        "java.util.List<dev.langchain4j.agentic.planner.AgentInstance>", "agentInstances"));
        put("dev.langchain4j.agentic.workflow.impl.ConditionalPlanner",
                c("conditionalSubagents", "java.util.List",
                        "java.util.List<dev.langchain4j.agentic.workflow.ConditionalAgent>", "conditionalSubagents"));
        put("dev.langchain4j.agentic.planner.PlanningContext",
                c("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope"),
                c("previousAgentInvocation", "dev.langchain4j.agentic.scope.AgentInvocation",
                        "dev.langchain4j.agentic.scope.AgentInvocation", "previousAgentInvocation"));
    }

    private Task2a3MetadataContractTest() {
    }

    public static void main(String[] args) throws Exception {
        Path moduleRoot = (args.length == 0 ? Paths.get(".") : Paths.get(args[0]))
                .toAbsolutePath().normalize();
        verifyExpectedTable();
        verifyOrigins(moduleRoot);
        String metadata = read(moduleRoot.resolve(SOURCE_ROOT).resolve(METADATA_SOURCE));
        String supervisor = read(moduleRoot.resolve(SOURCE_ROOT).resolve(SUPERVISOR_SOURCE));
        verifyMetadataBlocks(metadata);
        verifyMigratedSources(moduleRoot);
        verifyNegativeMutations(metadata);
        verifySupervisorBranch(supervisor);
        System.out.println("Task 2a3 metadata source-contract checks passed: 23 types");
    }

    private static void verifyExpectedTable() {
        assertEquals(23, EXPECTED.size(), "combined metadata table must contain exactly 23 types");
        assertEquals(21, publicTypeCount(), "upstream public API table must contain exactly 21 types");
        Set<String> names = new HashSet<String>(EXPECTED.keySet());
        assertEquals(EXPECTED.size(), names.size(), "fixed upstream metadata table contains duplicate type names");
        int componentCount = 0;
        for (Map.Entry<String, ComponentSpec[]> entry : EXPECTED.entrySet()) {
            assertTrue(entry.getKey().indexOf('$') >= 0 || entry.getKey().indexOf('.') >= 0,
                    "binary FQCN is missing: " + entry.getKey());
            Set<String> components = new HashSet<String>();
            for (ComponentSpec component : entry.getValue()) {
                assertTrue(components.add(component.name), "duplicate component: " + entry.getKey());
                assertTrue(component.name.equals(component.accessor), "accessor drift in expected table");
                assertTrue(component.rawType.length() > 0 && component.genericType.length() > 0,
                        "type description missing: " + entry.getKey() + "." + component.name);
                componentCount++;
            }
        }
        assertTrue(componentCount > 0, "expected metadata table is empty");
    }

    private static void verifyMetadataBlocks(String source) {
        assertTrue(source.indexOf("class AgenticRecordComponentMetadata") >= 0,
                "missing explicit metadata class");
        assertTrue(source.indexOf("private final Class") < 0,
                "metadata must not retain Class objects in fields");
        assertTrue(source.indexOf("Collections.unmodifiableMap") >= 0,
                "metadata registry must be immutable");
        for (Map.Entry<String, ComponentSpec[]> entry : EXPECTED.entrySet()) {
            String typeBlock = findTypeBlock(source, entry.getKey());
            List<String> actual = findComponentCalls(typeBlock);
            assertEquals(entry.getValue().length, actual.size(),
                    "metadata component count mismatch for " + entry.getKey());
            for (int i = 0; i < entry.getValue().length; i++) {
                String expected = normalize(componentCall(entry.getValue()[i]));
                assertTrue(expected.equals(normalize(actual.get(i))),
                        "metadata component block mismatch for " + entry.getKey() + "."
                                + entry.getValue()[i].name + ": " + actual.get(i));
            }
        }
    }

    private static String findTypeBlock(String source, String binaryName) {
        int typeStart = source.indexOf("put(types, " + quote(binaryName));
        assertTrue(typeStart >= 0, "metadata missing binary FQCN: " + binaryName);
        int typeEnd = source.indexOf("\n        put(types, ", typeStart + 1);
        if (typeEnd < 0) {
            typeEnd = source.indexOf("\n        return Collections.unmodifiableMap", typeStart + 1);
        }
        assertTrue(typeEnd > typeStart, "metadata type entry boundary missing: " + binaryName);
        return source.substring(typeStart, typeEnd);
    }

    private static String findComponentCall(String typeBlock, String componentName) {
        for (String call : findComponentCalls(typeBlock)) {
            if (call.indexOf("component(" + quote(componentName)) == 0) {
                return call;
            }
        }
        throw new AssertionError("metadata missing component: " + componentName);
    }

    private static List<String> findComponentCalls(String typeBlock) {
        List<String> calls = new ArrayList<String>();
        int start = typeBlock.indexOf("component(");
        while (start >= 0) {
            int depth = 0;
            boolean quoted = false;
            boolean escaped = false;
            for (int i = start; i < typeBlock.length(); i++) {
                char ch = typeBlock.charAt(i);
                if (quoted) {
                    if (escaped) escaped = false;
                    else if (ch == '\\') escaped = true;
                    else if (ch == '\"') quoted = false;
                } else if (ch == '\"') {
                    quoted = true;
                } else if (ch == '(') {
                    depth++;
                } else if (ch == ')' && --depth == 0) {
                    calls.add(typeBlock.substring(start, i + 1));
                    start = typeBlock.indexOf("component(", i + 1);
                    break;
                }
            }
            if (start >= 0 && (calls.isEmpty() || !typeBlock.substring(start).startsWith("component("))) {
                start = typeBlock.indexOf("component(", start + 1);
            }
        }
        return calls;
    }

    private static String componentCall(ComponentSpec component) {
        return "component(" + quote(component.name) + ", " + quote(component.rawType) + ", "
                + quote(component.genericType) + ", " + quote(component.accessor) + ", "
                + quote(component.visibility) + ")";
    }

    private static String normalize(String source) {
        return source.replaceAll("\\s+", "");
    }

    private static void verifySupervisorBranch(String source) {
        assertTrue(source.indexOf("isRecord(") < 0, "SupervisorPlanner still uses Class.isRecord");
        assertTrue(source.indexOf("getRecordComponents") < 0, "SupervisorPlanner still uses getRecordComponents");
        assertTrue(source.indexOf("AgenticRecordComponentMetadata.describe(type") >= 0,
                "SupervisorPlanner does not use explicit metadata description facade");
        assertTrue(source.indexOf("type.getDeclaredFields()") >= 0,
                "SupervisorPlanner no longer retains ordinary POJO field fallback");
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void put(String name, ComponentSpec... components) {
        EXPECTED.put(name, components);
    }

    private static ComponentSpec c(String name, String rawType, String genericType, String accessor) {
        return new ComponentSpec(name, rawType, genericType, accessor, "public");
    }

    private static int publicTypeCount() {
        int count = 0;
        for (String name : EXPECTED.keySet()) {
            if (!name.contains("DefaultAgenticScope$CompensableExecution")
                    && !name.contains("DeclarativeUtil$DefaultSupplierParameterResolverContext")) {
                count++;
            }
        }
        return count;
    }

    private static void verifyOrigins(Path moduleRoot) throws IOException {
        Path officialJar = requiredPropertyPath("agentic.official.jar");
        Path officialSources = requiredPropertyPath("agentic.official.sources");
        Set<String> expectedPublicRecords = new HashSet<String>(EXPECTED.keySet());
        expectedPublicRecords.remove(
                "dev.langchain4j.agentic.scope.DefaultAgenticScope$CompensableExecution");
        expectedPublicRecords.remove(
                "dev.langchain4j.agentic.declarative.DeclarativeUtil$DefaultSupplierParameterResolverContext");

        Set<String> actualPublicRecords = publicRecordTypes(officialJar);
        assertEquals(21, actualPublicRecords.size(), "fixed upstream public API declaration count");
        assertTrue(expectedPublicRecords.equals(actualPublicRecords),
                "fixed upstream public record types mismatch: expected " + expectedPublicRecords
                        + ", got " + actualPublicRecords);

        String scopeBase = readJarEntry(officialSources,
                "dev/langchain4j/agentic/scope/DefaultAgenticScope.java");
        String declarativeBase = readJarEntry(officialSources,
                "dev/langchain4j/agentic/declarative/DeclarativeUtil.java");
        assertTrue(scopeBase.indexOf("private record CompensableExecution") >= 0,
                "private nested compensation source must come from fixed upstream sources JAR");
        assertTrue(declarativeBase.indexOf("private record DefaultSupplierParameterResolverContext") >= 0,
                "private resolver context source must come from fixed upstream sources JAR");
    }

    private static Set<String> publicRecordTypes(Path jarPath) throws IOException {
        final Set<String> records = new HashSet<String>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("dev/langchain4j/agentic/")
                        || !entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    new ClassReader(readAll(input)).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(int version, int access, String name, String signature,
                                          String superName, String[] interfaces) {
                            if ((access & Opcodes.ACC_PUBLIC) != 0
                                    && (access & Opcodes.ACC_RECORD) != 0) {
                                records.add(name.replace('/', '.'));
                            }
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
        return records;
    }

    private static String readJarEntry(Path jarPath, String entryName) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(entryName);
            assertTrue(entry != null, "missing fixed upstream source entry " + entryName);
            try (InputStream input = jar.getInputStream(entry)) {
                return new String(readAll(input), StandardCharsets.UTF_8);
            }
        }
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

    private static Path requiredPropertyPath(String name) {
        String value = System.getProperty(name);
        assertTrue(value != null && value.length() > 0, "missing required property " + name);
        Path path = Paths.get(value).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(path), "missing required file " + name + ": " + path);
        return path;
    }

    private static void verifyMigratedSources(Path moduleRoot) throws IOException {
        for (Map.Entry<String, ComponentSpec[]> entry : EXPECTED.entrySet()) {
            Path sourcePath = sourcePath(moduleRoot, entry.getKey());
            String source = read(sourcePath);
            String region = entry.getKey().indexOf('$') >= 0
                    ? nestedRegion(source, entry.getKey().substring(entry.getKey().indexOf('$') + 1)) : source;
            Map<String, String> names = sourceTypeNames(source, sourcePath, moduleRoot.resolve(SOURCE_ROOT));
            for (ComponentSpec component : entry.getValue()) {
                AccessorSpec accessor = findAccessor(region, component.accessor);
                assertTrue(component.visibility.equals(accessor.visibility) && !accessor.isStatic,
                        "migrated source accessor must be public instance: " + entry.getKey() + "." + component.name);
                String genericType = normalizeType(accessor.returnType, names);
                String rawType = eraseGeneric(genericType);
                assertTrue(component.rawType.equals(rawType),
                        "migrated source raw type mismatch: " + entry.getKey() + "." + component.name
                                + ": " + rawType);
                assertTrue(component.genericType.equals(genericType),
                        "migrated source generic type mismatch: " + entry.getKey() + "." + component.name
                                + ": " + genericType);
            }
        }
    }

    private static String nestedRegion(String source, String nestedName) {
        int start = source.indexOf("class " + nestedName);
        if (start < 0) start = source.indexOf("record " + nestedName);
        if (start < 0) start = source.indexOf("interface " + nestedName);
        assertTrue(start >= 0, "missing nested source declaration: " + nestedName);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            if (source.charAt(i) == '{') depth++;
            if (source.charAt(i) == '}' && --depth == 0) return source.substring(start, i + 1);
        }
        throw new AssertionError("unclosed nested source declaration: " + nestedName);
    }

    private static AccessorSpec findAccessor(String region, String name) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?m)^\\s*(public|protected|private)?\\s*(static\\s+)?([^\\r\\n{};]+?)\\s+"
                        + java.util.regex.Pattern.quote(name) + "\\s*\\(\\s*\\)\\s*\\{");
        java.util.regex.Matcher matcher = pattern.matcher(region);
        assertTrue(matcher.find(), "missing accessor declaration: " + name);
        String visibility = matcher.group(1) == null ? "package" : matcher.group(1);
        return new AccessorSpec(visibility, matcher.group(2) != null, matcher.group(3).trim());
    }

    private static Map<String, String> sourceTypeNames(String source, Path sourcePath, Path sourceRoot) throws IOException {
        Map<String, String> names = new LinkedHashMap<String, String>();
        names.put("String", "java.lang.String");
        names.put("List", "java.util.List");
        names.put("Map", "java.util.Map");
        names.put("Function", "java.util.function.Function");
        names.put("Consumer", "java.util.function.Consumer");
        names.put("Supplier", "java.util.function.Supplier");
        names.put("Predicate", "java.util.function.Predicate");
        names.put("Type", "java.lang.reflect.Type");
        names.put("Method", "java.lang.reflect.Method");
        names.put("Parameter", "java.lang.reflect.Parameter");
        names.put("Object", "java.lang.Object");
        names.put("Class", "java.lang.Class");
        names.put("Boolean", "java.lang.Boolean");
        names.put("Byte", "java.lang.Byte");
        names.put("Short", "java.lang.Short");
        names.put("Integer", "java.lang.Integer");
        names.put("Long", "java.lang.Long");
        names.put("Float", "java.lang.Float");
        names.put("Double", "java.lang.Double");
        names.put("Character", "java.lang.Character");
        names.put("Throwable", "java.lang.Throwable");
        java.util.regex.Matcher imports = java.util.regex.Pattern.compile(
                "(?m)^\\s*import\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;").matcher(source);
        while (imports.find()) {
            String full = imports.group(1);
            names.put(full.substring(full.lastIndexOf('.') + 1), full);
        }
        java.util.regex.Matcher wildcardImports = java.util.regex.Pattern.compile(
                "(?m)^\\s*import\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\.\\*\\s*;").matcher(source);
        List<String> wildcardPackages = new ArrayList<String>();
        while (wildcardImports.find()) wildcardPackages.add(wildcardImports.group(1));
        String packageName = "";
        java.util.regex.Matcher packageMatcher = java.util.regex.Pattern.compile(
                "(?m)^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;").matcher(source);
        if (packageMatcher.find()) packageName = packageMatcher.group(1);
        String fileName = sourcePath.getFileName().toString();
        String topName = fileName.substring(0, fileName.length() - ".java".length());
        String topBinary = packageName.length() == 0 ? topName : packageName + "." + topName;
        names.put(topName, topBinary);
        java.util.regex.Matcher nested = java.util.regex.Pattern.compile(
                "\\b(class|interface|enum)\\s+([A-Za-z_$][\\w$]*)").matcher(source);
        while (nested.find()) names.put(nested.group(2), topBinary + "$" + nested.group(2));
        names.putAll(resolveSamePackageTypes(sourceRoot, packageName));
        for (String wildcardPackage : wildcardPackages) {
            for (Map.Entry<String, String> type : resolvePackageTypes(sourceRoot, wildcardPackage).entrySet()) {
                if (!names.containsKey(type.getKey())) names.put(type.getKey(), type.getValue());
            }
        }
        return names;
    }

    private static Map<String, String> resolveSamePackageTypes(Path sourceRoot, String packageName) throws IOException {
        return resolvePackageTypes(sourceRoot, packageName);
    }

    private static Map<String, String> resolvePackageTypes(Path sourceRoot, String packageName) throws IOException {
        Map<String, String> result = new LinkedHashMap<String, String>();
        Path packagePath = sourceRoot.resolve(packageName.replace('.', '/'));
        if (!Files.exists(packagePath)) return result;
        java.nio.file.DirectoryStream<Path> files = Files.newDirectoryStream(packagePath, "*.java");
        try {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String simpleName = fileName.substring(0, fileName.length() - 5);
                result.put(simpleName, packageName.length() == 0 ? simpleName : packageName + "." + simpleName);
            }
        } finally {
            files.close();
        }
        return result;
    }

    private static String normalizeType(String type, Map<String, String> names) {
        String compact = type.trim().replaceAll("\\s+", " ");
        java.util.regex.Matcher tokens = java.util.regex.Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*").matcher(compact);
        StringBuffer normalized = new StringBuffer();
        while (tokens.find()) {
            String token = tokens.group();
            int before = tokens.start() - 1;
            String replacement = names.get(token);
            if (replacement != null && (before < 0 || compact.charAt(before) != '.')) {
                tokens.appendReplacement(normalized, java.util.regex.Matcher.quoteReplacement(replacement));
            } else {
                tokens.appendReplacement(normalized, java.util.regex.Matcher.quoteReplacement(token));
            }
        }
        tokens.appendTail(normalized);
        return normalized.toString();
    }

    private static String eraseGeneric(String type) {
        StringBuilder erased = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < type.length(); i++) {
            char ch = type.charAt(i);
            if (ch == '<') depth++;
            else if (ch == '>') depth--;
            else if (depth == 0) erased.append(ch);
        }
        return erased.toString().trim();
    }

    private static Path sourcePath(Path moduleRoot, String binaryName) {
        String[] parts = binaryName.split("\\$");
        String top = parts[0];
        int dot = top.lastIndexOf('.');
        String path = top.substring(0, dot).replace('.', '/') + "/" + top.substring(dot + 1) + ".java";
        return moduleRoot.resolve(SOURCE_ROOT).resolve(path);
    }

    private static void verifyNegativeMutations(String source) {
        for (Map.Entry<String, ComponentSpec[]> entry : EXPECTED.entrySet()) {
            String typeBlock = findTypeBlock(source, entry.getKey());
            ComponentSpec component = entry.getValue()[0];
            assertMutationFails(typeBlock, normalize(componentCall(component).replace(quote(component.rawType), quote("broken.Raw"))),
                    entry.getKey() + " raw mutation");
            assertMutationFails(typeBlock, normalize(componentCall(component).replace(quote(component.genericType), quote("broken.Generic"))),
                    entry.getKey() + " generic mutation");
            assertMutationFails(typeBlock, normalize(componentCall(component).replace(quote(component.accessor), quote("brokenAccessor"))),
                    entry.getKey() + " accessor mutation");
        }
    }

    private static void assertMutationFails(String typeBlock, String mutated, String message) {
        assertTrue(!componentCallMatches(typeBlock, mutated), message + " was accepted");
    }

    private static boolean componentCallMatches(String typeBlock, String expected) {
        int start = typeBlock.indexOf("component(");
        while (start >= 0) {
            int nameStart = start + "component(\"".length();
            int nameEnd = typeBlock.indexOf('"', nameStart);
            String candidate = findComponentCall(typeBlock, typeBlock.substring(nameStart, nameEnd));
            if (normalize(candidate).equals(expected)) return true;
            start = typeBlock.indexOf("component(", start + 1);
        }
        return false;
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), "missing source file " + path);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void assertEquals(int expected, int actual, String message) {
        assertTrue(expected == actual, message + ": expected " + expected + ", got " + actual);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class AccessorSpec {
        private final String visibility;
        private final boolean isStatic;
        private final String returnType;

        private AccessorSpec(String visibility, boolean isStatic, String returnType) {
            this.visibility = visibility;
            this.isStatic = isStatic;
            this.returnType = returnType;
        }
    }

    private static final class ComponentSpec {
        private final String name;
        private final String rawType;
        private final String genericType;
        private final String accessor;
        private final String visibility;

        private ComponentSpec(String name, String rawType, String genericType, String accessor, String visibility) {
            this.name = name;
            this.rawType = rawType;
            this.genericType = genericType;
            this.accessor = accessor;
            this.visibility = visibility;
        }
    }
}
