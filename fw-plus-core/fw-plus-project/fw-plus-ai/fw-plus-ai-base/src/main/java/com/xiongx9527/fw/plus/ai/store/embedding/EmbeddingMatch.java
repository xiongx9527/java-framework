package com.xiongx9527.fw.plus.ai.store.embedding;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;

/**
 * 向量召回匹配项：相似度得分 + 命中的向量与原始内容。
 *
 * <p><b>作用</b>：{@code EmbeddingStore#findRelevant} 的返回单元，携带得分用于排序与阈值过滤。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.store.embedding.EmbeddingMatch}
 * （{@code score()/embeddingId()/embedding()/embedded()}）。</p>
 *
 * @param <Embedded> 被嵌入对象类型，通常为 {@code TextSegment}
 * @author xiongxiao
 */
public class EmbeddingMatch<Embedded> {

    private final Double score;
    private final String embeddingId;
    private final Embedding embedding;
    private final Embedded embedded;

    /**
     * 构造匹配项。
     *
     * @param score       相似度得分
     * @param embeddingId 向量库中的记录 ID
     * @param embedding   向量
     * @param embedded    原始内容
     */
    public EmbeddingMatch(Double score, String embeddingId, Embedding embedding, Embedded embedded) {
        this.score = score;
        this.embeddingId = embeddingId;
        this.embedding = embedding;
        this.embedded = embedded;
    }

    /**
     * 相似度得分。
     *
     * @return 得分
     */
    public Double score() {
        return score;
    }

    /**
     * 向量库记录 ID。
     *
     * @return ID
     */
    public String embeddingId() {
        return embeddingId;
    }

    /**
     * 命中的向量。
     *
     * @return 向量
     */
    public Embedding embedding() {
        return embedding;
    }

    /**
     * 命中的原始内容。
     *
     * @return 原始内容
     */
    public Embedded embedded() {
        return embedded;
    }

    @Override
    public String toString() {
        return "EmbeddingMatch{score=" + score + ", embeddingId='" + embeddingId + "'}";
    }
}
