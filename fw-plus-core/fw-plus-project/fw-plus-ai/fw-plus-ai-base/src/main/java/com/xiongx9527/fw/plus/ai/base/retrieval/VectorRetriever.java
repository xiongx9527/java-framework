package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.VectorStorePort;
import java.util.ArrayList;
import java.util.List;

/**
 * 向量检索：{@link EmbeddingPort} 编码 query → {@link VectorStorePort} 最近邻检索。
 */
public class VectorRetriever implements Retriever {

    private final EmbeddingPort embedding;

    private final VectorStorePort store;

    public VectorRetriever(EmbeddingPort embedding, VectorStorePort store) {
        if (embedding == null || store == null) {
            throw new IllegalArgumentException("embedding port and vector store port are required");
        }
        this.embedding = embedding;
        this.store = store;
    }

    @Override
    public List<ScoredDocument> retrieve(RetrievalQuery query) {
        if (query == null || query.getText() == null || query.getText().trim().isEmpty()) {
            return new ArrayList<ScoredDocument>();
        }
        double[] vector = embedding.embed(query.getText());
        if (vector == null) {
            return new ArrayList<ScoredDocument>();
        }
        List<ScoredDocument> hits = store.search(VectorSearchRequest.builder()
                .collection(query.getCollection())
                .vector(vector)
                .topK(query.getTopK())
                .candidateIds(query.getCandidateIds())
                .filter(query.getFilters())
                .build());
        return hits == null ? new ArrayList<ScoredDocument>() : hits;
    }
}
