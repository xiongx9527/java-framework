package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.nativeimpl.store.StoreConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 单个向量存储后端配置。公共连接 / 超时 / 个性化参数复用 {@link StoreConfig}（通过 {@link #common} 持有并委托），
 * 本类只额外保存类型化的 {@link #vendor} / {@link #protocol}，不再重复声明 baseUrl / apiKey 等字段。
 * 密钥仅保存在底层 {@link StoreConfig} 中，不提供 toString 避免意外泄漏。
 */
public final class VectorStoreConfig {
    private final VectorStoreVendor vendor;
    private final VectorStoreProtocol protocol;
    private final StoreConfig common;
    private final PointIdStrategy pointIdStrategy;

    private VectorStoreConfig(Builder b) {
        this.vendor = b.vendor;
        this.protocol = b.protocol;
        this.common = b.common != null ? b.common : buildCommon(b);
        this.pointIdStrategy = b.pointIdStrategy != null ? b.pointIdStrategy : PointIdStrategy.UUID;
    }

    private static StoreConfig buildCommon(Builder b) {
        StoreConfig.Builder cb = StoreConfig.builder().baseUrl(b.baseUrl);
        if (b.apiKey != null) cb.apiKey(b.apiKey);
        if (b.defaultCollection != null) cb.defaultCollection(b.defaultCollection);
        if (b.connectTimeoutMillis != null) cb.connectTimeoutMillis(b.connectTimeoutMillis);
        if (b.readTimeoutMillis != null) cb.readTimeoutMillis(b.readTimeoutMillis);
        if (b.properties != null) cb.properties(b.properties);
        return cb.build();
    }

    public static Builder builder() { return new Builder(); }
    public VectorStoreVendor getVendor() { return vendor; }
    public VectorStoreProtocol getProtocol() { return protocol; }
    /** 底层公共配置，便于门面与端口进一步复用连接 / 超时 / 个性化参数。 */
    public StoreConfig getCommon() { return common; }
    public String getBaseUrl() { return common.getBaseUrl(); }
    public String getApiKey() { return common.getApiKey(); }
    public String getDefaultCollection() { return common.getDefaultCollection(); }
    public Integer getConnectTimeoutMillis() { return common.getConnectTimeoutMillis(); }
    public Integer getReadTimeoutMillis() { return common.getReadTimeoutMillis(); }
    public Map<String, Object> getProperties() { return common.getProperties(); }
    /** Qdrant point id 生成策略；默认 {@link PointIdStrategy#UUID}。 */
    public PointIdStrategy getPointIdStrategy() { return pointIdStrategy; }

    public static final class Builder {
        private VectorStoreVendor vendor;
        private VectorStoreProtocol protocol;
        private StoreConfig common;
        private PointIdStrategy pointIdStrategy;
        private String baseUrl;
        private String apiKey;
        private String defaultCollection;
        private Integer connectTimeoutMillis;
        private Integer readTimeoutMillis;
        private Map<String, Object> properties;
        public Builder vendor(VectorStoreVendor value) { this.vendor = value; return this; }
        public Builder protocol(VectorStoreProtocol value) { this.protocol = value; return this; }
        /** 直接复用一份已构建的公共配置（由 {@link StoreConfig} 提供），避免逐字段拷贝。 */
        public Builder common(StoreConfig value) { this.common = value; return this; }
        /** Qdrant point id 策略；{@code RAW} 直接以字符串 id 作 point id，{@code UUID}（默认）哈希生成。 */
        public Builder pointIdStrategy(PointIdStrategy value) { this.pointIdStrategy = value; return this; }
        public Builder baseUrl(String value) { this.baseUrl = value; return this; }
        public Builder apiKey(String value) { this.apiKey = value; return this; }
        public Builder defaultCollection(String value) { this.defaultCollection = value; return this; }
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
        public VectorStoreConfig build() {
            if (common == null && (baseUrl == null || baseUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("vector store baseUrl is required");
            }
            return new VectorStoreConfig(this);
        }
    }
}
