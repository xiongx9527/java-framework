package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.ArrayList;
import java.util.List;

/**
 * 检索引擎：检索 → 可选重排 → scoreThreshold 过滤 → 排序 → topN 截断。
 * 与 Dify 对齐：阈值作用在融合/重排后的最终分上；topK 由检索器内部按每路上限使用。
 */
public class RetrievalEngine {

    private final Retriever retriever;
    private final Reranker reranker;

    public RetrievalEngine(Retriever retriever) {
        this(retriever, null);
    }

    public RetrievalEngine(Retriever retriever, Reranker reranker) {
        if (retriever == null) {
            throw new IllegalArgumentException("retriever is required");
        }
        this.retriever = retriever;
        this.reranker = reranker;
    }

    public List<ScoredDocument> retrieve(RetrievalQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        List<ScoredDocument> docs = retriever.retrieve(query);
        if (reranker != null && docs != null && !docs.isEmpty()) {
            docs = reranker.rerank(query.getText(), docs);
        }
        List<ScoredDocument> filtered = new ArrayList<>();
        if (docs != null) {
            double threshold = query.getScoreThreshold();
            for (ScoredDocument sd : docs) {
                if (threshold <= 0.0 || sd.getScore() >= threshold) {
                    filtered.add(sd);
                }
            }
        }
        filtered.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int topN = query.getTopN();
        if (topN > 0 && filtered.size() > topN) {
            return new ArrayList<>(filtered.subList(0, topN));
        }
        return filtered;
    }
}
