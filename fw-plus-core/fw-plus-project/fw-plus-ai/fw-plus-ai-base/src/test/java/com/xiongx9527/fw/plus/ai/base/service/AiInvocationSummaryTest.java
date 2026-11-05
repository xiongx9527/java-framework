package com.xiongx9527.fw.plus.ai.base.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.AbstractLlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import org.junit.jupiter.api.Test;

class AiInvocationSummaryTest {

    interface Assistant {
        @UserMessage("{{it}}")
        String ask(String question);
    }

    private static class FixedLlm extends AbstractLlmPort {
        @Override
        protected void doStream(LlmRequest request, LlmStreamHandler handler) {
            handler.onToken("answer", null);
            handler.onComplete();
        }
    }

    @Test
    void summaryCarriesOrderedEvents() {
        final AiCallObservation[] captured = new AiCallObservation[1];
        Assistant assistant = AiServices.builder(Assistant.class)
                .llm(new FixedLlm())
                .summaryListener(summary -> captured[0] = summary)
                .build();

        assistant.ask("question");

        assertNotNull(captured[0]);
        assertTrue(captured[0].getEvents().contains("request"));
        assertTrue(captured[0].getEvents().contains("llm.request"));
        assertTrue(captured[0].getEvents().contains("end"));
    }
}
