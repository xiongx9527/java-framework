package com.xiongx9527.fw.plus.ai.base.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调用上下文：链路追踪与身份。
 */
public final class CallContext {

    private final String traceId;

    private final String tenantId;

    private final String userId;

    private final Map<String, Object> attributes;

    private CallContext(Builder builder) {
        this.traceId = builder.traceId;
        this.tenantId = builder.tenantId;
        this.userId = builder.userId;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.attributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CallContext of(String traceId) {
        return builder().traceId(traceId).build();
    }

    public String getTraceId() {
        return traceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object attribute(String key) {
        return attributes.get(key);
    }

    public static final class Builder {

        private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

        private String traceId;

        private String tenantId;

        private String userId;

        private Builder() {
        }

        public Builder traceId(String value) {
            this.traceId = value;
            return this;
        }

        public Builder tenantId(String value) {
            this.tenantId = value;
            return this;
        }

        public Builder userId(String value) {
            this.userId = value;
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key != null) {
                this.attributes.put(key, value);
            }
            return this;
        }

        public CallContext build() {
            return new CallContext(this);
        }
    }
}
