package dev.langchain4j.agentic.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.declarative.TypedKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AgenticScopeBehaviorTest {

    @Test
    void stateWritesRespectNullBlankAndTypedKeySemantics() {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        scope.writeState("answer", "value");
        assertTrue(scope.hasState("answer"));
        scope.writeState("answer", null);
        assertFalse(scope.hasState("answer"));
        scope.writeState("asciiBlank", "   ");
        scope.writeState("unicodeBlank", "  ");
        assertFalse(scope.hasState("asciiBlank"));
        assertFalse(scope.hasState("unicodeBlank"));

        scope.writeState(TypedAnswer.class, "typed");
        assertTrue(scope.hasState(TypedAnswer.class));
        assertEquals("typed", scope.readState(TypedAnswer.class));
        assertEquals("fallback", scope.readState(TypedMissing.class));
    }

    @Test
    void writeStateIfAbsentIsAtomicUnderContention() throws Exception {
        final DefaultAgenticScope scope = new DefaultAgenticScope("memory", DefaultAgenticScope.Kind.PERSISTENT);
        final int writers = 32;
        final ExecutorService executor = Executors.newFixedThreadPool(writers);
        final CountDownLatch ready = new CountDownLatch(writers);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger completed = new AtomicInteger();
        final List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>());
        try {
            for (int i = 0; i < writers; i++) {
                final int value = i;
                executor.submit(() -> {
                    try {
                        ready.countDown();
                        assertTrue(start.await(10, TimeUnit.SECONDS));
                        scope.writeStateIfAbsent("winner", value);
                        completed.incrementAndGet();
                    } catch (Throwable failure) {
                        failures.add(failure);
                    }
                });
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(writers, completed.get());
        Object winner = scope.readState("winner");
        assertTrue(winner instanceof Integer);
        assertTrue(((Integer) winner).intValue() >= 0 && ((Integer) winner).intValue() < writers);

        scope.writeState("winner", "");
        scope.writeStateIfAbsent("winner", "replacement");
        assertEquals("replacement", scope.readState("winner"));
    }

    @Test
    void executionContextIsTransientAndTypeCheckedAtCallSite() {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        Object executionContext = new Object();
        scope.writeExecutionContext("runtime", executionContext);
        assertSame(executionContext, scope.executionContext("runtime"));
        assertSame(executionContext, scope.executionContextAs("runtime", Object.class));
        assertThrows(ClassCastException.class, () -> scope.executionContextAs("runtime", String.class));
    }

    @Test
    void pendingResponseConvenienceRequiresExactlyOneAndCompletesOnce() {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        dev.langchain4j.agentic.internal.PendingResponse<String> first =
                new dev.langchain4j.agentic.internal.PendingResponse<String>("first");
        dev.langchain4j.agentic.internal.PendingResponse<String> second =
                new dev.langchain4j.agentic.internal.PendingResponse<String>("second");
        scope.writeState("first", first);
        scope.writeState("second", second);

        assertEquals(new java.util.HashSet<String>(Arrays.asList("first", "second")), scope.pendingResponseIds());
        assertThrows(IllegalStateException.class, () -> scope.completePendingResponse("done"));
        assertTrue(scope.completePendingResponse("first", "done"));
        assertFalse(scope.completePendingResponse("first", "again"));
        assertEquals("done", scope.readState("first"));
        assertEquals(Collections.singleton("second"), scope.pendingResponseIds());
        assertTrue(scope.completePendingResponse("finished"));
        assertEquals("finished", scope.readState("second"));
        assertTrue(scope.pendingResponseIds().isEmpty());
    }

    public static final class TypedAnswer implements TypedKey<String> {
        public TypedAnswer() {
        }
    }

    public static final class TypedMissing implements TypedKey<String> {
        public TypedMissing() {
        }

        @Override
        public String defaultValue() {
            return "fallback";
        }
    }
}
