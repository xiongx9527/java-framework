package dev.langchain4j.model.chat.listener;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.Map;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/**
 * The chat response context.
 * It contains {@link ChatResponse}, corresponding {@link ChatRequest}, {@link ModelProvider} and attributes.
 * The attributes can be used to pass data between methods of a {@link ChatModelListener}
 * or between multiple {@link ChatModelListener}s.
 */
public class ChatModelResponseContext {

    private final ChatResponse chatResponse;
    private final ChatRequest chatRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;
    private final ChatModelResponse legacyResponse;
    private final ChatModelRequest legacyRequest;

    public ChatModelResponseContext(ChatModelResponse response, ChatModelRequest request, Map<Object, Object> attributes) {
        this.legacyResponse = response;
        this.legacyRequest = request;
        this.chatResponse = response == null ? null : ChatResponse.builder().aiMessage(response.aiMessage()).tokenUsage(response.tokenUsage()).finishReason(response.finishReason()).build();
        this.chatRequest = request == null ? null : ChatRequest.builder().messages(request.messages()).toolSpecifications(request.toolSpecifications()).build();
        this.modelProvider = null;
        this.attributes = ensureNotNull(attributes, "attributes");
    }

    public ChatModelResponseContext(ChatResponse chatResponse,
                                    ChatRequest chatRequest,
                                    ModelProvider modelProvider,
                                    Map<Object, Object> attributes) {
        this.legacyResponse = chatResponse == null ? null : ChatModelResponse.builder()
                .aiMessage(chatResponse.aiMessage())
                .tokenUsage(chatResponse.tokenUsage())
                .finishReason(chatResponse.finishReason())
                .build();
        this.legacyRequest = chatRequest == null ? null : ChatModelRequest.builder()
                .messages(chatRequest.messages())
                .toolSpecifications(chatRequest.toolSpecifications())
                .build();
        this.chatResponse = ensureNotNull(chatResponse, "chatResponse");
        this.chatRequest = ensureNotNull(chatRequest, "chatRequest");
        this.modelProvider = modelProvider;
        this.attributes = ensureNotNull(attributes, "attributes");
    }

    public ChatModelResponse response() {
        return legacyResponse;
    }

    public ChatModelRequest request() {
        return legacyRequest;
    }

    public ChatResponse chatResponse() {
        return chatResponse;
    }

    public ChatRequest chatRequest() {
        return chatRequest;
    }

    public ModelProvider modelProvider() {
        return modelProvider;
    }

    /**
     * @return The attributes map. It can be used to pass data between methods of a {@link ChatModelListener}
     * or between multiple {@link ChatModelListener}s.
     */
    public Map<Object, Object> attributes() {
        return attributes;
    }
}
