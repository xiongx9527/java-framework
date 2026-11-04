package com.xiongx9527.fw.plus.ai.interactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 反问帧（对应文档 §6.32.3 的 {@code clarify} 帧协议）。
 *
 * <p>中间帧，不是终态：用户回填后前端保持会话、不重建连接。一次会话可多次反问，受最大轮次限制。</p>
 *
 * <p>字段：</p>
 * <ul>
 *   <li>{@code id}：本次反问唯一标识，用户回填必须带回（多轮并发防止答错问题）；</li>
 *   <li>{@code reason}：四种触发之一，前端据此决定交互形态；</li>
 *   <li>{@code fields}：本次要问的字段（一次最多 3 个）；</li>
 *   <li>{@code hasMore} + {@code cursor}：大目录分页；</li>
 *   <li>{@code expiresAt}：过期后前端禁用选项。</li>
 * </ul>
 */
public final class ClarifyFrame {

    private final String id;
    private final ClarifyReason reason;
    private final String text;
    private final List<ClarifyField> fields;
    private final boolean hasMore;
    private final String cursor;
    private final long expiresAt;

    private ClarifyFrame(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.reason = builder.reason == null ? ClarifyReason.MISSING : builder.reason;
        this.text = builder.text;
        this.fields = builder.fields == null ? Collections.<ClarifyField>emptyList()
                : Collections.unmodifiableList(new ArrayList<ClarifyField>(builder.fields));
        this.hasMore = builder.hasMore;
        this.cursor = builder.cursor;
        this.expiresAt = builder.expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public ClarifyReason getReason() {
        return reason;
    }

    public String getText() {
        return text;
    }

    public List<ClarifyField> getFields() {
        return fields;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public String getCursor() {
        return cursor;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public static final class Builder {

        private String id;
        private ClarifyReason reason;
        private String text;
        private List<ClarifyField> fields;
        private boolean hasMore;
        private String cursor;
        private long expiresAt;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder reason(ClarifyReason value) {
            this.reason = value;
            return this;
        }

        public Builder text(String value) {
            this.text = value;
            return this;
        }

        public Builder fields(List<ClarifyField> value) {
            this.fields = value;
            return this;
        }

        public Builder hasMore(boolean value) {
            this.hasMore = value;
            return this;
        }

        public Builder cursor(String value) {
            this.cursor = value;
            return this;
        }

        public Builder expiresAt(long value) {
            this.expiresAt = value;
            return this;
        }

        public ClarifyFrame build() {
            return new ClarifyFrame(this);
        }
    }
}
