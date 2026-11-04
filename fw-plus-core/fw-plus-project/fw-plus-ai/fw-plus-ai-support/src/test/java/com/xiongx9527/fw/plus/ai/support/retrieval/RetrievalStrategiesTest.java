package com.xiongx9527.fw.plus.ai.support.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiongx9527.fw.plus.ai.base.retrieval.FullTextRetriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.VectorRetriever;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetrievalStrategiesTest {

    private final FullTextRetriever fullText = new FullTextRetriever();

    private final Document users = Document.of("users", "user account");

    private final Document orders = Document.of("orders", "order record");

    private Retriever<Document> vector(double score) {
        return new VectorRetriever(text -> new double[]{1.0}, new VectorStorePort() {
            @Override
            public boolean health() {
                return true;
            }

            @Override
            public String name() {
                return "fake";
            }

            @Override
            public com.xiongx9527.fw.plus.ai.base.store.StoreType type() {
                return com.xiongx9527.fw.plus.ai.base.store.StoreType.VECTOR;
            }

            @Override
            public String defaultCollection() {
                return "test";
            }

            @Override
            public void ensureCollection(String collection, int dimension) {
            }

            @Override
            public void dropCollection(String collection) {
            }

            @Override
            public void upsert(String collection, String id, double[] v, Map<String, Object> metadata) {
            }

            @Override
            public void delete(String collection, String id) {
            }

            @Override
            public VectorSearchResult search(VectorSearchRequest request) {
                return VectorSearchResult.of(Arrays.asList(new ScoredDocument(users, score), new ScoredDocument(orders, score / 2)));
            }
        });
    }

    @Test
    void economicalRejectsSemanticRetrieval() {
        assertThrows(IllegalArgumentException.class, () ->
                RetrievalStrategies.build(IndexMode.ECONOMICAL, RetrievalMode.SEMANTIC,
                        vector(0.9), fullText, null, null));
    }

    @Test
    void economicalIgnoresReranker() {
        Retriever<Document> chain = RetrievalStrategies.build(IndexMode.ECONOMICAL, null,
                null, fullText, (q, c) -> {
                    throw new IllegalStateException("rerank must not be called");
                }, null);

        List<Retrieved<Document>> hits = chain.retrieve(RetrievalQuery.builder()
                .text("user")
                .corpus(Arrays.asList(users, orders))
                .topN(5)
                .build());

        assertEquals("users", hits.get(0).getItem().getId());
    }

    @Test
    void hybridCombinesBothRoutes() {
        Retriever<Document> chain = RetrievalStrategies.build(IndexMode.HIGH_QUALITY, RetrievalMode.HYBRID,
                vector(0.2), fullText, null, null);

        List<Retrieved<Document>> hits = chain.retrieve(RetrievalQuery.builder()
                .text("user")
                .corpus(Arrays.asList(users, orders))
                .topN(5)
                .build());

        assertEquals("users", hits.get(0).getItem().getId());
    }

    @Test
    void semanticRequiresVectorRetriever() {
        assertThrows(IllegalArgumentException.class, () ->
                RetrievalStrategies.build(IndexMode.HIGH_QUALITY, RetrievalMode.SEMANTIC,
                        null, fullText, null, null));
    }
}
