package com.xiongx9527.fw.plus.ai.base.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息窗口记忆：只保留最近 {@code maxMessages} 条，超出时从最旧的非系统消息开始丢弃。
 *
 * <p>系统提示词始终保留——它是角色设定与输出约束，被窗口挤掉会让模型行为漂移。
 * 因此实际保留条数可能比 {@code maxMessages} 多出「系统消息条数」。</p>
 */
public class MessageWindowChatMemory implements ChatMemory {

    private final String id;

    private final int maxMessages;

    private final ChatMemoryStore store;

    private MessageWindowChatMemory(Builder builder) {
        this.id = builder.id;
        this.maxMessages = builder.maxMessages;
        this.store = builder.store == null ? new InMemoryChatMemoryStore() : builder.store;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void add(ChatMessage message) {
        if (message == null) {
            return;
        }
        List<ChatMessage> current = store.getMessages(id);
        current.add(message);
        store.updateMessages(id, trim(current));
    }

    @Override
    public List<ChatMessage> messages() {
        return store.getMessages(id);
    }

    @Override
    public void clear() {
        store.deleteMessages(id);
    }

    /** 从头部丢弃最旧的系统消息之外的消息，直到条数不超上限。 */
    private List<ChatMessage> trim(List<ChatMessage> messages) {
        if (maxMessages <= 0 || messages.size() <= maxMessages) {
            return messages;
        }
        List<ChatMessage> result = new ArrayList<ChatMessage>(messages);
        while (result.size() > maxMessages) {
            int dropIndex = -1;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i).getRole() != ChatRole.SYSTEM) {
                    dropIndex = i;
                    break;
                }
            }
            if (dropIndex < 0) {
                // 全是系统消息：只能从头部丢，否则无法收敛
                dropIndex = 0;
            }
            result.remove(dropIndex);
        }
        return result;
    }

    public static final class Builder {

        private String id;

        private int maxMessages = 20;

        private ChatMemoryStore store;

        private Builder() {
        }

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder maxMessages(int value) {
            this.maxMessages = value;
            return this;
        }

        public Builder store(ChatMemoryStore value) {
            this.store = value;
            return this;
        }

        public MessageWindowChatMemory build() {
            return new MessageWindowChatMemory(this);
        }
    }
}
