package dev.langchain4j.agentic.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.agentic.internal.AsyncResponse;
import dev.langchain4j.agentic.internal.DelayedResponse;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class AgenticAsyncStreamingTest {

    @Test
    void asyncResponseSupportsSuccessExceptionAndNullWithoutPermanentPending() throws Exception {
        AsyncResponse<String> success = new AsyncResponse<String>(() -> "done");
        assertEquals("done", success.blockingGet());
        assertTrue(success.isDone());

        AsyncResponse<String> failure = new AsyncResponse<String>(() -> {
            throw new IllegalStateException("boom");
        });
        IllegalStateException error = assertThrows(IllegalStateException.class, failure::blockingGet);
        assertEquals("boom", error.getMessage());
        assertTrue(failure.isDone());

        AsyncResponse<String> nullResponse = new AsyncResponse<String>(() -> null);
        assertNull(nullResponse.blockingGet());
        assertTrue(nullResponse.isDone());
    }

    @Test
    void delayedResponseResultDistinguishesPendingAndCompletedNull() {
        CompletableFuture<String> pendingFuture = new CompletableFuture<String>();
        DelayedResponse<String> pending = new FutureResponse<String>(pendingFuture);
        assertFalse(pending.isDone());
        assertEquals("<pending>", pending.result());

        pendingFuture.complete(null);
        assertTrue(pending.isDone());
        assertNull(pending.result());
    }

    @Test
    void delayedResponseJoinPreservesRuntimeFailureAndError() {
        CompletableFuture<String> runtimeFailure = new CompletableFuture<String>();
        runtimeFailure.completeExceptionally(new IllegalArgumentException("bad"));
        assertThrows(IllegalArgumentException.class,
                () -> DelayedResponse.join(runtimeFailure));

        CompletableFuture<String> error = new CompletableFuture<String>();
        error.completeExceptionally(new AssertionError("bad-error"));
        assertThrows(AssertionError.class, () -> DelayedResponse.join(error));
    }

    private static final class FutureResponse<T> implements DelayedResponse<T> {
        private final CompletableFuture<T> future;

        private FutureResponse(CompletableFuture<T> future) {
            this.future = future;
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public T blockingGet() {
            return DelayedResponse.join(future);
        }
    }
}
