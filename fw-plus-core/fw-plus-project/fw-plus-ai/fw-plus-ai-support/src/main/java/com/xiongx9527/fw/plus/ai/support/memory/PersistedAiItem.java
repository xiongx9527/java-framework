package com.xiongx9527.fw.plus.ai.support.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条带「持久化位置 + 优先级」的 AI 消息项。
 *
 * <p>位置决定插入点（{@link AiPersistencePosition}），优先级决定同位置内的先后顺序
 * （数值越大越靠前，同优先级保持策略返回的原始顺序）。</p>
 */
public final class PersistedAiItem {

    private final ChatMessage message;

    private final AiPersistencePosition position;

    private final int priority;

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
            return new ArrayList<PersistedAiItem>();
        }
        List<PersistedAiItem> items = new ArrayList<PersistedAiItem>(messages.size());
        for (ChatMessage m : messages) {
            items.add(PersistedAiItem.builder(m).position(defaultPosition).priority(0).build());
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
