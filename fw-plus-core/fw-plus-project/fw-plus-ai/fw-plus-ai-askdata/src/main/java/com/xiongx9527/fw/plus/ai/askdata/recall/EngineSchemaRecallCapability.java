package com.xiongx9527.fw.plus.ai.askdata.recall;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import com.xiongx9527.fw.plus.ai.support.retrieval.IndexMode;
import com.xiongx9527.fw.plus.ai.support.retrieval.RetrievalStrategies;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于通用检索契约（fw-plus-ai-support）的表结构召回适配器：
 * {@link TableSchema} → {@link Document}（content = 表结构简述）→ {@link Retriever}
 * → {@link Retrieved} → {@link TableSchema} → {@link SchemaContext}。
 *
 * <p>只依赖 {@code Retriever<Document>}，因此向量、词法、混合、规则检索都能直接注入；
 * 阈值 / 重排 / topN 由注入的检索链负责，本类只做「表结构 ↔ 文档」的映射。</p>
 *
 * <p>无命中时返回空 {@link SchemaContext}（不兜底全量），由下游 {@code SqlGenInput.validate()} fail-fast。</p>
 */
public class EngineSchemaRecallCapability implements SchemaRecallCapability {

    private final Retriever<Document> retriever;
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
        this.topK = topK;
        this.topN = topN;
        this.scoreThreshold = scoreThreshold;
    }

    /**
     * Build a schema recall capability with an economical/quality strategy (IndexMode).
     * Delegates to RetrievalStrategies.build so ECONOMICAL only allows lexical recall with no rerank,
     * while HIGH_QUALITY enables semantic/hybrid recall with optional rerank.
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
        List<Retrieved<Document>> hits = retriever.retrieve(query);
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
}
