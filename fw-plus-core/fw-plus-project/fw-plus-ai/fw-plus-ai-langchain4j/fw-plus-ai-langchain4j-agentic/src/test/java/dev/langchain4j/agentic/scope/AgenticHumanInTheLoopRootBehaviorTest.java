package dev.langchain4j.agentic.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.internal.SuspendedResponse;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.service.MemoryId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgenticHumanInTheLoopRootBehaviorTest {

    private final AtomicInteger completedBeforeHuman = new AtomicInteger();
    private final AtomicInteger humanRequests = new AtomicInteger();
    private final AtomicInteger completedAfterHuman = new AtomicInteger();

    @AfterEach
    void clearStore() {
        AgenticScopePersister.setStore(null);
    }

    @Test
    void publicRootResumesWithoutReplayingCompletedAgents() {
        Root root = buildRoot();

        ResultWithAgenticScope<String> suspended = root.run("conversation");

        assertTrue(suspended.suspended());
        assertEquals(Collections.singleton("approval"), suspended.agenticScope().pendingResponseIds());
        assertEquals(1, completedBeforeHuman.get());
        assertEquals(1, humanRequests.get());
        assertEquals(0, completedAfterHuman.get());

        ResultWithAgenticScope<String> completed = suspended.completePendingResponse("approved");

        assertFalse(completed.suspended());
        assertEquals("done", completed.result());
        assertEquals("approved", completed.agenticScope().readState("approval"));
        assertTrue(completed.agenticScope().pendingResponseIds().isEmpty());
        assertEquals(1, completedBeforeHuman.get());
        assertEquals(1, humanRequests.get());
        assertEquals(1, completedAfterHuman.get());
    }

    @Test
    void ordinaryStringRootReportsSuspensionWithoutWaitingForHumanResponse() throws Exception {
        final StringRoot root = buildStringRoot();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> invocation = executor.submit(() -> root.run("bounded-suspension"));
        try {
            invocation.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            invocation.cancel(true);
            throw new AssertionError("ordinary String root invocation hung while suspended", timeout);
        } catch (ExecutionException failure) {
            assertTrue(failure.getCause() instanceof AgenticSystemSuspendedException,
                    "ordinary root must propagate AgenticSystemSuspendedException, got " + failure.getCause());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS),
                    "ordinary root executor did not terminate");
        }
    }

    @Test
    void persistedCompletionSurvivesRegistryReloadBeforeRootReinvoke() {
        JsonStore store = new JsonStore();
        AgenticScopePersister.setStore(store);
        Root firstRoot = buildRoot();

        ResultWithAgenticScope<String> suspended = firstRoot.run("persistent-conversation");
        AgenticScope persistedScope = suspended.agenticScope();
        assertTrue(persistedScope.completePendingResponse("approval", "approved"));

        Root restartedRoot = buildRoot();
        ResultWithAgenticScope<String> completed = restartedRoot.run("persistent-conversation");

        assertFalse(completed.suspended());
        assertEquals("done", completed.result());
        assertEquals("approved", completed.agenticScope().readState("approval"));
        assertTrue(completed.agenticScope().pendingResponseIds().isEmpty());
    }

    private StringRoot buildStringRoot() {
        final HumanInTheLoop human = AgenticServices.humanInTheLoopBuilder()
                .outputKey("approval")
                .responseProvider(scope -> new SuspendedResponse<String>("ordinary-string-root"))
                .build();
        return AgenticServices.sequenceBuilder(StringRoot.class)
                .subAgents(human)
                .build();
    }

    private Root buildRoot() {
        AgenticServices.AgenticScopeAction before = AgenticServices.agentAction(scope -> {
            completedBeforeHuman.incrementAndGet();
        });
        HumanInTheLoop human = AgenticServices.humanInTheLoopBuilder()
                .outputKey("approval")
                .responseProvider(scope -> {
                    humanRequests.incrementAndGet();
                    return new SuspendedResponse<String>("approval");
                })
                .build();
        AgenticServices.AgenticScopeAction after = AgenticServices.agentAction(scope -> {
            assertEquals("approved", scope.readState("approval"));
            completedAfterHuman.incrementAndGet();
            scope.writeState("result", "done");
        });

        return AgenticServices.sequenceBuilder(Root.class)
                .outputKey("result")
                .subAgents(before, human, after)
                .build();
    }

    interface Root {

        @SequenceAgent(outputKey = "result", subAgents = {})
        ResultWithAgenticScope<String> run(@MemoryId String memoryId);
    }

    interface StringRoot {

        @SequenceAgent(outputKey = "approval", subAgents = {})
        String run(@MemoryId String memoryId);
    }

    private static final class JsonStore implements AgenticScopeStore {
        private final Map<AgenticScopeKey, String> values =
                new LinkedHashMap<AgenticScopeKey, String>();

        @Override
        public boolean save(AgenticScopeKey key, DefaultAgenticScope scope) {
            values.put(key, AgenticScopeSerializer.toJson(scope));
            return true;
        }

        @Override
        public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
            String json = values.get(key);
            return json == null ? Optional.empty() : Optional.of(AgenticScopeSerializer.fromJson(json));
        }

        @Override
        public boolean delete(AgenticScopeKey key) {
            return values.remove(key) != null;
        }

        @Override
        public Set<AgenticScopeKey> getAllKeys() {
            return values.keySet();
        }
    }
}
