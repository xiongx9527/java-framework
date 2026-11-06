package com.xiongx9527.fw.plus.ai.autoconfigure.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对话记忆配置属性（多实例模型）。绑定前缀 {@code fw-plus.ai.memory}。
 *
 * <p>{@code default} 为默认实例：未配置 base-url 时由自动装配暴露一个占位端口，保证应用缺省也能启动；
 * 命名实例放在 {@code instances} 下，按 key 区分多个记忆后端（如 mem0 服务 / 向量库 / 进程内存）。</p>
 *
 * <pre>
 * fw-plus.ai.memory.default.protocol=MEM0
 * fw-plus.ai.memory.default.base-url=http://localhost:8888
 * fw-plus.ai.memory.default.api-key=xxx
 * fw-plus.ai.memory.instances.local.protocol=IN_MEMORY
 * </pre>
 */
@ConfigurationProperties("fw-plus.ai.memory")
public class ConversationMemoryProperties {

    /** 总开关；缺省 {@code true}。 */
    private boolean enabled = true;

    /**
     * 默认实例。通过 JavaBean 属性名 {@code default} 暴露（Java 关键字无法作字段名），
     * 配置键为 {@code fw-plus.ai.memory.default.*}。
     */
    private ConversationMemoryInstance defaultInstance = new ConversationMemoryInstance();

    /** 命名实例集合；key 为实例名。 */
    private Map<String, ConversationMemoryInstance> instances = new LinkedHashMap<String, ConversationMemoryInstance>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 默认实例的 JavaBean 属性访问器（绑定键 {@code default}）。 */
    public ConversationMemoryInstance getDefault() {
        return defaultInstance;
    }

    public void setDefault(ConversationMemoryInstance defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public Map<String, ConversationMemoryInstance> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, ConversationMemoryInstance> instances) {
        this.instances = instances;
    }
}
