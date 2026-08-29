package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultLangChain4jEmbeddingStoreAdapterTest {

    @Test
    public void shouldExposeFrameworkForStrictNeutralSelection() {
        assertEquals("langchain4j", new DefaultLangChain4jEmbeddingStoreAdapter().getFramework());
    }

    @Test
    public void shouldPassTheNeutralDefinitionToTheProviderAdapter() {
        FakeEmbeddingStore nativeStore = new FakeEmbeddingStore();
        FakeAdapter provider = new FakeAdapter("milvus", nativeStore);
        AiEmbeddingStoreDefinition definition = definition("knowledge", "milvus");

        AiEmbeddingStore store = adapter("milvusProvider", provider).create(definition);

        assertSame(definition, provider.supportedDefinition);
        assertSame(definition, provider.createdDefinition);
        assertEquals("knowledge", store.getKey());
        assertEquals("langchain4j", store.getFramework());
        assertEquals("milvus", store.getProvider());
        assertEquals(AiEmbeddingStoreType.VECTOR, store.getType());
    }

    @Test
    public void shouldExposeLangChain4jFrameworkWhenDefinitionOmitsIt() {
        FakeAdapter provider = new FakeAdapter("milvus", new FakeEmbeddingStore());
        AiEmbeddingStoreDefinition definition = definition("knowledge", "milvus");
        definition.setFramework(null);

        AiEmbeddingStore store = adapter("milvusProvider", provider).create(definition);

        assertEquals("langchain4j", store.getFramework());
    }

    @Test
    public void shouldRejectMultipleMatchingProviderAdapters() {
        Map<String, LangChain4jEmbeddingStoreProviderAdapter> providers =
                new LinkedHashMap<String, LangChain4jEmbeddingStoreProviderAdapter>();
        providers.put("first", new FakeAdapter("milvus", new FakeEmbeddingStore()));
        providers.put("second", new FakeAdapter("milvus", new FakeEmbeddingStore()));

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultLangChain4jEmbeddingStoreAdapter(providers)
                        .create(definition("knowledge", "milvus")));

        assertTrue(exception.getMessage().contains("Multiple"));
        assertTrue(exception.getMessage().contains("first"));
        assertTrue(exception.getMessage().contains("second"));
        assertTrue(exception.getMessage().contains("knowledge"));
        assertTrue(exception.getMessage().contains("milvus"));
    }

    @Test
    public void shouldDeduplicateTheSameProviderAdapterInstance() {
        FakeAdapter provider = new FakeAdapter("milvus", new FakeEmbeddingStore());
        Map<String, LangChain4jEmbeddingStoreProviderAdapter> providers =
                new LinkedHashMap<String, LangChain4jEmbeddingStoreProviderAdapter>();
        providers.put("primary", provider);
        providers.put("alias", provider);

        AiEmbeddingStore store = new DefaultLangChain4jEmbeddingStoreAdapter(providers)
                .create(definition("knowledge", "milvus"));

        assertEquals("knowledge", store.getKey());
        assertEquals(1, provider.createCalls);
    }

    @Test
    public void shouldRejectUnknownProviderWithNeutralContext() {
        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> new DefaultLangChain4jEmbeddingStoreAdapter()
                        .create(definition("knowledge", "unknown")));

        assertTrue(exception.getMessage().contains("knowledge"));
        assertTrue(exception.getMessage().contains("langchain4j"));
        assertTrue(exception.getMessage().contains("unknown"));
        assertTrue(exception.getMessage().contains("VECTOR"));
    }

    @Test
    public void shouldPreserveProviderSupportFailure() {
        FailingSupportAdapter provider = new FailingSupportAdapter();

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> adapter("failing", provider).supports(definition("knowledge", "milvus")));

        assertSame(provider.failure, exception.getCause());
        assertFalse(exception.getMessage().contains(provider.failure.getMessage()));
    }

    @Test
    public void shouldPreserveProviderCreationFailure() {
        FakeAdapter provider = new FakeAdapter("milvus", null);

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> adapter("failing", provider).create(definition("knowledge", "milvus")));

        assertSame(provider.failure, exception.getCause());
        assertFalse(exception.getMessage().contains(provider.failure.getMessage()));
    }

    @Test
    public void shouldPreserveDocumentContentOrRejectUnsupportedExplicitId() {
        FakeEmbeddingStore nativeStore = new FakeEmbeddingStore();
        AiEmbeddingStore store = adapter("milvus", new FakeAdapter("milvus", nativeStore))
                .create(definition("knowledge.alias", "milvus"));
        AiEmbeddingDocument document = new AiEmbeddingDocument(null, "hello",
                Collections.<String, Object>singletonMap("tenant", "demo"),
                new AiEmbeddingVector(new float[]{1.0f, 2.0f}));

        assertEquals("generated-1", store.add(document));
        assertEquals(Collections.singletonList("generated-2"), store.addAll(Collections.singletonList(document)));
        store.remove("doc-1");
        store.removeAll(Arrays.asList("doc-1", "doc-2"));
        AiEmbeddingSearchResult result = store.search(
                new AiEmbeddingQuery(new AiEmbeddingVector(new float[]{1.0f, 2.0f}), 3, 0.7));

        assertArrayEquals(new float[]{1.0f, 2.0f}, nativeStore.addedEmbedding.vector());
        assertEquals("hello", nativeStore.addedSegment.text());
        assertEquals("demo", nativeStore.addedSegment.metadata().getString("tenant"));
        assertArrayEquals(new float[]{1.0f, 2.0f}, nativeStore.searchRequest.queryEmbedding().vector());
        assertEquals(3, nativeStore.searchRequest.maxResults());
        assertEquals(0.7, nativeStore.searchRequest.minScore());
        assertEquals("match-1", result.getMatches().get(0).getEmbeddingId());
        assertEquals("matched", result.getMatches().get(0).getDocument().getText());
        assertFalse(hasPublicNativeAccessor(store));

        AiEmbeddingDocument explicitId = new AiEmbeddingDocument("doc-1", "must-not-be-lost",
                Collections.<String, Object>singletonMap("tenant", "secret"),
                new AiEmbeddingVector(new float[]{1.0f, 2.0f}));
        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> store.add(explicitId));

        assertTrue(exception.getMessage().contains("explicit id"));
        assertEquals(0, nativeStore.addWithIdCalls);
    }

    @Test
    public void shouldRejectFiltersUnsupportedByLangChain4jStore() {
        FakeEmbeddingStore nativeStore = new FakeEmbeddingStore();
        AiEmbeddingStore store = adapter("milvus", new FakeAdapter("milvus", nativeStore))
                .create(definition("knowledge", "milvus"));
        AiEmbeddingQuery query = new AiEmbeddingQuery(new AiEmbeddingVector(new float[]{1.0f, 2.0f}),
                3, 0.7, Collections.<String, Object>singletonMap("projectId", "project-1"));

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> store.search(query));

        assertFalse(store.supportsFilters());
        assertTrue(exception.getMessage().contains("filters"));
        assertTrue(exception.getMessage().contains("knowledge"));
        assertNull(nativeStore.searchRequest);
    }

    private DefaultLangChain4jEmbeddingStoreAdapter adapter(
            String name, LangChain4jEmbeddingStoreProviderAdapter provider) {
        return new DefaultLangChain4jEmbeddingStoreAdapter(
                Collections.singletonMap(name, provider));
    }

    private AiEmbeddingStoreDefinition definition(String key, String provider) {
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

    private static class FakeAdapter implements LangChain4jEmbeddingStoreProviderAdapter {

        private final String provider;
        private final EmbeddingStore<TextSegment> store;
        private final IllegalStateException failure = new IllegalStateException("sensitive adapter failure");
        private AiEmbeddingStoreDefinition supportedDefinition;
        private AiEmbeddingStoreDefinition createdDefinition;
        private int createCalls;

        private FakeAdapter(String provider, EmbeddingStore<TextSegment> store) {
            this.provider = provider;
            this.store = store;
        }

        @Override
        public boolean supports(AiEmbeddingStoreDefinition definition) {
            this.supportedDefinition = definition;
            return provider.equals(definition.getProvider());
        }

        @Override
        public EmbeddingStore<TextSegment> create(AiEmbeddingStoreDefinition definition) {
            this.createdDefinition = definition;
            this.createCalls++;
            if (store == null) {
                throw failure;
            }
            return store;
        }
    }

    private static class FailingSupportAdapter implements LangChain4jEmbeddingStoreProviderAdapter {

        private final IllegalStateException failure = new IllegalStateException("sensitive support failure");

        @Override
        public boolean supports(AiEmbeddingStoreDefinition definition) {
            throw failure;
        }

        @Override
        public EmbeddingStore<TextSegment> create(AiEmbeddingStoreDefinition definition) {
            return null;
        }
    }

    private static class FakeEmbeddingStore implements EmbeddingStore<TextSegment> {

        private EmbeddingSearchRequest searchRequest;
        private Embedding addedEmbedding;
        private TextSegment addedSegment;
        private int addWithIdCalls;
        private int generatedIdSequence;

        @Override
        public String add(Embedding embedding) {
            return null;
        }

        @Override
        public void add(String id, Embedding embedding) {
            addWithIdCalls++;
        }

        @Override
        public String add(Embedding embedding, TextSegment embedded) {
            this.addedEmbedding = embedding;
            this.addedSegment = embedded;
            return "generated-" + (++generatedIdSequence);
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
