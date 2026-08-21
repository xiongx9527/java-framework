package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class LangChain4jChatModelAdapterTest {

    @Test
    public void shouldReturnNativeChatModelWithoutLegacyBridge() {
        NativeChatModel nativeModel = new NativeChatModel();

        ChatModel model = LangChain4jChatModelAdapter.asChatModel(nativeModel);
        ChatResponse response = model.chat(Collections.singletonList(UserMessage.from("question")));

        assertSame(nativeModel, model);
        assertEquals("native answer", response.aiMessage().text());
        assertEquals(new TokenUsage(Integer.valueOf(3), Integer.valueOf(5)), response.tokenUsage());
        assertEquals(FinishReason.STOP, response.finishReason());
    }

    @Test
    public void shouldReturnNativeStreamingChatModelWithoutLegacyBridge() {
        NativeStreamingChatModel nativeModel = new NativeStreamingChatModel();
        StreamingChatModel model = LangChain4jChatModelAdapter.asStreamingChatModel(nativeModel);
        AtomicReference<ChatResponse> completion = new AtomicReference<ChatResponse>();

        model.chat(Collections.singletonList(UserMessage.from("question")), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                completion.set(response);
            }

            @Override
            public void onError(Throwable error) {
            }
        });

        assertSame(nativeModel, model);
        assertEquals("complete", completion.get().aiMessage().text());
    }

    @Test
    public void shouldReturnNullForUnsupportedNativeObjects() {
        assertNull(LangChain4jChatModelAdapter.asChatModel(new Object()));
        assertNull(LangChain4jChatModelAdapter.asStreamingChatModel(new Object()));
    }

    private static final class NativeChatModel implements ChatModel {
        @Override
        public ChatResponse chat(ChatRequest request) {
            return ChatResponse.builder().aiMessage(AiMessage.from("native answer"))
                    .tokenUsage(new TokenUsage(Integer.valueOf(3), Integer.valueOf(5)))
                    .finishReason(FinishReason.STOP).build();
        }
    }

    private static final class NativeStreamingChatModel implements StreamingChatModel {
        @Override
        public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
            handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("complete")).build());
        }
    }
}
