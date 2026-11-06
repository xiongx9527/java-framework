package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 单个对话记忆后端端点配置。密钥仅保存在运行时对象中，不提供 toString 避免意外泄漏。
 *
 * <p>与 {@code LlmModelConfig} 同构；{@code embeddingPort} / {@code vectorStorePort} 仅
 * {@link ConversationMemoryProtocol#VECTOR} 提供商使用——它们是指向框架已有端口的实例引用，
 * 而非可序列化的连接串。</p>
 */
public final class ConversationMemoryConfig {
    private final ConversationMemoryVendor vendor;
    private final ConversationMemoryProtocol protocol;
    private final String baseUrl;
    private final String apiKey;
    private final MemoryAuthScheme authScheme;
    private final Integer connectTimeoutMillis;
    private final Integer readTimeoutMillis;
    private final EmbeddingPort embeddingPort;
    private final VectorStorePort vectorStorePort;
    private final Map<String, Object> properties;

    private ConversationMemoryConfig(Builder b) {
        this.vendor = b.vendor;
        this.protocol = b.protocol;
        this.baseUrl = b.baseUrl;
        this.apiKey = b.apiKey;
        this.authScheme = b.authScheme;
        this.connectTimeoutMillis = b.connectTimeoutMillis;
        this.readTimeoutMillis = b.readTimeoutMillis;
        this.embeddingPort = b.embeddingPort;
        this.vectorStorePort = b.vectorStorePort;
        this.properties = b.properties == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(b.properties));
    }

    public static Builder builder() {
        return new Builder();
    }

    public ConversationMemoryVendor getVendor() {
        return vendor;
    }

    public ConversationMemoryProtocol getProtocol() {
        return protocol;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public MemoryAuthScheme getAuthScheme() {
        return authScheme;
    }

    public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public Integer getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public EmbeddingPort getEmbeddingPort() {
        return embeddingPort;
    }

    public VectorStorePort getVectorStorePort() {
        return vectorStorePort;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public static final class Builder {
        private ConversationMemoryVendor vendor;
        private ConversationMemoryProtocol protocol;
        private String baseUrl;
        private String apiKey;
        private MemoryAuthScheme authScheme = MemoryAuthScheme.API_KEY;
        private Integer connectTimeoutMillis;
        private Integer readTimeoutMillis;
        private EmbeddingPort embeddingPort;
        private VectorStorePort vectorStorePort;
        private Map<String, Object> properties;

        public Builder vendor(ConversationMemoryVendor value) {
            this.vendor = value;
            return this;
        }

        public Builder protocol(ConversationMemoryProtocol value) {
            this.protocol = value;
            return this;
        }

        public Builder baseUrl(String value) {
            this.baseUrl = value;
            return this;
        }

        public Builder apiKey(String value) {
            this.apiKey = value;
            return this;
        }

        public Builder authScheme(MemoryAuthScheme value) {
            this.authScheme = value;
            return this;
        }

        public Builder connectTimeoutMillis(Integer value) {
            this.connectTimeoutMillis = value;
            return this;
        }

        public Builder readTimeoutMillis(Integer value) {
            this.readTimeoutMillis = value;
            return this;
        }

        public Builder embeddingPort(EmbeddingPort value) {
            this.embeddingPort = value;
            return this;
        }

        public Builder vectorStorePort(VectorStorePort value) {
            this.vectorStorePort = value;
            return this;
        }

        public Builder property(String key, Object value) {
            if (this.properties == null) {
                this.properties = new HashMap<String, Object>();
            }
            this.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> value) {
            if (this.properties == null) {
                this.properties = new HashMap<String, Object>();
            }
            this.properties.putAll(value);
            return this;
        }

        public ConversationMemoryConfig build() {
            return new ConversationMemoryConfig(this);
        }
    }
}
