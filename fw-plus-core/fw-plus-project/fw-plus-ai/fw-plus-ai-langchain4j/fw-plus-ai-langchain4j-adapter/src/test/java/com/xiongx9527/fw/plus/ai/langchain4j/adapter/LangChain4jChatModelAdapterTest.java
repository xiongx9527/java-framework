package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class LangChain4jChatModelAdapterTest {

    @Test
    public void shouldForwardNativeChatTokenUsageAndFinishReason() {
        ChatLanguageModel model = LangChain4jChatModelAdapter.asChatLanguageModel(new NativeChatModel());

        Response<AiMessage> response = model.generate(Collections.<ChatMessage>singletonList(UserMessage.from("question")));

        assertEquals("native answer", response.content().text());
        assertEquals(new TokenUsage(Integer.valueOf(3), Integer.valueOf(5)), response.tokenUsage());
        assertEquals(FinishReason.STOP, response.finishReason());
    }

    @Test
    public void shouldForwardNativeStreamingPartialAndCompletionMetadata() {
        StreamingChatLanguageModel model = LangChain4jChatModelAdapter
                .asStreamingChatLanguageModel(new CompletingStreamingChatModel());
        final List<String> tokens = new ArrayList<String>();
        final AtomicReference<Response<AiMessage>> completion = new AtomicReference<Response<AiMessage>>();

        model.generate(Collections.<ChatMessage>singletonList(UserMessage.from("question")), handler(tokens, completion));

        assertEquals(Collections.singletonList("partial"), tokens);
        assertEquals("complete", completion.get().content().text());
        assertEquals(new TokenUsage(Integer.valueOf(2), Integer.valueOf(4)), completion.get().tokenUsage());
        assertEquals(FinishReason.LENGTH, completion.get().finishReason());
    }

    @Test
    public void shouldForwardNativeStreamingError() {
        IllegalStateException expected = new IllegalStateException("native stream failed");
        StreamingChatLanguageModel model = LangChain4jChatModelAdapter
                .asStreamingChatLanguageModel(new FailingStreamingChatModel(expected));
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        model.generate(Collections.<ChatMessage>singletonList(UserMessage.from("question")),
                handler(new ArrayList<String>(), new AtomicReference<Response<AiMessage>>(), error));

        assertSame(expected, error.get());
    }

    private StreamingResponseHandler<AiMessage> handler(final List<String> tokens,
            final AtomicReference<Response<AiMessage>> completion) {
        return handler(tokens, completion, new AtomicReference<Throwable>());
    }

    private StreamingResponseHandler<AiMessage> handler(final List<String> tokens,
            final AtomicReference<Response<AiMessage>> completion, final AtomicReference<Throwable> error) {
        return new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                tokens.add(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                completion.set(response);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
            }
        };
    }

    private static final class NativeChatModel implements ChatModel {
        @Override
        public ChatResponse chat(ChatRequest request) {
            return ChatResponse.builder().aiMessage(AiMessage.from("native answer"))
                    .tokenUsage(new TokenUsage(Integer.valueOf(3), Integer.valueOf(5)))
                    .finishReason(FinishReason.STOP).build();
        }
    }

    private static final class CompletingStreamingChatModel implements StreamingChatModel {
        @Override
        public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
            handler.onPartialResponse("partial");
            handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("complete"))
                    .tokenUsage(new TokenUsage(Integer.valueOf(2), Integer.valueOf(4)))
                    .finishReason(FinishReason.LENGTH).build());
        }
    }

    private static final class FailingStreamingChatModel implements StreamingChatModel {
        private final IllegalStateException error;

        private FailingStreamingChatModel(IllegalStateException error) {
            this.error = error;
        }

        @Override
        public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
            handler.onError(error);
        }
    }
}
