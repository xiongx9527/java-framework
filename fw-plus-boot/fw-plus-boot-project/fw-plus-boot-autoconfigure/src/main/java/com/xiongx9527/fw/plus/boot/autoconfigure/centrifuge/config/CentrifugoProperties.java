package com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Centrifuge 配置，绑定前缀 {@code fw.plus.centrifuge}。
 */
@ConfigurationProperties("fw.plus.centrifuge")
public class CentrifugoProperties {

    /**
     * 是否启用 Centrifuge Starter，默认 true
     */
    private boolean enabled = true;

    /**
     * 单实例配置（兼容旧配置）
     */
    private SingleInstanceProperties single = new SingleInstanceProperties();

    /**
     * 多实例配置
     */
    private List<CentrifugoInstanceProperties> instances = new ArrayList<>();

    /**
     * Token 获取器 Bean 名称，为空时使用静态 token
     */
    private String tokenGetterBeanName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SingleInstanceProperties getSingle() {
        return single;
    }

    public void setSingle(SingleInstanceProperties single) {
        this.single = single;
    }

    public List<CentrifugoInstanceProperties> getInstances() {
        return instances;
    }

    public void setInstances(List<CentrifugoInstanceProperties> instances) {
        this.instances = instances;
    }

    public String getTokenGetterBeanName() {
        return tokenGetterBeanName;
    }

    public void setTokenGetterBeanName(String tokenGetterBeanName) {
        this.tokenGetterBeanName = tokenGetterBeanName;
    }

    /**
     * 单实例兼容配置。
     */
    public static class SingleInstanceProperties {

        /**
         * Centrifugo WebSocket 地址
         */
        private String endpoint = "ws://localhost:9000/connection/websocket";

        /**
         * 连接名称
         */
        private String name = "default";

        /**
         * 连接 token
         */
        private String token;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
