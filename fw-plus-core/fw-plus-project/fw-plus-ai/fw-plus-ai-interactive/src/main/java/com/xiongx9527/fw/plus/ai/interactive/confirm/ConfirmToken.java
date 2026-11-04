package com.xiongx9527.fw.plus.ai.interactive.confirm;

import java.util.UUID;

/**
 * 确认令牌（对应文档 §6.33.4）。
 *
 * <p>绑定动作类型 + 参数快照的摘要，防止"确认 A、提交 B"：提交时校验 {@code actionDigest}
 * 与本次参数完全一致；且一次性、有有效期（与幂等键职责不同，见 §6.33.5）。</p>
 */
public final class ConfirmToken {

    private final String token;
    private final String actionDigest;
    private final long uid;
    private final long expiresAt;

    private ConfirmToken(Builder builder) {
        this.token = builder.token != null ? builder.token : UUID.randomUUID().toString();
        this.actionDigest = builder.actionDigest;
        this.uid = builder.uid;
        this.expiresAt = builder.expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getToken() {
        return token;
    }

    public String getActionDigest() {
        return actionDigest;
    }

    public long getUid() {
        return uid;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return expiresAt > 0 && expiresAt < System.currentTimeMillis();
    }

    public static final class Builder {

        private String token;
        private String actionDigest;
        private long uid;
        private long expiresAt;

        public Builder token(String value) {
            this.token = value;
            return this;
        }

        public Builder actionDigest(String value) {
            this.actionDigest = value;
            return this;
        }

        public Builder uid(long value) {
            this.uid = value;
            return this;
        }

        public Builder expiresAt(long value) {
            this.expiresAt = value;
            return this;
        }

        public ConfirmToken build() {
            if (actionDigest == null) {
                throw new IllegalStateException("ConfirmToken.actionDigest is required");
            }
            return new ConfirmToken(this);
        }
    }
}
