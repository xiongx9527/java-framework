package com.xiongx9527.fw.plus.ai.store.embedding;

/**
 * 相关度分数换算工具。
 *
 * <p><b>作用</b>：把「余弦相似度」这一常见后端分数统一换算成 [0, 1] 的相关度分数，
 * 使跨向量库的 {@code minScore} 阈值具有可比性。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.RelevanceScore}。</p>
 *
 * @author xiongxiao
 */
public final class RelevanceScore {

    private RelevanceScore() {
    }

    /**
     * 由余弦相似度换算相关度分数。
     *
     * @param cosineSimilarity 余弦相似度，取值 [-1, 1]
     * @return 相关度分数，取值 [0, 1]
     */
    public static double fromCosineSimilarity(double cosineSimilarity) {
        return (1 + cosineSimilarity) / 2;
    }
}
