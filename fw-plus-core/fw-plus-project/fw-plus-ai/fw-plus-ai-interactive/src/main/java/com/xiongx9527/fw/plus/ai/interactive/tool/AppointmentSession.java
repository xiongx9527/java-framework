package com.xiongx9527.fw.plus.ai.interactive.tool;

import com.xiongx9527.fw.plus.ai.interactive.CollectionResult;

/**
 * 预约会话：携带会话 id 与开启后的首帧（或完成帧），供后续提交引用。
 */
public final class AppointmentSession {

    private final String sessionId;
    private final CollectionResult result;

    private AppointmentSession(Builder builder) {
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

        public AppointmentSession build() {
            return new AppointmentSession(this);
        }
    }
}
