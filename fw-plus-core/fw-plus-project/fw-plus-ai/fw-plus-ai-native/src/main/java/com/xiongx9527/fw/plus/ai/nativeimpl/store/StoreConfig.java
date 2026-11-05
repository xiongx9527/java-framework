package com.xiongx9527.fw.plus.ai.nativeimpl.store;

import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一存储配置：覆盖向量库与图库两类后端，由 {@link StoreResolver} 按 {@link #storeType} 派发到具体 resolver。
 *
 * <p>厂商 / 协议用字符串声明（大小写敏感，按各自枚举名匹配），便于调用方从一份配置里同时描述两类存储；
 * 字段按后端类型取舍：向量库用 {@link #apiKey} + {@link #defaultCollection}，图库用 {@link #username} /
 * {@link #password} / {@link #database}。其余为公共连接与超时参数。密钥不提供 toString 避免意外泄漏。</p>
 *
 * <p>本类同时作为各后端配置（{@code VectorStoreConfig} / {@code GraphStoreConfig}）的公共字段载体：
 * 后端配置通过组合复用这里的连接 / 超时 / 个性化参数，避免重复声明。{@link #storeType} 仅由门面
 * {@link StoreResolver} 在派发时校验，本类 {@link #build()} 不强制要求它，以便被后端配置直接持有。</p>
 */
public final class StoreConfig {
    private final StoreType storeType;
    private final String vendor;
    private final String protocol;
    private final String baseUrl;
    private final String apiKey;
    private final String username;
    private final String password;
    private final String database;
    private final String defaultCollection;
    /** 公共参数：连接超时（毫秒），null 表示交由端口使用默认值。 */
    private final Integer connectTimeoutMillis;
    /** 公共参数：读取超时（毫秒），null 表示交由端口使用默认值。 */
    private final Integer readTimeoutMillis;
    /** 个性化参数：厂商/协议特有的额外配置，原样透传给端口实现。 */
    private final Map<String, Object> properties;

    private StoreConfig(Builder b) {
        this.storeType = b.storeType;
        this.vendor = b.vendor;
        this.protocol = b.protocol;
        this.baseUrl = b.baseUrl;
        this.apiKey = b.apiKey;
        this.username = b.username;
        this.password = b.password;
        this.database = b.database;
        this.defaultCollection = b.defaultCollection;
        this.connectTimeoutMillis = b.connectTimeoutMillis;
        this.readTimeoutMillis = b.readTimeoutMillis;
        this.properties = b.properties == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(b.properties));
    }

    public static Builder builder() { return new Builder(); }
    public StoreType getStoreType() { return storeType; }
    public String getVendor() { return vendor; }
    public String getProtocol() { return protocol; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDatabase() { return database; }
    public String getDefaultCollection() { return defaultCollection; }
    public Integer getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public Integer getReadTimeoutMillis() { return readTimeoutMillis; }
    public Map<String, Object> getProperties() { return properties; }

    public static final class Builder {
        private StoreType storeType;
        private String vendor;
        private String protocol;
        private String baseUrl;
        private String apiKey;
        private String username;
        private String password;
        private String database;
        private String defaultCollection;
        private Integer connectTimeoutMillis;
        private Integer readTimeoutMillis;
        private Map<String, Object> properties;
        public Builder storeType(StoreType value) { this.storeType = value; return this; }
        public Builder vendor(String value) { this.vendor = value; return this; }
        public Builder protocol(String value) { this.protocol = value; return this; }
        public Builder baseUrl(String value) { this.baseUrl = value; return this; }
        public Builder apiKey(String value) { this.apiKey = value; return this; }
        public Builder username(String value) { this.username = value; return this; }
        public Builder password(String value) { this.password = value; return this; }
        public Builder database(String value) { this.database = value; return this; }
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
        public StoreConfig build() {
            if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("store baseUrl is required");
            return new StoreConfig(this);
        }
    }
}
