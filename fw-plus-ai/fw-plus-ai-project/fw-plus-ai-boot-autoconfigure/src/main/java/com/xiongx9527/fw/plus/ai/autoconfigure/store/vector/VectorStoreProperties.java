package com.xiongx9527.fw.plus.ai.autoconfigure.store.vector;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 向量库配置属性（多实例模型）。绑定前缀 {@code fw-plus.ai.vector-store}。
 *
 * <p>{@code default} 为默认实例：未配置 base-url 时暴露一个 {@code configured()=false} 的占位端口，
 * 保证产品不配向量库也能启动；命名实例放在 {@code instances} 下，按 key 区分多个后端
 * （如嵌入库 / 记忆库）。</p>
 *
 * <pre>
 * fw-plus.ai.vector-store.default.base-url=http://localhost:6333
 * fw-plus.ai.vector-store.default.vendor=QDRANT
 * fw-plus.ai.vector-store.instances.memory.base-url=http://localhost:6334
 * fw-plus.ai.vector-store.instances.memory.vendor=QDRANT
 * </pre>
 */
@ConfigurationProperties("fw-plus.ai.vector-store")
public class VectorStoreProperties {

    /** 总开关；缺省 {@code true}。 */
    private boolean enabled = true;

    /** 默认实例。 */
    private VectorStoreInstance defaultInstance = new VectorStoreInstance();

    /** 命名实例集合；key 为实例名。 */
    private Map<String, VectorStoreInstance> instances = new LinkedHashMap<String, VectorStoreInstance>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public VectorStoreInstance getDefaultInstance() {
        return defaultInstance;
    }

    public void setDefaultInstance(VectorStoreInstance defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public Map<String, VectorStoreInstance> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, VectorStoreInstance> instances) {
        this.instances = instances;
    }
}
