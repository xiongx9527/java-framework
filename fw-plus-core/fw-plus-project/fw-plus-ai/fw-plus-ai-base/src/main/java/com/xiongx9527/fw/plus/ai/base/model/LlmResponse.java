package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次非流式补全的结果。
 */
public final class LlmResponse {

    private final String content;

    private final String reasoningContent;

    private final LlmUsage usage;

    private final List<ToolExecutionRequest> toolCalls;

    private LlmResponse(Builder builder) {
        this.content = builder.content;
        this.reasoningContent = builder.reasoningContent;
        this.usage = builder.usage == null ? LlmUsage.ZERO : builder.usage;
        this.toolCalls = Collections.unmodifiableList(new ArrayList<ToolExecutionRequest>(builder.toolCalls));
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

    /** 模型要求执行的工具调用；没有时为空列表。 */
    public List<ToolExecutionRequest> getToolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    public static final class Builder {

        private final List<ToolExecutionRequest> toolCalls = new ArrayList<ToolExecutionRequest>();

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

        public Builder toolCalls(List<ToolExecutionRequest> value) {
            this.toolCalls.clear();
            if (value != null) {
                this.toolCalls.addAll(value);
            }
            return this;
        }

        public Builder addToolCall(ToolExecutionRequest value) {
            if (value != null) {
                this.toolCalls.add(value);
            }
            return this;
        }

        public LlmResponse build() {
            return new LlmResponse(this);
        }
    }
}
