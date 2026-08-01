package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LangChain4jLegacyChatContractTest {

    private static final String TOOLS_NOT_SUPPORTED = "Tools are currently not supported by this model";

    @Test
    public void shouldRejectLegacyChatToolOverloads() {
        ChatLanguageModel model = messages -> Response.from(AiMessage.from("answer"));
        List<ChatMessage> messages = Collections.<ChatMessage>singletonList(UserMessage.from("question"));
        ToolSpecification tool = ToolSpecification.builder().name("tool").description("tool").build();

        assertEquals(TOOLS_NOT_SUPPORTED, assertThrows(IllegalArgumentException.class,
                () -> model.generate(messages, Collections.singletonList(tool))).getMessage());
        assertEquals(TOOLS_NOT_SUPPORTED, assertThrows(IllegalArgumentException.class,
                () -> model.generate(messages, tool)).getMessage());
    }

    @Test
    public void shouldRejectChatRequestByDefault() {
        ChatLanguageModel model = messages -> Response.from(AiMessage.from("answer"));

        assertThrows(UnsupportedOperationException.class,
                () -> model.chat(ChatRequest.builder().messages(UserMessage.from("question")).build()));
    }

    @Test
    public void shouldRejectLegacyStreamingToolOverloads() {
        StreamingChatLanguageModel model = new StreamingChatLanguageModel() {
            @Override
            public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
                handler.onComplete(Response.from(AiMessage.from("answer")));
            }
        };
        List<ChatMessage> messages = Collections.<ChatMessage>singletonList(UserMessage.from("question"));
        ToolSpecification tool = ToolSpecification.builder().name("tool").description("tool").build();
        StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
            @Override public void onNext(String token) { }
            @Override public void onComplete(Response<AiMessage> response) { }
            @Override public void onError(Throwable error) { }
        };

        assertEquals(TOOLS_NOT_SUPPORTED, assertThrows(IllegalArgumentException.class,
                () -> model.generate(messages, Collections.singletonList(tool), handler)).getMessage());
        assertEquals(TOOLS_NOT_SUPPORTED, assertThrows(IllegalArgumentException.class,
                () -> model.generate(messages, tool, handler)).getMessage());
    }
}
