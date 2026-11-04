package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.DocumentRetriever;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.ArrayList;
import java.util.List;

/** 混合检索：多路并行（每路按 topK 截断）+ 融合。 */
public class HybridRetriever implements DocumentRetriever {

    private final List<Retriever<Document>> retrievers;

    private final List<Double> weights;

    private final FusionMode mode;

    public HybridRetriever(List<Retriever<Document>> retrievers, List<Double> weights, FusionMode mode) {
        if (retrievers == null || retrievers.isEmpty()) {
            throw new IllegalArgumentException("at least one retriever is required");
        }
        this.retrievers = retrievers;
        this.weights = weights;
        this.mode = mode == null ? FusionMode.WEIGHTED : mode;
    }

    @Override
    public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
        List<List<Retrieved<Document>>> lists = new ArrayList<List<Retrieved<Document>>>();
        for (Retriever<Document> r : retrievers) {
            lists.add(r.retrieve(query));
        }
        return Fusion.fuse(lists, weights, mode, query.getTopK(), Document::getId);
    }
}
