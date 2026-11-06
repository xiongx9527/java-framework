package com.xiongx9527.fw.plus.ai.base.memory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一条记忆记录：由后端从对话中抽取、存储、可被检索的事实单元。
 *
 * <p>语义对齐 mem0 的 {@code memory} 对象：{@link #getMemory()} 是抽取出的事实文本，
 * {@link #getScore()} 仅在 {@code search} 结果中出现（越大越相关），{@link #getEvent()}
 * 标记本次对该记忆的操作（ADD / UPDATE / DELETE / NONE）。</p>
 */
public final class MemoryRecord {

    private final String id;
    private final String memory;
    private final Double score;
    private final String userId;
    private final String agentId;
    private final String runId;
    private final String event;
    private final Map<String, Object> metadata;
    private final String createdAt;

    private MemoryRecord(Builder b) {
        this.id = b.id;
        this.memory = b.memory;
        this.score = b.score;
        this.userId = b.userId;
        this.agentId = b.agentId;
        this.runId = b.runId;
        this.event = b.event;
        this.metadata = b.metadata == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(b.metadata));
        this.createdAt = b.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getMemory() {
        return memory;
    }

    public Double getScore() {
        return score;
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

    public String getEvent() {
        return event;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public static final class Builder {
        private String id;
        private String memory;
        private Double score;
        private String userId;
        private String agentId;
        private String runId;
        private String event;
        private Map<String, Object> metadata;
        private String createdAt;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder memory(String value) {
            this.memory = value;
            return this;
        }

        public Builder score(Double value) {
            this.score = value;
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

        public Builder event(String value) {
            this.event = value;
            return this;
        }

        public Builder metadata(Map<String, Object> value) {
            this.metadata = value;
            return this;
        }

        public Builder createdAt(String value) {
            this.createdAt = value;
            return this;
        }

        public MemoryRecord build() {
            return new MemoryRecord(this);
        }
    }
}
