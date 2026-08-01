package dev.langchain4j.model.chat;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface ChatLanguageModel extends ChatModel {

    default String generate(String userMessage) {
        return generate(UserMessage.from(userMessage)).content().text();
    }

    default Response<AiMessage> generate(ChatMessage... messages) {
        return generate(Arrays.asList(messages));
    }

    Response<AiMessage> generate(List<ChatMessage> messages);

    default Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        throw new IllegalArgumentException("Tools are currently not supported by this model");
    }

    default Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        throw new IllegalArgumentException("Tools are currently not supported by this model");
    }

    @Override
    default ChatResponse chat(ChatRequest chatRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    default ChatResponse chat(ChatRequest chatRequest, ChatRequestOptions chatRequestOptions) {
        return chat(chatRequest);
    }

    @Override
    default ChatResponse doChat(ChatRequest chatRequest) {
        return chat(chatRequest);
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
    default Set<Capability> supportedCapabilities() {
        return Collections.emptySet();
    }
}
