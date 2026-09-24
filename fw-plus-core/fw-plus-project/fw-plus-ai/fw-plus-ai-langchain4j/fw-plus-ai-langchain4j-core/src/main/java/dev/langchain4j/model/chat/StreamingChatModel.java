package dev.langchain4j.model.chat;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Streaming chat model API bridged to the 0.35 callback model. */
public interface StreamingChatModel extends dev.langchain4j.model.chat.StreamingChatLanguageModel {

    default void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        chat(request, ChatRequestOptions.EMPTY, handler);
    }

    default void chat(ChatRequest request, ChatRequestOptions options, StreamingChatResponseHandler handler) {
        if (request == null) throw new NullPointerException("request");
        if (handler == null) throw new NullPointerException("handler");
        ChatModel.validateCompatibilityOptions(defaultRequestParameters(), options);
        ChatRequestOptions effective = options == null ? ChatRequestOptions.EMPTY : options;
        java.util.Map<Object, Object> attributes = new java.util.concurrent.ConcurrentHashMap<Object, Object>(effective.listenerAttributes());
        java.util.List<dev.langchain4j.model.chat.listener.ChatModelListener> modelListeners = listeners();
        ChatModelListenerUtils.onRequest(request, provider(), attributes, modelListeners);
        doChat(request, new StreamingChatResponseHandler() {
            @Override public void onPartialResponse(String token) { handler.onPartialResponse(token); }
            @Override public void onCompleteResponse(ChatResponse response) {
                ChatModelListenerUtils.onResponse(response, request, provider(), attributes, modelListeners);
                handler.onCompleteResponse(response);
            }
            @Override public void onError(Throwable error) {
                ChatModelListenerUtils.onError(error, request, provider(), attributes, modelListeners);
                handler.onError(error);
            }
        });
    }

    default void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
        throw new UnsupportedOperationException("StreamingChatModel.doChat must be implemented");
    }

    default dev.langchain4j.model.chat.request.ChatRequestParameters defaultRequestParameters() {
        return dev.langchain4j.model.chat.request.DefaultChatRequestParameters.EMPTY;
    }

    default List<dev.langchain4j.model.chat.listener.ChatModelListener> listeners() {
        return Collections.emptyList();
    }

    default ModelProvider provider() {
        return ModelProvider.OTHER;
    }

    default void chat(String userMessage, StreamingChatResponseHandler handler) {
        chat(ChatRequest.builder().messages(UserMessage.from(userMessage)).build(), handler);
    }

    default void chat(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
        chat(ChatRequest.builder().messages(messages).build(), handler);
    }

    default Set<Capability> supportedCapabilities() {
        return Collections.emptySet();
    }

    /** Adapts a 0.35 streaming model while preserving token, complete and error callbacks. */
    static StreamingChatModel fromLegacy(final dev.langchain4j.model.chat.StreamingChatLanguageModel legacy) {
        if (legacy == null) throw new NullPointerException("legacy");
        return new StreamingChatModel() {
            @Override
            public void generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages, final dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler) {
                generate(messages, Collections.<dev.langchain4j.agent.tool.ToolSpecification>emptyList(), handler);
            }

            @Override
            public void generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                                 java.util.List<dev.langchain4j.agent.tool.ToolSpecification> tools,
                                 final dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler) {
                if (tools != null && !tools.isEmpty()) {
                    legacy.generate(messages, tools, handler);
                    return;
                }
                doChat(ChatRequest.builder().messages(messages).build(), new StreamingChatResponseHandler() {
                    public void onPartialResponse(String text) { handler.onNext(text); }
                    public void onCompleteResponse(ChatResponse response) { handler.onComplete(new dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>(response.aiMessage(), response.tokenUsage(), response.finishReason())); }
                    public void onError(Throwable error) { handler.onError(error); }
                });
            }

            @Override
            public void doChat(ChatRequest request, final StreamingChatResponseHandler handler) {
                legacy.generate(request.messages(), new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        handler.onPartialResponse(token);
                    }

                    @Override
                    public void onComplete(dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                        ChatResponse complete = ChatResponse.builder()
                                .aiMessage(response.content())
                                .tokenUsage(response.tokenUsage())
                                .finishReason(response.finishReason())
                                .build();
                        handler.onCompleteResponse(complete);
                    }

                    @Override
                    public void onError(Throwable error) {
                        handler.onError(error);
                    }
                });
            }

            @Override
            public Set<Capability> supportedCapabilities() {
                return Collections.emptySet();
            }
        };
    }

}
