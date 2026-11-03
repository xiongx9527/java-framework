package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.ArrayList;
import java.util.List;

/** 混合检索：多路并行（每路按 topK 截断）+ 融合。 */
public class HybridRetriever implements Retriever {

    private final List<Retriever> retrievers;
    private final List<Double> weights;
    private final FusionMode mode;

    public HybridRetriever(List<Retriever> retrievers, List<Double> weights, FusionMode mode) {
        if (retrievers == null || retrievers.isEmpty()) {
            throw new IllegalArgumentException("at least one retriever is required");
        }
        this.retrievers = retrievers;
        this.weights = weights;
        this.mode = mode == null ? FusionMode.WEIGHTED : mode;
    }

    @Override
    public List<ScoredDocument> retrieve(RetrievalQuery query) {
        List<List<ScoredDocument>> lists = new ArrayList<>();
        for (Retriever r : retrievers) {
            lists.add(r.retrieve(query));
        }
        return Fusion.fuse(lists, weights, mode, query.getTopK());
    }
}
