package com.xiongx9527.fw.plus.ai.model.chat.request;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.Collections;
import java.util.List;

/** 聊天请求，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.chat.request.ChatRequest}（builder + 参数委派）。 */
public class ChatRequest {
    private final List<ChatMessage> messages;
    private final ChatRequestParameters parameters;

    private ChatRequest(Builder b) {
        this.messages = b.messages == null ? Collections.<ChatMessage>emptyList() : b.messages;
        this.parameters = b.parameters == null ? DefaultChatRequestParameters.builder().build() : b.parameters;
    }
    public List<ChatMessage> messages() { return messages; }
    public ChatRequestParameters parameters() { return parameters; }
    public String modelName() { return parameters.modelName(); }
    public Double temperature() { return parameters.temperature(); }
    public Double topP() { return parameters.topP(); }
    public Integer topK() { return parameters.topK(); }
    public Double frequencyPenalty() { return parameters.frequencyPenalty(); }
    public Double presencePenalty() { return parameters.presencePenalty(); }
    public Integer maxOutputTokens() { return parameters.maxOutputTokens(); }
    public List<String> stopSequences() { return parameters.stopSequences(); }
    public List<ToolSpecification> toolSpecifications() { return parameters.toolSpecifications(); }
    public ToolChoice toolChoice() { return parameters.toolChoice(); }
    public ResponseFormat responseFormat() { return parameters.responseFormat(); }

    public Builder toBuilder() { return builder().messages(messages).parameters(parameters); }
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private List<ChatMessage> messages;
        private ChatRequestParameters parameters;
        public Builder messages(List<ChatMessage> messages) { this.messages = messages; return this; }
        public Builder parameters(ChatRequestParameters parameters) { this.parameters = parameters; return this; }
        public ChatRequest build() { return new ChatRequest(this); }
    }
}
