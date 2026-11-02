package com.xiongx9527.fw.plus.ai.store.embedding;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;

/**
 * 余弦相似度计算工具。
 *
 * <p><b>作用</b>：内存向量库（如内存 / 文件实现）需要自己算相似度；同时提供
 * 「余弦相似度 ↔ 相关度分数」的换算，让不同后端返回的分数落在同一语义区间。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.CosineSimilarity}。</p>
 *
 * @author xiongxiao
 */
public final class CosineSimilarity {

    /** 防止除零的下限。 */
    public static final float EPSILON = 0.000001f;

    private CosineSimilarity() {
    }

    /**
     * 计算两个向量的余弦相似度。
     *
     * @param embedding1 向量 1
     * @param embedding2 向量 2
     * @return 相似度，取值 [-1, 1]；任一向量为零向量时返回 0
     */
    public static double between(Embedding embedding1, Embedding embedding2) {
        return between(embedding1.vectorAsList(), embedding2.vectorAsList());
    }

    /**
     * 把余弦相似度换算成相关度分数。
     *
     * <p>换算公式为 {@code (1 + cosine) / 2}，把 [-1, 1] 映射到 [0, 1]，
     * 与多数向量库「越大越相关」的约定一致。</p>
     *
     * @param cosineSimilarity 余弦相似度
     * @return 相关度分数，取值 [0, 1]
     */
    public static double fromRelevanceScore(double cosineSimilarity) {
        return (1 + cosineSimilarity) / 2;
    }

    private static double between(java.util.List<Float> a, java.util.List<Float> b) {
        if (a == null || b == null || a.size() != b.size()) {
            throw new IllegalArgumentException("vectors must be non-null and have the same dimension");
        }
        double dotProduct = 0.0d;
        double normA = 0.0d;
        double normB = 0.0d;
        for (int i = 0; i < a.size(); i++) {
            float va = a.get(i) == null ? 0f : a.get(i);
            float vb = b.get(i) == null ? 0f : b.get(i);
            dotProduct += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator < EPSILON) {
            return 0.0d;
        }
        return dotProduct / denominator;
    }
}
