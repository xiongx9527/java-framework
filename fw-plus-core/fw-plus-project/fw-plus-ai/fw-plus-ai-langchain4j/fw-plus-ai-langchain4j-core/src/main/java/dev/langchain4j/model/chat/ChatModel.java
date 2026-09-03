package dev.langchain4j.model.chat;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Chat model API used by Agentic and compatible with the 0.35 model contract. */
public interface ChatModel extends dev.langchain4j.model.chat.ChatLanguageModel {

    default ChatResponse chat(ChatRequest chatRequest) {
        if (chatRequest == null) throw new NullPointerException("chatRequest");
        return chat(chatRequest, ChatRequestOptions.EMPTY);
    }

    default ChatResponse chat(ChatRequest chatRequest, ChatRequestOptions options) {
        if (chatRequest == null) throw new NullPointerException("chatRequest");
        validateCompatibilityOptions(defaultRequestParameters(), options);
        ChatRequestOptions effective = options == null ? ChatRequestOptions.EMPTY : options;
        Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>(effective.listenerAttributes());
        List<dev.langchain4j.model.chat.listener.ChatModelListener> modelListeners = listeners();
        ChatModelListenerUtils.onRequest(chatRequest, provider(), attributes, modelListeners);
        try {
            ChatResponse response = doChat(chatRequest);
            ChatModelListenerUtils.onResponse(response, chatRequest, provider(), attributes, modelListeners);
            return response;
        } catch (RuntimeException error) {
            ChatModelListenerUtils.onError(error, chatRequest, provider(), attributes, modelListeners);
            throw error;
        }
    }

    static void validateCompatibilityOptions(
            dev.langchain4j.model.chat.request.ChatRequestParameters defaults,
            ChatRequestOptions options) {
        if (defaults == null || isEmptyParameters(defaults)) {
            return;
        }
        throw new dev.langchain4j.exception.LangChain4jException(
                "ChatRequest parameters are not expressible by LangChain4j 0.35 ChatRequest");
    }

    static boolean isEmptyParameters(dev.langchain4j.model.chat.request.ChatRequestParameters parameters) {
        return parameters.modelName() == null
                && parameters.temperature() == null
                && parameters.topP() == null
                && parameters.topK() == null
                && parameters.frequencyPenalty() == null
                && parameters.presencePenalty() == null
                && parameters.maxOutputTokens() == null
                && (parameters.stopSequences() == null || parameters.stopSequences().isEmpty())
                && (parameters.toolSpecifications() == null || parameters.toolSpecifications().isEmpty())
                && parameters.toolChoice() == null
                && parameters.responseFormat() == null;
    }

    default ChatResponse doChat(ChatRequest chatRequest) {
        throw new UnsupportedOperationException("ChatModel.doChat must be implemented");
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

    default String chat(String userMessage) {
        ChatResponse response = chat(ChatRequest.builder().messages(UserMessage.from(userMessage)).build());
        return response.aiMessage().text();
    }

    default ChatResponse chat(ChatMessage... messages) {
        return chat(ChatRequest.builder().messages(Arrays.asList(messages)).build());
    }

    default ChatResponse chat(List<ChatMessage> messages) {
        return chat(ChatRequest.builder().messages(messages).build());
    }

    default Set<Capability> supportedCapabilities() {
        return Collections.emptySet();
    }

    /** Adapts a LangChain4j 0.35 model without discarding its response metadata or tools. */
    static ChatModel fromLegacy(final dev.langchain4j.model.chat.ChatLanguageModel legacy) {
        if (legacy == null) {
            throw new NullPointerException("legacy");
        }
        return new ChatModel() {
            @Override
            public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
                return legacy.generate(messages);
            }

            @Override
            public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                    java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                    java.util.List<dev.langchain4j.agent.tool.ToolSpecification> tools) {
                return legacy.generate(messages, tools);
            }

            @Override
            public ChatResponse doChat(ChatRequest request) {
                List<dev.langchain4j.agent.tool.ToolSpecification> tools = request.toolSpecifications();
                dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response =
                        tools == null || tools.isEmpty()
                                ? legacy.generate(request.messages())
                                : legacy.generate(request.messages(), tools);
                if (response == null) {
                    return null;
                }
                return ChatResponse.builder()
                        .aiMessage(response.content())
                        .tokenUsage(response.tokenUsage())
                        .finishReason(response.finishReason())
                        .build();
            }

            @Override
            public Set<Capability> supportedCapabilities() {
                return legacy.supportedCapabilities();
            }
        };
    }
}
