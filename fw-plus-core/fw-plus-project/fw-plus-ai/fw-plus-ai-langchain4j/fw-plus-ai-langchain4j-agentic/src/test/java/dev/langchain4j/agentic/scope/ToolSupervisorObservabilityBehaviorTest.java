package dev.langchain4j.agentic.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agent.tool.CompensateFor;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.agentic.observability.AfterAgentToolExecution;
import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.observability.MonitoredExecution;
import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentArgument;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlanningContext;
import dev.langchain4j.agentic.supervisor.AgentInvocation;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorPlanner;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.exception.ToolArgumentsException;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.LangChain4jManaged;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServiceContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolService;
import dev.langchain4j.service.tool.ToolServiceContext;
import dev.langchain4j.service.tool.ToolServiceResult;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToolSupervisorObservabilityBehaviorTest {

    @Test
    void toolDiscoveryAndExecutionCallbacksUseTheRealToolService() {
        CallbackTools tools = new CallbackTools();
        List<dev.langchain4j.service.tool.AiServiceTool> discovered = ToolService.findTools(tools);
        assertEquals(Arrays.asList("fail", "first", "greet", "second"), sortedNames(discovered));

        ToolService service = new ToolService();
        service.tools(Collections.<Object>singletonList(tools));
        InvocationContext invocationContext = InvocationContext.builder().chatMemoryId("alice").build();
        ToolExecutionRequest request = request("call-greet", "greet", "{}");
        AtomicInteger before = new AtomicInteger();
        AtomicInteger after = new AtomicInteger();
        service.beforeToolExecution(event -> {
            assertSame(request, event.request());
            assertSame(invocationContext, event.invocationContext());
            before.incrementAndGet();
        });
        service.afterToolExecution(event -> {
            assertSame(request, event.request());
            assertEquals("hello", event.result());
            assertSame(invocationContext, event.invocationContext());
            after.incrementAndGet();
        });

        ToolExecutionResult result = service.executeTool(
                invocationContext, service.toolExecutors(), request, null, null);

        assertFalse(result.isError());
        assertEquals("hello", result.resultText());
        assertEquals(1, before.get());
        assertEquals(1, after.get());
        assertEquals("alice", tools.lastMemoryId);
    }

    @Test
    void toolArgumentAndExecutionErrorHandlersReceiveTheRealContext() {
        ToolExecutionRequest request = request("call-error", "broken", "{}");
        InvocationContext invocationContext = InvocationContext.builder().chatMemoryId("bob").build();
        AtomicInteger argumentErrors = new AtomicInteger();
        AtomicInteger executionErrors = new AtomicInteger();

        ToolExecutionResult argumentResult = ToolService.executeWithErrorHandling(
                request,
                new ToolExecutor() {
                    @Override
                    public String execute(ToolExecutionRequest ignored, Object memoryId) {
                        throw new ToolArgumentsException("bad arguments");
                    }
                },
                invocationContext,
                (error, context) -> {
                    assertSame(request, context.toolExecutionRequest());
                    assertSame(invocationContext, context.invocationContext());
                    assertEquals("bob", context.memoryId());
                    assertEquals("bad arguments", error.getMessage());
                    argumentErrors.incrementAndGet();
                    return ToolErrorHandlerResult.text("arguments handled");
                },
                (error, context) -> ToolErrorHandlerResult.text("unexpected execution handler"));

        ToolExecutionResult executionResult = ToolService.executeWithErrorHandling(
                request,
                new ToolExecutor() {
                    @Override
                    public String execute(ToolExecutionRequest ignored, Object memoryId) {
                        throw new IllegalStateException("service unavailable");
                    }
                },
                invocationContext,
                (error, context) -> ToolErrorHandlerResult.text("unexpected arguments handler"),
                (error, context) -> {
                    assertSame(request, context.toolExecutionRequest());
                    assertSame(invocationContext, context.invocationContext());
                    assertEquals("service unavailable", error.getMessage());
                    executionErrors.incrementAndGet();
                    return ToolErrorHandlerResult.text("execution handled");
                });

        assertTrue(argumentResult.isError());
        assertEquals("arguments handled", argumentResult.resultText());
        assertTrue(executionResult.isError());
        assertEquals("execution handled", executionResult.resultText());
        assertEquals(1, argumentErrors.get());
        assertEquals(1, executionErrors.get());
    }

    @Test
    void toolServiceCompensatesInReverseOrderAndContinuesAfterCompensationFailure() {
        CompensationTools tools = new CompensationTools();
        ToolService service = new ToolService();
        service.tools(Collections.<Object>singletonList(tools));
        service.compensateOnToolErrors(true);
        service.executionErrorHandler((error, context) -> ToolErrorHandlerResult.text(error.getMessage()));

        ToolServiceContext toolContext = ToolServiceContext.builder()
                .effectiveTools(service.toolSpecifications())
                .availableTools(service.toolSpecifications())
                .toolExecutors(service.toolExecutors())
                .returnBehaviors(Collections.emptyMap())
                .dynamicToolProviders(Collections.emptyList())
                .build();
        ChatResponse firstResponse = response(AiMessage.from(Arrays.asList(
                request("first-call", "first", "{}"),
                request("second-call", "second", "{}"),
                request("failure-call", "fail", "{}"))));
        ChatResponse finalResponse = response(AiMessage.from("recovered"));
        AiServiceContext aiServiceContext = new AiServiceContext(Assistant.class);
        InvocationContext invocationContext = InvocationContext.builder().chatMemoryId("compensation-session").build();

        ToolServiceResult result = service.executeInferenceAndToolsLoop(
                aiServiceContext,
                "compensation-session",
                firstResponse,
                ChatRequestParameters.builder().build(),
                new ArrayList<ChatMessage>(),
                null,
                invocationContext,
                toolContext,
                ignored -> finalResponse);

        assertEquals(Arrays.asList("undo-second", "undo-first"), tools.events);
        assertEquals(3, result.toolExecutions().size());
        assertTrue(result.toolExecutions().get(2).hasFailed());
        assertEquals("recovered", result.finalResponse().aiMessage().text());
    }

    @Test
    void supervisorPlannerStopsAtBudgetAndRestoresExecutionState() {
        DefaultAgenticScope scope = scope("supervisor-budget");
        SupervisorPlanner planner = new SupervisorPlanner(
                null,
                null,
                0,
                SupervisorContextStrategy.CHAT_MEMORY,
                SupervisorResponseStrategy.LAST,
                ignored -> "request",
                "answer",
                null);

        Action action = planner.nextAction(new PlanningContext(scope, null));

        assertTrue(action.isDone());
        assertEquals("", action.result());
        assertEquals(1, ((Number) planner.executionState().get("loopCount")).intValue());
        planner.restoreExecutionState(Collections.<String, Object>singletonMap("loopCount", Integer.valueOf(7)));
        assertEquals(7, ((Number) planner.executionState().get("loopCount")).intValue());
    }

    @Test
    void supervisorLastAndSummaryResponseStrategiesAreDeterministic() throws Exception {
        AgentInvocation done = new AgentInvocation();
        done.setAgentName("done");
        done.setArguments(Collections.<String, Object>singletonMap("response", "summary"));
        DefaultAgenticScope scope = scope("supervisor-response");

        assertEquals("last", invokeResult(SupervisorResponseStrategy.LAST, scope, done));
        assertEquals("summary", invokeResult(SupervisorResponseStrategy.SUMMARY, scope, done));
    }

    @Test
    void monitorTracksSuccessfulFailedOngoingExecutionsAndMemoryIds() {
        AgentMonitor monitor = new AgentMonitor();
        MonitorAgent successAgent = new MonitorAgent("success-agent");
        MonitorAgent failedAgent = new MonitorAgent("failed-agent");
        MonitorAgent ongoingAgent = new MonitorAgent("ongoing-agent");

        completeSuccessfully(monitor, scope("success"), successAgent, "ok");
        DefaultAgenticScope failedScope = scope("failed");
        monitor.beforeAgentInvocation(request(failedScope, failedAgent));
        monitor.onAgentInvocationError(new AgentInvocationError(
                failedScope, failedAgent, Collections.emptyMap(), new IllegalStateException("failed")));
        DefaultAgenticScope ongoingScope = scope("ongoing");
        monitor.beforeAgentInvocation(request(ongoingScope, ongoingAgent));

        assertEquals(1, monitor.successfulExecutionsFor("success").size());
        assertEquals(1, monitor.failedExecutionsFor("failed").size());
        assertNotNull(monitor.ongoingExecutionFor("ongoing"));
        assertEquals(3, monitor.allMemoryIds().size());
        assertTrue(monitor.allMemoryIds().contains("success"));
        assertTrue(monitor.allMemoryIds().contains("failed"));
        assertTrue(monitor.allMemoryIds().contains("ongoing"));
    }

    @Test
    void monitorRecordsToolEventsAndClearLeavesOngoingExecutionIntact() {
        AgentMonitor monitor = new AgentMonitor();
        MonitorAgent agent = new MonitorAgent("tool-agent");
        DefaultAgenticScope toolScope = scope("tool-session");
        monitor.beforeAgentInvocation(request(toolScope, agent));
        ToolExecution toolExecution = toolExecution(toolScope, "lookup", "found");
        monitor.afterAgentToolExecution(new AfterAgentToolExecution(agent, toolExecution));
        monitor.afterAgentInvocation(new AgentResponse(
                toolScope, agent, Collections.emptyMap(), "done", null, null));

        MonitoredExecution completed = monitor.successfulExecutionsFor("tool-session").get(0);
        assertEquals(1, completed.topLevelInvocations().toolExecutions().size());
        assertEquals("lookup", completed.topLevelInvocations().toolExecutions().get(0).request().name());

        DefaultAgenticScope ongoingScope = scope("still-ongoing");
        monitor.beforeAgentInvocation(request(ongoingScope, agent));
        monitor.clear();
        assertTrue(monitor.successfulExecutions().isEmpty());
        assertTrue(monitor.failedExecutions().isEmpty());
        assertNotNull(monitor.ongoingExecutionFor("still-ongoing"));
    }

    @Test
    void monitorRetentionEvictsOldestSessionPerOutcome() {
        AgentMonitor monitor = new AgentMonitor();
        monitor.setMaxRetainedSessions(1);
        MonitorAgent agent = new MonitorAgent("retention-agent");

        completeSuccessfully(monitor, scope("success-one"), agent, "one");
        completeSuccessfully(monitor, scope("success-two"), agent, "two");
        fail(monitor, scope("failed-one"), agent);
        fail(monitor, scope("failed-two"), agent);

        assertTrue(monitor.successfulExecutionsFor("success-one").isEmpty());
        assertEquals(1, monitor.successfulExecutionsFor("success-two").size());
        assertTrue(monitor.failedExecutionsFor("failed-one").isEmpty());
        assertEquals(1, monitor.failedExecutionsFor("failed-two").size());
        assertEquals(2, monitor.allMemoryIds().size());
    }

    private static Object invokeResult(
            SupervisorResponseStrategy strategy,
            DefaultAgenticScope scope,
            AgentInvocation done) throws Exception {
        SupervisorPlanner planner = new SupervisorPlanner(
                null, null, 1, SupervisorContextStrategy.CHAT_MEMORY, strategy,
                ignored -> "request", "answer", null);
        Method result = SupervisorPlanner.class.getDeclaredMethod(
                "result", dev.langchain4j.agentic.scope.AgenticScope.class,
                String.class, String.class, AgentInvocation.class);
        result.setAccessible(true);
        return result.invoke(planner, scope, "request", "last", done);
    }

    private static void completeSuccessfully(
            AgentMonitor monitor, DefaultAgenticScope scope, MonitorAgent agent, Object output) {
        monitor.beforeAgentInvocation(request(scope, agent));
        monitor.afterAgentInvocation(new AgentResponse(
                scope, agent, Collections.emptyMap(), output, null, null));
    }

    private static void fail(AgentMonitor monitor, DefaultAgenticScope scope, MonitorAgent agent) {
        monitor.beforeAgentInvocation(request(scope, agent));
        monitor.onAgentInvocationError(new AgentInvocationError(
                scope, agent, Collections.emptyMap(), new IllegalStateException("failure")));
    }

    private static AgentRequest request(DefaultAgenticScope scope, AgentInstance agent) {
        return new AgentRequest(scope, agent, Collections.<String, Object>emptyMap());
    }

    private static ToolExecution toolExecution(DefaultAgenticScope scope, String name, String result) {
        Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> managed =
                new HashMap<Class<? extends LangChain4jManaged>, LangChain4jManaged>();
        managed.put(AgenticScope.class, scope);
        InvocationContext context = InvocationContext.builder()
                .chatMemoryId(scope.memoryId())
                .managedParameters(managed)
                .build();
        return ToolExecution.builder()
                .request(request("tool-call", name, "{}"))
                .result(ToolExecutionResult.builder().resultText(result).build())
                .invocationContext(context)
                .build();
    }

    private static ToolExecutionRequest request(String id, String name, String arguments) {
        return ToolExecutionRequest.builder().id(id).name(name).arguments(arguments).build();
    }

    private static ChatResponse response(AiMessage message) {
        return ChatResponse.builder()
                .aiMessage(message)
                .metadata(ChatResponseMetadata.builder().tokenUsage(new TokenUsage(1, 1)).build())
                .build();
    }

    private static DefaultAgenticScope scope(Object memoryId) {
        return new DefaultAgenticScope(memoryId, DefaultAgenticScope.Kind.EPHEMERAL);
    }

    private static List<String> sortedNames(List<dev.langchain4j.service.tool.AiServiceTool> tools) {
        List<String> names = names(tools);
        Collections.sort(names);
        return names;
    }

    private static List<String> names(List<dev.langchain4j.service.tool.AiServiceTool> tools) {
        List<String> names = new ArrayList<String>();
        for (dev.langchain4j.service.tool.AiServiceTool tool : tools) {
            names.add(tool.name());
        }
        return names;
    }

    interface Assistant {
        String answer(String request);
    }

    static final class CallbackTools {
        private Object lastMemoryId;

        @Tool(name = "greet")
        public String greet(@ToolMemoryId Object memoryId) {
            lastMemoryId = memoryId;
            return "hello";
        }

        @Tool(name = "first")
        public String first() {
            return "first";
        }

        @Tool(name = "second")
        public String second() {
            return "second";
        }

        @Tool(name = "fail")
        public String fail() {
            return "unused";
        }
    }

    static final class CompensationTools {
        private final List<String> events = new ArrayList<String>();

        @Tool(name = "first")
        public String first() {
            return "first";
        }

        @CompensateFor("first")
        public void undoFirst(ToolExecution execution) {
            events.add("undo-first");
        }

        @Tool(name = "second")
        public String second() {
            return "second";
        }

        @CompensateFor("second")
        public void undoSecond(ToolExecution execution) {
            events.add("undo-second");
            throw new IllegalStateException("undo-second failed");
        }

        @Tool(name = "fail")
        public String fail() {
            throw new IllegalStateException("tool failed");
        }
    }

    static final class MonitorAgent implements AgentInstance {
        private final String id;

        private MonitorAgent(String id) {
            this.id = id;
        }

        @Override
        public Class<?> type() {
            return MonitorAgent.class;
        }

        @Override
        public Class<? extends Planner> plannerType() {
            return null;
        }

        @Override
        public String name() {
            return id;
        }

        @Override
        public String agentId() {
            return id;
        }

        @Override
        public String description() {
            return "test monitor agent";
        }

        @Override
        public Type outputType() {
            return String.class;
        }

        @Override
        public String outputKey() {
            return id;
        }

        @Override
        public boolean async() {
            return false;
        }

        @Override
        public List<AgentArgument> arguments() {
            return Collections.emptyList();
        }

        @Override
        public AgentInstance parent() {
            return null;
        }

        @Override
        public List<AgentInstance> subagents() {
            return Collections.emptyList();
        }

        @Override
        public AgenticSystemTopology topology() {
            return AgenticSystemTopology.SEQUENCE;
        }
    }
}
