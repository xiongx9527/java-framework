package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.VectorRetriever;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 按「索引模式 × 检索方式」组装检索链（对齐 Dify 的高质量索引 / 经济索引）。
 *
 * <p>组合规则：</p>
 * <ul>
 *   <li>经济索引：只有词法路，不允许语义 / 混合，也不接重排；</li>
 *   <li>高质量 + 语义：向量召回，可接重排；</li>
 *   <li>高质量 + 全文：词法召回（全文检索或关键字检索均可），可接重排；</li>
 *   <li>高质量 + 混合：向量与词法各召回 topK 后按权重融合，可接重排。</li>
 * </ul>
 *
 * <p>返回的检索链已包含阈值过滤与 topN 截断（{@link RetrievalPipeline}）。</p>
 */
public final class RetrievalStrategies {

    /** 混合检索默认权重：向量 0.7 / 词法 0.3（Dify 默认值）。 */
    public static final List<Double> DEFAULT_HYBRID_WEIGHTS =
            Collections.unmodifiableList(Arrays.asList(0.7, 0.3));

    private RetrievalStrategies() {
    }

    /**
     * 组装检索链。
     *
     * @param indexMode     索引模式，{@code null} 视为高质量
     * @param retrievalMode 检索方式，{@code null} 用索引模式的默认值
     * @param vector        向量检索器（经济索引可为 {@code null}）
     * @param lexical       词法检索器：全文检索（{@code FullTextRetriever}）或关键字检索
     *                      （{@code KeywordRetriever}），二者选其一注入
     * @param reranker      重排器，可 {@code null}；经济索引下会被忽略
     * @param weights       混合权重，{@code null} 用 {@link #DEFAULT_HYBRID_WEIGHTS}
     */
    public static Retriever<Document> build(IndexMode indexMode, RetrievalMode retrievalMode,
                                            Retriever<Document> vector, Retriever<Document> lexical,
                                            Reranker<Document> reranker, List<Double> weights) {
        IndexMode index = indexMode == null ? IndexMode.HIGH_QUALITY : indexMode;
        RetrievalMode mode = retrievalMode == null ? index.defaultMode() : retrievalMode;
        if (!index.supports(mode)) {
            throw new IllegalArgumentException("economical index supports lexical retrieval only");
        }
        Retriever<Document> base;
        switch (mode) {
            case SEMANTIC:
                if (vector == null) {
                    throw new IllegalArgumentException("vector retriever is required for semantic retrieval");
                }
                base = vector;
                break;
            case FULL_TEXT:
                if (lexical == null) {
                    throw new IllegalArgumentException("lexical retriever is required for full-text retrieval");
                }
                base = lexical;
                break;
            case HYBRID:
            default:
                if (vector == null || lexical == null) {
                    throw new IllegalArgumentException("both retrievers are required for hybrid retrieval");
                }
                base = new HybridRetriever(Arrays.asList(vector, lexical),
                        weights == null ? DEFAULT_HYBRID_WEIGHTS : weights, FusionMode.WEIGHTED);
                break;
        }
        Reranker<Document> effectiveReranker = index.supportsRerank() ? reranker : null;
        return RetrievalPipeline.of(base, effectiveReranker);
    }
}
