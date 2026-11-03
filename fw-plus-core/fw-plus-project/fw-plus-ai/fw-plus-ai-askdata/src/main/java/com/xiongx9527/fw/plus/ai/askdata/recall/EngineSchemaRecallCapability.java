package com.xiongx9527.fw.plus.ai.askdata.recall;

import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.support.retrieval.Document;
import com.xiongx9527.fw.plus.ai.support.retrieval.RetrievalEngine;
import com.xiongx9527.fw.plus.ai.support.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.support.retrieval.ScoredDocument;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于通用 RAG 检索引擎（fw-plus-ai-support）的表结构召回适配器：
 * {@link TableSchema} → {@link Document}（content = 表结构简述）→ {@link RetrievalEngine}
 * → {@link ScoredDocument} → {@link TableSchema} → {@link SchemaContext}。
 *
 * <p>无命中时返回空 {@link SchemaContext}（不兜底全量），由下游 {@code SqlGenInput.validate()} fail-fast。</p>
 */
public class EngineSchemaRecallCapability implements SchemaRecallCapability {

    private final RetrievalEngine engine;
    private final int topK;
    private final int topN;
    private final double scoreThreshold;

    public EngineSchemaRecallCapability(RetrievalEngine engine) {
        this(engine, 10, 8, 0.0);
    }

    public EngineSchemaRecallCapability(RetrievalEngine engine, int topK, int topN, double scoreThreshold) {
        if (engine == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.engine_required"));
        }
        this.engine = engine;
        this.topK = topK;
        this.topN = topN;
        this.scoreThreshold = scoreThreshold;
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
        Map<String, TableSchema> byId = new LinkedHashMap<>();
        List<Document> corpus = new ArrayList<>();
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
        List<ScoredDocument> hits = engine.retrieve(query);
        List<TableSchema> tables = new ArrayList<>();
        for (ScoredDocument sd : hits) {
            TableSchema t = byId.get(sd.getDocument().getId());
            if (t != null) {
                tables.add(t);
            }
        }
        return SchemaContext.builder().tables(tables).build();
    }
}
