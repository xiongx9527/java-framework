package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory;

import dev.langchain4j.data.message.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 可指定持久化位置的 AI 消息项。
 */
public class PersistedAiItem {

    private final ChatMessage message;
    private final AiPersistencePosition position;
    private final int priority; // 数值越大优先级越高（同位置内按 priority 降序）

    private PersistedAiItem(Builder builder) {
        this.message = builder.message;
        this.position = builder.position;
        this.priority = builder.priority;
    }

    public static Builder builder(ChatMessage message) {
        return new Builder(message);
    }

    public ChatMessage message() {
        return message;
    }

    public AiPersistencePosition position() {
        return position == null ? AiPersistencePosition.AFTER_LAST_USER : position;
    }

    public int priority() {
        return priority;
    }

    public static List<PersistedAiItem> fromMessages(List<ChatMessage> messages, AiPersistencePosition defaultPosition) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        List<PersistedAiItem> items = new ArrayList<>(messages.size());
        for (ChatMessage m : messages) {
            items.add(PersistedAiItem.builder(m)
                    .position(defaultPosition)
                    .priority(0)
                    .build());
        }
        return items;
    }

    public static List<PersistedAiItem> fromMessages(List<ChatMessage> messages) {
        return fromMessages(messages, AiPersistencePosition.AFTER_LAST_USER);
    }

    public static final class Builder {
        private final ChatMessage message;
        private AiPersistencePosition position = AiPersistencePosition.AFTER_LAST_USER;
        private int priority = 0;

        private Builder(ChatMessage message) {
            this.message = message;
        }

        public Builder position(AiPersistencePosition position) {
            this.position = position;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public PersistedAiItem build() {
            return new PersistedAiItem(this);
        }
    }
}
