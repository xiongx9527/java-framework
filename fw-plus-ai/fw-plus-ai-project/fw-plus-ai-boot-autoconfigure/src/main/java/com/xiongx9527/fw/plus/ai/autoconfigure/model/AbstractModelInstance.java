package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import org.springframework.util.StringUtils;

/**
 * 模型 / 记忆后端实例的通用属性：连接、鉴权、超时。子类补充厂商特有的字段
 * （如 embedding 的 dimension、记忆的 authScheme）。
 *
 * <p>与 {@code store.AbstractStoreInstance} 同构，仅抽取连接层公共字段，避免每个模型重复声明。</p>
 */
public abstract class AbstractModelInstance {

    /** 厂商；协议显式配置时由协议决定具体端口，厂商仅用于回退路由。 */
    private String vendor;

    /** 显式协议；厂商名称不能代替协议名称。 */
    private String protocol;

    /** 服务地址（重排等使用 endpoint 语义，但配置键统一为 {@code base-url}）。 */
    private String baseUrl;

    /** 鉴权密钥。 */
    private String apiKey;

    /** 连接超时（毫秒）。 */
    private Integer connectTimeoutMillis;

    /** 读取超时（毫秒）。 */
    private Integer readTimeoutMillis;

    public boolean isConfigured() {
        return StringUtils.hasText(baseUrl);
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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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
}
