package dev.langchain4j.model.chat;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.Response;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface StreamingChatLanguageModel extends StreamingChatModel {

    default void generate(String userMessage, StreamingResponseHandler<AiMessage> handler) {
        generate(UserMessage.from(userMessage), handler);
    }

    default void generate(UserMessage userMessage, StreamingResponseHandler<AiMessage> handler) {
        generate(Collections.singletonList(userMessage), handler);
    }

    void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler);

    default void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications,
                          StreamingResponseHandler<AiMessage> handler) {
        throw new IllegalArgumentException("Tools are currently not supported by this model");
    }

    default void generate(List<ChatMessage> messages, ToolSpecification toolSpecification,
                          StreamingResponseHandler<AiMessage> handler) {
        throw new IllegalArgumentException("Tools are currently not supported by this model");
    }

    @Override
    default void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        generate(chatRequest.messages(), adapter(handler));
    }

    @Override
    default void chat(ChatRequest chatRequest, ChatRequestOptions chatRequestOptions, StreamingChatResponseHandler handler) {
        chat(chatRequest, handler);
    }

    @Override
    default void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        chat(chatRequest, handler);
    }

    @Override
    default ChatRequestParameters defaultRequestParameters() {
        return null;
    }

    @Override
    default List<ChatModelListener> listeners() {
        return Collections.emptyList();
    }

    @Override
    default ModelProvider provider() {
        return ModelProvider.OTHER;
    }

    @Override
    default void chat(String userMessage, StreamingChatResponseHandler handler) {
        chat(ChatRequest.builder().messages(UserMessage.from(userMessage)).build(), handler);
    }

    @Override
    default void chat(List<ChatMessage> messages, StreamingChatResponseHandler handler) {
        chat(ChatRequest.builder().messages(messages).build(), handler);
    }

    @Override
    default Set<Capability> supportedCapabilities() {
        return Collections.emptySet();
    }

    static StreamingResponseHandler<AiMessage> adapter(StreamingChatResponseHandler handler) {
        return new StreamingResponseHandler<AiMessage>() {

            @Override
            public void onNext(String token) {
                handler.onPartialResponse(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                handler.onCompleteResponse(ChatResponse.builder()
                        .aiMessage(response.content())
                        .tokenUsage(response.tokenUsage())
                        .finishReason(response.finishReason())
                        .build());
            }

            @Override
            public void onError(Throwable error) {
                handler.onError(error);
            }
        };
    }
}
