package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次非流式补全的结果。
 *
 * <p>{@code id / modelName / finishReason / metadata} 是响应元数据，与 LangChain4j 1.x 的
 * {@code ChatResponse / ChatResponseMetadata} 对齐，用于计费、排障与重试策略；
 * 上游未上报时为 {@code null} / 空集合。</p>
 */
public final class LlmResponse {

    private final String content;

    private final String reasoningContent;

    private final LlmUsage usage;

    private final List<ToolExecutionRequest> toolCalls;

    /** 上游响应 id；未上报时为 {@code null}。 */
    private final String id;

    /** 实际处理请求的模型名；未上报时为 {@code null}。 */
    private final String modelName;

    /** 结束原因（如 stop / length / tool_calls）；未上报时为 {@code null}。 */
    private final String finishReason;

    /** 厂商私有响应元数据，adapter 原样透传，base 不解释。 */
    private final Map<String, Object> metadata;

    /** 模型生成的图片（URL 或 base64 data URL）；无则为 {@code null}。 */
    private final List<String> images;

    /** 模型生成的音频（URL 或 base64 data URL）；无则为 {@code null}。 */
    private final List<String> audios;

    private LlmResponse(Builder builder) {
        this.content = builder.content;
        this.reasoningContent = builder.reasoningContent;
        this.usage = builder.usage == null ? LlmUsage.ZERO : builder.usage;
        this.toolCalls = Collections.unmodifiableList(new ArrayList<ToolExecutionRequest>(builder.toolCalls));
        this.id = builder.id;
        this.modelName = builder.modelName;
        this.finishReason = builder.finishReason;
        this.metadata = builder.metadata == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
        this.images = builder.images == null ? null
                : Collections.unmodifiableList(new ArrayList<String>(builder.images));
        this.audios = builder.audios == null ? null
                : Collections.unmodifiableList(new ArrayList<String>(builder.audios));
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

    public String getId() {
        return id;
    }

    public String getModelName() {
        return modelName;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<String> getImages() {
        return images;
    }

    public List<String> getAudios() {
        return audios;
    }

    public static final class Builder {

        private final List<ToolExecutionRequest> toolCalls = new ArrayList<ToolExecutionRequest>();

        private String content;

        private String reasoningContent;

        private LlmUsage usage;

        private String id;

        private String modelName;

        private String finishReason;

        private Map<String, Object> metadata;

        private List<String> images;

        private List<String> audios;

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

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder modelName(String value) {
            this.modelName = value;
            return this;
        }

        public Builder finishReason(String value) {
            this.finishReason = value;
            return this;
        }

        public Builder metadata(Map<String, Object> value) {
            this.metadata = value == null ? null : new LinkedHashMap<String, Object>(value);
            return this;
        }

        public Builder images(List<String> value) {
            this.images = value == null ? null : new ArrayList<String>(value);
            return this;
        }

        public Builder audios(List<String> value) {
            this.audios = value == null ? null : new ArrayList<String>(value);
            return this;
        }

        public LlmResponse build() {
            return new LlmResponse(this);
        }
    }
}
