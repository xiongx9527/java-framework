package com.xiongx9527.fw.plus.ai.askdata.recall;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import com.xiongx9527.fw.plus.ai.support.retrieval.IndexMode;
import com.xiongx9527.fw.plus.ai.support.retrieval.RetrievalMode;
import com.xiongx9527.fw.plus.ai.support.retrieval.RetrievalStrategies;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于通用检索契约（fw-plus-ai-support）的表结构召回适配器：
 * TableSchema -> Document（content = 表结构简述）-> Retriever
 * -> Retrieved -> TableSchema -> SchemaContext。
 *
 * <p>只依赖 Retriever<Document>，因此向量、词法、混合、规则检索都能直接注入；
 * 阈值 / 重排 / topN 由注入的检索链负责，本类只做「表结构 <-> 文档」的映射。</p>
 *
 * <p>支持按请求的 IndexMode（质量型 / 经济型）在 recall 时选择检索链：经济型只走词法、不接重排；
 * 质量型可走语义 / 全文 / 混合 + 重排（由注入的检索器决定）。使用单检索器构造函数或 forStrategy
 * 时，链路在构造期固定，忽略请求级 indexMode。</p>
 *
 * <p>无命中时返回空 SchemaContext（不兜底全量），由下游 SqlGenInput.validate() fail-fast。</p>
 */
public class EngineSchemaRecallCapability implements SchemaRecallCapability {

    // 构造期固定的单一检索器（单检索器构造函数 / forStrategy），优先于组件式装配。
    private final Retriever<Document> retriever;
    // 组件式装配：分别注入向量 / 词法检索器与重排器，recall 时按 indexMode 现选链路。
    private final Retriever<Document> vector;
    private final Retriever<Document> lexical;
    private final Reranker<Document> reranker;
    private final int topK;
    private final int topN;
    private final double scoreThreshold;

    public EngineSchemaRecallCapability(Retriever<Document> retriever) {
        this(retriever, 10, 8, 0.0);
    }

    public EngineSchemaRecallCapability(Retriever<Document> retriever, int topK, int topN, double scoreThreshold) {
        if (retriever == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.engine_required"));
        }
        this.retriever = retriever;
        this.vector = null;
        this.lexical = null;
        this.reranker = null;
        this.topK = topK;
        this.topN = topN;
        this.scoreThreshold = scoreThreshold;
    }

    /**
     * 组件式构造函数：注入向量 / 词法检索器与重排器，recall 时按 SchemaRecallInput.indexMode
     * 选择检索链（经济型只走词法、质量型走语义/全文/混合 + 重排）。至少提供一个检索器。
     */
    public EngineSchemaRecallCapability(Retriever<Document> vector, Retriever<Document> lexical,
            Reranker<Document> reranker, int topK, int topN, double scoreThreshold) {
        if (vector == null && lexical == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.engine_required"));
        }
        this.retriever = null;
        this.vector = vector;
        this.lexical = lexical;
        this.reranker = reranker;
        this.topK = topK;
        this.topN = topN;
        this.scoreThreshold = scoreThreshold;
    }

    /**
     * 构造期固定的经济型/质量型召回能力（IndexMode）；链路在构造时确定，请求级 indexMode 被忽略。
     */
    public static SchemaRecallCapability forStrategy(IndexMode indexMode, Retriever<Document> vector,
            Retriever<Document> lexical, Reranker<Document> reranker, int topK, int topN, double threshold) {
        Retriever<Document> chain = RetrievalStrategies.build(indexMode, null, vector, lexical, reranker, null);
        return new EngineSchemaRecallCapability(chain, topK, topN, threshold);
    }

    @Override
    public SchemaContext recall(SchemaRecallInput input) {
        if (input == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.input_null"));
        }
        List<TableSchema> candidates = input.getCandidates();
        if (candidates == null || candidates.isEmpty()) {
            return SchemaContext.builder().build();
        }
        Map<String, TableSchema> byId = new LinkedHashMap<String, TableSchema>();
        List<Document> corpus = new ArrayList<Document>();
        for (TableSchema t : candidates) {
            String id = t.getName();
            byId.put(id, t);
            corpus.add(Document.builder().id(id).content(t.renderBrief()).build());
        }
        RetrievalQuery query = RetrievalQuery.builder()
                .text(input.getQuestion())
                .topK(topK)
                .topN(topN)
                .scoreThreshold(scoreThreshold)
                .corpus(corpus)
                .build();
        List<TableSchema> tables = new ArrayList<TableSchema>();
        Retriever<Document> active = resolveRetriever(input.getIndexMode());
        List<Retrieved<Document>> hits = active.retrieve(query);
        if (hits != null) {
            for (Retrieved<Document> hit : hits) {
                if (hit == null || hit.getItem() == null) {
                    continue;
                }
                TableSchema t = byId.get(hit.getItem().getId());
                if (t != null) {
                    tables.add(t);
                }
            }
        }
        return SchemaContext.builder().tables(tables).build();
    }

    private Retriever<Document> resolveRetriever(IndexMode requested) {
        if (retriever != null) {
            // 固定链路模式（单检索器构造 / forStrategy）：忽略请求级 indexMode。
            return retriever;
        }
        IndexMode index = requested == null ? IndexMode.HIGH_QUALITY : requested;
        if (index == IndexMode.ECONOMICAL) {
            if (lexical == null) {
                throw new IllegalStateException("economical index requires a lexical retriever");
            }
            return RetrievalStrategies.build(IndexMode.ECONOMICAL, null, null, lexical, null, null);
        }
        // 质量型：按可用检索器选择语义 / 全文 / 混合。
        if (vector != null && lexical != null) {
            return RetrievalStrategies.build(IndexMode.HIGH_QUALITY, null, vector, lexical, reranker, null);
        }
        if (vector != null) {
            return RetrievalStrategies.build(IndexMode.HIGH_QUALITY, RetrievalMode.SEMANTIC, vector, null, reranker, null);
        }
        if (lexical != null) {
            return RetrievalStrategies.build(IndexMode.HIGH_QUALITY, RetrievalMode.FULL_TEXT, null, lexical, reranker, null);
        }
        throw new IllegalStateException(I18nMessageResolver.resolveKey("askdata.contract.engine_required"));
    }
}
