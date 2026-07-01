package com.xx9527.fw.plus.boot.autoconfigure.config.event;

import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.Map;

/**
 * 通用配置变更事件，当底层配置中心检测到配置变更时发布。
 * 兼容 Spring Cloud Config、Apollo、Nacos 等多种配置中心。
 * 应用可通过 {@code @EventListener} 或 {@code @OnConfigChange} 监听此事件。
 */
public class ConfigChangeEvent extends ApplicationEvent {

    private final Map<String, Object> properties;

    public ConfigChangeEvent(Object source, Map<String, Object> properties) {
        super(source);
        this.properties = Collections.unmodifiableMap(properties);
    }

    /**
     * 发生变更的配置项（只读）。
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
}
