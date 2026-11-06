package com.xiongx9527.fw.plus.ai.autoconfigure.model;

/**
 * 单个向量（Embedding）模型实例配置。继承 {@link AbstractModelInstance} 的公共连接 / 鉴权 / 超时字段，
 * 并补充 Embedding 特有的 {@code dimension}。
 */
public class EmbeddingModelInstance extends AbstractModelInstance {

    /** 默认模型名。 */
    private String defaultModel;

    /** 向量维度；未知时留空，由后端实际探测。 */
    private Integer dimension;

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }
}
