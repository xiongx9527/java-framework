package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.ArrayList;
import java.util.List;

/** 向量检索：EmbeddingPort 编码 query → VectorIndexPort 检索。 */
public class VectorRetriever implements Retriever {

    private final EmbeddingPort embedding;
    private final VectorIndexPort index;

    public VectorRetriever(EmbeddingPort embedding, VectorIndexPort index) {
        if (embedding == null || index == null) {
            throw new IllegalArgumentException("embedding and index are required");
        }
        this.embedding = embedding;
        this.index = index;
    }

    @Override
    public List<ScoredDocument> retrieve(RetrievalQuery query) {
        if (query == null || query.getText() == null || query.getText().trim().isEmpty()) {
            return new ArrayList<>();
        }
        float[] vector = embedding.embed(query.getText());
        List<ScoredDocument> hits = index.search(vector, query.getTopK(), query.getFilters());
        return hits == null ? new ArrayList<>() : hits;
    }
}
