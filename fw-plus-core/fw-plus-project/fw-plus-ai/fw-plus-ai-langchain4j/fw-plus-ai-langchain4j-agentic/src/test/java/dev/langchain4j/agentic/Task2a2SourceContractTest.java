package dev.langchain4j.agentic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Exact source-contract checks for the eight Task 2a2 record replacements. */
public final class Task2a2SourceContractTest {

    private static final String SOURCE_ROOT = "src/main/java";
    private static final Map<String, DeclarationSpec[]> DECLARATIONS =
            new LinkedHashMap<String, DeclarationSpec[]>();

    static {
        DECLARATIONS.put("dev/langchain4j/agentic/AgenticServices.java", new DeclarationSpec[] {
                spec("public static final class DefaultDeclarativeAgentCreationContext", "DefaultDeclarativeAgentCreationContext",
                        "public static final class DefaultDeclarativeAgentCreationContext<T> implements DeclarativeAgentCreationContext<T> {",
                        "public DefaultDeclarativeAgentCreationContext(Class<T> agentServiceClass, AgentBuilder<T, ?> agentBuilder)",
                        "public", "DeclarativeAgentCreationContext<T>",
                        fields("private final Class<T> agentServiceClass;", "private final AgentBuilder<T, ?> agentBuilder;"),
                        accessors("public Class<T> agentServiceClass()", "public AgentBuilder<T, ?> agentBuilder()")),
                spec("public static final class AgentConfigurator", "AgentConfigurator",
                        "public static final class AgentConfigurator {", "public AgentConfigurator(Consumer<DeclarativeAgentCreationContext<?>> configurator, Function<Class<?>, Object> subAgentResolver, Function<InternalAgent, Object> agentInstanceFactory, Supplier<Object> defaultMemoryIdSupplier) {",
                        "public", null,
                        fields("private final Consumer<DeclarativeAgentCreationContext<?>> configurator;",
                                "private final Function<Class<?>, Object> subAgentResolver;",
                                "private final Function<InternalAgent, Object> agentInstanceFactory;",
                                "private final Supplier<Object> defaultMemoryIdSupplier;"),
                        accessors("public Consumer<DeclarativeAgentCreationContext<?>> configurator()",
                                "public Function<Class<?>, Object> subAgentResolver()",
                                "public Function<InternalAgent, Object> agentInstanceFactory()",
                                "public Supplier<Object> defaultMemoryIdSupplier()"))
        });
        DECLARATIONS.put("dev/langchain4j/agentic/internal/AgentExecutor.java", new DeclarationSpec[] {
                spec("public final class AgentExecutor", "AgentExecutor",
                        "public final class AgentExecutor implements AgentInstance, InternalAgent {",
                        "public AgentExecutor(AgentInvoker agentInvoker, Object agent)", "public", "AgentInstance, InternalAgent",
                        fields("private final AgentInvoker agentInvoker;", "private final Object agent;"),
                        accessors("public AgentInvoker agentInvoker()", "public Object agent()"))
        });
        DECLARATIONS.put("dev/langchain4j/agentic/workflow/HumanInTheLoop.java", new DeclarationSpec[] {
                spec("public final class HumanInTheLoop", "HumanInTheLoop",
                        "public final class HumanInTheLoop implements AgentSpecsProvider {",
                        "public HumanInTheLoop(String outputKey, String description, boolean async, Function<AgenticScope, ?> responseProvider, AgentListener listener, List<AgentArgument> arguments)",
                        "public", "AgentSpecsProvider",
                        fields("private final String outputKey;", "private final String description;", "private final boolean async;",
                                "private final Function<AgenticScope, ?> responseProvider;", "private final AgentListener listener;",
                                "private final List<AgentArgument> arguments;"),
                        accessors("public String outputKey()", "public String description()", "public boolean async()",
                                "public Function<AgenticScope, ?> responseProvider()", "public AgentListener listener()",
                                "public List<AgentArgument> arguments()"), "@Agent(\"An agent that asks the user for missing information\")")
        });
        DECLARATIONS.put("dev/langchain4j/agentic/scope/DefaultAgenticScope.java", new DeclarationSpec[] {
                spec("public static final class AgentMessage", "AgentMessage",
                        "public static final class AgentMessage {",
                        "public AgentMessage(String agentName, String agentId, ChatMessage message)", "public", null,
                        fields("private final String agentName;", "private final String agentId;", "private final ChatMessage message;"),
                        accessors("public String agentName()", "public String agentId()", "public ChatMessage message()")),
                spec("private static final class CompensableExecution", "CompensableExecution",
                        "private static final class CompensableExecution {",
                        "private CompensableExecution(ToolExecution toolExecution, Consumer<ToolExecution> compensatingAction)", "private", null,
                        fields("private final ToolExecution toolExecution;", "private final Consumer<ToolExecution> compensatingAction;"),
                        accessors("public ToolExecution toolExecution()", "public Consumer<ToolExecution> compensatingAction()"))
        });
        DECLARATIONS.put("dev/langchain4j/agentic/declarative/DeclarativeUtil.java", new DeclarationSpec[] {
                spec("private static final class DefaultSupplierParameterResolverContext", "DefaultSupplierParameterResolverContext",
                        "private static final class DefaultSupplierParameterResolverContext implements SupplierParameterResolver.Context {",
                        "private DefaultSupplierParameterResolverContext(Class<?> declaringAgentClass, Method supplierMethod, Parameter parameter)",
                        "private", "SupplierParameterResolver.Context",
                        fields("private final Class<?> declaringAgentClass;", "private final Method supplierMethod;", "private final Parameter parameter;"),
                        accessors("public Class<?> declaringAgentClass()", "public Method supplierMethod()", "public Parameter parameter()"))
        });
        DECLARATIONS.put("dev/langchain4j/agentic/observability/HtmlReportGenerator.java", new DeclarationSpec[] {
                spec("public final class HtmlReportGenerator", "HtmlReportGenerator",
                        "public final class HtmlReportGenerator {",
                        "public HtmlReportGenerator(AgentMonitor monitor, AgentInstance rootAgent, Object memoryId)", "public", null,
                        fields("private final AgentMonitor monitor;", "private final AgentInstance rootAgent;", "private final Object memoryId;"),
                        accessors("public AgentMonitor monitor()", "public AgentInstance rootAgent()", "public Object memoryId()"))
        });
    }

    private Task2a2SourceContractTest() {
    }

    public static void main(String[] args) throws Exception {
        Path moduleRoot = (args.length == 0 ? Paths.get(".") : Paths.get(args[0]))
                .toAbsolutePath().normalize();
        verifyDeclarations(moduleRoot);
        System.out.println("Task 2a2 source-contract checks passed: 8 declarations in 6 files");
    }

    private static void verifyDeclarations(Path moduleRoot) throws IOException {
        int declarationCount = 0;
        for (Map.Entry<String, DeclarationSpec[]> entry : DECLARATIONS.entrySet()) {
            String source = read(moduleRoot.resolve(SOURCE_ROOT).resolve(entry.getKey()));
            for (DeclarationSpec declaration : entry.getValue()) {
                declarationCount++;
                String region = declarationRegion(source, declaration.marker);
                String normalized = normalizeWhitespace(region);
                assertTrue(!Pattern.compile("\\brecord\\b").matcher(region).find(),
                        declaration.name + " target region still contains record");
                assertTrue(normalized.contains(normalizeWhitespace(declaration.header)),
                        declaration.name + " class visibility/static/final/implements mismatch");
                verifyFields(normalized, declaration);
                verifyConstructor(normalized, declaration);
                verifyAccessors(normalized, declaration);
                assertTrue(normalized.contains("boolean equals(Object o)"), declaration.name + " missing equals");
                assertTrue(normalized.contains("int hashCode()"), declaration.name + " missing hashCode");
                assertTrue(normalized.contains("String toString()"), declaration.name + " missing toString");
                assertTrue(normalized.contains("Objects.equals") || normalized.contains("== null"),
                        declaration.name + " missing null-safe equality");
                assertTrue(normalized.contains("31 * hash"), declaration.name + " missing independent hash accumulation");
                assertTrue(normalized.contains(declaration.name + "["), declaration.name + " missing record-format toString");
                if (declaration.implementsText != null) {
                    assertTrue(normalized.contains("implements " + declaration.implementsText),
                            declaration.name + " implements contract changed");
                }
                if (declaration.annotation != null) {
                    assertTrue(region.contains(declaration.annotation), declaration.name + " annotation changed");
                }
                if ("AgentConfigurator".equals(declaration.name)) {
                    assertTrue(normalized.contains("private static final AgentConfigurator EMPTY"), "AgentConfigurator.EMPTY missing");
                    assertTrue(normalized.contains("public static AgentConfigurator empty()"), "AgentConfigurator.empty() missing");
                    assertTrue(normalized.contains("public AgentConfigurator(Consumer<DeclarativeAgentCreationContext<?>> configurator)"),
                            "AgentConfigurator one-argument overload missing");
                    assertTrue(normalizeSignature(normalized).contains(normalizeSignature("public AgentConfigurator(Consumer<DeclarativeAgentCreationContext<?>> configurator, Function<Class<?>, Object> subAgentResolver, Function<InternalAgent, Object> agentInstanceFactory)")),
                            "AgentConfigurator three-argument overload missing");
                }
                if ("CompensableExecution".equals(declaration.name)) {
                    assertTrue(normalized.contains("public void compensate()"), "CompensableExecution.compensate missing");
                }
            }
        }
        assertEquals(8, declarationCount, "Task2a2 declaration checklist");
    }

    private static void verifyFields(String source, DeclarationSpec declaration) {
        int previous = -1;
        for (String field : declaration.fields) {
            String expected = normalizeWhitespace(field);
            int current = source.indexOf(expected);
            assertTrue(current >= 0, declaration.name + " missing field " + field);
            assertTrue(current > previous, declaration.name + " field/component order changed");
            previous = current;
        }
    }

    private static void verifyConstructor(String source, DeclarationSpec declaration) {
        String expected = normalizeSignature(declaration.constructor);
        assertTrue(normalizeSignature(source).contains(expected),
                declaration.name + " canonical raw/generic constructor signature changed");
        String visibility = declaration.constructorVisibility + " " + declaration.name + "(";
        assertTrue(source.contains(visibility), declaration.name + " canonical constructor visibility changed");
    }

    private static void verifyAccessors(String source, DeclarationSpec declaration) {
        for (String accessor : declaration.accessors) {
            assertTrue(source.contains(normalizeWhitespace(accessor) + " {"),
                    declaration.name + " accessor signature/visibility changed: " + accessor);
        }
    }

    private static String declarationRegion(String source, String marker) {
        int start = source.indexOf(marker);
        assertTrue(start >= 0, "missing target declaration " + marker);
        int open = findOpeningBrace(source, start);
        int close = matchingBrace(source, open);
        return source.substring(start, close + 1);
    }

    private static int findOpeningBrace(String source, int start) {
        int brace = source.indexOf('{', start);
        assertTrue(brace >= 0, "missing declaration opening brace");
        return brace;
    }

    private static int matchingBrace(String source, int opening) {
        int depth = 0;
        int state = 0;
        for (int i = opening; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
            if (state == 1) {
                if (c == '\n') state = 0;
            } else if (state == 2) {
                if (c == '*' && next == '/') { state = 0; i++; }
            } else if (state == 3) {
                if (c == '\\') i++;
                else if (c == '"') state = 0;
            } else if (state == 4) {
                if (c == '\\') i++;
                else if (c == '\'') state = 0;
            } else if (state == 5) {
                if (c == '"' && i + 2 < source.length()
                        && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"') {
                    state = 0;
                    i += 2;
                }
            } else if (c == '/' && next == '/') {
                state = 1; i++;
            } else if (c == '/' && next == '*') {
                state = 2; i++;
            } else if (c == '"' && i + 2 < source.length()
                    && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"') {
                state = 5; i += 2;
            } else if (c == '"') {
                state = 3;
            } else if (c == '\'') {
                state = 4;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        throw new AssertionError("unbalanced declaration braces");
    }

    private static String normalizeWhitespace(String source) {
        return source.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeSignature(String source) {
        return normalizeWhitespace(source).replace("( ", "(").replace(" )", ")")
                .replace(" ,", ",").replace(", ", ",");
    }

    private static DeclarationSpec spec(String marker, String name, String header, String constructor,
                                        String constructorVisibility, String implementsText, String[] fields,
                                        String[] accessors) {
        return new DeclarationSpec(marker, name, header, constructor, constructorVisibility, implementsText,
                fields, accessors, null);
    }

    private static DeclarationSpec spec(String marker, String name, String header, String constructor,
                                        String constructorVisibility, String implementsText, String[] fields,
                                        String[] accessors, String annotation) {
        return new DeclarationSpec(marker, name, header, constructor, constructorVisibility, implementsText,
                fields, accessors, annotation);
    }

    private static String[] fields(String... fields) {
        return fields;
    }

    private static String[] accessors(String... accessors) {
        return accessors;
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), "Missing source file " + path);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) throw new AssertionError(message);
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class DeclarationSpec {
        private final String marker;
        private final String name;
        private final String header;
        private final String constructor;
        private final String constructorVisibility;
        private final String implementsText;
        private final String[] fields;
        private final String[] accessors;
        private final String annotation;

        private DeclarationSpec(String marker, String name, String header, String constructor,
                                String constructorVisibility, String implementsText, String[] fields,
                                String[] accessors, String annotation) {
            this.marker = marker;
            this.name = name;
            this.header = header;
            this.constructor = constructor;
            this.constructorVisibility = constructorVisibility;
            this.implementsText = implementsText;
            this.fields = fields;
            this.accessors = accessors;
            this.annotation = annotation;
        }
    }
}
