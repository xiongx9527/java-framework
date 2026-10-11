package com.xiongx9527.fw.plus.ai.sample.agentscope.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 样例使用的 Ollama 连接配置。
 *
 * <p>{@code AgentScope} 官方为各模型厂商分别提供 {@code *-spring-boot-starter}；本分发线不引入这些
 * starter，因此样例自行以 {@code @ConfigurationProperties} 承载连接参数，并构造
 * {@link io.agentscope.extensions.model.ollama.OllamaChatModel} 作为 {@code Model} bean。
 *
 * <p>配置示例（application.yaml）：
 *
 * <pre>{@code
 * sample:
 *   agentscope:
 *     ollama:
 *       base-url: http://192.168.1.100:11434
 *       model-name: gpt-oss:120b-cloud
 * }</pre>
 */
@ConfigurationProperties(prefix = "sample.agentscope.ollama")
public class SampleOllamaProperties {

    /** Ollama 服务地址，根路径即可（扩展内部会拼接 {@code /api/chat}）。 */
    private String baseUrl = "http://localhost:11434";

    /** 模型名，支持本地模型与云端模型（如 {@code gpt-oss:120b-cloud}）。 */
    private String modelName = "gpt-oss:120b-cloud";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
