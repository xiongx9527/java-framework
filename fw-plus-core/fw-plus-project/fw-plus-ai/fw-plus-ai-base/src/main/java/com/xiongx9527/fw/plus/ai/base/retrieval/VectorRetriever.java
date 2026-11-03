package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.VectorStorePort;
import java.util.ArrayList;
import java.util.List;

/** 向量检索：EmbeddingPort 编码 query → VectorStorePort 最近邻检索。 */
public class VectorRetriever implements DocumentRetriever {

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
    public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
        List<Retrieved<Document>> retrieved = new ArrayList<Retrieved<Document>>();
        if (query == null || query.getText() == null || query.getText().trim().isEmpty()) {
            return retrieved;
        }
        double[] vector = embedding.embed(query.getText());
        if (vector == null) {
            return retrieved;
        }
        VectorSearchResult result = store.search(VectorSearchRequest.builder()
                .collection(query.getCollection())
                .vector(vector)
                .topK(query.getTopK())
                .candidateIds(query.getCandidateIds())
                .filter(query.getFilters())
                .build());
        if (result == null || result.isEmpty()) {
            return retrieved;
        }
        for (ScoredDocument hit : result.getHits()) {
            if (hit != null && hit.getDocument() != null) {
                retrieved.add(Retrieved.of(hit.getDocument(), hit.getScore(), query.getCollection()));
            }
        }
        return retrieved;
    }
}
