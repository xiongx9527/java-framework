package com.xiongx9527.fw.plus.ai.model.chat.response;

import com.xiongx9527.fw.plus.ai.model.output.FinishReason;
import com.xiongx9527.fw.plus.ai.model.output.TokenUsage;
import com.xiongx9527.fw.plus.ai.message.AiMessage;

/** 聊天响应，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.chat.response.ChatResponse}。 */
public class ChatResponse {
    private final AiMessage aiMessage;
    private final ChatResponseMetadata metadata;

    private ChatResponse(Builder b) {
        this.aiMessage = b.aiMessage;
        this.metadata = b.metadata == null ? ChatResponseMetadata.builder().build() : b.metadata;
    }
    public AiMessage aiMessage() { return aiMessage; }
    public ChatResponseMetadata metadata() { return metadata; }
    public String id() { return metadata.id(); }
    public String modelName() { return metadata.modelName(); }
    public TokenUsage tokenUsage() { return metadata.tokenUsage(); }
    public FinishReason finishReason() { return metadata.finishReason(); }

    public Builder toBuilder() { return builder().aiMessage(aiMessage).metadata(metadata); }
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private AiMessage aiMessage;
        private ChatResponseMetadata metadata;
        public Builder aiMessage(AiMessage v) { this.aiMessage = v; return this; }
        public Builder metadata(ChatResponseMetadata v) { this.metadata = v; return this; }
        public ChatResponse build() { return new ChatResponse(this); }
    }
}
