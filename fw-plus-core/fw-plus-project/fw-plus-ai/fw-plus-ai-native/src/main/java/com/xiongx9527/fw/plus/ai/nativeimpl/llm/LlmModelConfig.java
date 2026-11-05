package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 单个模型端点配置。密钥仅保存在运行时对象中，不提供 toString 避免意外泄漏。 */
public final class LlmModelConfig {
    private final LlmVendor vendor;
    private final LlmProtocol protocol;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final String apiVersion;
    /** 公共参数：连接超时（毫秒），null 表示交由端口使用默认值。 */
    private final Integer connectTimeoutMillis;
    /** 公共参数：读取超时（毫秒），null 表示交由端口使用默认值。 */
    private final Integer readTimeoutMillis;
    /** 个性化参数：厂商/协议特有的额外配置，原样透传给端口实现。 */
    private final Map<String, Object> properties;

    private LlmModelConfig(Builder b) {
        this.vendor = b.vendor;
        this.protocol = b.protocol;
        this.baseUrl = b.baseUrl;
        this.apiKey = b.apiKey;
        this.defaultModel = b.defaultModel;
        this.apiVersion = b.apiVersion;
        this.connectTimeoutMillis = b.connectTimeoutMillis;
        this.readTimeoutMillis = b.readTimeoutMillis;
        this.properties = b.properties == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(b.properties));
    }

    public static Builder builder() { return new Builder(); }
    public LlmVendor getVendor() { return vendor; }
    public LlmProtocol getProtocol() { return protocol; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getDefaultModel() { return defaultModel; }
    public String getApiVersion() { return apiVersion; }
    public Integer getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public Integer getReadTimeoutMillis() { return readTimeoutMillis; }
    public Map<String, Object> getProperties() { return properties; }

    public static final class Builder {
        private LlmVendor vendor;
        private LlmProtocol protocol;
        private String baseUrl;
        private String apiKey;
        private String defaultModel;
        private String apiVersion;
        private Integer connectTimeoutMillis;
        private Integer readTimeoutMillis;
        private Map<String, Object> properties;
        public Builder vendor(LlmVendor value) { this.vendor = value; return this; }
        public Builder protocol(LlmProtocol value) { this.protocol = value; return this; }
        public Builder baseUrl(String value) { this.baseUrl = value; return this; }
        public Builder apiKey(String value) { this.apiKey = value; return this; }
        public Builder defaultModel(String value) { this.defaultModel = value; return this; }
        public Builder apiVersion(String value) { this.apiVersion = value; return this; }
        public Builder connectTimeoutMillis(Integer value) { this.connectTimeoutMillis = value; return this; }
        public Builder readTimeoutMillis(Integer value) { this.readTimeoutMillis = value; return this; }
        public Builder property(String key, Object value) {
            if (this.properties == null) this.properties = new HashMap<String, Object>();
            this.properties.put(key, value);
            return this;
        }
        public Builder properties(Map<String, Object> value) {
            if (this.properties == null) this.properties = new HashMap<String, Object>();
            this.properties.putAll(value);
            return this;
        }
        public LlmModelConfig build() {
            if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("llm baseUrl is required");
            return new LlmModelConfig(this);
        }
    }
}
