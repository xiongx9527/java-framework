package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

import com.xiongx9527.fw.plus.ai.nativeimpl.store.StoreConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 单个图存储后端配置。公共连接 / 超时 / 个性化参数复用 {@link StoreConfig}（通过 {@link #common} 持有并委托），
 * 本类只额外保存类型化的 {@link #vendor} / {@link #protocol}，不再重复声明 baseUrl / username / password 等字段。
 * 密钥仅保存在底层 {@link StoreConfig} 中，不提供 toString 避免意外泄漏。
 */
public final class GraphStoreConfig {
    private final GraphStoreVendor vendor;
    private final GraphStoreProtocol protocol;
    private final StoreConfig common;

    private GraphStoreConfig(Builder b) {
        this.vendor = b.vendor;
        this.protocol = b.protocol;
        this.common = b.common != null ? b.common : buildCommon(b);
    }

    private static StoreConfig buildCommon(Builder b) {
        StoreConfig.Builder cb = StoreConfig.builder().baseUrl(b.baseUrl);
        if (b.username != null) cb.username(b.username);
        if (b.password != null) cb.password(b.password);
        if (b.database != null) cb.database(b.database);
        if (b.connectTimeoutMillis != null) cb.connectTimeoutMillis(b.connectTimeoutMillis);
        if (b.readTimeoutMillis != null) cb.readTimeoutMillis(b.readTimeoutMillis);
        if (b.properties != null) cb.properties(b.properties);
        return cb.build();
    }

    public static Builder builder() { return new Builder(); }
    public GraphStoreVendor getVendor() { return vendor; }
    public GraphStoreProtocol getProtocol() { return protocol; }
    /** 底层公共配置，便于门面与端口进一步复用连接 / 超时 / 个性化参数。 */
    public StoreConfig getCommon() { return common; }
    public String getBaseUrl() { return common.getBaseUrl(); }
    public String getUsername() { return common.getUsername(); }
    public String getPassword() { return common.getPassword(); }
    public String getDatabase() { return common.getDatabase(); }
    public Integer getConnectTimeoutMillis() { return common.getConnectTimeoutMillis(); }
    public Integer getReadTimeoutMillis() { return common.getReadTimeoutMillis(); }
    public Map<String, Object> getProperties() { return common.getProperties(); }

    public static final class Builder {
        private GraphStoreVendor vendor;
        private GraphStoreProtocol protocol;
        private StoreConfig common;
        private String baseUrl;
        private String username;
        private String password;
        private String database;
        private Integer connectTimeoutMillis;
        private Integer readTimeoutMillis;
        private Map<String, Object> properties;
        public Builder vendor(GraphStoreVendor value) { this.vendor = value; return this; }
        public Builder protocol(GraphStoreProtocol value) { this.protocol = value; return this; }
        /** 直接复用一份已构建的公共配置（由 {@link StoreConfig} 提供），避免逐字段拷贝。 */
        public Builder common(StoreConfig value) { this.common = value; return this; }
        public Builder baseUrl(String value) { this.baseUrl = value; return this; }
        public Builder username(String value) { this.username = value; return this; }
        public Builder password(String value) { this.password = value; return this; }
        public Builder database(String value) { this.database = value; return this; }
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
        public GraphStoreConfig build() {
            if (common == null && (baseUrl == null || baseUrl.trim().isEmpty())) {
                throw new IllegalArgumentException("graph store baseUrl is required");
            }
            return new GraphStoreConfig(this);
        }
    }
}
