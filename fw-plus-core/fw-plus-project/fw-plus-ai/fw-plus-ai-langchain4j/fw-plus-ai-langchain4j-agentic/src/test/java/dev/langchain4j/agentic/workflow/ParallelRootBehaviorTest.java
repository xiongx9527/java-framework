package dev.langchain4j.agentic.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ParallelRootBehaviorTest {

    @Test
    void publicParallelRootPropagatesFailureAfterEverySiblingFinishes() throws Exception {
        ExecutorService workerExecutor = Executors.newFixedThreadPool(2);
        ExecutorService callerExecutor = Executors.newSingleThreadExecutor();
        CountDownLatch siblingsStarted = new CountDownLatch(2);
        CountDownLatch releaseSibling = new CountDownLatch(1);
        CountDownLatch siblingFinished = new CountDownLatch(1);
        AtomicInteger failureCalls = new AtomicInteger();
        AtomicBoolean siblingInterrupted = new AtomicBoolean();
        try {
            Object failing = AgenticServices.agentAction(
                    (AgenticServices.AgentAction.NonThrowingRunnable) () -> {
                        failureCalls.incrementAndGet();
                        siblingsStarted.countDown();
                        throw new IllegalStateException("parallel failure");
                    });
            Object sibling = AgenticServices.agentAction(
                    (AgenticServices.AgentAction.NonThrowingRunnable) () -> {
                        siblingsStarted.countDown();
                        try {
                            assertTrue(releaseSibling.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            siblingInterrupted.set(true);
                            Thread.currentThread().interrupt();
                        } finally {
                            siblingFinished.countDown();
                        }
                    });

            Root root = AgenticServices.parallelBuilder(Root.class)
                    .subAgents(failing, sibling)
                    .executor(workerExecutor)
                    .build();
            CompletableFuture<String> invocation = CompletableFuture.supplyAsync(root::run, callerExecutor);

            assertTrue(siblingsStarted.await(5, TimeUnit.SECONDS));
            assertEquals(1, failureCalls.get());
            assertFalse(invocation.isDone(), "allOf must wait for the running sibling");
            assertEquals(1L, siblingFinished.getCount());

            releaseSibling.countDown();
            ExecutionException error = assertThrows(
                    ExecutionException.class, () -> invocation.get(5, TimeUnit.SECONDS));
            assertNotNull(error.getCause());
            assertTrue(causeChainContains(error.getCause(), "parallel failure"),
                    "root failure must preserve the failing sibling message: " + error.getCause());
            assertTrue(siblingFinished.await(1, TimeUnit.SECONDS));
            assertFalse(siblingInterrupted.get(), "the fixed upstream parallel path does not cancel siblings");
        } finally {
            releaseSibling.countDown();
            callerExecutor.shutdownNow();
            workerExecutor.shutdownNow();
        }
    }

    private static boolean causeChainContains(Throwable error, String text) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(text)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    interface Root {
        @ParallelAgent(subAgents = {})
        String run();
    }
}
