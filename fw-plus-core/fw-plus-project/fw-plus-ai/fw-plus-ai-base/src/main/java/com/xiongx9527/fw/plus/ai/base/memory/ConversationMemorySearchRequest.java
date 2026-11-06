package com.xiongx9527.fw.plus.ai.base.memory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 检索记忆的请求：自然语言查询 + 作用域 + 可选过滤条件 + 返回条数。
 *
 * <p>{@link #getQuery()} 是面向用户的自然语言（如「我的饮食禁忌是什么」），
 * 后端会做语义向量检索；{@link #getFilters()} 是结构化等值过滤（如按业务渠道）。
 * {@link #getTopK()} 为 null 时交由后端决定默认条数。</p>
 */
public final class ConversationMemorySearchRequest {

    private final String query;
    private final String userId;
    private final String agentId;
    private final String runId;
    private final Map<String, Object> filters;
    private final Integer topK;

    private ConversationMemorySearchRequest(Builder b) {
        this.query = b.query;
        this.userId = b.userId;
        this.agentId = b.agentId;
        this.runId = b.runId;
        this.filters = b.filters == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(b.filters));
        this.topK = b.topK;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getQuery() {
        return query;
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

    public Map<String, Object> getFilters() {
        return filters;
    }

    public Integer getTopK() {
        return topK;
    }

    public static final class Builder {
        private String query;
        private String userId;
        private String agentId;
        private String runId;
        private Map<String, Object> filters;
        private Integer topK;

        public Builder query(String value) {
            this.query = value;
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

        public Builder filters(Map<String, Object> value) {
            this.filters = value;
            return this;
        }

        public Builder topK(Integer value) {
            this.topK = value;
            return this;
        }

        public ConversationMemorySearchRequest build() {
            return new ConversationMemorySearchRequest(this);
        }
    }
}
