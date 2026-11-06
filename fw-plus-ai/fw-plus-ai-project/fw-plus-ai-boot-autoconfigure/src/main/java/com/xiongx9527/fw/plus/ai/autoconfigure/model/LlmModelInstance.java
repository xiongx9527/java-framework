package com.xiongx9527.fw.plus.ai.autoconfigure.model;

/**
 * 单个 LLM 模型实例配置。继承 {@link AbstractModelInstance} 的公共连接 / 鉴权 / 超时字段，
 * 并补充 LLM 特有的 {@code defaultModel} 与 {@code apiVersion}。
 */
public class LlmModelInstance extends AbstractModelInstance {

    /** 默认模型名（如 gpt-4o、qwen-max）。 */
    private String defaultModel;

    /** API 版本（Azure OpenAI / Gemini 等需要）。 */
    private String apiVersion;

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}
