package com.xiongx9527.fw.plus.ai.askdata.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

public class ResilientLlmChatPortTest {

    /** 前 failures 次抛 IOException，之后返回 ok。 */
    static class FlakyPort implements LlmChatPort {
        final AtomicInteger calls = new AtomicInteger();
        final int failures;
        final RuntimeException hardError;

        FlakyPort(int failures) {
            this(failures, null);
        }

        FlakyPort(int failures, RuntimeException hardError) {
            this.failures = failures;
            this.hardError = hardError;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            int n = calls.incrementAndGet();
            if (hardError != null && n > failures) {
                throw hardError;
            }
            if (n <= failures) {
                throw new RuntimeException("connection timeout simulated transient #" + n);
            }
            return "ok";
        }
    }

    static class ConstantPort implements LlmChatPort {
        final AtomicInteger calls = new AtomicInteger();
        final Throwable error;

        ConstantPort(Throwable error) {
            this.error = error;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            calls.incrementAndGet();
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new RuntimeException(error);
        }
    }

    @Test
    void passesThroughOnSuccess() {
        StaticLlmChatPort delegate = new StaticLlmChatPort("answer");
        ResilientLlmChatPort port = new ResilientLlmChatPort(delegate);
        assertEquals("answer", port.complete("s", "u"));
        assertEquals("CLOSED", port.getCircuitState());
    }

    @Test
    void retriesTransientThenSucceeds() {
        FlakyPort delegate = new FlakyPort(2);
        ResilientLlmChatPort.ResilienceConfig cfg = ResilientLlmChatPort.ResilienceConfig.builder()
                .maxRetries(2).baseDelayMs(0).maxDelayMs(0).failureThreshold(5).build();
        ResilientLlmChatPort port = new ResilientLlmChatPort(delegate, cfg);
        assertEquals("ok", port.complete("s", "u"));
        assertEquals(3, delegate.calls.get());
        assertEquals("CLOSED", port.getCircuitState());
    }

    @Test
    void retriesExhaustedThrowsAndOpensCircuit() {
        ConstantPort delegate = new ConstantPort(new RuntimeException("connection timeout boom"));
        ResilientLlmChatPort.ResilienceConfig cfg = ResilientLlmChatPort.ResilienceConfig.builder()
                .maxRetries(2).baseDelayMs(0).maxDelayMs(0).failureThreshold(3).build();
        ResilientLlmChatPort port = new ResilientLlmChatPort(delegate, cfg);
        LlmResilienceException ex = assertThrows(LlmResilienceException.class,
                () -> port.complete("s", "u"));
        assertTrue(ex.getMessage().contains("重试后仍失败"));
        assertEquals(3, delegate.calls.get());
        assertEquals("OPEN", port.getCircuitState());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u"));
        assertEquals(3, delegate.calls.get());
    }

    @Test
    void nonRetryableNotRetriedAndDoesNotTripBreakerByDefault() {
        RuntimeException auth = new RuntimeException("401 unauthorized");
        ConstantPort delegate = new ConstantPort(auth);
        ResilientLlmChatPort port = new ResilientLlmChatPort(delegate,
                ResilientLlmChatPort.ResilienceConfig.builder()
                        .maxRetries(2).baseDelayMs(0).maxDelayMs(0).failureThreshold(1).build());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u"));
        assertEquals(1, delegate.calls.get());
        assertEquals("CLOSED", port.getCircuitState());
    }

    @Test
    void halfOpenProbeSuccessClosesCircuit() {
        FlakyPort delegate = new FlakyPort(2);
        ResilientLlmChatPort.ResilienceConfig cfg = ResilientLlmChatPort.ResilienceConfig.builder()
                .maxRetries(0).baseDelayMs(0).maxDelayMs(0).failureThreshold(2).cooldownMs(0).build();
        ResilientLlmChatPort port = new ResilientLlmChatPort(delegate, cfg);
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u"));
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u"));
        assertEquals("OPEN", port.getCircuitState());
        assertEquals("ok", port.complete("s", "u"));
        assertEquals("CLOSED", port.getCircuitState());
        assertEquals(3, delegate.calls.get());
    }

    @Test
    void halfOpenProbeFailureReopensCircuit() {
        FlakyPort delegate = new FlakyPort(3);
        ResilientLlmChatPort.ResilienceConfig cfg = ResilientLlmChatPort.ResilienceConfig.builder()
                .maxRetries(0).baseDelayMs(0).maxDelayMs(0).failureThreshold(2).cooldownMs(0).build();
        ResilientLlmChatPort port = new ResilientLlmChatPort(delegate, cfg);
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u"));
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u"));
        assertEquals("OPEN", port.getCircuitState());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u"));
        assertEquals("OPEN", port.getCircuitState());
    }

    @Test
    void customRetryablePredicateControlsRetries() {
        AtomicInteger calls = new AtomicInteger();
        LlmChatPort delegate = (s, u) -> {
            calls.incrementAndGet();
            throw new RuntimeException("weird but retryable");
        };
        Predicate<Throwable> alwaysRetry = t -> true;
        ResilientLlmChatPort port = new ResilientLlmChatPort(delegate,
                ResilientLlmChatPort.ResilienceConfig.builder()
                        .maxRetries(1).baseDelayMs(0).maxDelayMs(0).failureThreshold(99)
                        .retryable(alwaysRetry).build());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u"));
        assertEquals(2, calls.get());
    }
}
