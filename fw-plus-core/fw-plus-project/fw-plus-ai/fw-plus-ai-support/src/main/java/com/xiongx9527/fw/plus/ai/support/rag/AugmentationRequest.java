package com.xiongx9527.fw.plus.ai.support.rag;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索增强请求：原始问题 + 已有消息 + 检索参数。
 */
public final class AugmentationRequest {

    private final String query;

    private final List<ChatMessage> messages;

    private final Map<String, Object> parameters;

    private AugmentationRequest(Builder builder) {
        this.query = builder.query;
        this.messages = Collections.unmodifiableList(new ArrayList<ChatMessage>(builder.messages));
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.parameters));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getQuery() {
        return query;
    }

    /** 已有对话消息（注入时在它们基础上追加资料）。 */
    public List<ChatMessage> getMessages() {
        return messages;
    }

    /** 检索参数（如 collection / topK / threshold），由检索实现自行解释。 */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    public static final class Builder {

        private final List<ChatMessage> messages = new ArrayList<ChatMessage>();

        private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();

        private String query;

        private Builder() {
        }

        public Builder query(String value) {
            this.query = value;
            return this;
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

        public Builder parameter(String key, Object value) {
            if (key != null) {
                this.parameters.put(key, value);
            }
            return this;
        }

        public AugmentationRequest build() {
            return new AugmentationRequest(this);
        }
    }
}
