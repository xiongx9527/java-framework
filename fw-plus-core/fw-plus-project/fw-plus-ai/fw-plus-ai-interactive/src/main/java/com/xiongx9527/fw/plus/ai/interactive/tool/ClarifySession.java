package com.xiongx9527.fw.plus.ai.interactive.tool;

import com.xiongx9527.fw.plus.ai.interactive.CollectionResult;

/**
 * 反问 / 收集会话：携带会话 id 与首帧结果，供后续 {@code answer/state} 引用。
 */
public final class ClarifySession {

    private final String sessionId;
    private final CollectionResult result;

    private ClarifySession(Builder builder) {
        this.sessionId = builder.sessionId;
        this.result = builder.result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSessionId() {
        return sessionId;
    }

    public CollectionResult getResult() {
        return result;
    }

    public static final class Builder {

        private String sessionId;
        private CollectionResult result;

        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        public Builder result(CollectionResult value) {
            this.result = value;
            return this;
        }

        public ClarifySession build() {
            return new ClarifySession(this);
        }
    }
}
