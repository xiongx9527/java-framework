package dev.langchain4j.agentic.workflow;

import static dev.langchain4j.agentic.internal.AgentUtil.agentToExecutor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.service.V;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.internal.PlannerExecutor;
import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.planner.PlanningContext;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.agentic.workflow.impl.ConditionalPlanner;
import dev.langchain4j.agentic.workflow.impl.LoopPlanner;
import dev.langchain4j.agentic.workflow.impl.ParallelMapperPlanner;
import dev.langchain4j.agentic.workflow.impl.ParallelPlanner;
import dev.langchain4j.agentic.workflow.impl.SequentialPlanner;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class AgenticWorkflowBehaviorTest {

    @Test
    void defaultWorkflowEntrypointsReturnUsableBuilders() {
        assertTrue(AgenticServices.sequenceBuilder() instanceof SequentialAgentService);
        assertTrue(AgenticServices.parallelBuilder() instanceof ParallelAgentService);
        assertTrue(AgenticServices.parallelMapperBuilder() instanceof ParallelMapperService);
        assertTrue(AgenticServices.loopBuilder() instanceof LoopAgentService);
        assertTrue(AgenticServices.conditionalBuilder() instanceof ConditionalAgentService);
        assertTrue(AgenticServices.parallelMapperBuilder(MapperRoot.class) instanceof ParallelMapperService);
    }

    @Test
    void sequentialPlannerSchedulesInDeclarationOrderAndRestoresScheduledAgent() {
        AgentExecutor first = action("first");
        AgentExecutor second = action("second");
        SequentialPlanner planner = new SequentialPlanner();
        planner.init(context(DefaultAgenticScope.ephemeralAgenticScope(), action("root"), first, second));

        Action firstAction = planner.firstAction(planningContext());
        assertEquals(Collections.singletonList(first), firstAction(firstAction).agentsToCall());
        assertEquals(Collections.singletonMap("cursor", 0), planner.executionState());

        SequentialPlanner restored = new SequentialPlanner();
        restored.init(context(DefaultAgenticScope.ephemeralAgenticScope(), action("root"), first, second));
        restored.restoreExecutionState(planner.executionState());
        assertEquals(Collections.singletonList(first), firstAction(restored.firstAction(planningContext())).agentsToCall());

        Action secondAction = planner.nextAction(planningContext());
        assertEquals(Collections.singletonList(second), firstAction(secondAction).agentsToCall());
        assertTrue(planner.terminated());
        assertTrue(planner.nextAction(planningContext()).isDone());
    }

    @Test
    void conditionalPlannerSelectsAllTrueBranchesInDeclarationOrder() {
        AgentExecutor first = action("first");
        AgentExecutor second = action("second");
        AgentExecutor skipped = action("skipped");
        ConditionalPlanner planner = new ConditionalPlanner(Arrays.asList(
                new ConditionalAgent("false", scope -> false, Collections.singletonList(skipped)),
                new ConditionalAgent("first", scope -> true, Collections.singletonList(first)),
                new ConditionalAgent("second", scope -> true, Collections.singletonList(second))));

        Action result = planner.firstAction(planningContext());
        assertEquals(Arrays.asList(first, second), firstAction(result).agentsToCall());
        assertEquals(AgenticSystemTopology.ROUTER, planner.topology());
        assertTrue(planner.terminated());

        ConditionalAgentInstance instance = planner.as(ConditionalAgentInstance.class, action("root"));
        assertEquals(3, instance.conditionalSubagents().size());
    }

    @Test
    void conditionalPlannerWithNoMatchingBranchCompletesWithoutInvocation() {
        ConditionalPlanner planner = new ConditionalPlanner(Collections.singletonList(
                new ConditionalAgent("false", scope -> false, Collections.singletonList(action("skipped")))));

        Action result = planner.firstAction(planningContext());
        assertTrue(result.isDone());
        assertFalse(result instanceof Action.AgentCallAction);
    }

    @Test
    void loopPlannerHonorsMaximumIterationsAndPersistsCursorAndIteration() {
        AgentExecutor first = action("first");
        AgentExecutor second = action("second");
        LoopPlanner planner = new LoopPlanner(2, true, (scope, iteration) -> false, "never");
        planner.init(context(DefaultAgenticScope.ephemeralAgenticScope(), action("root"), first, second));

        assertEquals(Collections.singletonList(first), firstAction(planner.firstAction(planningContext())).agentsToCall());
        assertEquals(mapOf("cursor", 0, "iteration", 1), planner.executionState());
        assertEquals(Collections.singletonList(second), firstAction(planner.nextAction(planningContext())).agentsToCall());
        assertEquals(mapOf("cursor", 1, "iteration", 1), planner.executionState());
        assertEquals(Collections.singletonList(first), firstAction(planner.nextAction(planningContext())).agentsToCall());
        assertEquals(mapOf("cursor", 0, "iteration", 2), planner.executionState());
        assertEquals(Collections.singletonList(second), firstAction(planner.nextAction(planningContext())).agentsToCall());
        assertTrue(planner.nextAction(planningContext()).isDone());
    }

    @Test
    void loopPlannerCanExitAfterAnIteration() {
        AtomicInteger checks = new AtomicInteger();
        LoopPlanner planner = new LoopPlanner(10, true, (scope, iteration) -> {
            checks.incrementAndGet();
            return iteration == 1;
        }, "after first iteration");
        planner.init(context(DefaultAgenticScope.ephemeralAgenticScope(), action("root"), action("only")));

        planner.firstAction(planningContext());
        assertTrue(planner.nextAction(planningContext()).isDone());
        assertEquals(1, checks.get());
    }

    @Test
    void parallelPlannerDispatchesEverySiblingAsOneCall() {
        AgentExecutor first = action("first");
        AgentExecutor second = action("second");
        ParallelPlanner planner = new ParallelPlanner();
        planner.init(context(DefaultAgenticScope.ephemeralAgenticScope(), action("root"), first, second));

        Action call = planner.firstAction(planningContext());
        assertEquals(Arrays.asList(first, second), firstAction(call).agentsToCall());
        assertEquals(AgenticSystemTopology.PARALLEL, planner.topology());
        assertTrue(planner.nextAction(planningContext()).isDone());
    }

    @Test
    void parallelSiblingFailureRemainsAnInvocationFailure() {
        AgentExecutor failing = agentToExecutor(AgenticServices.agentAction(() -> {
            throw new IllegalStateException("parallel sibling failed");
        }));
        AgentExecutor successful = agentToExecutor(AgenticServices.agentAction(() -> { }));
        ParallelPlanner planner = new ParallelPlanner();
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        planner.init(context(scope, action("root"), failing, successful));

        Action call = planner.firstAction(new PlanningContext(scope, null));
        assertEquals(Arrays.asList(failing, successful), firstAction(call).agentsToCall());
        assertThrows(dev.langchain4j.agentic.agent.AgentInvocationException.class,
                () -> failing.execute(scope, null));
        assertEquals(Collections.emptyList(), scope.agentInvocations());
    }

    @Test
    void parallelMapperReturnsResultsInInputOrderAndSupportsEmptyInput() {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        AgentExecutor root = agentToExecutor(new MapperRoot());
        AgentExecutor worker = agentToExecutor(new MapperWorker());
        ParallelMapperPlanner planner = new ParallelMapperPlanner("items", false, Object[].class);
        planner.init(context(scope, root, worker));
        scope.writeState("items", Arrays.asList("a", "b", "c"));

        Action call = planner.firstAction(new PlanningContext(scope, null));
        List<AgentExecutor> mapped = firstAction(call).agentsToCall();
        assertEquals(3, mapped.size());
        assertEquals("item_0", mapped.get(0).outputKey());
        assertEquals("item_1", mapped.get(1).outputKey());
        assertEquals("item_2", mapped.get(2).outputKey());

        RecordingPlannerExecutor execution = new RecordingPlannerExecutor();
        for (AgentExecutor instance : mapped) {
            instance.execute(scope, execution);
            Action completion = planner.nextAction(new PlanningContext(scope, null));
            if (completion instanceof Action.DoneWithResultAction) {
                assertEquals(Arrays.asList("A", "B", "C"), completion.result());
            }
        }
        assertEquals(Arrays.asList("A", "B", "C"), scope.readState("mapped"));
        assertEquals(3, execution.invocations.size());

        DefaultAgenticScope emptyScope = DefaultAgenticScope.ephemeralAgenticScope();
        emptyScope.writeState("items", Collections.emptyList());
        ParallelMapperPlanner emptyPlanner = new ParallelMapperPlanner("items", false, Object[].class);
        emptyPlanner.init(context(emptyScope, root, worker));
        Action empty = emptyPlanner.firstAction(new PlanningContext(emptyScope, null));
        assertTrue(empty.isDone());
        assertEquals(Collections.emptyList(), empty.result());
    }

    @Test
    void parallelMapperRejectsScalarItemsProvider() {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        AgentExecutor worker = agentToExecutor(new MapperWorker());
        ParallelMapperPlanner planner = new ParallelMapperPlanner("items", false, Object[].class);
        planner.init(context(scope, agentToExecutor(new MapperRoot()), worker));
        scope.writeState("items", "not-a-collection");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> planner.firstAction(new PlanningContext(scope, null)));
        assertTrue(error.getMessage().contains("itemsProvider"));
    }

    private static Action.AgentCallAction firstAction(Action action) {
        return assertInstanceOf(Action.AgentCallAction.class, action);
    }

    private static InitPlanningContext context(DefaultAgenticScope scope, AgentInstance root, AgentExecutor... agents) {
        return new InitPlanningContext(scope, root, Arrays.asList(agents));
    }

    private static PlanningContext planningContext() {
        return new PlanningContext(DefaultAgenticScope.ephemeralAgenticScope(), null);
    }

    private static Map<String, Object> mapOf(String firstKey, Object firstValue, String secondKey, Object secondValue) {
        java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();
        values.put(firstKey, firstValue);
        values.put(secondKey, secondValue);
        return values;
    }

    private static AgentExecutor action(final String value) {
        return agentToExecutor(AgenticServices.agentAction(() -> { }));
    }

    static class MapperRoot {
        @Agent(outputKey = "mapped")
        public String root() {
            return null;
        }
    }

    static class MapperWorker {
        @Agent(name = "item", outputKey = "item")
        public String process(@V("item") String item) {
            return item.toUpperCase();
        }
    }

    private static class RecordingPlannerExecutor implements PlannerExecutor {
        private final java.util.ArrayList<AgentInvocation> invocations = new java.util.ArrayList<>();

        @Override
        public void onSubagentInvoked(AgentInvocation agentInvocation) {
            invocations.add(agentInvocation);
        }

        @Override
        public boolean propagateStreaming() {
            return false;
        }
    }
}
