package com.xiongx9527.fw.plus.ai.base.memory;

/**
 * 记忆的作用域标识：用于 {@code list} / {@code deleteAll} 与检索过滤。
 *
 * <p>三者都是可选的，但至少要提供一个，否则后端不知道「谁的记忆」。
 * 优先级通常为用户 {@code userId} → 智能体 {@code agentId} → 单次运行 {@code runId}。</p>
 */
public final class ConversationMemoryScope {

    private final String userId;
    private final String agentId;
    private final String runId;

    private ConversationMemoryScope(Builder b) {
        this.userId = b.userId;
        this.agentId = b.agentId;
        this.runId = b.runId;
    }

    public static Builder builder() {
        return new Builder();
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

    public boolean isEmpty() {
        return (userId == null || userId.trim().isEmpty())
                && (agentId == null || agentId.trim().isEmpty())
                && (runId == null || runId.trim().isEmpty());
    }

    public static final class Builder {
        private String userId;
        private String agentId;
        private String runId;

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

        public ConversationMemoryScope build() {
            return new ConversationMemoryScope(this);
        }
    }
}
