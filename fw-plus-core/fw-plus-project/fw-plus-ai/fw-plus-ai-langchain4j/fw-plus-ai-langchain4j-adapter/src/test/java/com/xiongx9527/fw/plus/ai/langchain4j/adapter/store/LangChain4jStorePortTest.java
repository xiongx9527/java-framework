package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LangChain4jStorePortTest {

    /** 只实现测试所需语义的内存 EmbeddingStore，避免引入上游 SPI / gson 依赖。 */
    private static class MemoryStore implements EmbeddingStore<TextSegment> {

        private static final class Entry {
            final String id;
            final Embedding embedding;
            final TextSegment segment;

            Entry(String id, Embedding embedding, TextSegment segment) {
                this.id = id;
                this.embedding = embedding;
                this.segment = segment;
            }
        }

        final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();

        /** 模拟 0.35 默认实现：search 忽略 filter。 */
        boolean ignoreFilter;

        /** 模拟 0.35 默认实现：remove* 抛 UnsupportedOperationException。 */
        boolean noRemove;

        private int seq;

        @Override
        public String add(Embedding embedding) {
            return add(embedding, null);
        }

        @Override
        public void add(String id, Embedding embedding) {
            entries.put(id, new Entry(id, embedding, null));
        }

        @Override
        public String add(Embedding embedding, TextSegment segment) {
            String id = "e" + (++seq);
            entries.put(id, new Entry(id, embedding, segment));
            return id;
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            List<String> ids = new ArrayList<String>();
            if (embeddings != null) {
                for (Embedding embedding : embeddings) {
                    ids.add(add(embedding));
                }
            }
            return ids;
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
            List<String> ids = new ArrayList<String>();
            if (embeddings != null) {
                for (int i = 0; i < embeddings.size(); i++) {
                    ids.add(add(embeddings.get(i), segments == null ? null : segments.get(i)));
                }
            }
            return ids;
        }

        @Override
        public void remove(String id) {
            if (noRemove) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            entries.remove(id);
        }

        @Override
        public void removeAll(Collection<String> ids) {
            if (ids != null) {
                for (String id : ids) {
                    remove(id);
                }
            }
        }

        @Override
        public void removeAll(dev.langchain4j.store.embedding.filter.Filter filter) {
            if (noRemove) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next().getValue();
                Metadata metadata = entry.segment == null ? new Metadata() : entry.segment.metadata();
                if (filter.test(metadata)) {
                    iterator.remove();
                }
            }
        }

        @Override
        public void removeAll() {
            entries.clear();
        }

        @Override
        public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<EmbeddingMatch<TextSegment>>();
            for (Entry entry : entries.values()) {
                Metadata metadata = entry.segment == null ? new Metadata() : entry.segment.metadata();
                if (!ignoreFilter && request.filter() != null && !request.filter().test(metadata)) {
                    continue;
                }
                double score = cosine(request.queryEmbedding().vector(), entry.embedding.vector());
                if (score < request.minScore()) {
                    continue;
                }
                matches.add(new EmbeddingMatch<TextSegment>(Double.valueOf(score), entry.id,
                        entry.embedding, entry.segment));
            }
            matches.sort((left, right) -> Double.compare(right.score(), left.score()));
            if (matches.size() > request.maxResults()) {
                matches = new ArrayList<EmbeddingMatch<TextSegment>>(
                        matches.subList(0, request.maxResults()));
            }
            return new EmbeddingSearchResult<TextSegment>(matches);
        }

        private static double cosine(float[] left, float[] right) {
            double dot = 0.0;
            double normLeft = 0.0;
            double normRight = 0.0;
            int length = Math.min(left.length, right.length);
            for (int i = 0; i < length; i++) {
                dot += left[i] * right[i];
                normLeft += left[i] * left[i];
                normRight += right[i] * right[i];
            }
            return normLeft == 0.0 || normRight == 0.0
                    ? 0.0 : dot / Math.sqrt(normLeft * normRight);
        }
    }

    private static Map<String, Object> metadata(String id, int dsId, String tag) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("content", "text of " + id);
        values.put("dsId", Integer.valueOf(dsId));
        values.put("tag", tag);
        return values;
    }

    private static LangChain4jStorePort store(MemoryStore backend) {
        return new LangChain4jStorePort(backend, "test", "c1");
    }

    private static VectorSearchRequest query(double[] vector) {
        return VectorSearchRequest.builder().collection("c1").vector(vector).topK(5).build();
    }

    @Test
    void upsertAndSearchRoundTrip() {
        MemoryStore backend = new MemoryStore();
        LangChain4jStorePort store = store(backend);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "a"));

        VectorSearchResult result = store.search(query(new double[]{1.0, 0.0}));

        assertEquals(1, result.size());
        Document doc = result.getHits().get(0).getDocument();
        assertEquals("d1", doc.getId());
        assertEquals("text of d1", doc.getContent());
        assertTrue(doc.getMetadata().containsKey("dsId"));
        assertTrue(result.getHits().get(0).getScore() > 0.9);
        assertEquals("c1", store.defaultCollection());
    }

    @Test
    void collectionsAreIsolated() {
        MemoryStore backend = new MemoryStore();
        LangChain4jStorePort store = store(backend);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "a"));
        store.upsert("c2", "d2", new double[]{1.0, 0.0}, metadata("d2", 7, "a"));

        assertEquals(1, store.search(query(new double[]{1.0, 0.0})).size());
        assertEquals(1, store.search(VectorSearchRequest.builder()
                .collection("c2").vector(new double[]{1.0, 0.0}).topK(5).build()).size());
    }

    @Test
    void supportedFilterIsPushedDown() {
        MemoryStore backend = new MemoryStore();
        LangChain4jStorePort store = store(backend);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "a"));
        store.upsert("c1", "d2", new double[]{1.0, 0.0}, metadata("d2", 8, "b"));

        VectorSearchResult result = store.search(VectorSearchRequest.builder()
                .collection("c1").vector(new double[]{1.0, 0.0}).topK(5)
                .filter(Filter.builder().eq("dsId", 7).build())
                .build());

        assertEquals(1, result.size());
        assertEquals("d1", result.getHits().get(0).getDocument().getId());
    }

    @Test
    void unsupportedFilterFallsBackToPostFiltering() {
        MemoryStore backend = new MemoryStore();
        LangChain4jStorePort store = store(backend);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "quarterly-report"));
        store.upsert("c1", "d2", new double[]{1.0, 0.0}, metadata("d2", 7, "weekly"));

        VectorSearchResult result = store.search(VectorSearchRequest.builder()
                .collection("c1").vector(new double[]{1.0, 0.0}).topK(5)
                .filter(Filter.builder().contains("tag", "quarterly").build())
                .build());

        assertEquals(1, result.size());
        assertEquals("d1", result.getHits().get(0).getDocument().getId());
    }

    @Test
    void dimensionMismatchIsRejected() {
        MemoryStore backend = new MemoryStore();
        LangChain4jStorePort store = store(backend);
        store.ensureCollection("c1", 3);

        assertThrows(VectorStorePort.DimensionMismatchException.class,
                () -> store.upsert("c1", "d1", new double[]{1.0, 0.0}, null));
        assertEquals(Integer.valueOf(3), store.collectionDimension("c1"));
    }

    @Test
    void deleteAndDropCollection() {
        MemoryStore backend = new MemoryStore();
        LangChain4jStorePort store = store(backend);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "a"));

        store.delete("c1", "d1");
        assertTrue(store.search(query(new double[]{1.0, 0.0})).isEmpty());

        store.upsert("c1", "d2", new double[]{1.0, 0.0}, metadata("d2", 7, "a"));
        store.dropCollection("c1");
        assertTrue(store.search(query(new double[]{1.0, 0.0})).isEmpty());
    }

    /** 模拟 Neo4jEmbeddingStore：search 默认实现忽略 filter，客户端过滤仍须生效。 */
    @Test
    void filterIsAppliedClientSideWhenBackendIgnoresIt() {
        MemoryStore backend = new MemoryStore();
        backend.ignoreFilter = true;
        LangChain4jStorePort store = store(backend);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "a"));
        store.upsert("c1", "d2", new double[]{1.0, 0.0}, metadata("d2", 8, "b"));
        store.upsert("c2", "d3", new double[]{1.0, 0.0}, metadata("d3", 7, "c"));

        VectorSearchResult result = store.search(VectorSearchRequest.builder()
                .collection("c1").vector(new double[]{1.0, 0.0}).topK(5)
                .filter(Filter.builder().eq("dsId", 7).build())
                .build());

        assertEquals(1, result.size());
        assertEquals("d1", result.getHits().get(0).getDocument().getId());
    }

    @Test
    void unsupportedRemoveFailsLoudly() {
        MemoryStore backend = new MemoryStore();
        backend.noRemove = true;
        LangChain4jStorePort store = store(backend);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "a"));

        assertThrows(UnsupportedOperationException.class, () -> store.delete("c1", "d1"));
    }

    @Test
    void injectedRemoverHandlesDelete() {
        MemoryStore backend = new MemoryStore();
        backend.noRemove = true;
        final List<String> removed = new ArrayList<String>();
        EmbeddingRemover remover = new EmbeddingRemover() {
            @Override
            public void remove(String embeddingId) {
                removed.add(embeddingId);
                backend.entries.remove(embeddingId);
            }

            @Override
            public void removeAll(dev.langchain4j.store.embedding.filter.Filter filter) {
                // 测试只覆盖按 id 删除
            }
        };
        LangChain4jStorePort store =
                new LangChain4jStorePort(backend, "test", "c1", false, remover);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "a"));

        store.delete("c1", "d1");

        assertEquals(1, removed.size());
        assertTrue(store.search(query(new double[]{1.0, 0.0})).isEmpty());
    }

    @Test
    void pushdownFiltersCanBeEnabled() {
        MemoryStore backend = new MemoryStore();
        LangChain4jStorePort store =
                new LangChain4jStorePort(backend, "test", "c1", true, null);
        store.upsert("c1", "d1", new double[]{1.0, 0.0}, metadata("d1", 7, "a"));
        store.upsert("c1", "d2", new double[]{1.0, 0.0}, metadata("d2", 8, "b"));

        VectorSearchResult result = store.search(VectorSearchRequest.builder()
                .collection("c1").vector(new double[]{1.0, 0.0}).topK(5)
                .filter(Filter.builder().eq("dsId", 7).build())
                .build());

        assertEquals(1, result.size());
        assertEquals("d1", result.getHits().get(0).getDocument().getId());
    }
}
