package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.tool.ToolChoice;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次大语言补全请求。
 *
 * <p>与具体厂商无关：{@code model} 为空表示用 adapter 的默认模型；{@code extraParams}
 * 承载厂商私有推理参数（如 {@code enable_thinking}），adapter 原样并入请求体。</p>
 *
 * <p>{@code tools} 非空时请求具备工具调用能力：adapter 把 {@link ToolSpecification} 翻译成
 * 上游的 {@code tools[]} 报文，模型可能在 {@link LlmResponse#getToolCalls()} 里返回调用。</p>
 */
public final class LlmRequest {

    private final List<ChatMessage> messages;

    private final String model;

    private final Double temperature;

    private final Double topP;

    private final Integer maxTokens;

    private final Map<String, Object> extraParams;

    private final List<LlmListener> listeners;

    private final List<ToolSpecification> tools;

    private final ToolChoice toolChoice;

    private LlmRequest(Builder builder) {
        this.messages = Collections.unmodifiableList(new ArrayList<ChatMessage>(builder.messages));
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.maxTokens = builder.maxTokens;
        this.extraParams = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.extraParams));
        this.listeners = Collections.unmodifiableList(new ArrayList<LlmListener>(builder.listeners));
        this.tools = Collections.unmodifiableList(new ArrayList<ToolSpecification>(builder.tools));
        this.toolChoice = builder.toolChoice;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 单轮 system + user 请求的快捷构造。 */
    public static LlmRequest systemUser(String systemPrompt, String userPrompt) {
        return builder().addMessage(ChatMessage.system(systemPrompt)).addMessage(ChatMessage.user(userPrompt)).build();
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public String getModel() {
        return model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    /** 本次调用的事件监听器（流式与非流式都会派发）。 */
    public List<LlmListener> getListeners() {
        return listeners;
    }

    /** 允许模型调用的工具；空列表表示不带工具。 */
    public List<ToolSpecification> getTools() {
        return tools;
    }

    public boolean hasTools() {
        return !tools.isEmpty();
    }

    /** 工具选择策略；{@code null} 表示由 adapter 决定（通常等同 {@code auto}）。 */
    public ToolChoice getToolChoice() {
        return toolChoice;
    }

    /** 流式请求时是否要求上游返回思考过程；{@code false} 表示未显式开启。 */
    public boolean isThinkingEnabled() {
        Object value = extraParams.get("enable_thinking");
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    public static final class Builder {

        private final List<ChatMessage> messages = new ArrayList<ChatMessage>();

        private final Map<String, Object> extraParams = new LinkedHashMap<String, Object>();

        private final List<LlmListener> listeners = new ArrayList<LlmListener>();

        private final List<ToolSpecification> tools = new ArrayList<ToolSpecification>();

        private String model;

        private Double temperature;

        private Double topP;

        private Integer maxTokens;

        private ToolChoice toolChoice;

        private Builder() {
        }

        public Builder messages(List<ChatMessage> value) {
            this.messages.clear();
            if (value != null) {
                this.messages.addAll(value);
            }
            return this;
        }

        public Builder addMessage(ChatMessage value) {
            if (value != null) {
                this.messages.add(value);
            }
            return this;
        }

        public Builder model(String value) {
            this.model = value;
            return this;
        }

        public Builder temperature(Double value) {
            this.temperature = value;
            return this;
        }

        public Builder topP(Double value) {
            this.topP = value;
            return this;
        }

        public Builder maxTokens(Integer value) {
            this.maxTokens = value;
            return this;
        }

        public Builder extraParam(String key, Object value) {
            if (key != null) {
                this.extraParams.put(key, value);
            }
            return this;
        }

        public Builder extraParams(Map<String, Object> value) {
            if (value != null) {
                this.extraParams.putAll(value);
            }
            return this;
        }

        public Builder listener(LlmListener value) {
            if (value != null) {
                this.listeners.add(value);
            }
            return this;
        }

        public Builder listeners(List<LlmListener> value) {
            this.listeners.clear();
            if (value != null) {
                this.listeners.addAll(value);
            }
            return this;
        }

        public Builder tools(List<ToolSpecification> value) {
            this.tools.clear();
            if (value != null) {
                this.tools.addAll(value);
            }
            return this;
        }

        public Builder addTool(ToolSpecification value) {
            if (value != null) {
                this.tools.add(value);
            }
            return this;
        }

        public Builder toolChoice(ToolChoice value) {
            this.toolChoice = value;
            return this;
        }

        public LlmRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalArgumentException("llm request requires at least one message");
            }
            return new LlmRequest(this);
        }
    }
}
