package dev.langchain4j.agentic.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import static dev.langchain4j.agentic.internal.AgentUtil.agentToExecutor;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.service.MemoryId;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class AgenticPublicRootWorkflowTest {

    @Test
    void publicSequenceRootInvokesActionsAndReturnsScopeOutput() {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();

        RootAgent root = AgenticServices.sequenceBuilder(RootAgent.class)
                .subAgents(
                        AgenticServices.agentAction(() -> firstCalls.incrementAndGet()),
                        AgenticServices.agentAction(scope -> {
                            secondCalls.incrementAndGet();
                            scope.writeState("result", "completed");
                        }))
                .output(scope -> scope.readState("result"))
                .build();

        assertNotNull(root);
        assertEquals("completed", root.run("root-memory"));
        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
        assertNotNull(root.getAgenticScope("root-memory"));
    }

    @Test
    void declarativeCreateAgenticSystemRunsSequenceAndOutputFunction() {
        DeclarativeRoot root = AgenticServices.createAgenticSystem(
                DeclarativeRoot.class,
                null,
                new AgenticServices.AgentConfigurator(
                        configuration -> {},
                        subAgentClass -> subAgentClass == DeclarativeRoot.DeclarativeFirst.class
                                ? AgenticServices.agentAction(() -> DeclarativeRoot.firstCalls.incrementAndGet())
                                : subAgentClass == DeclarativeRoot.DeclarativeSecond.class
                                ? AgenticServices.agentAction(() -> DeclarativeRoot.secondCalls.incrementAndGet())
                                : null,
                        null,
                        null));

        assertEquals("declarative-done", root.run("declarative-memory"));
        assertEquals(1, DeclarativeRoot.firstCalls.get());
        assertEquals(1, DeclarativeRoot.secondCalls.get());
    }

    @Test
    void publicConditionalRootInvokesOnlyActivatedBranch() {
        AtomicInteger activeCalls = new AtomicInteger();
        AtomicInteger skippedCalls = new AtomicInteger();
        ConditionalRoot root = AgenticServices.conditionalBuilder(ConditionalRoot.class)
                .subAgent(scope -> "yes".equals(scope.readState("choice")),
                        agentToExecutor(AgenticServices.agentAction(scope -> {
                            activeCalls.incrementAndGet();
                            scope.writeState("result", "active");
                        })))
                .subAgent(scope -> false,
                        agentToExecutor(AgenticServices.agentAction(() -> skippedCalls.incrementAndGet())))
                .beforeCall(scope -> scope.writeState("choice", "yes"))
                .output(scope -> scope.readState("result"))
                .build();

        assertEquals("active", root.run("conditional-memory"));
        assertEquals(1, activeCalls.get());
        assertEquals(0, skippedCalls.get());
    }

    @Test
    void publicLoopRootRunsConfiguredIterationsAndReturnsState() {
        AtomicInteger calls = new AtomicInteger();
        LoopRoot root = AgenticServices.loopBuilder(LoopRoot.class)
                .subAgents(AgenticServices.agentAction(scope -> {
                    int value = calls.incrementAndGet();
                    scope.writeState("result", value);
                }))
                .maxIterations(2)
                .output(scope -> scope.readState("result"))
                .build();

        assertEquals(2, root.run("loop-memory"));
        assertEquals(2, calls.get());
    }

    public interface RootAgent extends AgenticScopeAccess {
        @Agent(outputKey = "result")
        String run(@MemoryId String memoryId);
    }

    public interface ConditionalRoot extends AgenticScopeAccess {
        @Agent
        String run(@MemoryId String memoryId);
    }

    public interface LoopRoot extends AgenticScopeAccess {
        @Agent
        Integer run(@MemoryId String memoryId);
    }

    public interface DeclarativeRoot extends AgenticScopeAccess {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();

        @SequenceAgent(outputKey = "result", subAgents = {DeclarativeFirst.class, DeclarativeSecond.class})
        String run(@MemoryId String memoryId);

        public static class DeclarativeFirst {
            @Agent
            public String run() {
                firstCalls.incrementAndGet();
                return "first";
            }
        }

        public static class DeclarativeSecond {
            @Agent
            public String run() {
                secondCalls.incrementAndGet();
                return "second";
            }
        }

        @Output
        static String output(AgenticScope scope) {
            return "declarative-done";
        }
    }
}
