package com.xiongx9527.fw.plus.ai.base.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 写入记忆的请求：一段对话（多条消息）+ 作用域 + 附加元数据。
 *
 * <p>后端（如 mem0）会先用 LLM 把对话拆成若干事实再存储，因此传入的是原始
 * {@link ChatMessage} 列表，而不是已经抽取好的记忆文本。</p>
 */
public final class ConversationMemoryAddRequest {

    private final List<ChatMessage> messages;
    private final String userId;
    private final String agentId;
    private final String runId;
    private final Map<String, Object> metadata;

    private ConversationMemoryAddRequest(Builder b) {
        this.messages = b.messages == null ? new ArrayList<ChatMessage>()
                : new ArrayList<ChatMessage>(b.messages);
        this.userId = b.userId;
        this.agentId = b.agentId;
        this.runId = b.runId;
        this.metadata = b.metadata == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(b.metadata));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public String getUserId() {
        return userId;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getRunId() {
        return runId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static final class Builder {
        private List<ChatMessage> messages;
        private String userId;
        private String agentId;
        private String runId;
        private Map<String, Object> metadata;

        public Builder messages(List<ChatMessage> value) {
            this.messages = value;
            return this;
        }

        public Builder message(ChatMessage value) {
            if (this.messages == null) {
                this.messages = new ArrayList<ChatMessage>();
            }
            this.messages.add(value);
            return this;
        }

        public Builder userId(String value) {
            this.userId = value;
            return this;
        }

        public Builder agentId(String value) {
            this.agentId = value;
            return this;
        }

        public Builder runId(String value) {
            this.runId = value;
            return this;
        }

        public Builder metadata(Map<String, Object> value) {
            this.metadata = value;
            return this;
        }

        public ConversationMemoryAddRequest build() {
            return new ConversationMemoryAddRequest(this);
        }
    }
}
