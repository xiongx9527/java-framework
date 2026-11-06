package com.xiongx9527.fw.plus.ai.autoconfigure.model;

/**
 * 单个重排（Rerank）模型实例配置。继承 {@link AbstractModelInstance} 的公共字段，
 * 重排服务用 {@code endpoint} 语义承载于 {@link AbstractModelInstance#getBaseUrl()}（配置键仍为 {@code base-url}）。
 */
public class RerankModelInstance extends AbstractModelInstance {

    /** 默认模型名。 */
    private String defaultModel;

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }
}
