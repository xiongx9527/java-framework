package com.xiongx9527.fw.plus.ai.autoconfigure.store.graph;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 图库配置属性（多实例模型）。绑定前缀 {@code fw-plus.ai.graph-store}。
 *
 * <p>{@code default} 为默认实例；命名实例放在 {@code instances} 下，按 key 区分多个图库后端。</p>
 *
 * <pre>
 * fw-plus.ai.graph-store.default.base-url=bolt://localhost:7687
 * fw-plus.ai.graph-store.default.vendor=NEO4J
 * fw-plus.ai.graph-store.instances.kg.base-url=bolt://localhost:7688
 * fw-plus.ai.graph-store.instances.kg.vendor=NEO4J
 * </pre>
 */
@ConfigurationProperties("fw-plus.ai.graph-store")
public class GraphStoreProperties {

    /** 总开关；缺省 {@code true}。 */
    private boolean enabled = true;

    /**
     * 默认实例。通过 JavaBean 属性名 {@code default} 暴露（Java 关键字无法作字段名），
     * 因此配置键为 {@code fw-plus.ai.graph-store.default.*}。
     */
    private GraphStoreInstance defaultInstance = new GraphStoreInstance();

    /** 命名实例集合；key 为实例名。 */
    private Map<String, GraphStoreInstance> instances = new LinkedHashMap<String, GraphStoreInstance>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 默认实例的 JavaBean 属性访问器（绑定键 {@code default}）。 */
    public GraphStoreInstance getDefault() {
        return defaultInstance;
    }

    public void setDefault(GraphStoreInstance defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public Map<String, GraphStoreInstance> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, GraphStoreInstance> instances) {
        this.instances = instances;
    }
}
