package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.DefaultRetrievalCapability;
import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalCapability;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.VectorRetriever;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
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
 * <p>web / 搜索引擎检索是「额外的独立一路」：传入后，它与上面按索引模式装配出来的基础链再做一次
 * 加权融合，成为最终的检索来源组合。web 路由不参与重排器（重排只作用于基础链内部），
 * 最终仍然由 {@link RetrievalPipeline} 统一做阈值过滤与 topN 截断。</p>
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

    /** web 路由默认权重：基础链 0.7 / web 0.3。 */
    public static final double DEFAULT_WEB_WEIGHT = 0.3;

    /**
     * 组装检索链，并把 web / 搜索引擎检索作为额外一路与基础链融合。
     *
     * <p>{@code web} 为 {@code null} 时等价于 {@link #build(IndexMode, RetrievalMode, Retriever, Retriever, Reranker, List)}
     * （即没有 web 路）。否则先按索引模式 / 检索方式装配出基础链（含重排 / 阈值 / topN），
     * 再与 web 检索器按 {@link #DEFAULT_WEB_WEIGHT} 做加权融合，最后由外层统一做阈值过滤与 topN 截断。</p>
     *
     * @param web web / 搜索引擎检索器，可 {@code null}
     */
    public static Retriever<Document> build(IndexMode indexMode, RetrievalMode retrievalMode,
                                            Retriever<Document> vector, Retriever<Document> lexical,
                                            Retriever<Document> web, Reranker<Document> reranker,
                                            List<Double> weights) {
        return build(indexMode, retrievalMode, vector, lexical, web, reranker, weights, DEFAULT_WEB_WEIGHT);
    }

    /**
     * 组装检索链并指定 web 路由权重。
     *
     * @param web       web / 搜索引擎检索器，可 {@code null}
     * @param webWeight web 路由权重，范围 [0, 1]，基础链权重为 {@code 1 - webWeight}
     */
    public static Retriever<Document> build(IndexMode indexMode, RetrievalMode retrievalMode,
                                            Retriever<Document> vector, Retriever<Document> lexical,
                                            Retriever<Document> web, Reranker<Document> reranker,
                                            List<Double> weights, double webWeight) {
        Retriever<Document> base = build(indexMode, retrievalMode, vector, lexical, reranker, weights);
        if (web == null) {
            return base;
        }
        if (webWeight < 0 || webWeight > 1) {
            throw new IllegalArgumentException("webWeight must be in [0, 1]");
        }
        double baseWeight = 1.0 - webWeight;
        return new HybridRetriever(Arrays.asList(base, web),
                Arrays.asList(baseWeight, webWeight), FusionMode.WEIGHTED);
    }

    /**
     * 组装检索链并包装为 {@link RetrievalCapability}（与 {@link #build} 同义，但返回能力接口而非底层契约）。
     */
    public static RetrievalCapability<Document> capability(IndexMode indexMode, RetrievalMode retrievalMode,
                                                          Retriever<Document> vector, Retriever<Document> lexical,
                                                          Retriever<Document> web, Reranker<Document> reranker,
                                                          List<Double> weights, double webWeight) {
        return new DefaultRetrievalCapability<Document>(
                build(indexMode, retrievalMode, vector, lexical, web, reranker, weights, webWeight));
    }
}
