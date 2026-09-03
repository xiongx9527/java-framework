package dev.langchain4j.agentic.internal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Isolation harness for the explicit Agentic metadata and Supervisor description path.
 *
 * <p>The production metadata and SupervisorPlanner sources are copied into a temporary harness.
 * All fixture types and external dependencies are generated in that harness only.</p>
 */
public final class Task2a3RuntimeContractTest {

    private static final String MAIN_ROOT = "src/main/java";
    private static final String METADATA =
            "dev/langchain4j/agentic/internal/AgenticRecordComponentMetadata.java";
    private static final String SUPERVISOR =
            "dev/langchain4j/agentic/supervisor/SupervisorPlanner.java";

    private static final String[] REGISTERED_TYPES = {
            "dev.langchain4j.agentic.AgenticServices$DefaultDeclarativeAgentCreationContext",
            "dev.langchain4j.agentic.AgenticServices$AgentConfigurator",
            "dev.langchain4j.agentic.observability.BeforeAgentToolExecution",
            "dev.langchain4j.agentic.agent.ErrorContext",
            "dev.langchain4j.agentic.scope.AgentInvocation",
            "dev.langchain4j.agentic.planner.InitPlanningContext",
            "dev.langchain4j.agentic.workflow.HumanInTheLoop",
            "dev.langchain4j.agentic.observability.AgentRequest",
            "dev.langchain4j.agentic.observability.AgentInvocationError",
            "dev.langchain4j.agentic.observability.AgentResponse",
            "dev.langchain4j.agentic.planner.AgentArgument",
            "dev.langchain4j.agentic.agent.ErrorRecoveryResult",
            "dev.langchain4j.agentic.internal.AgentInvocationArguments",
            "dev.langchain4j.agentic.observability.HtmlReportGenerator",
            "dev.langchain4j.agentic.internal.AgentExecutor",
            "dev.langchain4j.agentic.scope.DefaultAgenticScope$AgentMessage",
            "dev.langchain4j.agentic.scope.DefaultAgenticScope$CompensableExecution",
            "dev.langchain4j.agentic.scope.AgenticScopeKey",
            "dev.langchain4j.agentic.observability.AfterAgentToolExecution",
            "dev.langchain4j.agentic.declarative.DeclarativeUtil$DefaultSupplierParameterResolverContext",
            "dev.langchain4j.agentic.workflow.ConditionalAgent",
            "dev.langchain4j.agentic.workflow.impl.ConditionalPlanner",
            "dev.langchain4j.agentic.planner.PlanningContext"
    };

    private static final Map<String, String[]> EXPECTED_COMPONENT_NAMES =
            new LinkedHashMap<String, String[]>();

    static {
        expected("dev.langchain4j.agentic.AgenticServices$DefaultDeclarativeAgentCreationContext",
                "agentServiceClass", "agentBuilder");
        expected("dev.langchain4j.agentic.AgenticServices$AgentConfigurator",
                "configurator", "subAgentResolver", "agentInstanceFactory", "defaultMemoryIdSupplier");
        expected("dev.langchain4j.agentic.observability.BeforeAgentToolExecution",
                "agentInstance", "toolExecution");
        expected("dev.langchain4j.agentic.agent.ErrorContext",
                "agentName", "agenticScope", "exception");
        expected("dev.langchain4j.agentic.scope.AgentInvocation",
                "agentType", "agentName", "agentId", "input", "output");
        expected("dev.langchain4j.agentic.planner.InitPlanningContext",
                "agenticScope", "plannerAgent", "subagents");
        expected("dev.langchain4j.agentic.workflow.HumanInTheLoop",
                "outputKey", "description", "async", "responseProvider", "listener", "arguments");
        expected("dev.langchain4j.agentic.observability.AgentRequest",
                "agenticScope", "agent", "inputs");
        expected("dev.langchain4j.agentic.observability.AgentInvocationError",
                "agenticScope", "agent", "inputs", "error");
        expected("dev.langchain4j.agentic.observability.AgentResponse",
                "agenticScope", "agent", "inputs", "output", "chatRequest", "chatResponse");
        expected("dev.langchain4j.agentic.planner.AgentArgument",
                "type", "name", "defaultValue", "isOptional");
        expected("dev.langchain4j.agentic.agent.ErrorRecoveryResult", "type", "result");
        expected("dev.langchain4j.agentic.internal.AgentInvocationArguments", "namedArgs", "positionalArgs");
        expected("dev.langchain4j.agentic.observability.HtmlReportGenerator", "monitor", "rootAgent", "memoryId");
        expected("dev.langchain4j.agentic.internal.AgentExecutor", "agentInvoker", "agent");
        expected("dev.langchain4j.agentic.scope.DefaultAgenticScope$AgentMessage",
                "agentName", "agentId", "message");
        expected("dev.langchain4j.agentic.scope.DefaultAgenticScope$CompensableExecution",
                "toolExecution", "compensatingAction");
        expected("dev.langchain4j.agentic.scope.AgenticScopeKey", "agentId", "memoryId");
        expected("dev.langchain4j.agentic.observability.AfterAgentToolExecution",
                "agentInstance", "toolExecution");
        expected("dev.langchain4j.agentic.declarative.DeclarativeUtil$DefaultSupplierParameterResolverContext",
                "declaringAgentClass", "supplierMethod", "parameter");
        expected("dev.langchain4j.agentic.workflow.ConditionalAgent",
                "condition", "predicate", "agentInstances");
        expected("dev.langchain4j.agentic.workflow.impl.ConditionalPlanner", "conditionalSubagents");
        expected("dev.langchain4j.agentic.planner.PlanningContext", "agenticScope", "previousAgentInvocation");
    }

    private Task2a3RuntimeContractTest() {
    }

    public static void main(String[] args) throws Exception {
        Path moduleRoot = (args.length == 0 ? Paths.get(".") : Paths.get(args[0]))
                .toAbsolutePath().normalize();
        assertEquals(23, REGISTERED_TYPES.length, "runtime registered type checklist");
        assertEquals(23, EXPECTED_COMPONENT_NAMES.size(), "runtime component checklist");
        Path harnessRoot = Files.createTempDirectory("task2a3-runtime-");
        Path sourceRoot = harnessRoot.resolve("src");
        Path classes = harnessRoot.resolve("classes");
        Files.createDirectories(classes);
        copyProductionTargets(moduleRoot.resolve(MAIN_ROOT), sourceRoot);
        writeFixtures(sourceRoot);
        compile(sourceRoot, classes);
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, null);
        try {
            verifyMetadata(loader);
            verifySupervisorDescriptions(loader);
        } finally {
            loader.close();
        }
        verifyDriftFails(moduleRoot.resolve(MAIN_ROOT), harnessRoot.resolve("drift"));
        System.out.println("Task 2a3 runtime-contract checks passed: 23 types");
        System.out.println("Supervisor descriptions verified: simple, generic, nested, ordinary POJO");
        System.out.println("Registered accessor/type drift verified as fail-fast");
        System.out.println("Isolation harness used synthetic same-FQCN fixtures; temporary root: " + harnessRoot);
    }

    private static void expected(String binaryName, String... components) {
        EXPECTED_COMPONENT_NAMES.put(binaryName, components);
    }

    private static void copyProductionTargets(Path productRoot, Path destinationRoot) throws IOException {
        write(destinationRoot.resolve(METADATA), read(productRoot.resolve(METADATA)));
        String collectionHelper = "dev/langchain4j/agentic/internal/CollectionHelper.java";
        write(destinationRoot.resolve(collectionHelper), read(productRoot.resolve(collectionHelper)));
        write(destinationRoot.resolve(SUPERVISOR), read(productRoot.resolve(SUPERVISOR)));
    }

    private static void writeFixtures(Path root) throws IOException {
        write(root.resolve("dev/langchain4j/agentic/AgenticServices.java"),
                "package dev.langchain4j.agentic;\n"
                        + "import java.util.function.Consumer; import java.util.function.Function; import java.util.function.Supplier;\n"
                        + "import dev.langchain4j.agentic.agent.AgentBuilder; import dev.langchain4j.agentic.internal.InternalAgent;\n"
                        + "public final class AgenticServices {\n"
                        + " public interface DeclarativeAgentCreationContext<T> {}\n"
                        + " public static final class DefaultDeclarativeAgentCreationContext<T> {\n"
                        + "  public Class<T> agentServiceClass(){return null;}\n"
                        + "  public AgentBuilder<T, ?> agentBuilder(){return null;}\n"
                        + " }\n"
                        + " public static final class AgentConfigurator {\n"
                        + "  public Consumer<DeclarativeAgentCreationContext<?>> configurator(){return null;}\n"
                        + "  public Function<Class<?>, Object> subAgentResolver(){return null;}\n"
                        + "  public Function<InternalAgent, Object> agentInstanceFactory(){return null;}\n"
                        + "  public Supplier<Object> defaultMemoryIdSupplier(){return null;}\n"
                        + " }\n"
                        + "}\n");
        write(root.resolve("dev/langchain4j/agentic/agent/AgentBuilder.java"),
                "package dev.langchain4j.agentic.agent; public class AgentBuilder<T,B> {}\n");
        write(root.resolve("dev/langchain4j/agentic/internal/InternalAgent.java"),
                "package dev.langchain4j.agentic.internal; public interface InternalAgent {}\n");
        write(root.resolve("dev/langchain4j/agentic/agent/AgentInvocationException.java"),
                "package dev.langchain4j.agentic.agent; public class AgentInvocationException {}\n");
        write(root.resolve("dev/langchain4j/agentic/agent/ErrorContext.java"),
                "package dev.langchain4j.agentic.agent; import dev.langchain4j.agentic.scope.AgenticScope;\n"
                        + "public final class ErrorContext { public String agentName(){return null;} public AgenticScope agenticScope(){return null;} public AgentInvocationException exception(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/agent/ErrorRecoveryResult.java"),
                "package dev.langchain4j.agentic.agent; public final class ErrorRecoveryResult {\n"
                        + " public enum Type { THROW_EXCEPTION, RETURN_RESULT, RETRY }\n"
                        + " public Type type(){return null;} public Object result(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/scope/AgenticScope.java"),
                "package dev.langchain4j.agentic.scope; public interface AgenticScope extends dev.langchain4j.invocation.LangChain4jManaged {\n"
                        + " boolean hasState(String key); <T> T readState(String key, T defaultValue); Object readState(String key);\n"
                        + " Object memoryId(); void writeState(String key,Object value); }\n");
        write(root.resolve("dev/langchain4j/agentic/scope/AgentInvocation.java"),
                "package dev.langchain4j.agentic.scope; import java.util.Map;\n"
                        + "public final class AgentInvocation { public Class<?> agentType(){return null;} public String agentName(){return null;}\n"
                        + " public String agentId(){return null;} public Map<String,Object> input(){return null;} public Object output(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/scope/AgenticScopeKey.java"),
                "package dev.langchain4j.agentic.scope; public final class AgenticScopeKey {\n"
                        + " public String agentId(){return null;} public Object memoryId(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/scope/DefaultAgenticScope.java"),
                "package dev.langchain4j.agentic.scope; import java.util.function.Consumer; import dev.langchain4j.data.message.ChatMessage;\n"
                        + "import dev.langchain4j.service.tool.ToolExecution; import dev.langchain4j.agentic.supervisor.PlannerAgent;\n"
                        + "public class DefaultAgenticScope implements AgenticScope {\n"
                        + " public static final class AgentMessage { public String agentName(){return null;} public String agentId(){return null;} public ChatMessage message(){return null;} }\n"
                        + " private static final class CompensableExecution { public ToolExecution toolExecution(){return null;} public Consumer<ToolExecution> compensatingAction(){return null;} }\n"
                        + " public boolean hasState(String key){return false;} public <T> T readState(String key,T value){return value;} public Object readState(String key){return null;}\n"
                        + " public Object memoryId(){return null;} public void writeState(String key,Object value){}\n"
                        + " public PlannerAgent getOrCreateAgent(String id, java.util.function.Function<AgenticScope,PlannerAgent> f){return null;}\n"
                        + "}\n");
        write(root.resolve("dev/langchain4j/agentic/planner/AgentInstance.java"),
                "package dev.langchain4j.agentic.planner; import java.lang.reflect.Type; import java.util.List;\n"
                        + "public interface AgentInstance { String agentId(); String name(); String description(); List<AgentArgument> arguments(); }\n");
        write(root.resolve("dev/langchain4j/agentic/planner/AgentArgument.java"),
                "package dev.langchain4j.agentic.planner; import java.lang.reflect.Type;\n"
                        + "public final class AgentArgument { private final Type type; private final String name; private final Object defaultValue; private final boolean isOptional;\n"
                        + " public AgentArgument(Type t,String n){type=t;name=n;defaultValue=null;isOptional=false;}\n"
                        + " public Type type(){return type;} public String name(){return name;} public Object defaultValue(){return defaultValue;} public boolean isOptional(){return isOptional;}\n"
                        + " public Class<?> rawType(){return type instanceof Class ? (Class<?>) type : Object.class;} }\n");
        write(root.resolve("dev/langchain4j/agentic/planner/InitPlanningContext.java"),
                "package dev.langchain4j.agentic.planner; import java.util.List; import dev.langchain4j.agentic.scope.AgenticScope;\n"
                        + "public final class InitPlanningContext { public AgenticScope agenticScope(){return null;} public AgentInstance plannerAgent(){return null;} public List<AgentInstance> subagents(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/planner/PlanningContext.java"),
                "package dev.langchain4j.agentic.planner; import dev.langchain4j.agentic.scope.AgenticScope; import dev.langchain4j.agentic.scope.AgentInvocation;\n"
                        + "public final class PlanningContext { public AgenticScope agenticScope(){return null;} public AgentInvocation previousAgentInvocation(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/planner/Action.java"),
                "package dev.langchain4j.agentic.planner; public interface Action {}\n");
        write(root.resolve("dev/langchain4j/agentic/planner/Planner.java"),
                "package dev.langchain4j.agentic.planner; import java.util.Map; import dev.langchain4j.agentic.scope.AgenticScope;\n"
                        + "public interface Planner { Action nextAction(PlanningContext c); default Action done(){return null;} default Action done(Object o){return null;}\n"
                        + " default Action call(AgentInstance a){return null;} default void init(InitPlanningContext c){} default Map<String,Object> executionState(){return null;}\n"
                        + " default void restoreExecutionState(Map<String,Object> s){} }\n");
        write(root.resolve("dev/langchain4j/agentic/planner/ChatMemoryAccessProvider.java"),
                "package dev.langchain4j.agentic.planner; import dev.langchain4j.agentic.scope.AgenticScope; import dev.langchain4j.service.memory.ChatMemoryAccess;\n"
                        + "public interface ChatMemoryAccessProvider { ChatMemoryAccess chatMemoryAccess(AgenticScope scope); }\n");
        write(root.resolve("dev/langchain4j/agentic/observability/BeforeAgentToolExecution.java"),
                "package dev.langchain4j.agentic.observability; import dev.langchain4j.agentic.planner.AgentInstance; import dev.langchain4j.service.tool.BeforeToolExecution;\n"
                        + "public final class BeforeAgentToolExecution { public AgentInstance agentInstance(){return null;} public BeforeToolExecution toolExecution(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/observability/AfterAgentToolExecution.java"),
                "package dev.langchain4j.agentic.observability; import dev.langchain4j.agentic.planner.AgentInstance; import dev.langchain4j.service.tool.ToolExecution;\n"
                        + "public final class AfterAgentToolExecution { public AgentInstance agentInstance(){return null;} public ToolExecution toolExecution(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/observability/AgentListener.java"),
                "package dev.langchain4j.agentic.observability; public interface AgentListener {}\n");
        write(root.resolve("dev/langchain4j/agentic/observability/AgentRequest.java"),
                "package dev.langchain4j.agentic.observability; import java.util.Map; import dev.langchain4j.agentic.planner.AgentInstance; import dev.langchain4j.agentic.scope.AgenticScope;\n"
                        + "public final class AgentRequest { public AgenticScope agenticScope(){return null;} public AgentInstance agent(){return null;} public Map<String,Object> inputs(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/observability/AgentInvocationError.java"),
                "package dev.langchain4j.agentic.observability; import java.util.Map; import dev.langchain4j.agentic.planner.AgentInstance; import dev.langchain4j.agentic.scope.AgenticScope;\n"
                        + "public final class AgentInvocationError { public AgenticScope agenticScope(){return null;} public AgentInstance agent(){return null;} public Map<String,Object> inputs(){return null;} public Throwable error(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/observability/AgentResponse.java"),
                "package dev.langchain4j.agentic.observability; import java.util.Map; import dev.langchain4j.agentic.planner.AgentInstance; import dev.langchain4j.agentic.scope.AgenticScope; import dev.langchain4j.model.chat.request.ChatRequest; import dev.langchain4j.model.chat.response.ChatResponse;\n"
                        + "public final class AgentResponse { public AgenticScope agenticScope(){return null;} public AgentInstance agent(){return null;} public Map<String,Object> inputs(){return null;} public Object output(){return null;} public ChatRequest chatRequest(){return null;} public ChatResponse chatResponse(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/observability/HtmlReportGenerator.java"),
                "package dev.langchain4j.agentic.observability; import dev.langchain4j.agentic.planner.AgentInstance;\n"
                        + "public final class HtmlReportGenerator { public AgentMonitor monitor(){return null;} public AgentInstance rootAgent(){return null;} public Object memoryId(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/observability/AgentMonitor.java"),
                "package dev.langchain4j.agentic.observability; public class AgentMonitor {}\n");
        write(root.resolve("dev/langchain4j/agentic/internal/AgentInvocationArguments.java"),
                "package dev.langchain4j.agentic.internal; import java.util.Map; public final class AgentInvocationArguments {\n"
                        + " public Map<String,Object> namedArgs(){return null;} public Object[] positionalArgs(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/internal/AgentInvoker.java"),
                "package dev.langchain4j.agentic.internal; public interface AgentInvoker {}\n");
        write(root.resolve("dev/langchain4j/agentic/internal/AgentExecutor.java"),
                "package dev.langchain4j.agentic.internal; public final class AgentExecutor {\n"
                        + " public AgentInvoker agentInvoker(){return null;} public Object agent(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/workflow/HumanInTheLoop.java"),
                "package dev.langchain4j.agentic.workflow; import java.util.List; import java.util.function.Function; import dev.langchain4j.agentic.scope.AgenticScope; import dev.langchain4j.agentic.observability.AgentListener; import dev.langchain4j.agentic.planner.AgentArgument;\n"
                        + "public final class HumanInTheLoop { public String outputKey(){return null;} public String description(){return null;} public boolean async(){return false;}\n"
                        + " public Function<AgenticScope,?> responseProvider(){return null;} public AgentListener listener(){return null;} public List<AgentArgument> arguments(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/declarative/DeclarativeUtil.java"),
                "package dev.langchain4j.agentic.declarative; import java.lang.reflect.Method; import java.lang.reflect.Parameter;\n"
                        + "public class DeclarativeUtil { private static final class DefaultSupplierParameterResolverContext {\n"
                        + " public Class<?> declaringAgentClass(){return null;} public Method supplierMethod(){return null;} public Parameter parameter(){return null;} } }\n");
        write(root.resolve("dev/langchain4j/agentic/workflow/ConditionalAgent.java"),
                "package dev.langchain4j.agentic.workflow; import java.util.List; import java.util.function.Predicate; import dev.langchain4j.agentic.scope.AgenticScope; import dev.langchain4j.agentic.planner.AgentInstance;\n"
                        + "public final class ConditionalAgent { public String condition(){return null;} public Predicate<AgenticScope> predicate(){return null;} public List<AgentInstance> agentInstances(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/workflow/impl/ConditionalPlanner.java"),
                "package dev.langchain4j.agentic.workflow.impl; import java.util.List; import dev.langchain4j.agentic.workflow.ConditionalAgent;\n"
                        + "public final class ConditionalPlanner { public List<ConditionalAgent> conditionalSubagents(){return null;} }\n");
        write(root.resolve("dev/langchain4j/data/message/ChatMessage.java"),
                "package dev.langchain4j.data.message; public class ChatMessage {}\n");
        write(root.resolve("dev/langchain4j/service/tool/BeforeToolExecution.java"),
                "package dev.langchain4j.service.tool; public class BeforeToolExecution {}\n");
        write(root.resolve("dev/langchain4j/service/tool/ToolExecution.java"),
                "package dev.langchain4j.service.tool; public class ToolExecution {}\n");
        write(root.resolve("dev/langchain4j/model/chat/request/ChatRequest.java"),
                "package dev.langchain4j.model.chat.request; public class ChatRequest {}\n");
        write(root.resolve("dev/langchain4j/model/chat/response/ChatResponse.java"),
                "package dev.langchain4j.model.chat.response; public class ChatResponse {}\n");
        write(root.resolve("dev/langchain4j/model/chat/ChatModel.java"),
                "package dev.langchain4j.model.chat; public interface ChatModel {}\n");
        write(root.resolve("dev/langchain4j/memory/chat/ChatMemoryProvider.java"),
                "package dev.langchain4j.memory.chat; public interface ChatMemoryProvider { ChatMemory get(Object memoryId); }\n");
        write(root.resolve("dev/langchain4j/memory/chat/ChatMemory.java"),
                "package dev.langchain4j.memory.chat; public interface ChatMemory {}\n");
        write(root.resolve("dev/langchain4j/memory/chat/MessageWindowChatMemory.java"),
                "package dev.langchain4j.memory.chat; public final class MessageWindowChatMemory { public static ChatMemory withMaxMessages(int n){return null;} }\n");
        write(root.resolve("dev/langchain4j/service/memory/ChatMemoryAccess.java"),
                "package dev.langchain4j.service.memory; public interface ChatMemoryAccess {}\n");
        write(root.resolve("dev/langchain4j/service/ParameterNameResolver.java"),
                "package dev.langchain4j.service; import java.lang.reflect.Parameter; public final class ParameterNameResolver { public static String name(Parameter p){return p.getName();} }\n");
        write(root.resolve("dev/langchain4j/invocation/LangChain4jManaged.java"),
                "package dev.langchain4j.invocation; import java.util.Map; public interface LangChain4jManaged { "
                        + "static void setCurrent(Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> m){} "
                        + "static Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> push("
                        + "Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> m){return null;} "
                        + "static void restore(Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> m){} "
                        + "static void removeCurrent(){} }\n");
        write(root.resolve("dev/langchain4j/agentic/internal/Context.java"),
                "package dev.langchain4j.agentic.internal; import dev.langchain4j.agentic.scope.AgenticScope; import dev.langchain4j.model.chat.ChatModel;\n"
                        + "public final class Context { public static final class Summarizer { public Summarizer(AgenticScope s,ChatModel m){} } }\n");
        write(root.resolve("org/slf4j/Logger.java"),
                "package org.slf4j; public interface Logger { void info(String s,Object... o); }\n");
        write(root.resolve("org/slf4j/LoggerFactory.java"),
                "package org.slf4j; public final class LoggerFactory { private static final Logger L=new Logger(){public void info(String s,Object...o){}}; public static Logger getLogger(Class<?> c){return L;} }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/SupervisorContextStrategy.java"),
                "package dev.langchain4j.agentic.supervisor; public enum SupervisorContextStrategy { CHAT_MEMORY,SUMMARIZATION,CHAT_MEMORY_AND_SUMMARIZATION }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/SupervisorResponseStrategy.java"),
                "package dev.langchain4j.agentic.supervisor; public enum SupervisorResponseStrategy { LAST,SUMMARY,SCORED }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/ResponseScore.java"),
                "package dev.langchain4j.agentic.supervisor; public final class ResponseScore { public int getScore1(){return 0;} public int getScore2(){return 0;} }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/ResponseAgent.java"),
                "package dev.langchain4j.agentic.supervisor; public interface ResponseAgent { ResponseScore scoreResponses(String a,String b,String c); }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/PlannerAgent.java"),
                "package dev.langchain4j.agentic.supervisor; public interface PlannerAgent extends dev.langchain4j.service.memory.ChatMemoryAccess { AgentInvocation plan(Object m,String a,String b,String c,String d); }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/AgentInvocation.java"),
                "package dev.langchain4j.agentic.supervisor; import java.util.Map; public final class AgentInvocation { public String getAgentName(){return null;} public Map<String,Object> getArguments(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/SupervisorPlanner.java.stub"), "");
        write(root.resolve("dev/langchain4j/agentic/supervisor/SupervisorAgent.java"),
                "package dev.langchain4j.agentic.supervisor; public interface SupervisorAgent {}\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/SupervisorAgentService.java"),
                "package dev.langchain4j.agentic.supervisor; public interface SupervisorAgentService<T> {}\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/SupervisorAgentServiceImpl.java"),
                "package dev.langchain4j.agentic.supervisor; public final class SupervisorAgentServiceImpl {}\n");
        write(root.resolve("dev/langchain4j/service/AiServices.java"),
                "package dev.langchain4j.service; import dev.langchain4j.model.chat.ChatModel; import dev.langchain4j.memory.chat.ChatMemoryProvider;\n"
                        + "public final class AiServices<T> { public static <T> AiServices<T> builder(Class<T> c){return null;} public AiServices<T> chatModel(ChatModel m){return this;} public AiServices<T> chatMemoryProvider(ChatMemoryProvider p){return this;} public AiServices<T> chatRequestTransformer(Object t){return this;} public T build(){return null;} }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/SupervisorPlanner.java.stub2"), "");
        write(root.resolve("OrdinaryPojo.java"),
                "public class OrdinaryPojo { String plain; OrdinaryNested nested; }\n");
        write(root.resolve("OrdinaryNested.java"),
                "public class OrdinaryNested { String leaf; }\n");
        write(root.resolve("dev/langchain4j/agentic/supervisor/Unused.java"), "package dev.langchain4j.agentic.supervisor; final class Unused {}\n");
    }

    private static void verifyMetadata(ClassLoader loader) throws Exception {
        Class<?> metadataType = loader.loadClass("dev.langchain4j.agentic.internal.AgenticRecordComponentMetadata");
        Method count = metadataType.getDeclaredMethod("registeredTypeCount");
        count.setAccessible(true);
        assertEquals(23, ((Integer) count.invoke(null)).intValue(), "metadata registered count");
        Method namesMethod = metadataType.getDeclaredMethod("registeredTypeNames");
        namesMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> registered = (Set<String>) namesMethod.invoke(null);
        assertEquals(new HashSet<String>(Arrays.asList(REGISTERED_TYPES)), new HashSet<String>(registered),
                "metadata registered binary names");
        try {
            registered.add("unexpected");
            throw new AssertionError("metadata registered names must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        for (FieldCheck field : fieldsOf(metadataType)) {
            assertTrue(field.type != Class.class && !ClassLoader.class.isAssignableFrom(field.type),
                    "metadata retains a Class/ClassLoader field: " + field.name);
        }
        Method lookup = metadataType.getMethod("lookup", Class.class);
        Method describe = metadataType.getMethod("describe", Class.class, java.util.function.BiFunction.class);
        for (String binaryName : REGISTERED_TYPES) {
            Class<?> target = loader.loadClass(binaryName);
            Object found = lookup.invoke(null, target);
            assertEquals(Boolean.TRUE, found, "registered lookup returned false: " + binaryName);
            String description = (String) describe.invoke(null, target, new ComponentNameDescription());
            assertDescriptionComponents(description, EXPECTED_COMPONENT_NAMES.get(binaryName), binaryName);
        }
        Class<?> pojo = loader.loadClass("OrdinaryPojo");
        assertEquals(Boolean.FALSE, lookup.invoke(null, pojo), "ordinary POJO must not be registered");
        assertEquals(null, describe.invoke(null, pojo, new ComponentNameDescription()),
                "ordinary POJO describe must be empty");
    }

    private static void assertDescriptionComponents(String description, String[] expected, String binaryName) {
        assertTrue(expected != null, "missing independent expected components: " + binaryName);
        String[] actual = description.length() == 0 ? new String[0] : description.split(", ");
        assertEquals(expected.length, actual.length, "runtime component count: " + binaryName);
        for (int i = 0; i < expected.length; i++) {
            int separator = actual[i].indexOf(": ");
            assertTrue(separator > 0, "runtime component description malformed: " + binaryName + "." + i);
            assertEquals(expected[i], actual[i].substring(0, separator),
                    "runtime component order/name: " + binaryName + "." + i);
        }
    }

    private static final class ComponentNameDescription
            implements java.util.function.BiFunction<Class<?>, String, String> {
        @Override
        public String apply(Class<?> type, String name) {
            return name + ": " + type.getSimpleName();
        }
    }

    private static void verifySupervisorDescriptions(ClassLoader loader) throws Exception {
        Class<?> supervisor = loader.loadClass("dev.langchain4j.agentic.supervisor.SupervisorPlanner");
        Method description = supervisor.getDeclaredMethod("argumentDescription", Class.class, String.class);
        description.setAccessible(true);
        assertEquals("value: String", description.invoke(null, String.class, "value"), "simple description");
        assertEquals("ctx: {agentServiceClass: Class, agentBuilder: AgentBuilder}",
                description.invoke(null, loader.loadClass("dev.langchain4j.agentic.AgenticServices$DefaultDeclarativeAgentCreationContext"), "ctx"),
                "generic record description");
        assertEquals("config: {configurator: Consumer, subAgentResolver: Function, agentInstanceFactory: Function, defaultMemoryIdSupplier: Supplier}",
                description.invoke(null, loader.loadClass("dev.langchain4j.agentic.AgenticServices$AgentConfigurator"), "config"),
                "nested record description");
        assertEquals("value: {plain: String, nested: {leaf: String}}",
                description.invoke(null, loader.loadClass("OrdinaryPojo"), "value"), "ordinary POJO field fallback");
        assertEquals("", description.invoke(null, loader.loadClass("java.lang.String"), (Object) null),
                "null argument name");
    }

    private static void verifyDriftFails(Path productRoot, Path root) throws Exception {
        Path source = root.resolve("src");
        Path classes = root.resolve("classes");
        Files.createDirectories(classes);
        write(source.resolve(METADATA), read(productRoot.resolve(METADATA)));
        write(source.resolve("dev/langchain4j/agentic/scope/AgenticScopeKey.java"),
                "package dev.langchain4j.agentic.scope; public final class AgenticScopeKey {\n"
                        + " public String agentId(){return null;} public String memoryId(){return null;} }\n");
        compile(source, classes);
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, null);
        try {
            Class<?> metadata = loader.loadClass("dev.langchain4j.agentic.internal.AgenticRecordComponentMetadata");
            Class<?> drift = loader.loadClass("dev.langchain4j.agentic.scope.AgenticScopeKey");
            try {
                metadata.getMethod("lookup", Class.class).invoke(null, drift);
                throw new AssertionError("registered accessor/type drift must fail fast");
            } catch (InvocationTargetException expected) {
                Throwable cause = expected.getCause();
                assertTrue(cause instanceof IllegalStateException, "drift exception type");
                assertTrue(cause.getMessage().indexOf("dev.langchain4j.agentic.scope.AgenticScopeKey.memoryId") >= 0,
                        "drift message must identify FQCN/component: " + cause.getMessage());
            }
        } finally {
            loader.close();
        }
    }

    private static List<FieldCheck> fieldsOf(Class<?> type) {
        List<FieldCheck> fields = new ArrayList<FieldCheck>();
        for (java.lang.reflect.Field field : type.getDeclaredFields()) {
            fields.add(new FieldCheck(field.getName(), field.getType()));
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            fields.addAll(fieldsOf(nested));
        }
        return fields;
    }

    private static void compile(Path sourceRoot, Path classes) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null, "system compiler unavailable");
        List<Path> sources = new ArrayList<Path>();
        collectJava(sourceRoot, sources);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
        try {
            List<String> options = Arrays.asList("-source", "8", "-target", "8", "-proc:none", "-Xlint:all", "-d", classes.toString());
            Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromFiles(toFiles(sources));
            Boolean success = compiler.getTask(null, manager, diagnostics, options, null, units).call();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.WARNING
                        || diagnostic.getKind() == Diagnostic.Kind.MANDATORY_WARNING) {
                    throw new AssertionError("runtime harness warning: " + diagnostic);
                }
            }
            assertTrue(Boolean.TRUE.equals(success), "runtime harness compilation failed: " + diagnostics.getDiagnostics());
        } finally {
            manager.close();
        }
    }

    private static void collectJava(Path root, List<Path> files) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(root);
        try {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    collectJava(path, files);
                } else if (path.toString().endsWith(".java")) {
                    files.add(path);
                }
            }
        } finally {
            stream.close();
        }
    }

    private static List<java.io.File> toFiles(List<Path> paths) {
        List<java.io.File> files = new ArrayList<java.io.File>();
        for (Path path : paths) {
            files.add(path.toFile());
        }
        return files;
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), "missing source " + path);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void write(Path path, String value) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FieldCheck {
        private final String name;
        private final Class<?> type;

        private FieldCheck(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }
    }
}
