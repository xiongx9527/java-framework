package com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.config;

/**
 * 单个 Centrifugo 实例配置。
 */
public class CentrifugoInstanceProperties {

    /**
     * 实例名
     */
    private String name;

    /**
     * Centrifugo WebSocket 地址
     */
    private String endpoint;

    /**
     * 连接名称
     */
    private String clientName;

    /**
     * 连接 token
     */
    private String token;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
