package com.xiongx9527.fw.plus.ai.model.chat.response;

import com.xiongx9527.fw.plus.ai.model.output.FinishReason;
import com.xiongx9527.fw.plus.ai.model.output.TokenUsage;

/** 聊天响应元数据，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.chat.response.ChatResponseMetadata}。 */
public class ChatResponseMetadata {
    private final String id;
    private final String modelName;
    private final TokenUsage tokenUsage;
    private final FinishReason finishReason;

    private ChatResponseMetadata(Builder b) {
        this.id = b.id; this.modelName = b.modelName; this.tokenUsage = b.tokenUsage; this.finishReason = b.finishReason;
    }
    public String id() { return id; }
    public String modelName() { return modelName; }
    public TokenUsage tokenUsage() { return tokenUsage; }
    public FinishReason finishReason() { return finishReason; }
    public Builder toBuilder() { return builder().id(id).modelName(modelName).tokenUsage(tokenUsage).finishReason(finishReason); }
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String id; private String modelName; private TokenUsage tokenUsage; private FinishReason finishReason;
        public Builder id(String v) { this.id = v; return this; }
        public Builder modelName(String v) { this.modelName = v; return this; }
        public Builder tokenUsage(TokenUsage v) { this.tokenUsage = v; return this; }
        public Builder finishReason(FinishReason v) { this.finishReason = v; return this; }
        public ChatResponseMetadata build() { return new ChatResponseMetadata(this); }
    }
}
