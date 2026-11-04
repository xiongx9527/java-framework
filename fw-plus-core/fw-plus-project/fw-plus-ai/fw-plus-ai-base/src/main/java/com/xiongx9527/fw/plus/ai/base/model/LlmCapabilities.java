package com.xiongx9527.fw.plus.ai.base.model;

/**
 * LLM 端口声明的协议能力。能力是端口级约束，业务可据此决定是否展示工具、多模态或推理参数。
 */
public final class LlmCapabilities {

    private final boolean streaming;
    private final boolean toolCalling;
    private final boolean structuredOutput;
    private final boolean imageInput;
    private final boolean audioInput;
    private final boolean fileInput;
    private final boolean reasoning;
    private final boolean usage;

    private LlmCapabilities(Builder builder) {
        this.streaming = builder.streaming;
        this.toolCalling = builder.toolCalling;
        this.structuredOutput = builder.structuredOutput;
        this.imageInput = builder.imageInput;
        this.audioInput = builder.audioInput;
        this.fileInput = builder.fileInput;
        this.reasoning = builder.reasoning;
        this.usage = builder.usage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LlmCapabilities basic() {
        return builder().streaming(true).usage(true).build();
    }

    public boolean isStreaming() {
        return streaming;
    }

    public boolean isToolCalling() {
        return toolCalling;
    }

    public boolean isStructuredOutput() {
        return structuredOutput;
    }

    public boolean isImageInput() {
        return imageInput;
    }

    public boolean isAudioInput() {
        return audioInput;
    }

    public boolean isFileInput() {
        return fileInput;
    }

    public boolean isReasoning() {
        return reasoning;
    }

    public boolean isUsage() {
        return usage;
    }

    public static final class Builder {
        private boolean streaming;
        private boolean toolCalling;
        private boolean structuredOutput;
        private boolean imageInput;
        private boolean audioInput;
        private boolean fileInput;
        private boolean reasoning;
        private boolean usage;

        public Builder streaming(boolean value) { this.streaming = value; return this; }
        public Builder toolCalling(boolean value) { this.toolCalling = value; return this; }
        public Builder structuredOutput(boolean value) { this.structuredOutput = value; return this; }
        public Builder imageInput(boolean value) { this.imageInput = value; return this; }
        public Builder audioInput(boolean value) { this.audioInput = value; return this; }
        public Builder fileInput(boolean value) { this.fileInput = value; return this; }
        public Builder reasoning(boolean value) { this.reasoning = value; return this; }
        public Builder usage(boolean value) { this.usage = value; return this; }

        public LlmCapabilities build() {
            return new LlmCapabilities(this);
        }
    }
}
