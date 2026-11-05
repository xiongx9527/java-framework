package com.xiongx9527.fw.plus.ai.base.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ModelContractExtensionTest {

    @Test
    void requestCarriesExtendedParameters() {
        LlmRequest r = LlmRequest.builder().addMessage(ChatMessage.user("hi"))
                .model("m").topK(50).frequencyPenalty(0.1).presencePenalty(0.2)
                .addStopSequence("STOP")
                .responseFormat(Collections.<String, Object>singletonMap("type", "json_object"))
                .build();

        assertEquals(Integer.valueOf(50), r.getTopK());
        assertEquals(Double.valueOf(0.1), r.getFrequencyPenalty());
        assertEquals(Double.valueOf(0.2), r.getPresencePenalty());
        assertEquals(Collections.singletonList("STOP"), r.getStopSequences());
        assertEquals("json_object", r.getResponseFormat().get("type"));
    }

    @Test
    void toBuilderPreservesAllFields() {
        LlmRequest r = LlmRequest.builder().addMessage(ChatMessage.user("hi"))
                .model("m").temperature(0.3).topK(10).seed(7)
                .addStopSequence("X").extraParam("k", "v").build();

        LlmRequest copy = r.toBuilder().temperature(0.9).build();

        assertEquals("m", copy.getModel());
        assertEquals(Double.valueOf(0.9), copy.getTemperature());
        assertEquals(Integer.valueOf(10), copy.getTopK());
        assertEquals(Integer.valueOf(7), copy.getSeed());
        assertEquals(Collections.singletonList("X"), copy.getStopSequences());
        assertEquals("v", copy.getExtraParams().get("k"));
    }

    @Test
    void responseCarriesMetadataAndGeneratedMedia() {
        LlmResponse r = LlmResponse.builder().content("a")
                .id("resp-1").modelName("m").finishReason("stop")
                .metadata(Collections.<String, Object>singletonMap("vendor", "x"))
                .images(Arrays.asList("http://img")).audios(Arrays.asList("http://audio"))
                .build();

        assertEquals("resp-1", r.getId());
        assertEquals("m", r.getModelName());
        assertEquals("stop", r.getFinishReason());
        assertEquals("x", r.getMetadata().get("vendor"));
        assertEquals("http://img", r.getImages().get(0));
        assertEquals("http://audio", r.getAudios().get(0));
    }

    @Test
    void chatMessageAttributesAreImmutable() {
        ChatMessage m = ChatMessage.user("hi")
                .withAttributes(Collections.<String, Object>singletonMap("trace", "t1"));

        assertEquals("t1", m.getAttributes().get("trace"));
        assertThrows(UnsupportedOperationException.class,
                () -> m.getAttributes().put("x", "y"));
        assertTrue(ChatMessage.user("hi").getAttributes().isEmpty());
    }

    @Test
    void negotiateRejectsUndeclaredCapabilities() {
        LlmPort port = new LlmPort() {
            @Override
            public LlmTokenStream stream(LlmRequest request) {
                return null;
            }
        };
        LlmRequest withTools = LlmRequest.builder().addMessage(ChatMessage.user("hi"))
                .addTool(ToolSpecification.builder().name("t").build())
                .build();

        assertThrows(UnsupportedOperationException.class, () -> port.negotiate(withTools));
        assertEquals("hi", port.negotiate(LlmRequest.systemUser("s", "hi"))
                .getMessages().get(1).getContent());
    }
}
