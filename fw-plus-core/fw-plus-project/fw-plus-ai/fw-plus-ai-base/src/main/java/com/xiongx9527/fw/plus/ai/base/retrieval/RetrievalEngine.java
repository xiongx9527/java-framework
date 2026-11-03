package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import java.util.ArrayList;
import java.util.List;

/**
 * 检索引擎：检索 → 可选重排 → scoreThreshold 过滤 → 排序 → topN 截断。
 *
 * <p>与 Dify 对齐：阈值作用在融合 / 重排后的最终分上；{@code topK} 由检索器内部按每路上限使用。</p>
 *
 * <p>可选挂 {@link RetrievalListener}：查询开始、最终命中、失败都会派发，异常被吞不影响检索。</p>
 */
public class RetrievalEngine {

    private final Retriever retriever;

    private final Reranker reranker;

    private final List<RetrievalListener> listeners;

    public RetrievalEngine(Retriever retriever) {
        this(retriever, null, null);
    }

    public RetrievalEngine(Retriever retriever, Reranker reranker) {
        this(retriever, reranker, null);
    }

    public RetrievalEngine(Retriever retriever, Reranker reranker, List<RetrievalListener> listeners) {
        if (retriever == null) {
            throw new IllegalArgumentException("retriever is required");
        }
        this.retriever = retriever;
        this.reranker = reranker;
        this.listeners = listeners == null ? new ArrayList<RetrievalListener>() : listeners;
    }

    public List<ScoredDocument> retrieve(RetrievalQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        for (RetrievalListener listener : listeners) {
            try {
                listener.onQuery(query);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
        List<ScoredDocument> result;
        try {
            result = doRetrieve(query);
        } catch (RuntimeException e) {
            for (RetrievalListener listener : listeners) {
                try {
                    listener.onError(e.getMessage());
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
            throw e;
        }
        for (RetrievalListener listener : listeners) {
            try {
                listener.onHits(result);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
        return result;
    }

    private List<ScoredDocument> doRetrieve(RetrievalQuery query) {
        List<ScoredDocument> docs = retriever.retrieve(query);
        if (reranker != null && docs != null && !docs.isEmpty()) {
            docs = reranker.rerank(query.getText(), docs);
        }
        List<ScoredDocument> filtered = new ArrayList<ScoredDocument>();
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
            return new ArrayList<ScoredDocument>(filtered.subList(0, topN));
        }
        return filtered;
    }
}
