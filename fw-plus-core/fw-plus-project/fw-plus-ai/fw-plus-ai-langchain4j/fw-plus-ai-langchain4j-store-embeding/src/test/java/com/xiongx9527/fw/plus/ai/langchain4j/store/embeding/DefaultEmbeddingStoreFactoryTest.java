package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultEmbeddingStoreFactoryTest {

    @Test
    public void shouldExposeCompleteNeutralStoreWithoutNativeAccessor() {
        FakeEmbeddingStore nativeStore = new FakeEmbeddingStore();
        AiEmbeddingStoreAdapter adapter = new DefaultEmbeddingStoreFactory(
                Collections.<EmbeddingStoreProviderAdapter>singletonList(new FakeAdapter("milvus-compatible", nativeStore)));
        AiEmbeddingStoreDefinition definition = neutralDefinition("knowledge.alias", "milvus-compatible");

        AiEmbeddingStore store = adapter.create(definition);
        AiEmbeddingDocument document = new AiEmbeddingDocument("doc-1", "hello",
                Collections.<String, Object>singletonMap("tenant", "demo"),
                new AiEmbeddingVector(new float[]{1.0f, 2.0f}));

        assertEquals("doc-1", store.add(document));
        assertEquals(Collections.singletonList("doc-1"), store.addAll(Collections.singletonList(document)));
        store.remove("doc-1");
        store.removeAll(Arrays.asList("doc-1", "doc-2"));
        AiEmbeddingSearchResult result = store.search(
                new AiEmbeddingQuery(new AiEmbeddingVector(new float[]{1.0f, 2.0f}), 3, 0.7));

        assertEquals("knowledge.alias", store.getKey());
        assertEquals("langchain4j", store.getFramework());
        assertEquals("milvus-compatible", store.getProvider());
        assertEquals(AiEmbeddingStoreType.VECTOR, store.getType());
        assertArrayEquals(new float[]{1.0f, 2.0f}, nativeStore.searchRequest.queryEmbedding().vector());
        assertEquals(3, nativeStore.searchRequest.maxResults());
        assertEquals(0.7, nativeStore.searchRequest.minScore());
        assertEquals("match-1", result.getMatches().get(0).getEmbeddingId());
        assertEquals("matched", result.getMatches().get(0).getDocument().getText());
        assertFalse(hasPublicNativeAccessor(store));
    }

    @Test
    public void shouldCreateStoreWithMatchingProviderAdapter() {
        EmbeddingStoreDefinition definition = definition("knowledge", "milvus");
        EmbeddingStore<TextSegment> store = new FakeEmbeddingStore();
        EmbeddingStoreProviderAdapter adapter = new FakeAdapter("milvus", store);

        EmbeddingStoreFactory factory = new DefaultEmbeddingStoreFactory();

        assertSame(store, factory.create(definition, Collections.singletonList(adapter)));
    }

    @Test
    public void shouldRejectUnknownProvider() {
        EmbeddingStoreFactory factory = new DefaultEmbeddingStoreFactory();

        EmbeddingStoreException exception = assertThrows(EmbeddingStoreException.class,
                () -> factory.create(definition("knowledge", "unknown"), Collections.<EmbeddingStoreProviderAdapter>emptyList()));

        assertTrue(exception.getMessage().contains("unknown"));
    }

    @Test
    public void shouldIncludeProviderAndKeyWhenAdapterCreationFails() {
        EmbeddingStoreDefinition definition = definition("knowledge", "milvus");
        EmbeddingStoreProviderAdapter adapter = new FakeAdapter("milvus", null);
        EmbeddingStoreFactory factory = new DefaultEmbeddingStoreFactory();

        EmbeddingStoreException exception = assertThrows(EmbeddingStoreException.class,
                () -> factory.create(definition, Collections.singletonList(adapter)));

        assertTrue(exception.getMessage().contains("milvus"));
        assertTrue(exception.getMessage().contains("knowledge"));
    }

    @Test
    public void shouldIncludeProviderAndKeyWhenAdapterSupportCheckFails() {
        EmbeddingStoreDefinition definition = definition("knowledge", "milvus");
        EmbeddingStoreProviderAdapter adapter = new FailingSupportAdapter();
        EmbeddingStoreFactory factory = new DefaultEmbeddingStoreFactory();

        EmbeddingStoreException exception = assertThrows(EmbeddingStoreException.class,
                () -> factory.create(definition, Collections.singletonList(adapter)));

        assertTrue(exception.getMessage().contains("milvus"));
        assertTrue(exception.getMessage().contains("knowledge"));
    }

    private EmbeddingStoreDefinition definition(String key, String provider) {
        EmbeddingStoreDefinition definition = new EmbeddingStoreDefinition();
        definition.setKey(key);
        definition.setProvider(provider);
        return definition;
    }

    private AiEmbeddingStoreDefinition neutralDefinition(String key, String provider) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("dimension", 2);
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey(key);
        definition.setFramework("langchain4j");
        definition.setProvider(provider);
        definition.setType(AiEmbeddingStoreType.VECTOR);
        definition.setEnabled(true);
        definition.setProperties(properties);
        return definition;
    }

    private boolean hasPublicNativeAccessor(AiEmbeddingStore store) {
        for (Method method : store.getClass().getMethods()) {
            if (method.getReturnType().getName().startsWith("dev.langchain4j.")) {
                return true;
            }
        }
        return false;
    }

    private static class FakeAdapter implements EmbeddingStoreProviderAdapter {

        private final String provider;
        private final EmbeddingStore<TextSegment> store;

        private FakeAdapter(String provider, EmbeddingStore<TextSegment> store) {
            this.provider = provider;
            this.store = store;
        }

        @Override
        public boolean supports(String provider) {
            return this.provider.equals(provider);
        }

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
            if (store == null) {
                throw new IllegalStateException("adapter failure");
            }
            return store;
        }
    }

    private static class FailingSupportAdapter implements EmbeddingStoreProviderAdapter {

        @Override
        public boolean supports(String provider) {
            throw new IllegalStateException("adapter support failure");
        }

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
            return null;
        }
    }

    private static class FakeEmbeddingStore implements EmbeddingStore<TextSegment> {

        private EmbeddingSearchRequest searchRequest;

        @Override
        public String add(Embedding embedding) {
            return null;
        }

        @Override
        public void add(String id, Embedding embedding) {
        }

        @Override
        public String add(Embedding embedding, TextSegment embedded) {
            return null;
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            return Collections.emptyList();
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
            return Collections.emptyList();
        }

        @Override
        public void remove(String id) {
        }

        @Override
        public void removeAll(java.util.Collection<String> ids) {
        }

        @Override
        public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            this.searchRequest = request;
            TextSegment segment = TextSegment.from("matched", new dev.langchain4j.data.document.Metadata(
                    Collections.<String, Object>singletonMap("tenant", "demo")));
            EmbeddingMatch<TextSegment> match = new EmbeddingMatch<TextSegment>(0.9, "match-1",
                    Embedding.from(new float[]{1.0f, 2.0f}), segment);
            return new EmbeddingSearchResult<TextSegment>(Collections.singletonList(match));
        }
    }
}
