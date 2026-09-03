package dev.langchain4j.agentic;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Isolated runtime contract harness for Task 2a1.
 *
 * <p>This harness copies exactly the 15 migrated sources plus the two real conditional-wrapper
 * implementation sources to a temporary directory, supplies only the minimal dependency
 * interfaces, and exercises the resulting classes through reflection and real object instances.</p>
 */
public final class Task2a1RuntimeContractTest {

    private static final String MAIN_ROOT = "src/main/java";
    private static final String[] TARGETS = {
            "dev/langchain4j/agentic/observability/BeforeAgentToolExecution.java",
            "dev/langchain4j/agentic/agent/ErrorContext.java",
            "dev/langchain4j/agentic/scope/AgentInvocation.java",
            "dev/langchain4j/agentic/planner/InitPlanningContext.java",
            "dev/langchain4j/agentic/observability/AgentRequest.java",
            "dev/langchain4j/agentic/observability/AgentInvocationError.java",
            "dev/langchain4j/agentic/observability/AgentResponse.java",
            "dev/langchain4j/agentic/planner/AgentArgument.java",
            "dev/langchain4j/agentic/agent/ErrorRecoveryResult.java",
            "dev/langchain4j/agentic/internal/AgentInvocationArguments.java",
            "dev/langchain4j/agentic/scope/AgenticScopeKey.java",
            "dev/langchain4j/agentic/observability/AfterAgentToolExecution.java",
            "dev/langchain4j/agentic/workflow/ConditionalAgent.java",
            "dev/langchain4j/agentic/workflow/impl/ConditionalPlanner.java",
            "dev/langchain4j/agentic/planner/PlanningContext.java"
    };

    /** Real wrapper implementation sources; kept separate from the 15 migrated record targets. */
    private static final String[] REAL_WRAPPER_TARGETS = {
            "dev/langchain4j/agentic/internal/CollectionHelper.java",
            "dev/langchain4j/agentic/workflow/impl/AbstractAgentInstance.java",
            "dev/langchain4j/agentic/workflow/impl/DefaultConditionalAgentInstance.java"
    };

    private static final Map<String, String[]> COMPONENTS = new LinkedHashMap<String, String[]>();

    static {
        COMPONENTS.put("BeforeAgentToolExecution", new String[] {"agentInstance", "toolExecution"});
        COMPONENTS.put("ErrorContext", new String[] {"agentName", "agenticScope", "exception"});
        COMPONENTS.put("AgentInvocation", new String[] {"agentType", "agentName", "agentId", "input", "output"});
        COMPONENTS.put("InitPlanningContext", new String[] {"agenticScope", "plannerAgent", "subagents"});
        COMPONENTS.put("AgentRequest", new String[] {"agenticScope", "agent", "inputs"});
        COMPONENTS.put("AgentInvocationError", new String[] {"agenticScope", "agent", "inputs", "error"});
        COMPONENTS.put("AgentResponse", new String[] {"agenticScope", "agent", "inputs", "output", "chatRequest", "chatResponse"});
        COMPONENTS.put("AgentArgument", new String[] {"type", "name", "defaultValue", "isOptional"});
        COMPONENTS.put("ErrorRecoveryResult", new String[] {"type", "result"});
        COMPONENTS.put("AgentInvocationArguments", new String[] {"namedArgs", "positionalArgs"});
        COMPONENTS.put("AgenticScopeKey", new String[] {"agentId", "memoryId"});
        COMPONENTS.put("AfterAgentToolExecution", new String[] {"agentInstance", "toolExecution"});
        COMPONENTS.put("ConditionalAgent", new String[] {"condition", "predicate", "agentInstances"});
        COMPONENTS.put("ConditionalPlanner", new String[] {"conditionalSubagents"});
        COMPONENTS.put("PlanningContext", new String[] {"agenticScope", "previousAgentInvocation"});
    }

    /** Explicit canonical signatures copied from the fixed upstream declarations. */
    private static final String STRING_TYPE = "java.lang.String";
    private static final String OBJECT_TYPE = "java.lang.Object";
    private static final String MAP_STRING_OBJECT_TYPE = "java.util.Map<java.lang.String, java.lang.Object>";
    private static final String LIST_AGENT_INSTANCE_TYPE =
            "java.util.List<dev.langchain4j.agentic.planner.AgentInstance>";
    private static final String LIST_CONDITIONAL_AGENT_TYPE =
            "java.util.List<dev.langchain4j.agentic.workflow.ConditionalAgent>";
    private static final String PREDICATE_SCOPE_TYPE =
            "java.util.function.Predicate<dev.langchain4j.agentic.scope.AgenticScope>";
    private static final String CLASS_WILDCARD_TYPE = "java.lang.Class<?>";
    private static final String TYPE_TYPE = "java.lang.reflect.Type";
    private static final String BOOLEAN_TYPE = "boolean";

    private static final Map<String, ExpectedSignature> EXPECTED_SIGNATURES =
            new LinkedHashMap<String, ExpectedSignature>();

    static {
        EXPECTED_SIGNATURES.put("BeforeAgentToolExecution", signature(
                new String[] {"dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.service.tool.BeforeToolExecution"},
                new String[] {"dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.service.tool.BeforeToolExecution"}));
        EXPECTED_SIGNATURES.put("ErrorContext", signature(
                new String[] {STRING_TYPE, "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.agent.AgentInvocationException"},
                new String[] {STRING_TYPE, "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.agent.AgentInvocationException"}));
        EXPECTED_SIGNATURES.put("AgentInvocation", signature(
                new String[] {"java.lang.Class", STRING_TYPE, STRING_TYPE, "java.util.Map",
                        OBJECT_TYPE},
                new String[] {CLASS_WILDCARD_TYPE, STRING_TYPE, STRING_TYPE,
                        MAP_STRING_OBJECT_TYPE, OBJECT_TYPE}));
        EXPECTED_SIGNATURES.put("InitPlanningContext", signature(
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.planner.AgentInstance", "java.util.List"},
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.planner.AgentInstance",
                        LIST_AGENT_INSTANCE_TYPE}));
        EXPECTED_SIGNATURES.put("AgentRequest", signature(
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.planner.AgentInstance", "java.util.Map"},
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.planner.AgentInstance",
                        MAP_STRING_OBJECT_TYPE}));
        EXPECTED_SIGNATURES.put("AgentInvocationError", signature(
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.planner.AgentInstance", "java.util.Map", "java.lang.Throwable"},
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.planner.AgentInstance",
                        MAP_STRING_OBJECT_TYPE, "java.lang.Throwable"}));
        EXPECTED_SIGNATURES.put("AgentResponse", signature(
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.planner.AgentInstance", "java.util.Map", OBJECT_TYPE,
                        "dev.langchain4j.model.chat.request.ChatRequest",
                        "dev.langchain4j.model.chat.response.ChatResponse"},
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.planner.AgentInstance",
                        MAP_STRING_OBJECT_TYPE, OBJECT_TYPE,
                        "dev.langchain4j.model.chat.request.ChatRequest",
                        "dev.langchain4j.model.chat.response.ChatResponse"}));
        EXPECTED_SIGNATURES.put("AgentArgument", signature(
                new String[] {TYPE_TYPE, STRING_TYPE, OBJECT_TYPE, BOOLEAN_TYPE},
                new String[] {TYPE_TYPE, STRING_TYPE, OBJECT_TYPE, BOOLEAN_TYPE}));
        EXPECTED_SIGNATURES.put("ErrorRecoveryResult", signature(
                new String[] {"dev.langchain4j.agentic.agent.ErrorRecoveryResult$Type", OBJECT_TYPE},
                new String[] {"dev.langchain4j.agentic.agent.ErrorRecoveryResult$Type", OBJECT_TYPE}));
        EXPECTED_SIGNATURES.put("AgentInvocationArguments", signature(
                new String[] {"java.util.Map", "java.lang.Object[]"},
                new String[] {MAP_STRING_OBJECT_TYPE, "java.lang.Object[]"}));
        EXPECTED_SIGNATURES.put("AgenticScopeKey", signature(
                new String[] {STRING_TYPE, OBJECT_TYPE},
                new String[] {STRING_TYPE, OBJECT_TYPE}));
        EXPECTED_SIGNATURES.put("AfterAgentToolExecution", signature(
                new String[] {"dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.service.tool.ToolExecution"},
                new String[] {"dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.service.tool.ToolExecution"}));
        EXPECTED_SIGNATURES.put("ConditionalAgent", signature(
                new String[] {STRING_TYPE, "java.util.function.Predicate", "java.util.List"},
                new String[] {STRING_TYPE, PREDICATE_SCOPE_TYPE, LIST_AGENT_INSTANCE_TYPE}));
        EXPECTED_SIGNATURES.put("ConditionalPlanner", signature(
                new String[] {"java.util.List"},
                new String[] {LIST_CONDITIONAL_AGENT_TYPE}));
        EXPECTED_SIGNATURES.put("PlanningContext", signature(
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgentInvocation"},
                new String[] {"dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgentInvocation"}));
    }

    private static ExpectedSignature signature(String[] rawTypeNames, String[] genericTypeNames) {
        return new ExpectedSignature(rawTypeNames, genericTypeNames);
    }

    private static final class ExpectedSignature {
        private final String[] rawTypeNames;
        private final String[] genericTypeNames;

        private ExpectedSignature(String[] rawTypeNames, String[] genericTypeNames) {
            this.rawTypeNames = rawTypeNames;
            this.genericTypeNames = genericTypeNames;
        }
    }

    private Task2a1RuntimeContractTest() {
    }

    public static void main(String[] args) throws Exception {
        Path moduleRoot = args.length == 0 ? Paths.get(".") : Paths.get(args[0]);
        Path harnessRoot = Files.createTempDirectory("task2a1-runtime-");
        Path sourceRoot = harnessRoot.resolve("src");
        Path classes = harnessRoot.resolve("classes");
        Files.createDirectories(classes);
        copyTargets(moduleRoot.resolve(MAIN_ROOT), sourceRoot);
        copyRealWrapperTargets(moduleRoot.resolve(MAIN_ROOT), sourceRoot);
        writeStubs(sourceRoot);
        compile(sourceRoot, classes);
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, null);
        try {
            verifyRuntimeContracts(loader);
        } finally {
            loader.close();
        }
        System.out.println("Task 2a1 runtime-contract checks passed: " + TARGETS.length + " types");
        System.out.println("Runtime harness compiled copied targets and stubs under: " + harnessRoot);
    }

    private static void copyTargets(Path productRoot, Path destinationRoot) throws IOException {
        for (String relative : TARGETS) {
            Path source = productRoot.resolve(relative);
            String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            if (relative.endsWith("scope/AgentInvocation.java")) {
                text = text.replace(
                        "output instanceof DelayedResponse<?> delayedResponse ? delayedResponse.result() : output",
                        "output instanceof DelayedResponse ? ((DelayedResponse<?>) output).result() : output");
            }
            write(destinationRoot.resolve(relative), text);
        }
    }

    private static void copyRealWrapperTargets(Path productRoot, Path destinationRoot) throws IOException {
        for (String relative : REAL_WRAPPER_TARGETS) {
            Path source = productRoot.resolve(relative);
            String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            write(destinationRoot.resolve(relative), text);
        }
    }

    private static void writeStubs(Path root) throws IOException {
        write(root.resolve("dev/langchain4j/agentic/planner/AgentInstance.java"),
                "package dev.langchain4j.agentic.planner;\n"
                        + "import java.lang.reflect.Type;\n"
                        + "import java.util.List;\n"
                        + "public interface AgentInstance {\n"
                        + " Class<?> type(); Class<? extends Planner> plannerType(); String name(); String agentId();\n"
                        + " String description(); Type outputType(); String outputKey(); boolean async();\n"
                        + " List<AgentArgument> arguments(); AgentInstance parent(); List<AgentInstance> subagents();\n"
                        + " boolean leaf(); AgenticSystemTopology topology();\n"
                        + " <T extends AgentInstance> T as(Class<T> type);\n"
                        + "}\n");
        write(root.resolve("dev/langchain4j/agentic/planner/AgenticSystemTopology.java"),
                "package dev.langchain4j.agentic.planner;\n"
                        + "public enum AgenticSystemTopology { AI_AGENT, NON_AI_AGENT, HUMAN_IN_THE_LOOP, SEQUENCE, PARALLEL, LOOP, ROUTER, STAR }\n");
        write(root.resolve("dev/langchain4j/agentic/planner/Action.java"),
                "package dev.langchain4j.agentic.planner;\n"
                        + "import java.util.List;\n"
                        + "public interface Action { boolean isDone(); Object result();\n"
                        + " class DoneAction implements Action { public boolean isDone(){return true;} public Object result(){return null;} }\n"
                        + " class AgentCallAction implements Action { private final List<AgentInstance> agents; public AgentCallAction(List<AgentInstance> a){agents=a;} public boolean isDone(){return false;} public Object result(){return null;} public List<AgentInstance> agentsToCall(){return agents;} }\n"
                        + "}\n");
        write(root.resolve("dev/langchain4j/agentic/planner/Planner.java"),
                "package dev.langchain4j.agentic.planner;\n"
                        + "import java.util.List; import java.util.Map;\n"
                        + "public interface Planner {\n"
                        + " default void init(InitPlanningContext c) {} default Map<String,Object> executionState(){return java.util.Collections.<String,Object>emptyMap();}\n"
                        + " default void restoreExecutionState(Map<String,Object> state) {}\n"
                        + " default Action firstAction(PlanningContext c){return nextAction(c);}\n"
                        + " default AgenticSystemTopology topology(){return AgenticSystemTopology.SEQUENCE;}\n"
                        + " Action nextAction(PlanningContext c); default boolean terminated(){return false;}\n"
                        + " default Action noOp(){return new Action.DoneAction();}\n"
                        + " default Action call(AgentInstance... agents){return new Action.AgentCallAction(java.util.Arrays.asList(agents));}\n"
                        + " default Action call(List<AgentInstance> agents){return call(agents.toArray(new AgentInstance[agents.size()]));}\n"
                        + " default Action done(){return new Action.DoneAction();} default Action done(Object result){return new Action.DoneAction();}\n"
                        + " default Action suspend(){return new Action.DoneAction();}\n"
                        + " default <T extends AgentInstance> T as(Class<T> type, AgentInstance agent){throw new ClassCastException();}\n"
                        + "}\n");
        write(root.resolve("dev/langchain4j/agentic/workflow/ConditionalAgentInstance.java"),
                "package dev.langchain4j.agentic.workflow;\n"
                        + "import dev.langchain4j.agentic.planner.AgentInstance;\n"
                        + "import java.util.List;\n"
                        + "public interface ConditionalAgentInstance extends AgentInstance { List<ConditionalAgent> conditionalSubagents(); }\n");
        write(root.resolve("dev/langchain4j/agentic/scope/AgenticScope.java"),
                "package dev.langchain4j.agentic.scope;\npublic interface AgenticScope {}\n");
        write(root.resolve("dev/langchain4j/agentic/internal/DelayedResponse.java"),
                "package dev.langchain4j.agentic.internal;\n"
                        + "public interface DelayedResponse<T> { boolean isDone(); T blockingGet(); default Object result(){return isDone()?blockingGet():\"<pending>\";} }\n");
        write(root.resolve("dev/langchain4j/agentic/internal/AgentUtil.java"),
                "package dev.langchain4j.agentic.internal;\n"
                        + "import java.lang.reflect.ParameterizedType; import java.lang.reflect.Type;\n"
                        + "public final class AgentUtil { private AgentUtil(){} public static Class<?> rawType(Type t){ if(t instanceof Class<?>) return (Class<?>)t; if(t instanceof ParameterizedType) return (Class<?>)((ParameterizedType)t).getRawType(); throw new IllegalArgumentException(); } }\n");
        write(root.resolve("dev/langchain4j/agentic/agent/AgentInvocationException.java"),
                "package dev.langchain4j.agentic.agent;\npublic class AgentInvocationException extends RuntimeException { public AgentInvocationException(String m){super(m);} }\n");
        write(root.resolve("dev/langchain4j/agentic/declarative/TypedKey.java"),
                "package dev.langchain4j.agentic.declarative;\npublic interface TypedKey<T> {}\n");
        write(root.resolve("dev/langchain4j/service/tool/BeforeToolExecution.java"),
                "package dev.langchain4j.service.tool;\n"
                        + "import dev.langchain4j.invocation.InvocationContext;\n"
                        + "public class BeforeToolExecution { private final InvocationContext context; public BeforeToolExecution(InvocationContext c){context=c;} public InvocationContext invocationContext(){return context;} @Override public String toString(){return \"before\";} }\n");
        write(root.resolve("dev/langchain4j/service/tool/ToolExecution.java"),
                "package dev.langchain4j.service.tool;\n"
                        + "import dev.langchain4j.invocation.InvocationContext;\n"
                        + "public class ToolExecution { private final InvocationContext context; public ToolExecution(InvocationContext c){context=c;} public InvocationContext invocationContext(){return context;} @Override public String toString(){return \"after\";} }\n");
        write(root.resolve("dev/langchain4j/invocation/InvocationContext.java"),
                "package dev.langchain4j.invocation;\nimport java.util.Map;\npublic interface InvocationContext { Map<Class<?>, Object> managedParameters(); }\n");
        write(root.resolve("dev/langchain4j/model/chat/request/ChatRequest.java"),
                "package dev.langchain4j.model.chat.request;\npublic class ChatRequest { @Override public String toString(){return \"chatRequest\";} }\n");
        write(root.resolve("dev/langchain4j/model/chat/response/ChatResponse.java"),
                "package dev.langchain4j.model.chat.response;\npublic class ChatResponse { @Override public String toString(){return \"chatResponse\";} }\n");
    }

    private static void compile(Path sourceRoot, Path classes) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null, "system compiler is unavailable");
        List<Path> files = new ArrayList<Path>();
        collectJava(sourceRoot, files);
        StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        try {
            List<String> options = Arrays.asList("-source", "8", "-target", "8", "-proc:none", "-d", classes.toString());
            Boolean success = compiler.getTask(null, manager, null, options, null,
                    manager.getJavaFileObjectsFromFiles(toFiles(files))).call();
            assertTrue(Boolean.TRUE.equals(success), "compiler rejected copied Task 2a1 sources/stubs");
        } finally {
            manager.close();
        }
    }

    private static void collectJava(Path root, List<Path> files) throws IOException {
        java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(root);
        try {
            for (Path path : stream) {
                if (Files.isDirectory(path)) collectJava(path, files);
                else if (path.toString().endsWith(".java")) files.add(path);
            }
        } finally {
            stream.close();
        }
    }

    private static List<java.io.File> toFiles(List<Path> paths) {
        List<java.io.File> files = new ArrayList<java.io.File>();
        for (Path path : paths) files.add(path.toFile());
        return files;
    }

    private static void verifyRuntimeContracts(ClassLoader loader) throws Exception {
        Object agent = agent(loader, "agent", "agent-id");
        Object scope = Proxy.newProxyInstance(loader, new Class<?>[] {load(loader, "dev.langchain4j.agentic.scope.AgenticScope")},
                new NamedHandler("scope"));
        Map<String, Object> inputs = new LinkedHashMap<String, Object>();
        inputs.put("key", "value");
        Object exception = load(loader, "dev.langchain4j.agentic.agent.AgentInvocationException")
                .getConstructor(String.class).newInstance("boom");
        Object error = new IllegalStateException("error");
        Object before = toolEvent(loader, "dev.langchain4j.service.tool.BeforeToolExecution", scope);
        Object after = toolEvent(loader, "dev.langchain4j.service.tool.ToolExecution", scope);
        Object predicate = new FixedPredicate("predicate", true);
        List<Object> agentList = Collections.singletonList(agent);

        Map<String, Object[]> values = new LinkedHashMap<String, Object[]>();
        values.put("BeforeAgentToolExecution", new Object[] {agent, before});
        values.put("ErrorContext", new Object[] {"name", scope, exception});
        values.put("AgentInvocation", new Object[] {String.class, "name", "id", inputs, "out"});
        values.put("InitPlanningContext", new Object[] {scope, agent, agentList});
        values.put("AgentRequest", new Object[] {scope, agent, inputs});
        values.put("AgentInvocationError", new Object[] {scope, agent, inputs, error});
        values.put("AgentResponse", new Object[] {scope, agent, inputs, "out",
                load(loader, "dev.langchain4j.model.chat.request.ChatRequest").newInstance(),
                load(loader, "dev.langchain4j.model.chat.response.ChatResponse").newInstance()});
        values.put("AgentArgument", new Object[] {String.class, "arg", "default", Boolean.TRUE});
        Class<?> recoveryType = load(loader, "dev.langchain4j.agentic.agent.ErrorRecoveryResult$Type");
        values.put("ErrorRecoveryResult", new Object[] {enumValue(recoveryType, "RETURN_RESULT"), "result"});
        Object[] positional = new Object[] {"x", Integer.valueOf(1)};
        values.put("AgentInvocationArguments", new Object[] {inputs, positional});
        values.put("AgenticScopeKey", new Object[] {"id", "memory"});
        values.put("AfterAgentToolExecution", new Object[] {agent, after});
        values.put("ConditionalAgent", new Object[] {"condition", predicate, agentList});
        Object conditionalAgent = construct(loader, "dev.langchain4j.agentic.workflow.ConditionalAgent",
                new Class<?>[] {String.class, Predicate.class, List.class},
                new Object[] {"condition", predicate, agentList});
        values.put("ConditionalPlanner", new Object[] {Collections.singletonList(conditionalAgent)});
        Object invocation = construct(loader, "dev.langchain4j.agentic.scope.AgentInvocation",
                new Class<?>[] {Class.class, String.class, String.class, Map.class, Object.class},
                new Object[] {String.class, "name", "id", inputs, "out"});
        values.put("PlanningContext", new Object[] {scope, invocation});

        assertEquals(EXPECTED_SIGNATURES.size(), TARGETS.length, "explicit signature table size");
        for (String name : COMPONENTS.keySet()) {
            verifyShapeAndValue(loader, name, COMPONENTS.get(name), values.get(name), EXPECTED_SIGNATURES.get(name));
        }
        verifyObjectArrayReferenceSemantics(loader, inputs);
        verifyDelayedResponse(loader, inputs);
        verifyAgentResponseOverload(loader, agent, inputs);
        verifyAgentArgumentOverloads(loader);
        verifyToolScope(loader, scope);
        verifyRecoveryFactories(loader);
        verifyConditionalPlanner(loader, scope, agent, conditionalAgent);
    }

    private static void verifyShapeAndValue(ClassLoader loader, String name, String[] components, Object[] values,
                                            ExpectedSignature expected) throws Exception {
        Class<?> type = classForTarget(loader, name);
        assertTrue(expected != null, name + " missing explicit expected signature");
        assertTrue(Modifier.isFinal(type.getModifiers()), name + " must be final");
        Field[] fields = type.getDeclaredFields();
        assertEquals(components.length, fields.length, name + " field count");
        assertEquals(expected.rawTypeNames.length, components.length, name + " expected raw signature field count");
        assertEquals(expected.genericTypeNames.length, components.length, name + " expected generic signature field count");
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Method[] accessors = new Method[components.length];
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            assertTrue(Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
                    name + " component field modifiers");
            assertEquals(components[i], field.getName(), name + " component field order");
            parameterTypes[i] = loadType(loader, expected.rawTypeNames[i]);
            assertEquals(parameterTypes[i], field.getType(), name + " field raw type");
            accessors[i] = type.getDeclaredMethod(components[i]);
            assertTrue(Modifier.isPublic(accessors[i].getModifiers()), name + " accessor visibility");
            assertEquals(parameterTypes[i], accessors[i].getReturnType(), name + " accessor raw return type");
            assertEquals(expected.genericTypeNames[i], accessors[i].getGenericReturnType().getTypeName(),
                    name + " accessor generic return type " + components[i]);
        }
        Constructor<?> canonical = type.getDeclaredConstructor(parameterTypes);
        assertTrue(Modifier.isPublic(canonical.getModifiers()), name + " canonical constructor visibility");
        assertGenericSignature(canonical, expected.genericTypeNames, name);
        Object value = canonical.newInstance(values);
        Object[] equalValues = equalValues(parameterTypes, values);
        Object equal = canonical.newInstance(equalValues);
        assertTrue(value.equals(equal), name + " distinct-but-equal values");
        assertEquals(value.hashCode(), equal.hashCode(), name + " distinct-but-equal hashCode");
        int expectedHash = expectedRecordHash(values);
        assertEquals(Integer.valueOf(expectedHash), Integer.valueOf(value.hashCode()),
                name + " independent record-style hashCode");
        int equalExpectedHash = expectedRecordHash(equalValues);
        assertEquals(Integer.valueOf(equalExpectedHash), Integer.valueOf(equal.hashCode()),
                name + " independent equal hashCode");
        for (int changedIndex = 0; changedIndex < components.length; changedIndex++) {
            Object[] changed = values.clone();
            changed[changedIndex] = differentValue(parameterTypes[changedIndex], changed[changedIndex], changedIndex);
            int changedExpectedHash = expectedRecordHash(changed);
            if (changedExpectedHash == expectedHash) {
                changed[changedIndex] = differentValue(parameterTypes[changedIndex], changed[changedIndex], changedIndex + 100);
                changedExpectedHash = expectedRecordHash(changed);
            }
            assertTrue(changedExpectedHash != expectedHash,
                    name + " mutation produced a hash collision for " + components[changedIndex]);
            Object unequal = canonical.newInstance(changed);
            assertTrue(!value.equals(unequal), name + " unequal component " + components[changedIndex]);
            assertEquals(Integer.valueOf(changedExpectedHash), Integer.valueOf(unequal.hashCode()),
                    name + " independent mutated hashCode " + components[changedIndex]);
        }
        Object[] nulls = new Object[components.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == boolean.class) nulls[i] = Boolean.FALSE;
        }
        Object nullValue = canonical.newInstance(nulls);
        Object nullEqual = canonical.newInstance(nulls);
        assertTrue(nullValue.equals(nullEqual), name + " null components equality");
        assertEquals(nullValue.hashCode(), nullEqual.hashCode(), name + " null components hashCode");
        assertEquals(Integer.valueOf(expectedRecordHash(nulls)), Integer.valueOf(nullValue.hashCode()),
                name + " independent null hashCode");
        for (int i = 0; i < components.length; i++) {
            Object actual = accessors[i].invoke(value);
            assertComponentValue(values[i], actual, name + " accessor value " + components[i]);
        }
        String expectedString = expectedToString(name, values);
        assertEquals(expectedString, value.toString(), name + " exact toString");
    }

    private static Object differentValue(Class<?> type, Object current, int index) {
        if (type == boolean.class) return !((Boolean) current);
        if (type == int.class) return ((Integer) current) + 1;
        if (type == long.class) return ((Long) current) + 1L;
        if (type == double.class) return ((Double) current) + 1D;
        if (type == float.class) return ((Float) current) + 1F;
        if (type == short.class) return (short) (((Short) current) + 1);
        if (type == byte.class) return (byte) (((Byte) current) + 1);
        if (type == char.class) return (char) (((Character) current) + 1);
        if (type == String.class) return new String(("changed-" + index).toCharArray());
        if (type == Object.class) return new Object();
        if (type == Class.class) return Integer.class;
        if (type == java.lang.reflect.Type.class) return Integer.class;
        if (type == java.util.Map.class) return Collections.singletonMap("changed", index);
        if (type == java.util.List.class) return Collections.singletonList("changed-" + index);
        if (type == java.util.function.Predicate.class) return new FixedPredicate("changed-" + index, false);
        if (type == java.lang.Throwable.class) return new IllegalArgumentException("changed-" + index);
        if (type.isArray()) return java.lang.reflect.Array.newInstance(type.getComponentType(), index % 2 + 1);
        if (current != null) return null;
        return new Object();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object[] equalValues(Class<?>[] types, Object[] values) {
        Object[] equal = values.clone();
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type == String.class) equal[i] = new String(((String) values[i]).toCharArray());
            else if (type == java.util.Map.class) equal[i] = new LinkedHashMap<String, Object>((Map) values[i]);
            else if (type == java.util.List.class) equal[i] = new ArrayList<Object>((List) values[i]);
            // Class, Type, Predicate, proxy and agent components retain the same reference,
            // matching their own component equals semantics rather than inventing equality.
        }
        return equal;
    }

    private static void assertComponentValue(Object expected, Object actual, String message) {
        if (expected != null && expected.getClass().isArray()) {
            assertTrue(expected == actual, message + ": array accessor must preserve reference");
        } else {
            assertTrue(expected == actual || Objects.equals(expected, actual), message);
        }
    }

    private static int expectedRecordHash(Object[] values) {
        int hash = 0;
        for (Object value : values) {
            hash = 31 * hash + componentHash(value);
        }
        return hash;
    }

    private static int componentHash(Object value) {
        if (value == null) return 0;
        if (value instanceof Boolean) return Boolean.valueOf((Boolean) value).hashCode();
        if (value.getClass().isArray()) return System.identityHashCode(value);
        return value.hashCode();
    }

    private static void verifyObjectArrayReferenceSemantics(ClassLoader loader, Map<String, Object> inputs) throws Exception {
        Class<?> type = load(loader, "dev.langchain4j.agentic.internal.AgentInvocationArguments");
        Object[] first = new Object[] {"x"};
        Object[] same = first;
        Object[] distinct = new Object[] {"x"};
        Object a = construct(loader, type.getName(), new Class<?>[] {Map.class, Object[].class}, new Object[] {inputs, first});
        Object b = construct(loader, type.getName(), new Class<?>[] {Map.class, Object[].class}, new Object[] {inputs, same});
        Object c = construct(loader, type.getName(), new Class<?>[] {Map.class, Object[].class}, new Object[] {inputs, distinct});
        assertTrue(a.equals(b), "Object[] same reference must compare equal");
        assertTrue(!a.equals(c), "Object[] distinct reference must remain unequal");
        assertEquals(a.hashCode(), b.hashCode(), "Object[] same reference hashCode");
        assertTrue(a.toString().contains("positionalArgs=" + first), "Object[] toString must use reference form");
    }

    private static void verifyDelayedResponse(ClassLoader loader, Map<String, Object> inputs) throws Exception {
        Class<?> delayedType = load(loader, "dev.langchain4j.agentic.internal.DelayedResponse");
        CountingDelayedResponse done = new CountingDelayedResponse("blocking-value", "result-value", true);
        Object doneProxy = Proxy.newProxyInstance(loader, new Class<?>[] {delayedType}, done);
        Object invocation = construct(loader, "dev.langchain4j.agentic.scope.AgentInvocation",
                new Class<?>[] {Class.class, String.class, String.class, Map.class, Object.class},
                new Object[] {String.class, "agent", "id", inputs, doneProxy});
        assertEquals("result-value", invocation.getClass().getMethod("output").invoke(invocation),
                "AgentInvocation done DelayedResponse must use result()");
        assertEquals(Integer.valueOf(1), Integer.valueOf(done.resultCalls), "done DelayedResponse result call count");
        assertEquals(Integer.valueOf(0), Integer.valueOf(done.blockingGetCalls), "done DelayedResponse blockingGet calls");

        CountingDelayedResponse pending = new CountingDelayedResponse("blocking-value", "result-value", false);
        Object pendingProxy = Proxy.newProxyInstance(loader, new Class<?>[] {delayedType}, pending);
        Object pendingInvocation = construct(loader, "dev.langchain4j.agentic.scope.AgentInvocation",
                new Class<?>[] {Class.class, String.class, String.class, Map.class, Object.class},
                new Object[] {String.class, "agent", "id", inputs, pendingProxy});
        assertEquals("pending-value", pendingInvocation.getClass().getMethod("output").invoke(pendingInvocation),
                "AgentInvocation pending DelayedResponse must use result() pending path");
        assertEquals(Integer.valueOf(1), Integer.valueOf(pending.resultCalls), "pending DelayedResponse result call count");
        assertEquals(Integer.valueOf(0), Integer.valueOf(pending.blockingGetCalls), "pending DelayedResponse blockingGet calls");
    }

    private static void verifyAgentResponseOverload(ClassLoader loader, Object agent, Map<String, Object> inputs)
            throws Exception {
        Class<?> type = load(loader, "dev.langchain4j.agentic.observability.AgentResponse");
        Constructor<?> four = type.getDeclaredConstructor(load(loader, "dev.langchain4j.agentic.scope.AgenticScope"),
                load(loader, "dev.langchain4j.agentic.planner.AgentInstance"), Map.class, Object.class);
        assertTrue(!Modifier.isPublic(four.getModifiers())
                        && !Modifier.isProtected(four.getModifiers())
                        && !Modifier.isPrivate(four.getModifiers()),
                "AgentResponse four-argument overload must be package-private");
        four.setAccessible(true);
        Object shortResponse = four.newInstance(null, agent, inputs, "out");
        assertEquals(null, type.getMethod("chatRequest").invoke(shortResponse), "AgentResponse four-arg chatRequest");
        assertEquals(null, type.getMethod("chatResponse").invoke(shortResponse), "AgentResponse four-arg chatResponse");
        assertEquals("agent", type.getMethod("agentName").invoke(shortResponse), "AgentResponse.agentName");
        assertEquals("agent-id", type.getMethod("agentId").invoke(shortResponse), "AgentResponse.agentId");
    }

    private static void verifyAgentArgumentOverloads(ClassLoader loader) throws Exception {
        Class<?> type = load(loader, "dev.langchain4j.agentic.planner.AgentArgument");
        Object basic = type.getConstructor(Type.class, String.class).newInstance(String.class, "arg");
        Object defaulted = type.getConstructor(Type.class, String.class, Object.class)
                .newInstance(String.class, "arg", "fallback");
        Object optional = type.getConstructor(Type.class, String.class, Object.class, boolean.class)
                .newInstance(String.class, "arg", "fallback", Boolean.TRUE);
        assertEquals(null, type.getMethod("defaultValue").invoke(basic), "AgentArgument basic default");
        assertEquals(Boolean.FALSE, type.getMethod("isOptional").invoke(basic), "AgentArgument basic optional");
        assertEquals("fallback", type.getMethod("defaultValue").invoke(defaulted), "AgentArgument default value");
        assertEquals(Boolean.FALSE, type.getMethod("isOptional").invoke(defaulted), "AgentArgument default optional");
        assertEquals(Boolean.TRUE, type.getMethod("isOptional").invoke(optional), "AgentArgument optional");
        assertEquals(String.class, type.getMethod("rawType").invoke(optional), "AgentArgument.rawType");
    }

    private static void verifyToolScope(ClassLoader loader, Object scope) throws Exception {
        Map<Class<?>, Object> managed = new HashMap<Class<?>, Object>();
        managed.put(load(loader, "dev.langchain4j.agentic.scope.AgenticScope"), scope);
        Object context = Proxy.newProxyInstance(loader,
                new Class<?>[] {load(loader, "dev.langchain4j.invocation.InvocationContext")},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        return managed;
                    }
                });
        Object beforeTool = load(loader, "dev.langchain4j.service.tool.BeforeToolExecution")
                .getConstructor(load(loader, "dev.langchain4j.invocation.InvocationContext")).newInstance(context);
        Object afterTool = load(loader, "dev.langchain4j.service.tool.ToolExecution")
                .getConstructor(load(loader, "dev.langchain4j.invocation.InvocationContext")).newInstance(context);
        Object before = construct(loader, "dev.langchain4j.agentic.observability.BeforeAgentToolExecution",
                new Class<?>[] {load(loader, "dev.langchain4j.agentic.planner.AgentInstance"),
                        load(loader, "dev.langchain4j.service.tool.BeforeToolExecution")},
                new Object[] {agent(loader, "agent", "agent-id"), beforeTool});
        Object after = construct(loader, "dev.langchain4j.agentic.observability.AfterAgentToolExecution",
                new Class<?>[] {load(loader, "dev.langchain4j.agentic.planner.AgentInstance"),
                        load(loader, "dev.langchain4j.service.tool.ToolExecution")},
                new Object[] {agent(loader, "agent", "agent-id"), afterTool});
        assertTrue(load(loader, "dev.langchain4j.agentic.scope.AgenticScope").isInstance(
                        load(loader, "dev.langchain4j.agentic.observability.BeforeAgentToolExecution")
                                .getMethod("agenticScope").invoke(before)),
                "Before event scope accessor type");
        assertTrue(load(loader, "dev.langchain4j.agentic.observability.BeforeAgentToolExecution")
                        .getMethod("agenticScope").invoke(before) == scope,
                "Before event scope accessor value");
        assertTrue(load(loader, "dev.langchain4j.agentic.observability.AfterAgentToolExecution")
                        .getMethod("agenticScope").invoke(after) == scope,
                "After event scope accessor value");
    }

    private static void verifyRecoveryFactories(ClassLoader loader) throws Exception {
        Class<?> type = load(loader, "dev.langchain4j.agentic.agent.ErrorRecoveryResult");
        Object thrown = type.getMethod("throwException").invoke(null);
        Object retry = type.getMethod("retry").invoke(null);
        Object result = type.getMethod("result", Object.class).invoke(null, "value");
        assertEquals("THROW_EXCEPTION", type.getMethod("type").invoke(thrown).toString(), "throwException type");
        assertEquals("RETRY", type.getMethod("type").invoke(retry).toString(), "retry type");
        assertEquals("RETURN_RESULT", type.getMethod("type").invoke(result).toString(), "result type");
        assertEquals(null, type.getMethod("result").invoke(thrown), "throwException result");
        assertEquals("value", type.getMethod("result").invoke(result), "result value");
    }

    private static void verifyConditionalPlanner(ClassLoader loader, Object scope, Object agent, Object conditionalAgent)
            throws Exception {
        Class<?> plannerType = load(loader, "dev.langchain4j.agentic.workflow.impl.ConditionalPlanner");
        Object secondAgent = agent(loader, "second", "second-id");
        Object falseAgent = agent(loader, "false", "false-id");
        FixedPredicate truePredicate = new FixedPredicate("true", true);
        FixedPredicate falsePredicate = new FixedPredicate("false", false);
        Object trueConditional = construct(loader, "dev.langchain4j.agentic.workflow.ConditionalAgent",
                new Class<?>[] {String.class, Predicate.class, List.class},
                new Object[] {"true", truePredicate, Arrays.asList(agent, secondAgent)});
        Object falseConditional = construct(loader, "dev.langchain4j.agentic.workflow.ConditionalAgent",
                new Class<?>[] {String.class, Predicate.class, List.class},
                new Object[] {"false", falsePredicate, Collections.singletonList(falseAgent)});
        List<Object> plannerInputs = Arrays.<Object>asList(trueConditional, falseConditional);
        Object planner = construct(loader, plannerType.getName(), new Class<?>[] {List.class},
                new Object[] {plannerInputs});
        Object planning = construct(loader, "dev.langchain4j.agentic.planner.PlanningContext",
                new Class<?>[] {load(loader, "dev.langchain4j.agentic.scope.AgenticScope"),
                        load(loader, "dev.langchain4j.agentic.scope.AgentInvocation")},
                new Object[] {scope, null});
        Object first = plannerType.getMethod("firstAction",
                load(loader, "dev.langchain4j.agentic.planner.PlanningContext")).invoke(planner, planning);
        Class<?> actionType = load(loader, "dev.langchain4j.agentic.planner.Action$AgentCallAction");
        assertTrue(actionType.isInstance(first), "ConditionalPlanner firstAction must call selected agents");
        List<?> selected = (List<?>) actionType.getMethod("agentsToCall").invoke(first);
        assertEquals(2, selected.size(), "ConditionalPlanner selected agent count");
        assertTrue(selected.get(0) == agent && selected.get(1) == secondAgent,
                "ConditionalPlanner selected agent identity/order");
        assertTrue(truePredicate.received == scope, "true predicate must receive planning scope");
        assertTrue(falsePredicate.received == scope, "false predicate must receive planning scope");

        Object noneConditional = construct(loader, "dev.langchain4j.agentic.workflow.ConditionalAgent",
                new Class<?>[] {String.class, Predicate.class, List.class},
                new Object[] {"none", new FixedPredicate("none", false), Collections.singletonList(agent)});
        Object nonePlanner = construct(loader, plannerType.getName(), new Class<?>[] {List.class},
                new Object[] {Collections.singletonList(noneConditional)});
        Object done = plannerType.getMethod("firstAction",
                load(loader, "dev.langchain4j.agentic.planner.PlanningContext")).invoke(nonePlanner, planning);
        assertTrue((Boolean) done.getClass().getMethod("isDone").invoke(done),
                "ConditionalPlanner empty selection must be done");
        Object next = plannerType.getMethod("nextAction",
                load(loader, "dev.langchain4j.agentic.planner.PlanningContext")).invoke(planner, planning);
        assertTrue((Boolean) next.getClass().getMethod("isDone").invoke(next),
                "ConditionalPlanner nextAction must be done");
        assertEquals("ROUTER", plannerType.getMethod("topology").invoke(planner).toString(),
                "ConditionalPlanner topology");
        assertEquals(Boolean.TRUE, plannerType.getMethod("terminated").invoke(planner),
                "ConditionalPlanner terminated");
        Class<?> conditionalInstance = load(loader, "dev.langchain4j.agentic.workflow.ConditionalAgentInstance");
        Object converted = plannerType.getMethod("as", Class.class,
                load(loader, "dev.langchain4j.agentic.planner.AgentInstance")).invoke(planner, conditionalInstance, agent);
        assertTrue(conditionalInstance.isInstance(converted), "ConditionalPlanner.as return type");
        verifyConditionalWrapper(loader, converted, agent, planner, plannerInputs, trueConditional, falseConditional);
        assertTrue(converted.getClass().getSuperclass().getName().equals(
                        "dev.langchain4j.agentic.workflow.impl.AbstractAgentInstance"),
                "ConditionalPlanner.as must use the production AbstractAgentInstance base");
        try {
            plannerType.getMethod("as", Class.class, load(loader, "dev.langchain4j.agentic.planner.AgentInstance"))
                    .invoke(planner, String.class, agent);
            throw new AssertionError("ConditionalPlanner.as must reject incompatible type");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof ClassCastException, "ConditionalPlanner.as exception type");
        }
    }

    private static void verifyConditionalWrapper(ClassLoader loader, Object wrapper, Object delegate,
                                                 Object planner, List<?> plannerInputs,
                                                 Object trueConditional, Object falseConditional) throws Exception {
        String[] delegatedMethods = {"type", "plannerType", "name", "agentId", "description", "outputType",
                "outputKey", "async", "arguments", "parent", "subagents", "leaf", "topology", "as"};
        Class<?> agentInstance = load(loader, "dev.langchain4j.agentic.planner.AgentInstance");
        for (String methodName : delegatedMethods) {
            Method method;
            Object[] arguments;
            if (methodName.equals("as")) {
                method = agentInstance.getMethod("as", Class.class);
                arguments = new Object[] {agentInstance};
            } else {
                method = agentInstance.getMethod(methodName);
                arguments = new Object[0];
            }
            Object expected = method.invoke(delegate, arguments);
            Object actual = method.invoke(wrapper, arguments);
            assertTrue(expected == actual || Objects.equals(expected, actual),
                    "Conditional wrapper delegated method " + methodName);
        }
        assertEquals("agent", agentInstance.getMethod("name").invoke(wrapper),
                "ConditionalPlanner.as wrapper name");
        assertEquals("agent-id", agentInstance.getMethod("agentId").invoke(wrapper),
                "ConditionalPlanner.as wrapper id");
        List<?> wrappedConditionals = (List<?>) wrapper.getClass().getMethod("conditionalSubagents").invoke(wrapper);
        List<?> plannerConditionals = (List<?>) planner.getClass().getMethod("conditionalSubagents").invoke(planner);
        assertTrue(wrappedConditionals == plannerConditionals, "ConditionalPlanner.as wrapper list identity");
        assertTrue(wrappedConditionals == plannerInputs, "ConditionalPlanner.as wrapper input list identity");
        Field delegateField = findField(wrapper.getClass(), "delegate");
        delegateField.setAccessible(true);
        assertTrue(delegateField.get(wrapper) == delegate, "ConditionalPlanner.as wrapper delegate identity");
        assertEquals(2, wrappedConditionals.size(), "ConditionalPlanner.as wrapper list size");
        assertTrue(wrappedConditionals.get(0) == trueConditional && wrappedConditionals.get(1) == falseConditional,
                "ConditionalPlanner.as wrapper conditional identity/order");
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static String expectedToString(String name, Object[] values) {
        if (name.equals("AgentInvocation")) {
            return "AgentInvocation{agentName=name, input={key=value}, output=out}";
        }
        if (name.equals("AgentInvocationArguments")) {
            return "AgentInvocationArguments[namedArgs={key=value}, positionalArgs=" + values[1] + "]";
        }
        if (name.equals("ErrorContext")) {
            return "ErrorContext[agentName=name, agenticScope=scope, exception=dev.langchain4j.agentic.agent.AgentInvocationException: boom]";
        }
        if (name.equals("AgentInvocationError")) {
            return "AgentInvocationError[agenticScope=scope, agent=agent, inputs={key=value}, error=java.lang.IllegalStateException: error]";
        }
        if (name.equals("AgentResponse")) {
            return "AgentResponse[agenticScope=scope, agent=agent, inputs={key=value}, output=out, chatRequest=chatRequest, chatResponse=chatResponse]";
        }
        if (name.equals("AgentArgument")) {
            return "AgentArgument[type=class java.lang.String, name=arg, defaultValue=default, isOptional=true]";
        }
        if (name.equals("ErrorRecoveryResult")) {
            return "ErrorRecoveryResult[type=RETURN_RESULT, result=result]";
        }
        if (name.equals("BeforeAgentToolExecution")) {
            return "BeforeAgentToolExecution[agentInstance=agent, toolExecution=before]";
        }
        if (name.equals("AfterAgentToolExecution")) {
            return "AfterAgentToolExecution[agentInstance=agent, toolExecution=after]";
        }
        if (name.equals("AgentRequest")) {
            return "AgentRequest[agenticScope=scope, agent=agent, inputs={key=value}]";
        }
        if (name.equals("InitPlanningContext")) {
            return "InitPlanningContext[agenticScope=scope, plannerAgent=agent, subagents=[agent]]";
        }
        if (name.equals("AgenticScopeKey")) {
            return "AgenticScopeKey[agentId=id, memoryId=memory]";
        }
        if (name.equals("ConditionalAgent")) {
            return "ConditionalAgent[condition=condition, predicate=predicate, agentInstances=[agent]]";
        }
        if (name.equals("ConditionalPlanner")) {
            return "ConditionalPlanner[conditionalSubagents=[ConditionalAgent[condition=condition, predicate=predicate, agentInstances=[agent]]]]";
        }
        if (name.equals("PlanningContext")) {
            return "PlanningContext[agenticScope=scope, previousAgentInvocation=AgentInvocation{agentName=name, input={key=value}, output=out}]";
        }
        throw new AssertionError("No exact toString expectation for " + name);
    }

    private static Object enumValue(Class<?> enumType, String name) throws Exception {
        Method valueOf = enumType.getMethod("valueOf", String.class);
        return valueOf.invoke(null, name);
    }

    private static Object toolEvent(ClassLoader loader, String typeName, Object scope) throws Exception {
        Map<Class<?>, Object> managed = new HashMap<Class<?>, Object>();
        managed.put(load(loader, "dev.langchain4j.agentic.scope.AgenticScope"), scope);
        Object context = Proxy.newProxyInstance(loader,
                new Class<?>[] {load(loader, "dev.langchain4j.invocation.InvocationContext")},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) { return managed; }
                });
        return load(loader, typeName).getConstructor(load(loader, "dev.langchain4j.invocation.InvocationContext"))
                .newInstance(context);
    }

    private static Object agent(ClassLoader loader, final String name, final String id) throws Exception {
        Class<?> type = load(loader, "dev.langchain4j.agentic.planner.AgentInstance");
        return Proxy.newProxyInstance(loader, new Class<?>[] {type}, new NamedHandler(name, id));
    }

    private static Object construct(ClassLoader loader, String className, Class<?>[] parameterTypes, Object[] values)
            throws Exception {
        return load(loader, className).getConstructor(parameterTypes).newInstance(values);
    }

    private static Class<?> load(ClassLoader loader, String name) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    }

    private static Class<?> classForTarget(ClassLoader loader, String simpleName) throws ClassNotFoundException {
        for (String target : TARGETS) {
            String binary = target.substring(0, target.length() - ".java".length()).replace('/', '.');
            if (binary.substring(binary.lastIndexOf('.') + 1).equals(simpleName)) {
                return load(loader, binary);
            }
        }
        throw new AssertionError("Unknown target " + simpleName);
    }

    private static Class<?> loadType(ClassLoader loader, String name) throws ClassNotFoundException {
        if (name.equals("boolean")) return Boolean.TYPE;
        if (name.equals("int")) return Integer.TYPE;
        if (name.equals("long")) return Long.TYPE;
        if (name.equals("double")) return Double.TYPE;
        if (name.equals("float")) return Float.TYPE;
        if (name.equals("short")) return Short.TYPE;
        if (name.equals("byte")) return Byte.TYPE;
        if (name.equals("char")) return Character.TYPE;
        if (name.equals("java.lang.Class")) return Class.class;
        if (name.equals("java.lang.Object[]")) return Object[].class;
        return load(loader, name);
    }

    private static void assertGenericSignature(Constructor<?> constructor, String[] expected, String name) {
        Type[] actual = constructor.getGenericParameterTypes();
        assertEquals(expected.length, actual.length, name + " generic parameter count");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i].getTypeName(), name + " canonical generic parameter " + i);
        }
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class CountingDelayedResponse implements InvocationHandler {
        private final String blockingValue;
        private final String resultValue;
        private final boolean done;
        private int resultCalls;
        private int blockingGetCalls;

        private CountingDelayedResponse(String blockingValue, String resultValue, boolean done) {
            this.blockingValue = blockingValue;
            this.resultValue = resultValue;
            this.done = done;
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("isDone")) return Boolean.valueOf(done);
            if (method.getName().equals("blockingGet")) {
                blockingGetCalls++;
                return blockingValue;
            }
            if (method.getName().equals("result")) {
                resultCalls++;
                return done ? resultValue : "pending-value";
            }
            if (method.getName().equals("toString")) return "delayed";
            if (method.getName().equals("hashCode")) return Integer.valueOf(System.identityHashCode(proxy));
            if (method.getName().equals("equals")) return Boolean.valueOf(proxy == args[0]);
            return null;
        }
    }

    private static final class NamedHandler implements InvocationHandler {
        private final String name;
        private final String id;

        private NamedHandler(String name) {
            this(name, name);
        }

        private NamedHandler(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if (methodName.equals("type")) return String.class;
            if (methodName.equals("plannerType")) return plannerType(proxy);
            if (methodName.equals("name")) return name;
            if (methodName.equals("agentId")) return id;
            if (methodName.equals("description")) return name + " description";
            if (methodName.equals("outputType")) return Integer.class;
            if (methodName.equals("outputKey")) return id + "-output";
            if (methodName.equals("async")) return Boolean.TRUE;
            if (methodName.equals("arguments")) return Collections.emptyList();
            if (methodName.equals("parent")) return null;
            if (methodName.equals("subagents")) return Collections.emptyList();
            if (methodName.equals("leaf")) return Boolean.TRUE;
            if (methodName.equals("topology")) return topology(proxy);
            if (methodName.equals("as")) return proxy;
            if (methodName.equals("toString")) return name;
            if (methodName.equals("hashCode")) return System.identityHashCode(proxy);
            if (methodName.equals("equals")) return proxy == args[0];
            if (method.getReturnType() == boolean.class) return Boolean.FALSE;
            return null;
        }

        private Object plannerType(Object proxy) {
            try {
                return Class.forName("dev.langchain4j.agentic.planner.Planner", false,
                        proxy.getClass().getClassLoader());
            } catch (ClassNotFoundException exception) {
                throw new AssertionError(exception);
            }
        }

        private Object topology(Object proxy) {
            try {
                Class<?> type = Class.forName("dev.langchain4j.agentic.planner.AgenticSystemTopology", false,
                        proxy.getClass().getClassLoader());
                for (Object constant : type.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equals("PARALLEL")) return constant;
                }
                throw new AssertionError("Missing PARALLEL topology");
            } catch (ClassNotFoundException exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static final class FixedPredicate implements Predicate<Object> {
        private final String label;
        private final boolean result;
        private Object received;

        private FixedPredicate(String label, boolean result) {
            this.label = label;
            this.result = result;
        }

        public boolean test(Object value) {
            received = value;
            return result;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
