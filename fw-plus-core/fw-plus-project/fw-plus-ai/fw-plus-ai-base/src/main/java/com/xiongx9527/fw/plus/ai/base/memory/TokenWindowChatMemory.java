package com.xiongx9527.fw.plus.ai.base.memory;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.model.TokenCountEstimator;
import java.util.ArrayList;
import java.util.List;

/**
 * token 窗口记忆：按估算 token 数裁剪，超出 {@code maxTokens} 时从最旧的非系统消息开始丢弃。
 *
 * <p>与 {@link MessageWindowChatMemory} 的区别：按消息条数裁剪会被「一条超长消息」打穿，
 * 按 token 裁剪才对应模型真实的上下文上限。系统提示词同样始终保留。</p>
 *
 * <p><b>分词器是必填项</b>：窗口大小直接决定上下文能否装下，框架不猜字符与 token 的换算比例
 * （中文、代码、多语言的比例差别很大，猜错会静默截断上下文）。{@link #DEFAULT_ESTIMATOR}
 * 只是「4 字符 ≈ 1 token」的粗糙参考，需要时由调用方显式传入。</p>
 */
public class TokenWindowChatMemory implements ChatMemory {

    /** 默认估算：4 字符 ≈ 1 token，至少 1。 */
    public static final TokenCountEstimator DEFAULT_ESTIMATOR = text -> {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    };

    private final String id;

    private final int maxTokens;

    private final ChatMemoryStore store;

    private final TokenCountEstimator estimator;

    private TokenWindowChatMemory(Builder builder) {
        this.id = builder.id;
        this.maxTokens = builder.maxTokens;
        this.store = builder.store == null ? new InMemoryChatMemoryStore() : builder.store;
        this.estimator = builder.estimator;
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

    /** 从头部丢弃最旧的非系统消息，直到估算 token 不超上限。 */
    private List<ChatMessage> trim(List<ChatMessage> messages) {
        if (maxTokens <= 0) {
            return messages;
        }
        List<ChatMessage> result = new ArrayList<ChatMessage>(messages);
        while (result.size() > 1 && estimator.estimateInMessages(result) > maxTokens) {
            int dropIndex = -1;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i).getRole() != ChatRole.SYSTEM) {
                    dropIndex = i;
                    break;
                }
            }
            if (dropIndex < 0) {
                // 只剩系统消息仍超限：保留最后一条，避免清空上下文
                break;
            }
            result.remove(dropIndex);
        }
        return result;
    }

    public static final class Builder {

        private String id;

        private int maxTokens = 2048;

        private ChatMemoryStore store;

        private TokenCountEstimator estimator;

        private Builder() {
        }

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder maxTokens(int value) {
            this.maxTokens = value;
            return this;
        }

        public Builder store(ChatMemoryStore value) {
            this.store = value;
            return this;
        }

        public Builder estimator(TokenCountEstimator value) {
            this.estimator = value;
            return this;
        }

        public TokenWindowChatMemory build() {
            if (estimator == null) {
                throw new IllegalArgumentException("token count estimator is required");
            }
            return new TokenWindowChatMemory(this);
        }
    }
}
