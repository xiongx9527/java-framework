package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 一次非流式补全的结果。
 */
public final class LlmResponse {

    private final String content;

    private final String reasoningContent;

    private final LlmUsage usage;

    private LlmResponse(Builder builder) {
        this.content = builder.content;
        this.reasoningContent = builder.reasoningContent;
        this.usage = builder.usage == null ? LlmUsage.ZERO : builder.usage;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 正文内容；上游无输出时为 {@code null}。 */
    public String getContent() {
        return content;
    }

    /** 思考过程（推理模型才有）；其余为 {@code null}。 */
    public String getReasoningContent() {
        return reasoningContent;
    }

    public LlmUsage getUsage() {
        return usage;
    }

    public static final class Builder {

        private String content;

        private String reasoningContent;

        private LlmUsage usage;

        private Builder() {
        }

        public Builder content(String value) {
            this.content = value;
            return this;
        }

        public Builder reasoningContent(String value) {
            this.reasoningContent = value;
            return this;
        }

        public Builder usage(LlmUsage value) {
            this.usage = value;
            return this;
        }

        public LlmResponse build() {
            return new LlmResponse(this);
        }
    }
}
