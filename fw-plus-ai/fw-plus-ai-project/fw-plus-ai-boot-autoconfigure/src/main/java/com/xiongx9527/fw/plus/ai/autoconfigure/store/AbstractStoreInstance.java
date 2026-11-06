package com.xiongx9527.fw.plus.ai.autoconfigure.store;

import com.xiongx9527.fw.plus.ai.nativeimpl.store.StoreConfig;
import org.springframework.util.StringUtils;

/**
 * 向量库 / 图库共有配置属性的基类。
 *
 * <p>两种存储后端的自动装配共享同一套连接 / 鉴权 / 超时字段与「未配置占位」语义；
 * 子类仅补充各自特有字段（如向量库的 {@code point-id-strategy}、图库的 {@code database}）
 * 并声明各自的 {@code @ConfigurationProperties} 前缀。所有 getter/setter 由本类统一提供，
 * Spring 绑定时会写到具体子类实例（继承可见）。</p>
 */
public abstract class AbstractStoreInstance {

    /** 是否启用自动装配；缺省 {@code true}。 */
    private boolean enabled = true;

    /** 厂商（如 QDRANT / NEO4J）；与 protocol 二选一，二者皆有时以 protocol 为准。 */
    private String vendor;

    /** 协议（如 QDRANT / NEO4J）；与 vendor 二选一，二者皆有时以 protocol 为准。 */
    private String protocol;

    /** 后端 base URL（PgVector 为 JDBC URL）；配置了即视为已启用。 */
    private String baseUrl;

    /** 用户名（Redis ACL / Neo4j / Nebula / PgVector / Elasticsearch 鉴权）。 */
    private String username;

    /** 密码（同上）。 */
    private String password;

    /** 连接超时（毫秒）；null 交由端口使用默认值。 */
    private Integer connectTimeoutMillis;

    /** 读取超时（毫秒）；null 交由端口使用默认值。 */
    private Integer readTimeoutMillis;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(Integer connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Integer getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(Integer readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    /** 是否已配置后端（base-url 非空即视为已配置）。 */
    public boolean isConfigured() {
        return StringUtils.hasText(baseUrl);
    }

    /**
     * 用本类公共字段构建 {@link StoreConfig}（用户名 / 密码 / 超时由子类在其配置里额外补充 database 等）。
     * 子类经 {@code .common(...)} 接入各自配置。
     */
    public StoreConfig.Builder commonBuilder() {
        StoreConfig.Builder b = StoreConfig.builder()
                .baseUrl(baseUrl)
                .username(username)
                .password(password)
                .connectTimeoutMillis(connectTimeoutMillis)
                .readTimeoutMillis(readTimeoutMillis);
        return b;
    }
}
