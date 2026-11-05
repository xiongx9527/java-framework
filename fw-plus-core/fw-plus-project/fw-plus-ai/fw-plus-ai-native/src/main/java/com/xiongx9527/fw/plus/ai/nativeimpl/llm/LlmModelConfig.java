package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

/** 单个模型端点配置。密钥仅保存在运行时对象中，不提供 toString 避免意外泄漏。 */
public final class LlmModelConfig {
    private final LlmVendor vendor;
    private final LlmProtocol protocol;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final String apiVersion;

    private LlmModelConfig(Builder b) {
        this.vendor = b.vendor;
        this.protocol = b.protocol;
        this.baseUrl = b.baseUrl;
        this.apiKey = b.apiKey;
        this.defaultModel = b.defaultModel;
        this.apiVersion = b.apiVersion;
    }

    public static Builder builder() { return new Builder(); }
    public LlmVendor getVendor() { return vendor; }
    public LlmProtocol getProtocol() { return protocol; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getDefaultModel() { return defaultModel; }
    public String getApiVersion() { return apiVersion; }

    public static final class Builder {
        private LlmVendor vendor;
        private LlmProtocol protocol;
        private String baseUrl;
        private String apiKey;
        private String defaultModel;
        private String apiVersion;
        public Builder vendor(LlmVendor value) { this.vendor = value; return this; }
        public Builder protocol(LlmProtocol value) { this.protocol = value; return this; }
        public Builder baseUrl(String value) { this.baseUrl = value; return this; }
        public Builder apiKey(String value) { this.apiKey = value; return this; }
        public Builder defaultModel(String value) { this.defaultModel = value; return this; }
        public Builder apiVersion(String value) { this.apiVersion = value; return this; }
        public LlmModelConfig build() {
            if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("llm baseUrl is required");
            return new LlmModelConfig(this);
        }
    }
}
