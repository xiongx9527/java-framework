package dev.langchain4j.model.chat.listener;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.request.ChatRequest;

import java.util.Map;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/**
 * The chat model request context.
 * It contains the {@link ChatRequest}, {@link ModelProvider} and attributes.
 * The attributes can be used to pass data between methods of a {@link ChatModelListener}
 * or between multiple {@link ChatModelListener}s.
 */
public class ChatModelRequestContext {

    private final ChatRequest chatRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;
    private final ChatModelRequest legacyRequest;

    public ChatModelRequestContext(ChatRequest chatRequest,
                                   ModelProvider modelProvider,
                                   Map<Object, Object> attributes) {
        this.chatRequest = ensureNotNull(chatRequest, "chatRequest");
        this.modelProvider = modelProvider;
        this.legacyRequest = toLegacy(chatRequest);
        this.attributes = ensureNotNull(attributes, "attributes");
    }

    public ChatModelRequestContext(ChatModelRequest request, Map<Object, Object> attributes) {
        this.chatRequest = request == null ? null : ChatRequest.builder().messages(request.messages()).toolSpecifications(request.toolSpecifications()).build();
        this.modelProvider = null;
        this.legacyRequest = request;
        this.attributes = ensureNotNull(attributes, "attributes");
    }

    private static ChatModelRequest toLegacy(ChatRequest request) {
        return ChatModelRequest.builder()
                .messages(request.messages())
                .toolSpecifications(request.toolSpecifications())
                .build();
    }

    /** Legacy 0.35 accessor retained for binary/source compatibility. */
    public ChatModelRequest request() {
        return legacyRequest;
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
