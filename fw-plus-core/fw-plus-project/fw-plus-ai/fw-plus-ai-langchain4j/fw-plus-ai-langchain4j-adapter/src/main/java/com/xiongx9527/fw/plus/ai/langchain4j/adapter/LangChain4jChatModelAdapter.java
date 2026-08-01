package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.ChatRequestOptions;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.Response;

import java.util.List;

public final class LangChain4jChatModelAdapter {

    private LangChain4jChatModelAdapter() {
    }

    public static ChatLanguageModel asChatLanguageModel(Object model) {
        if (model instanceof ChatLanguageModel) {
            return (ChatLanguageModel) model;
        }
        if (model instanceof ChatModel) {
            return new ChatModelBridge((ChatModel) model);
        }
        return null;
    }

    public static StreamingChatLanguageModel asStreamingChatLanguageModel(Object model) {
        if (model instanceof StreamingChatLanguageModel) {
            return (StreamingChatLanguageModel) model;
        }
        if (model instanceof StreamingChatModel) {
            return new StreamingChatModelBridge((StreamingChatModel) model);
        }
        return null;
    }

    private static final class ChatModelBridge implements ChatLanguageModel {

        private final ChatModel delegate;

        private ChatModelBridge(ChatModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            ChatResponse response = delegate.chat(ChatRequest.builder().messages(messages).build());
            return Response.from(response.aiMessage(), response.tokenUsage(), response.finishReason());
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            return delegate.chat(request);
        }

        @Override
        public ChatResponse chat(ChatRequest request, ChatRequestOptions options) {
            return delegate.chat(request, options);
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            return delegate.doChat(request);
        }
    }

    private static final class StreamingChatModelBridge implements StreamingChatLanguageModel {

        private final StreamingChatModel delegate;

        private StreamingChatModelBridge(StreamingChatModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
            delegate.chat(ChatRequest.builder().messages(messages).build(), new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    handler.onNext(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    handler.onComplete(Response.from(response.aiMessage(), response.tokenUsage(), response.finishReason()));
                }

                @Override
                public void onError(Throwable error) {
                    handler.onError(error);
                }
            });
        }

        @Override
        public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
            delegate.chat(request, handler);
        }

        @Override
        public void chat(ChatRequest request, ChatRequestOptions options, StreamingChatResponseHandler handler) {
            delegate.chat(request, options, handler);
        }

        @Override
        public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
            delegate.doChat(request, handler);
        }
    }
}
