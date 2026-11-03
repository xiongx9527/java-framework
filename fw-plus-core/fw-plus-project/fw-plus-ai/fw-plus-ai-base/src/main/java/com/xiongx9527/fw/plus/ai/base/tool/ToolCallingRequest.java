package com.xiongx9527.fw.plus.ai.base.tool;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.List;
import java.util.Set;

/**
 * 工具调用环的一次请求。
 */
public final class ToolCallingRequest {

    private final List<ChatMessage> messages;

    private final String model;

    private final Set<String> allowedTools;

    private final int maxSteps;

    public ToolCallingRequest(List<ChatMessage> messages, String model, Set<String> allowedTools, int maxSteps) {
        this.messages = messages;
        this.model = model;
        this.allowedTools = allowedTools;
        this.maxSteps = maxSteps;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public String getModel() {
        return model;
    }

    /** 允许模型使用的工具名；{@code null} 或空集表示不限制。 */
    public Set<String> getAllowedTools() {
        return allowedTools;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public static final class Builder {

        private List<ChatMessage> messages;

        private String model;

        private Set<String> allowedTools;

        private int maxSteps;

        public Builder messages(List<ChatMessage> value) {
            this.messages = value;
            return this;
        }

        public Builder model(String value) {
            this.model = value;
            return this;
        }

        public Builder allowedTools(Set<String> value) {
            this.allowedTools = value;
            return this;
        }

        public Builder maxSteps(int value) {
            this.maxSteps = value;
            return this;
        }

        public ToolCallingRequest build() {
            return new ToolCallingRequest(messages, model, allowedTools, maxSteps);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
