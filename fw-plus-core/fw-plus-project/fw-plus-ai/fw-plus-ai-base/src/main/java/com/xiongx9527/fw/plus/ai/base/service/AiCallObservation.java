package com.xiongx9527.fw.plus.ai.base.service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次 AI 服务调用的观测快照，交给 {@link AiObservationListener} 做追踪 / 监控。
 *
 * <p>把「意图分类 → 检索增强 → 工具循环 → 最终回答」收敛成一条调用级 trace，
 * 便于上报到 Langfuse / Langsmith 等观测平台（这些具体后端在 {@code support} 层实现）。</p>
 */
public final class AiCallObservation {

    private final Class<?> serviceType;

    private final String methodName;

    private final String userText;

    private final String intentRef;

    private final boolean retrievalInjected;

    private final String answer;

    private final long latencyMillis;

    private final String error;

    private final Map<String, Object> attributes;

    private AiCallObservation(Builder b) {
        this.serviceType = b.serviceType;
        this.methodName = b.methodName;
        this.userText = b.userText;
        this.intentRef = b.intentRef;
        this.retrievalInjected = b.retrievalInjected;
        this.answer = b.answer;
        this.latencyMillis = b.latencyMillis;
        this.error = b.error;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(b.attributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Class<?> getServiceType() {
        return serviceType;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getUserText() {
        return userText;
    }

    public String getIntentRef() {
        return intentRef;
    }

    public boolean isRetrievalInjected() {
        return retrievalInjected;
    }

    public String getAnswer() {
        return answer;
    }

    public long getLatencyMillis() {
        return latencyMillis;
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static final class Builder {

        private Class<?> serviceType;

        private String methodName;

        private String userText;

        private String intentRef;

        private boolean retrievalInjected;

        private String answer;

        private long latencyMillis;

        private String error;

        private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

        public Builder serviceType(Class<?> value) {
            this.serviceType = value;
            return this;
        }

        public Builder method(Method value) {
            this.methodName = value == null ? null : value.getName();
            return this;
        }

        public Builder methodName(String value) {
            this.methodName = value;
            return this;
        }

        public Builder userText(String value) {
            this.userText = value;
            return this;
        }

        public Builder intentRef(String value) {
            this.intentRef = value;
            return this;
        }

        public Builder retrievalInjected(boolean value) {
            this.retrievalInjected = value;
            return this;
        }

        public Builder answer(String value) {
            this.answer = value;
            return this;
        }

        public Builder latencyMillis(long value) {
            this.latencyMillis = value;
            return this;
        }

        public Builder error(String value) {
            this.error = value;
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key != null) {
                this.attributes.put(key, value);
            }
            return this;
        }

        public AiCallObservation build() {
            return new AiCallObservation(this);
        }
    }
}
