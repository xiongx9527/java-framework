package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 重排（Rerank）模型配置属性（多实例模型）。绑定前缀 {@code fw-plus.ai.rerank}。
 *
 * <pre>
 * fw-plus.ai.rerank.default.protocol=BGE_RERANK
 * fw-plus.ai.rerank.default.base-url=https://api.xxx.com/v1
 * fw-plus.ai.rerank.default.api-key=sk-xxx
 * fw-plus.ai.rerank.default.default-model=bge-reranker-v2-m3
 * </pre>
 */
@ConfigurationProperties("fw-plus.ai.rerank")
public class RerankModelProperties {

    /** 总开关；缺省 {@code true}。 */
    private boolean enabled = true;

    /** 默认实例。通过 JavaBean 属性名 {@code default} 暴露。 */
    private RerankModelInstance defaultInstance = new RerankModelInstance();

    /** 命名实例集合；key 为实例名。 */
    private Map<String, RerankModelInstance> instances = new LinkedHashMap<String, RerankModelInstance>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 默认实例的 JavaBean 属性访问器（绑定键 {@code default}）。 */
    public RerankModelInstance getDefault() {
        return defaultInstance;
    }

    public void setDefault(RerankModelInstance defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public Map<String, RerankModelInstance> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, RerankModelInstance> instances) {
        this.instances = instances;
    }
}
