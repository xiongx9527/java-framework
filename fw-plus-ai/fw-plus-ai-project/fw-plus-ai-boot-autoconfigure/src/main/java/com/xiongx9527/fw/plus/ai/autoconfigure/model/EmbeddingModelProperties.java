package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 向量（Embedding）模型配置属性（多实例模型）。绑定前缀 {@code fw-plus.ai.embedding}。
 *
 * <pre>
 * fw-plus.ai.embedding.default.protocol=OPENAI
 * fw-plus.ai.embedding.default.base-url=https://api.openai.com/v1
 * fw-plus.ai.embedding.default.api-key=sk-xxx
 * fw-plus.ai.embedding.default.default-model=text-embedding-3-small
 * fw-plus.ai.embedding.default.dimension=1536
 * </pre>
 */
@ConfigurationProperties("fw-plus.ai.embedding")
public class EmbeddingModelProperties {

    /** 总开关；缺省 {@code true}。 */
    private boolean enabled = true;

    /** 默认实例。通过 JavaBean 属性名 {@code default} 暴露。 */
    private EmbeddingModelInstance defaultInstance = new EmbeddingModelInstance();

    /** 命名实例集合；key 为实例名。 */
    private Map<String, EmbeddingModelInstance> instances = new LinkedHashMap<String, EmbeddingModelInstance>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 默认实例的 JavaBean 属性访问器（绑定键 {@code default}）。 */
    public EmbeddingModelInstance getDefault() {
        return defaultInstance;
    }

    public void setDefault(EmbeddingModelInstance defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public Map<String, EmbeddingModelInstance> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, EmbeddingModelInstance> instances) {
        this.instances = instances;
    }
}
