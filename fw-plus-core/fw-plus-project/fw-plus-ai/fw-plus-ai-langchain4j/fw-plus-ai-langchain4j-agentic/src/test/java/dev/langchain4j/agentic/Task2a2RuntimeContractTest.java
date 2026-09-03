package dev.langchain4j.agentic;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Minimal runtime checks for the eight Task2a2 record replacements. */
public final class Task2a2RuntimeContractTest {

    private static final String MAIN_ROOT = "src/main/java";

    private Task2a2RuntimeContractTest() {
    }

    public static void main(String[] args) throws Exception {
        Path moduleRoot = (args.length == 0 ? Paths.get(".") : Paths.get(args[0]))
                .toAbsolutePath().normalize();
        Path productRoot = moduleRoot.resolve(MAIN_ROOT);
        Path harnessRoot = Files.createTempDirectory("task2a2-runtime-");
        runScopeAndResolver(productRoot, harnessRoot.resolve("scope-resolver"));
        runServicesAndHuman(productRoot, harnessRoot.resolve("services-human"));
        runExecutorValue(productRoot, harnessRoot.resolve("executor"));
        runHtmlValue(productRoot, harnessRoot.resolve("html"));
        System.out.println("Task 2a2 runtime-contract checks passed: 8 declarations");
        System.out.println("Scope/Resolver and Services/Human value behavior verified with real copied target regions.");
        System.out.println("AgentExecutor delegation and complete HTML output remain deferred to Task3 dependency/API integration.");
    }

    private static void runScopeAndResolver(Path productRoot, Path root) throws Exception {
        String scope = read(productRoot.resolve("dev/langchain4j/agentic/scope/DefaultAgenticScope.java"));
        String message = extractRegion(scope, "public static final class AgentMessage");
        String compensation = extractRegion(scope, "private static final class CompensableExecution");
        write(root.resolve("src/dev/langchain4j/agentic/scope/DefaultAgenticScope.java"),
                "package dev.langchain4j.agentic.scope;\n"
                        + "import java.util.Objects; import java.util.function.Consumer;\n"
                        + "import dev.langchain4j.service.tool.ToolExecution; import dev.langchain4j.data.message.ChatMessage;\n"
                        + "public class DefaultAgenticScope { static final Logger LOG = new Logger();\n"
                        + " public static class Logger { public void warn(String format, Object... args) {} }\n"
                        + message + "\n" + compensation + "\n}\n");
        write(root.resolve("src/dev/langchain4j/data/message/ChatMessage.java"),
                "package dev.langchain4j.data.message;\n"
                        + "public class ChatMessage { private final String text; public ChatMessage(String t){text=t;}\n"
                        + " public boolean equals(Object o){return o instanceof ChatMessage && text.equals(((ChatMessage)o).text);}\n"
                        + " public int hashCode(){return text.hashCode();} public String toString(){return text;} }\n");
        write(root.resolve("src/dev/langchain4j/service/tool/ToolExecution.java"),
                "package dev.langchain4j.service.tool;\n"
                        + "public class ToolExecution { private final Request request; public ToolExecution(String n){request=new Request(n);}\n"
                        + " public Request request(){return request;} public String toString(){return request.name();}\n"
                        + " public boolean equals(Object o){return o instanceof ToolExecution && request.name().equals(((ToolExecution)o).request.name());}\n"
                        + " public int hashCode(){return request.name().hashCode();}\n"
                        + " public static class Request { private final String name; public Request(String n){name=n;} public String name(){return name;} } }\n");

        String declarative = read(productRoot.resolve("dev/langchain4j/agentic/declarative/DeclarativeUtil.java"));
        String resolverContext = extractRegion(declarative, "private static final class DefaultSupplierParameterResolverContext");
        write(root.resolve("src/dev/langchain4j/agentic/declarative/DeclarativeUtil.java"),
                "package dev.langchain4j.agentic.declarative;\n"
                        + "import java.lang.reflect.Method; import java.lang.reflect.Parameter; import java.util.Objects;\n"
                        + "public class DeclarativeUtil {\n" + resolverContext + "\n}\n");
        write(root.resolve("src/dev/langchain4j/agentic/declarative/SupplierParameterResolver.java"),
                "package dev.langchain4j.agentic.declarative; import java.lang.reflect.Method; import java.lang.reflect.Parameter;\n"
                        + "public interface SupplierParameterResolver { interface Context { Class<?> declaringAgentClass(); Method supplierMethod(); Parameter parameter(); } }\n");

        ClassLoader loader = compile(root);
        try {
            Class<?> messageType = load(loader, "dev.langchain4j.agentic.scope.DefaultAgenticScope$AgentMessage");
            assertNested(messageType, true, "AgentMessage");
            Class<?> chatType = load(loader, "dev.langchain4j.data.message.ChatMessage");
            Constructor<?> messageCtor = messageType.getDeclaredConstructor(String.class, String.class, chatType);
            assertPublic(messageCtor, "AgentMessage constructor");
            Object first = messageCtor.newInstance("agent", "id", chatType.getConstructor(String.class).newInstance("hello"));
            Object second = messageCtor.newInstance("agent", "id", chatType.getConstructor(String.class).newInstance("hello"));
            assertEquals("agent", messageType.getMethod("agentName").invoke(first), "AgentMessage agentName");
            assertEquals("id", messageType.getMethod("agentId").invoke(first), "AgentMessage agentId");
            assertTrue(first.equals(second), "AgentMessage distinct equal values");
            assertEquals(first.hashCode(), second.hashCode(), "AgentMessage equal hashCode");
            assertEquals("AgentMessage[agentName=agent, agentId=id, message=hello]", first.toString(), "AgentMessage toString");

            Class<?> compensationType = load(loader, "dev.langchain4j.agentic.scope.DefaultAgenticScope$CompensableExecution");
            assertNested(compensationType, false, "CompensableExecution");
            Class<?> toolType = load(loader, "dev.langchain4j.service.tool.ToolExecution");
            Constructor<?> compensationCtor = compensationType.getDeclaredConstructor(toolType, Consumer.class);
            assertTrue(Modifier.isPrivate(compensationCtor.getModifiers()), "CompensableExecution constructor visibility");
            compensationCtor.setAccessible(true);
            Object tool = toolType.getConstructor(String.class).newInstance("tool");
            RecordingConsumer success = new RecordingConsumer("success", false);
            Object execution = compensationCtor.newInstance(tool, success);
            Method compensate = compensationType.getDeclaredMethod("compensate");
            compensate.setAccessible(true);
            compensate.invoke(execution);
            assertTrue(success.received == tool, "successful compensation receives tool");
            Object equalExecution = compensationCtor.newInstance(toolType.getConstructor(String.class).newInstance("tool"),
                    new RecordingConsumer("success", false));
            assertTrue(execution.equals(equalExecution), "CompensableExecution distinct equal values");
            assertEquals(execution.hashCode(), equalExecution.hashCode(), "CompensableExecution equal hashCode");
            assertEquals("CompensableExecution[toolExecution=tool, compensatingAction=success]", execution.toString(),
                    "CompensableExecution toString");
            RecordingConsumer failure = new RecordingConsumer("failure", true);
            Object failed = compensationCtor.newInstance(tool, failure);
            compensate.invoke(failed);
            assertTrue(failure.received == tool, "failed compensation invokes action and swallows exception");

            Class<?> resolverType = load(loader, "dev.langchain4j.agentic.declarative.DeclarativeUtil$DefaultSupplierParameterResolverContext");
            assertNested(resolverType, false, "DefaultSupplierParameterResolverContext");
            Method method = String.class.getMethod("substring", int.class);
            Parameter parameter = method.getParameters()[0];
            Constructor<?> resolverCtor = resolverType.getDeclaredConstructor(Class.class, Method.class, Parameter.class);
            assertTrue(Modifier.isPrivate(resolverCtor.getModifiers()), "resolver context constructor visibility");
            resolverCtor.setAccessible(true);
            Object resolver = resolverCtor.newInstance(String.class, method, parameter);
            Object equalResolver = resolverCtor.newInstance(String.class, String.class.getMethod("substring", int.class),
                    String.class.getMethod("substring", int.class).getParameters()[0]);
            Method declaringClassAccessor = resolverType.getDeclaredMethod("declaringAgentClass");
            Method supplierMethodAccessor = resolverType.getDeclaredMethod("supplierMethod");
            Method parameterAccessor = resolverType.getDeclaredMethod("parameter");
            declaringClassAccessor.setAccessible(true);
            supplierMethodAccessor.setAccessible(true);
            parameterAccessor.setAccessible(true);
            assertEquals(String.class, declaringClassAccessor.invoke(resolver), "resolver class accessor");
            assertEquals(method, supplierMethodAccessor.invoke(resolver), "resolver method accessor");
            assertEquals(parameter, parameterAccessor.invoke(resolver), "resolver parameter accessor");
            assertTrue(resolver.equals(equalResolver), "resolver distinct equal values");
            assertEquals(resolver.hashCode(), equalResolver.hashCode(), "resolver equal hashCode");
            assertTrue(resolver.toString().startsWith("DefaultSupplierParameterResolverContext[declaringAgentClass=class java.lang.String"),
                    "resolver context toString");
        } finally {
            close(loader);
        }
    }

    private static void runServicesAndHuman(Path productRoot, Path root) throws Exception {
        String services = read(productRoot.resolve("dev/langchain4j/agentic/AgenticServices.java"));
        String contextSource = extractRegion(services, "public static final class DefaultDeclarativeAgentCreationContext");
        String configurator = extractRegion(services, "public static final class AgentConfigurator");
        write(root.resolve("src/dev/langchain4j/agentic/AgenticServices.java"),
                "package dev.langchain4j.agentic;\nimport java.util.Objects; import java.util.function.Consumer; import java.util.function.Function; import java.util.function.Supplier;\n"
                        + "import dev.langchain4j.agentic.agent.AgentBuilder; import dev.langchain4j.agentic.internal.InternalAgent;\n"
                        + "public final class AgenticServices { public interface DeclarativeAgentCreationContext<T> { Class<T> agentServiceClass(); AgentBuilder<T, ?> agentBuilder(); }\n"
                        + contextSource + "\n" + configurator + "\n}\n");
        write(root.resolve("src/dev/langchain4j/agentic/agent/AgentBuilder.java"),
                "package dev.langchain4j.agentic.agent;\npublic class AgentBuilder<T,B> { private final String id; public AgentBuilder(String i){id=i;}\n"
                        + " public boolean equals(Object o){return o instanceof AgentBuilder && id.equals(((AgentBuilder<?,?>)o).id);}\n"
                        + " public int hashCode(){return id.hashCode();} public String toString(){return id;} }\n");
        write(root.resolve("src/dev/langchain4j/agentic/internal/InternalAgent.java"),
                "package dev.langchain4j.agentic.internal; public interface InternalAgent {}\n");

        String human = read(productRoot.resolve("dev/langchain4j/agentic/workflow/HumanInTheLoop.java"));
        write(root.resolve("src/dev/langchain4j/agentic/workflow/HumanInTheLoop.java"), human);
        write(root.resolve("src/dev/langchain4j/agentic/internal/CollectionHelper.java"),
                read(productRoot.resolve("dev/langchain4j/agentic/internal/CollectionHelper.java")));
        write(root.resolve("src/dev/langchain4j/agentic/Agent.java"),
                "package dev.langchain4j.agentic; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface Agent { String value() default \"\"; }\n");
        write(root.resolve("src/dev/langchain4j/agentic/internal/AgentSpecsProvider.java"),
                "package dev.langchain4j.agentic.internal; import java.util.List; import dev.langchain4j.agentic.observability.AgentListener; import dev.langchain4j.agentic.planner.AgentArgument;\n"
                        + "public interface AgentSpecsProvider { String outputKey(); String description(); boolean async(); AgentListener listener(); default List<AgentArgument> arguments(){return null;} }\n");
        write(root.resolve("src/dev/langchain4j/agentic/observability/AgentListener.java"),
                "package dev.langchain4j.agentic.observability; public interface AgentListener {}\n");
        write(root.resolve("src/dev/langchain4j/agentic/planner/AgentArgument.java"),
                "package dev.langchain4j.agentic.planner; import java.lang.reflect.Type; public class AgentArgument { private final Type type; private final String name; public AgentArgument(Type t,String n){type=t;name=n;} public Type type(){return type;} public String name(){return name;} public boolean equals(Object o){return o instanceof AgentArgument && type.equals(((AgentArgument)o).type) && name.equals(((AgentArgument)o).name);} public int hashCode(){return 31*type.hashCode()+name.hashCode();} public String toString(){return \"AgentArgument[type=\"+type+\", name=\"+name+\"]\";} }\n");
        write(root.resolve("src/dev/langchain4j/agentic/scope/AgenticScope.java"),
                "package dev.langchain4j.agentic.scope; public interface AgenticScope {}\n");

        ClassLoader loader = compile(root);
        try {
            Class<?> contextType = load(loader, "dev.langchain4j.agentic.AgenticServices$DefaultDeclarativeAgentCreationContext");
            Class<?> builderType = load(loader, "dev.langchain4j.agentic.agent.AgentBuilder");
            Constructor<?> contextCtor = contextType.getDeclaredConstructor(Class.class, builderType);
            assertPublic(contextCtor, "context constructor");
            assertGenericTypes(contextCtor, new String[] {"java.lang.Class<T>", "dev.langchain4j.agentic.agent.AgentBuilder<T, ?>"});
            Object contextValue = contextCtor.newInstance(String.class, builderType.getConstructor(String.class).newInstance("builder"));
            Object equalContext = contextCtor.newInstance(String.class, builderType.getConstructor(String.class).newInstance("builder"));
            assertTrue(contextValue.equals(equalContext), "context distinct equal values");
            assertEquals(contextValue.hashCode(), equalContext.hashCode(), "context equal hashCode");
            assertEquals(String.class, contextType.getMethod("agentServiceClass").invoke(contextValue), "context class accessor");
            assertEquals("DefaultDeclarativeAgentCreationContext[agentServiceClass=class java.lang.String, agentBuilder=builder]",
                    contextValue.toString(), "context toString");

            Class<?> configType = load(loader, "dev.langchain4j.agentic.AgenticServices$AgentConfigurator");
            Constructor<?> canonical = configType.getDeclaredConstructor(Consumer.class, Function.class, Function.class, Supplier.class);
            assertPublic(canonical, "configurator canonical constructor");
            assertGenericTypes(canonical, new String[] {
                    "java.util.function.Consumer<dev.langchain4j.agentic.AgenticServices$DeclarativeAgentCreationContext<?>>",
                    "java.util.function.Function<java.lang.Class<?>, java.lang.Object>",
                    "java.util.function.Function<dev.langchain4j.agentic.internal.InternalAgent, java.lang.Object>",
                    "java.util.function.Supplier<java.lang.Object>"});
            NamedConsumer consumer = new NamedConsumer("consumer");
            NamedFunction resolver = new NamedFunction("resolver");
            NamedFunction factory = new NamedFunction("factory");
            NamedSupplier memory = new NamedSupplier("memory");
            Object configured = canonical.newInstance(consumer, resolver, factory, memory);
            assertEquals(consumer, configType.getMethod("configurator").invoke(configured), "configurator accessor");
            assertEquals(resolver, configType.getMethod("subAgentResolver").invoke(configured), "resolver accessor");
            assertEquals(factory, configType.getMethod("agentInstanceFactory").invoke(configured), "factory accessor");
            assertEquals(memory, configType.getMethod("defaultMemoryIdSupplier").invoke(configured), "memory accessor");
            Object equalConfigured = canonical.newInstance(new NamedConsumer("consumer"), new NamedFunction("resolver"),
                    new NamedFunction("factory"), new NamedSupplier("memory"));
            assertTrue(configured.equals(equalConfigured), "configurator distinct equal values");
            assertEquals(configured.hashCode(), equalConfigured.hashCode(), "configurator equal hashCode");
            Constructor<?> one = configType.getDeclaredConstructor(Consumer.class);
            Constructor<?> three = configType.getDeclaredConstructor(Consumer.class, Function.class, Function.class);
            assertPublic(one, "configurator one-arg constructor");
            assertPublic(three, "configurator three-arg constructor");
            Object oneValue = one.newInstance(consumer);
            assertEquals(null, configType.getMethod("subAgentResolver").invoke(oneValue), "one-arg resolver default");
            assertEquals(null, configType.getMethod("agentInstanceFactory").invoke(oneValue), "one-arg factory default");
            assertEquals(null, configType.getMethod("defaultMemoryIdSupplier").invoke(oneValue), "one-arg memory default");
            Object threeValue = three.newInstance(consumer, resolver, factory);
            assertEquals(null, configType.getMethod("defaultMemoryIdSupplier").invoke(threeValue), "three-arg memory default");
            Object empty = configType.getMethod("empty").invoke(null);
            java.lang.reflect.Field emptyField = configType.getDeclaredField("EMPTY");
            emptyField.setAccessible(true);
            assertTrue(empty == emptyField.get(null), "empty returns EMPTY singleton");

            Class<?> humanType = load(loader, "dev.langchain4j.agentic.workflow.HumanInTheLoop");
            Class<?> argumentType = load(loader, "dev.langchain4j.agentic.planner.AgentArgument");
            Constructor<?> humanCtor = humanType.getDeclaredConstructor(String.class, String.class, boolean.class, Function.class,
                    load(loader, "dev.langchain4j.agentic.observability.AgentListener"), List.class);
            assertPublic(humanCtor, "HumanInTheLoop constructor");
            Function<Object, Object> response = new NamedFunction("response", "answer");
            Object humanValue = humanCtor.newInstance("out", "desc", Boolean.TRUE, response, null,
                    Arrays.asList(argumentType.getConstructor(Type.class, String.class).newInstance(String.class, "question")));
            assertEquals("out", humanType.getMethod("outputKey").invoke(humanValue), "Human outputKey");
            assertEquals("answer", humanType.getMethod("askUser", load(loader, "dev.langchain4j.agentic.scope.AgenticScope"))
                    .invoke(humanValue, new Object[] {null}), "Human askUser");
            Class<?> humanBuilderType = load(loader, "dev.langchain4j.agentic.workflow.HumanInTheLoop$HumanInTheLoopBuilder");
            Object builder = humanBuilderType.getConstructor().newInstance();
            Object built = humanBuilderType.getMethod("outputKey", String.class).invoke(builder, "built");
            humanBuilderType.getMethod("responseProvider", Function.class).invoke(built, response);
            Object builtHuman = humanBuilderType.getMethod("build").invoke(built);
            assertEquals("built", humanType.getMethod("outputKey").invoke(builtHuman), "Human builder outputKey");
            assertEquals(response, humanType.getMethod("responseProvider").invoke(builtHuman), "Human builder responseProvider");
        } finally {
            close(loader);
        }
    }

    private static void runExecutorValue(Path productRoot, Path root) throws Exception {
        String actual = read(productRoot.resolve("dev/langchain4j/agentic/internal/AgentExecutor.java"));
        String members = actual.substring(actual.indexOf("    private final AgentInvoker agentInvoker;"),
                actual.indexOf("    private static final Logger LOG"));
        write(root.resolve("src/dev/langchain4j/agentic/internal/AgentExecutor.java"),
                "package dev.langchain4j.agentic.internal; import java.util.Objects;\n"
                        + "public final class AgentExecutor implements AgentInstance, InternalAgent {\n" + members + "}\n");
        write(root.resolve("src/dev/langchain4j/agentic/internal/AgentInvoker.java"),
                "package dev.langchain4j.agentic.internal; public class AgentInvoker { private final String id; public AgentInvoker(String i){id=i;} public boolean equals(Object o){return o instanceof AgentInvoker && id.equals(((AgentInvoker)o).id);} public int hashCode(){return id.hashCode();} public String toString(){return id;} }\n");
        write(root.resolve("src/dev/langchain4j/agentic/internal/AgentInstance.java"),
                "package dev.langchain4j.agentic.internal; public interface AgentInstance {}\n");
        write(root.resolve("src/dev/langchain4j/agentic/internal/InternalAgent.java"),
                "package dev.langchain4j.agentic.internal; public interface InternalAgent {}\n");
        ClassLoader loader = compile(root);
        try {
            Class<?> type = load(loader, "dev.langchain4j.agentic.internal.AgentExecutor");
            Class<?> invoker = load(loader, "dev.langchain4j.agentic.internal.AgentInvoker");
            Constructor<?> ctor = type.getDeclaredConstructor(invoker, Object.class);
            assertPublic(ctor, "AgentExecutor constructor");
            Object first = ctor.newInstance(invoker.getConstructor(String.class).newInstance("invoker"), "agent");
            Object second = ctor.newInstance(invoker.getConstructor(String.class).newInstance("invoker"), new String("agent"));
            assertEquals("invoker", type.getMethod("agentInvoker").invoke(first).toString(), "Executor invoker accessor");
            assertEquals("agent", type.getMethod("agent").invoke(first), "Executor agent accessor");
            assertTrue(first.equals(second), "Executor distinct equal values");
            assertEquals(first.hashCode(), second.hashCode(), "Executor equal hashCode");
            assertEquals("AgentExecutor[agentInvoker=invoker, agent=agent]", first.toString(), "Executor toString");
        } finally {
            close(loader);
        }
    }

    private static void runHtmlValue(Path productRoot, Path root) throws Exception {
        String actual = read(productRoot.resolve("dev/langchain4j/agentic/observability/HtmlReportGenerator.java"));
        String members = actual.substring(actual.indexOf("    private final AgentMonitor monitor;"),
                actual.indexOf("    private static final DateTimeFormatter TIME_FMT"));
        write(root.resolve("src/dev/langchain4j/agentic/observability/HtmlReportGenerator.java"),
                "package dev.langchain4j.agentic.observability; import java.util.Objects; import dev.langchain4j.agentic.planner.AgentInstance;\n"
                        + "public final class HtmlReportGenerator {\n" + members + "}\n");
        write(root.resolve("src/dev/langchain4j/agentic/observability/AgentMonitor.java"),
                "package dev.langchain4j.agentic.observability; public class AgentMonitor { private final String id; public AgentMonitor(String i){id=i;} public boolean equals(Object o){return o instanceof AgentMonitor && id.equals(((AgentMonitor)o).id);} public int hashCode(){return id.hashCode();} public String toString(){return id;} }\n");
        write(root.resolve("src/dev/langchain4j/agentic/planner/AgentInstance.java"),
                "package dev.langchain4j.agentic.planner; public class AgentInstance { private final String id; public AgentInstance(String i){id=i;} public boolean equals(Object o){return o instanceof AgentInstance && id.equals(((AgentInstance)o).id);} public int hashCode(){return id.hashCode();} public String toString(){return id;} }\n");
        ClassLoader loader = compile(root);
        try {
            Class<?> type = load(loader, "dev.langchain4j.agentic.observability.HtmlReportGenerator");
            Class<?> monitor = load(loader, "dev.langchain4j.agentic.observability.AgentMonitor");
            Class<?> agent = load(loader, "dev.langchain4j.agentic.planner.AgentInstance");
            Constructor<?> ctor = type.getDeclaredConstructor(monitor, agent, Object.class);
            assertPublic(ctor, "Html constructor");
            Object first = ctor.newInstance(monitor.getConstructor(String.class).newInstance("monitor"),
                    agent.getConstructor(String.class).newInstance("root"), "memory");
            Object second = ctor.newInstance(monitor.getConstructor(String.class).newInstance("monitor"),
                    agent.getConstructor(String.class).newInstance("root"), new String("memory"));
            assertEquals("monitor", type.getMethod("monitor").invoke(first).toString(), "Html monitor accessor");
            assertEquals("root", type.getMethod("rootAgent").invoke(first).toString(), "Html root accessor");
            assertEquals("memory", type.getMethod("memoryId").invoke(first), "Html memory accessor");
            assertTrue(first.equals(second), "Html distinct equal values");
            assertEquals(first.hashCode(), second.hashCode(), "Html equal hashCode");
            assertEquals("HtmlReportGenerator[monitor=monitor, rootAgent=root, memoryId=memory]", first.toString(), "Html toString");
        } finally {
            close(loader);
        }
    }

    private static ClassLoader compile(Path root) throws IOException {
        Path sourceRoot = root.resolve("src");
        Path classes = root.resolve("classes");
        Files.createDirectories(classes);
        List<Path> sources = new ArrayList<Path>();
        collectJava(sourceRoot, sources);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null, "system compiler unavailable");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        try {
            List<String> options = Arrays.asList("-source", "8", "-target", "8", "-proc:none", "-Xlint:all", "-d", classes.toString());
            Boolean success = compiler.getTask(null, manager, diagnostics, options, null,
                    manager.getJavaFileObjectsFromFiles(toFiles(sources))).call();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.WARNING || diagnostic.getKind() == Diagnostic.Kind.MANDATORY_WARNING) {
                    throw new AssertionError("runtime harness warning: " + diagnostic);
                }
            }
            assertTrue(Boolean.TRUE.equals(success), "runtime harness compilation failed: " + diagnostics.getDiagnostics());
        } finally {
            manager.close();
        }
        return new URLClassLoader(new URL[] {classes.toUri().toURL()}, null);
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

    private static String extractRegion(String source, String marker) {
        int start = source.indexOf(marker);
        assertTrue(start >= 0, "missing source marker " + marker);
        int opening = source.indexOf('{', start);
        assertTrue(opening >= 0, "missing source opening brace " + marker);
        return source.substring(start, matchingBrace(source, opening) + 1);
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
            } else if (c == '/' && next == '/') {
                state = 1; i++;
            } else if (c == '/' && next == '*') {
                state = 2; i++;
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
        throw new AssertionError("unbalanced source braces");
    }

    private static void assertNested(Class<?> type, boolean expectedPublic, String name) {
        assertTrue(Modifier.isStatic(type.getModifiers()) && Modifier.isFinal(type.getModifiers()), name + " static/final");
        assertTrue(Modifier.isPublic(type.getModifiers()) == expectedPublic, name + " visibility");
    }

    private static void assertGenericTypes(Constructor<?> constructor, String[] expected) {
        Type[] actual = constructor.getGenericParameterTypes();
        assertEquals(Integer.valueOf(expected.length), Integer.valueOf(actual.length), "constructor parameter count");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i].getTypeName(), "constructor generic parameter " + i);
        }
    }

    private static void assertPublic(Constructor<?> constructor, String message) {
        assertTrue(Modifier.isPublic(constructor.getModifiers()), message + " must be public");
    }

    private static Class<?> load(ClassLoader loader, String name) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    }

    private static void close(ClassLoader loader) throws IOException {
        if (loader instanceof URLClassLoader) ((URLClassLoader) loader).close();
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), "missing source " + path);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class NamedConsumer implements Consumer<Object> {
        private final String id;
        private NamedConsumer(String id) { this.id = id; }
        public void accept(Object value) { }
        public boolean equals(Object o) { return o instanceof NamedConsumer && id.equals(((NamedConsumer) o).id); }
        public int hashCode() { return id.hashCode(); }
        public String toString() { return id; }
    }

    private static final class NamedFunction implements Function<Object, Object> {
        private final String id;
        private final Object result;
        private NamedFunction(String id) { this(id, null); }
        private NamedFunction(String id, Object result) { this.id = id; this.result = result; }
        public Object apply(Object value) { return result; }
        public boolean equals(Object o) { return o instanceof NamedFunction && id.equals(((NamedFunction) o).id); }
        public int hashCode() { return id.hashCode(); }
        public String toString() { return id; }
    }

    private static final class NamedSupplier implements Supplier<Object> {
        private final String id;
        private NamedSupplier(String id) { this.id = id; }
        public Object get() { return id; }
        public boolean equals(Object o) { return o instanceof NamedSupplier && id.equals(((NamedSupplier) o).id); }
        public int hashCode() { return id.hashCode(); }
        public String toString() { return id; }
    }

    private static final class NamedObject {
        private final String id;
        private NamedObject(String id) { this.id = id; }
        public boolean equals(Object o) { return o instanceof NamedObject && id.equals(((NamedObject) o).id); }
        public int hashCode() { return id.hashCode(); }
        public String toString() { return id; }
    }

    private static final class RecordingConsumer implements Consumer<Object> {
        private final String id;
        private final boolean fail;
        private Object received;
        private RecordingConsumer(String id, boolean fail) { this.id = id; this.fail = fail; }
        public void accept(Object value) { received = value; if (fail) throw new IllegalStateException("expected"); }
        public String toString() { return id; }
        public boolean equals(Object o) { return o instanceof RecordingConsumer && id.equals(((RecordingConsumer) o).id); }
        public int hashCode() { return id.hashCode(); }
    }
}
