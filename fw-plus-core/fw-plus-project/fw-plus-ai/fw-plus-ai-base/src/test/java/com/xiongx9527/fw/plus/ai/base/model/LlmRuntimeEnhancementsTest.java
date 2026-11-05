package com.xiongx9527.fw.plus.ai.base.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.guard.GuardrailException;
import com.xiongx9527.fw.plus.ai.base.guard.GuardrailResult;
import com.xiongx9527.fw.plus.ai.base.guard.GuardedLlmPort;
import com.xiongx9527.fw.plus.ai.base.guard.InputGuardrail;
import com.xiongx9527.fw.plus.ai.base.guard.OutputGuardrail;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmRuntimeEnhancementsTest {

    private static class FixedPort extends AbstractLlmPort {
        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            handler.onToken("ok", null);
            handler.onComplete();
        }
    }

    @Test
    void completeAsyncReturnsResponse() throws Exception {
        assertEquals("ok", new FixedPort()
                .completeAsync(LlmRequest.systemUser("s", "hi")).get().getContent());
    }

    @Test
    void withListenersDeliversPortLevelEvents() {
        final List<String> events = new ArrayList<String>();
        LlmListener listener = new LlmListener() {
            @Override
            public void onRequest(LlmRequest request) {
                events.add("request");
            }

            @Override
            public void onComplete() {
                events.add("complete");
            }
        };
        LlmPort port = new FixedPort().withListeners(Collections.singletonList(listener));

        port.complete(LlmRequest.systemUser("s", "hi"));

        assertTrue(events.contains("request"));
        assertTrue(events.contains("complete"));
    }

    @Test
    void cancelSilencesAllSubsequentCallbacks() {
        final LlmTokenStream[] holder = new LlmTokenStream[1];
        final List<String> events = new ArrayList<String>();
        LlmTokenStream stream = LlmTokenStream.create(handler -> {
            holder[0].cancel();
            handler.onToken("a", null);
            handler.onComplete();
        });
        holder[0] = stream;

        stream.onPartialResponse(events::add).start();

        assertTrue(stream.isCancelled());
        assertTrue(events.isEmpty());
    }

    @Test
    void guardrailRejectsInput() {
        InputGuardrail deny = input -> GuardrailResult.reject("blocked");
        LlmPort port = new GuardedLlmPort(new FixedPort(),
                Collections.singletonList(deny), null, 0);

        assertThrows(GuardrailException.class,
                () -> port.complete(LlmRequest.systemUser("s", "hi")));
    }

    @Test
    void guardrailRewritesOutput() {
        OutputGuardrail rewrite = output -> GuardrailResult.rewrite("rewritten");
        LlmPort port = new GuardedLlmPort(new FixedPort(), null,
                Collections.singletonList(rewrite), 0);

        assertEquals("rewritten", port.complete(LlmRequest.systemUser("s", "hi")).getContent());
    }
}
