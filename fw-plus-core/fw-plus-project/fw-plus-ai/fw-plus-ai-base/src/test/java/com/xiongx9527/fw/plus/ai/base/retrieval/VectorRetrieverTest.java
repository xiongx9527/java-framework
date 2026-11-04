package com.xiongx9527.fw.plus.ai.base.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VectorRetrieverTest {

    /** 记录最后一次检索请求的假存储。 */
    private static class FakeStore implements VectorStorePort {

        private VectorSearchRequest last;

        private final List<ScoredDocument> hits;

        FakeStore(List<ScoredDocument> hits) {
            this.hits = hits;
        }

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
        public boolean configured() {
            return true;
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
        public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        }

        @Override
        public void delete(String collection, String id) {
        }

        @Override
        public VectorSearchResult search(VectorSearchRequest request) {
            this.last = request;
            return VectorSearchResult.of(hits);
        }
    }

    @Test
    void passesCollectionCandidatesAndFilters() {
        FakeStore store = new FakeStore(Arrays.asList(new ScoredDocument(Document.of("1", "t"), 0.8)));
        VectorRetriever retriever = new VectorRetriever(text -> new double[]{1.0, 0.0}, store);

        List<Retrieved<Document>> hits = retriever.retrieve(RetrievalQuery.builder()
                .text("q")
                .collection("askdata-core_table")
                .topK(5)
                .candidateIds(Arrays.asList("1", "2"))
                .filter("ds_id", 7L)
                .build());

        assertEquals(1, hits.size());
        assertEquals("askdata-core_table", store.last.getCollection());
        assertEquals(5, store.last.getTopK());
        assertEquals(2, store.last.getCandidateIds().size());
        assertTrue(store.last.getFilter().matches(java.util.Collections.singletonMap("ds_id", 7L)));
    }

    @Test
    void unavailableEmbeddingYieldsEmpty() {
        FakeStore store = new FakeStore(new ArrayList<ScoredDocument>());
        VectorRetriever retriever = new VectorRetriever(text -> null, store);

        assertTrue(retriever.retrieve(RetrievalQuery.builder().text("q").build()).isEmpty());
        assertTrue(store.last == null);
    }

    @Test
    void blankQuerySkipsStore() {
        FakeStore store = new FakeStore(new ArrayList<ScoredDocument>());
        VectorRetriever retriever = new VectorRetriever(text -> new double[]{1.0}, store);

        assertTrue(retriever.retrieve(RetrievalQuery.builder().text("  ").build()).isEmpty());
    }
}
