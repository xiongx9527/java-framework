package com.xiongx9527.fw.plus.ai.model.embedding;

/**
 * 维度感知的嵌入模型：可声明已知维度，避免每次调用都探测。
 *
 * <p><b>作用</b>：向量库建集合、索引规划都需要提前知道维度。实现可在构造时传入已知维度，
 * 未知时由 {@link #dimension()} 现算。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel}
 * （{@code protected Integer dimension} + {@code knownDimension()} + {@code dimension()}）。</p>
 *
 * @author xiongxiao
 */
public abstract class DimensionAwareEmbeddingModel implements EmbeddingModel {

    /** 已知维度；{@code null} 表示未知。 */
    protected Integer dimension;

    /** 创建维度未知的模型。 */
    public DimensionAwareEmbeddingModel() {
    }

    /**
     * 已知维度。
     *
     * @return 维度；未知时为 {@code null}
     */
    protected Integer knownDimension() {
        return dimension;
    }

    /**
     * 向量维度：已知则直接返回，否则由实现探测并缓存。
     *
     * @return 维度
     */
    @Override
    public int dimension() {
        if (dimension != null) {
            return dimension;
        }
        dimension = embed("test").content().dimension();
        return dimension;
    }
}
