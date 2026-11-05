package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmException;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LangChain4jLlmPortTest {

    private static class StubChatModel implements ChatLanguageModel {

        AiMessage reply = AiMessage.from("hello");

        TokenUsage usage = new TokenUsage(3, 5, 8);

        List<dev.langchain4j.data.message.ChatMessage> lastMessages;

        List<dev.langchain4j.agent.tool.ToolSpecification> lastSpecs;

        RuntimeException failure;

        @Override
        public Response<AiMessage> generate(List<dev.langchain4j.data.message.ChatMessage> messages) {
            this.lastMessages = messages;
            if (failure != null) {
                throw failure;
            }
            return Response.from(reply, usage);
        }

        @Override
        public Response<AiMessage> generate(List<dev.langchain4j.data.message.ChatMessage> messages,
                                            List<dev.langchain4j.agent.tool.ToolSpecification> specs) {
            this.lastSpecs = specs;
            return generate(messages);
        }
    }

    private static class StubStreamingModel implements StreamingChatLanguageModel {

        @Override
        public void generate(List<dev.langchain4j.data.message.ChatMessage> messages,
                             StreamingResponseHandler<AiMessage> handler) {
            handler.onNext("he");
            handler.onNext("llo");
            handler.onComplete(Response.from(AiMessage.from("hello"), new TokenUsage(1, 2, 3)));
        }
    }

    private static LlmRequest singleUser() {
        return LlmRequest.builder().addMessage(ChatMessage.user("hi")).build();
    }

    @Test
    void nonStreamingCompleteAggregatesContentAndUsage() {
        StubChatModel model = new StubChatModel();
        LangChain4jLlmPort port = new LangChain4jLlmPort(model);

        LlmResponse response = port.complete(singleUser());

        assertEquals("hello", response.getContent());
        assertEquals(3L, response.getUsage().getInputTokens());
        assertEquals(5L, response.getUsage().getOutputTokens());
        assertEquals(8L, response.getUsage().getTotalTokens());
        assertEquals(1, model.lastMessages.size());
        assertFalse(port.capabilities().isStreaming());
        assertTrue(port.capabilities().isToolCalling());
    }

    @Test
    void streamingEmitsEachTokenOnce() {
        StubStreamingModel streaming = new StubStreamingModel();
        LangChain4jLlmPort port = new LangChain4jLlmPort(null, streaming);
        final List<String> tokens = new ArrayList<String>();

        port.stream(singleUser()).onPartialResponse(tokens::add).start();

        assertEquals(Arrays.asList("he", "llo"), tokens);
        assertTrue(port.capabilities().isStreaming());
        assertFalse(port.capabilities().isToolCalling());
    }

    @Test
    void streamingCompleteDoesNotDuplicateContent() {
        StubStreamingModel streaming = new StubStreamingModel();
        LangChain4jLlmPort port = new LangChain4jLlmPort(null, streaming);

        assertEquals("hello", port.complete(singleUser()).getContent());
    }

    @Test
    void toolCallsAreForwardedToStreamHandler() {
        StubChatModel model = new StubChatModel();
        model.reply = AiMessage.from(Collections.singletonList(
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("c1").name("search").arguments("{\"q\":\"x\"}").build()));
        LangChain4jLlmPort port = new LangChain4jLlmPort(model);

        LlmResponse response = port.complete(LlmRequest.builder()
                .addMessage(ChatMessage.user("find x"))
                .addTool(ToolSpecification.builder().name("search").build())
                .build());

        assertTrue(response.hasToolCalls());
        assertEquals("c1", response.getToolCalls().get(0).getId());
        assertEquals("search", response.getToolCalls().get(0).getName());
        assertEquals(1, model.lastSpecs.size());
        assertEquals("search", model.lastSpecs.get(0).name());
    }

    @Test
    void upstreamFailureBecomesLlmException() {
        StubChatModel model = new StubChatModel();
        model.failure = new IllegalStateException("upstream down");
        LangChain4jLlmPort port = new LangChain4jLlmPort(model);

        LlmException ex = assertThrows(LlmException.class, () -> port.complete(singleUser()));
        assertTrue(ex.getMessage().contains("upstream down"));
    }

    @Test
    void validateModelUsesARealCompletion() {
        StubChatModel model = new StubChatModel();
        LangChain4jLlmPort port = new LangChain4jLlmPort(model);

        ModelValidation validation = port.validateModel("gpt-4o-mini");
        assertTrue(validation.isValid());
        assertFalse(port.validateModel(null).isValid());
    }
}
