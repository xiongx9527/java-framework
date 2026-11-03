package com.xiongx9527.fw.plus.ai.askdata.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

public class ResilientLlmPortTest {

    /** 前 failures 次抛 IOException，之后返回 ok。 */
    static class FlakyPort implements LlmPort {
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
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            handler.onToken(complete(request).getContent(), null);
            handler.onComplete();
        }

        @Override
        public LlmResponse complete(LlmRequest request) {
            int n = calls.incrementAndGet();
            if (hardError != null && n > failures) {
                throw hardError;
            }
            if (n <= failures) {
                throw new RuntimeException("connection timeout simulated transient #" + n);
            }
            return LlmResponse.builder().content("ok").build();
        }
    }

    static class ConstantPort implements LlmPort {
        final AtomicInteger calls = new AtomicInteger();
        final Throwable error;

        ConstantPort(Throwable error) {
            this.error = error;
        }

        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            complete(request);
        }

        @Override
        public LlmResponse complete(LlmRequest request) {
            calls.incrementAndGet();
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new RuntimeException(error);
        }
    }

    @Test
    void passesThroughOnSuccess() {
        StaticLlmPort delegate = new StaticLlmPort("answer");
        ResilientLlmPort port = new ResilientLlmPort(delegate);
        assertEquals("answer", port.complete("s", "u").getContent());
        assertEquals("CLOSED", port.getCircuitState());
    }

    @Test
    void retriesTransientThenSucceeds() {
        FlakyPort delegate = new FlakyPort(2);
        ResilientLlmPort.ResilienceConfig cfg = ResilientLlmPort.ResilienceConfig.builder()
                .maxRetries(2).baseDelayMs(0).maxDelayMs(0).failureThreshold(5).build();
        ResilientLlmPort port = new ResilientLlmPort(delegate, cfg);
        assertEquals("ok", port.complete("s", "u").getContent());
        assertEquals(3, delegate.calls.get());
        assertEquals("CLOSED", port.getCircuitState());
    }

    @Test
    void retriesExhaustedThrowsAndOpensCircuit() {
        ConstantPort delegate = new ConstantPort(new RuntimeException("connection timeout boom"));
        ResilientLlmPort.ResilienceConfig cfg = ResilientLlmPort.ResilienceConfig.builder()
                .maxRetries(2).baseDelayMs(0).maxDelayMs(0).failureThreshold(3).build();
        ResilientLlmPort port = new ResilientLlmPort(delegate, cfg);
        LlmResilienceException ex = assertThrows(LlmResilienceException.class,
                () -> port.complete("s", "u").getContent());
        assertTrue(ex.getMessage().contains("重试后仍失败"));
        assertEquals(3, delegate.calls.get());
        assertEquals("OPEN", port.getCircuitState());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u").getContent());
        assertEquals(3, delegate.calls.get());
    }

    @Test
    void nonRetryableNotRetriedAndDoesNotTripBreakerByDefault() {
        RuntimeException auth = new RuntimeException("401 unauthorized");
        ConstantPort delegate = new ConstantPort(auth);
        ResilientLlmPort port = new ResilientLlmPort(delegate,
                ResilientLlmPort.ResilienceConfig.builder()
                        .maxRetries(2).baseDelayMs(0).maxDelayMs(0).failureThreshold(1).build());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u").getContent());
        assertEquals(1, delegate.calls.get());
        assertEquals("CLOSED", port.getCircuitState());
    }

    @Test
    void halfOpenProbeSuccessClosesCircuit() {
        FlakyPort delegate = new FlakyPort(2);
        ResilientLlmPort.ResilienceConfig cfg = ResilientLlmPort.ResilienceConfig.builder()
                .maxRetries(0).baseDelayMs(0).maxDelayMs(0).failureThreshold(2).cooldownMs(0).build();
        ResilientLlmPort port = new ResilientLlmPort(delegate, cfg);
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u").getContent());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u").getContent());
        assertEquals("OPEN", port.getCircuitState());
        assertEquals("ok", port.complete("s", "u").getContent());
        assertEquals("CLOSED", port.getCircuitState());
        assertEquals(3, delegate.calls.get());
    }

    @Test
    void halfOpenProbeFailureReopensCircuit() {
        FlakyPort delegate = new FlakyPort(3);
        ResilientLlmPort.ResilienceConfig cfg = ResilientLlmPort.ResilienceConfig.builder()
                .maxRetries(0).baseDelayMs(0).maxDelayMs(0).failureThreshold(2).cooldownMs(0).build();
        ResilientLlmPort port = new ResilientLlmPort(delegate, cfg);
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u").getContent());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u").getContent());
        assertEquals("OPEN", port.getCircuitState());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u").getContent());
        assertEquals("OPEN", port.getCircuitState());
    }

    @Test
    void customRetryablePredicateControlsRetries() {
        AtomicInteger calls = new AtomicInteger();
        LlmPort delegate = new LlmPort() {
            @Override
            public void stream(LlmRequest request, LlmStreamHandler handler) {
                complete(request);
            }

            @Override
            public LlmResponse complete(LlmRequest request) {
                calls.incrementAndGet();
                throw new RuntimeException("weird but retryable");
            }
        };
        Predicate<Throwable> alwaysRetry = t -> true;
        ResilientLlmPort port = new ResilientLlmPort(delegate,
                ResilientLlmPort.ResilienceConfig.builder()
                        .maxRetries(1).baseDelayMs(0).maxDelayMs(0).failureThreshold(99)
                        .retryable(alwaysRetry).build());
        assertThrows(LlmResilienceException.class, () -> port.complete("s", "u").getContent());
        assertEquals(2, calls.get());
    }
}
