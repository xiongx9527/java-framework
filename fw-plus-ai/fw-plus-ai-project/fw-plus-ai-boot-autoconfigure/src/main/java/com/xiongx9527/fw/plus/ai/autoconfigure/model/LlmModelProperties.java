package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 模型配置属性（多实例模型）。绑定前缀 {@code fw-plus.ai.llm}。
 *
 * <p>{@code default} 为默认实例；命名实例放在 {@code instances} 下，按 key 区分多个模型后端
 * （如主模型 / 兜底模型）。未配置时由自动装配暴露 {@code @Primary} 占位端口，保证应用缺省可启动。</p>
 *
 * <pre>
 * fw-plus.ai.llm.default.protocol=OPENAI_CHAT_COMPLETIONS
 * fw-plus.ai.llm.default.base-url=https://api.openai.com/v1
 * fw-plus.ai.llm.default.api-key=sk-xxx
 * fw-plus.ai.llm.default.default-model=gpt-4o
 * </pre>
 */
@ConfigurationProperties("fw-plus.ai.llm")
public class LlmModelProperties {

    /** 总开关；缺省 {@code true}。 */
    private boolean enabled = true;

    /** 默认实例。通过 JavaBean 属性名 {@code default} 暴露（Java 关键字无法作字段名）。 */
    private LlmModelInstance defaultInstance = new LlmModelInstance();

    /** 命名实例集合；key 为实例名。 */
    private Map<String, LlmModelInstance> instances = new LinkedHashMap<String, LlmModelInstance>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 默认实例的 JavaBean 属性访问器（绑定键 {@code default}）。 */
    public LlmModelInstance getDefault() {
        return defaultInstance;
    }

    public void setDefault(LlmModelInstance defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public Map<String, LlmModelInstance> getInstances() {
        return instances;
    }

    public void setInstances(Map<String, LlmModelInstance> instances) {
        this.instances = instances;
    }
}
