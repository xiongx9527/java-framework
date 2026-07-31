package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreRegistry;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultEmbeddingStoreRegistryTest {

    @Test
    public void shouldResolveMultipleStoresProviderAliasAndDefaultKeyThroughNeutralRegistry() {
        AiEmbeddingStoreDefinition knowledge = definition("knowledge.alias");
        AiEmbeddingStoreDefinition archive = definition("archive.alias");
        Map<String, EmbeddingStore<TextSegment>> nativeStores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        nativeStores.put("knowledge.alias", new FakeEmbeddingStore());
        nativeStores.put("archive.alias", new FakeEmbeddingStore());
        AiEmbeddingStoreAdapter adapter = new DefaultEmbeddingStoreFactory(
                Collections.<EmbeddingStoreProviderAdapter>singletonList(
                        new AliasAdapter(nativeStores)));

        DefaultAiEmbeddingStoreRegistry registry = new DefaultAiEmbeddingStoreRegistry(
                Arrays.asList(knowledge, archive), "archive.alias", Collections.singletonList(adapter));

        AiEmbeddingStore knowledgeStore = registry.get("knowledge.alias");
        AiEmbeddingStore archiveStore = registry.get("archive.alias");
        assertSame(archiveStore, registry.getDefault());
        assertTrue(knowledgeStore != archiveStore);
        assertEquals("archive.alias", registry.getDefaultKey());
        assertEquals("milvus-compatible", knowledgeStore.getProvider());
        assertEquals("milvus-compatible", archiveStore.getProvider());
    }

    private AiEmbeddingStoreDefinition definition(String key) {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey(key);
        definition.setFramework("langchain4j");
        definition.setProvider("milvus-compatible");
        definition.setType(AiEmbeddingStoreType.VECTOR);
        definition.setEnabled(true);
        return definition;
    }

    @Test
    public void shouldReturnStoreByKeyAndDefaultStore() {
        EmbeddingStore<TextSegment> knowledge = new FakeEmbeddingStore();
        EmbeddingStore<TextSegment> local = new FakeEmbeddingStore();
        Map<String, EmbeddingStore<TextSegment>> stores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        stores.put("knowledge", knowledge);
        stores.put("local-cache", local);

        EmbeddingStoreRegistry registry = new DefaultEmbeddingStoreRegistry(stores, "knowledge");

        assertSame(knowledge, registry.get("knowledge"));
        assertSame(local, registry.get("local-cache"));
        assertSame(knowledge, registry.getDefault());
        assertTrue(registry.contains("knowledge"));
        assertTrue(registry.keys().contains("local-cache"));
    }

    @Test
    public void shouldRejectUnknownStoreKeyOrNullStore() {
        EmbeddingStoreRegistry registry = new DefaultEmbeddingStoreRegistry(
                Collections.<String, EmbeddingStore<TextSegment>>emptyMap(), null);

        EmbeddingStoreException exception = assertThrows(EmbeddingStoreException.class, () -> registry.get("unknown"));

        assertEquals("Embedding store key is not registered: unknown", exception.getMessage());

        Map<String, EmbeddingStore<TextSegment>> nullStores =
                new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        nullStores.put("null-store", null);
        EmbeddingStoreRegistry nullRegistry = new DefaultEmbeddingStoreRegistry(nullStores, "null-store");

        EmbeddingStoreException getException = assertThrows(EmbeddingStoreException.class,
                () -> nullRegistry.get("null-store"));
        EmbeddingStoreException defaultException = assertThrows(EmbeddingStoreException.class,
                nullRegistry::getDefault);

        assertTrue(getException.getMessage().contains("null-store"));
        assertTrue(defaultException.getMessage().contains("null-store"));
    }

    @Test
    public void shouldRejectMissingDefaultStoreConfiguration() {
        EmbeddingStoreRegistry registry = new DefaultEmbeddingStoreRegistry(
                Collections.<String, EmbeddingStore<TextSegment>>emptyMap(), "");

        EmbeddingStoreException exception = assertThrows(EmbeddingStoreException.class, registry::getDefault);

        assertEquals("Default embedding store is not configured", exception.getMessage());
    }

    @Test
    public void shouldRejectNullDefaultStoreConfiguration() {
        EmbeddingStoreRegistry registry = new DefaultEmbeddingStoreRegistry(
                Collections.<String, EmbeddingStore<TextSegment>>emptyMap(), null);

        EmbeddingStoreException exception = assertThrows(EmbeddingStoreException.class, registry::getDefault);

        assertEquals("Default embedding store is not configured", exception.getMessage());
    }

    @Test
    public void shouldRejectInvalidDefaultStoreKey() {
        EmbeddingStoreRegistry registry = new DefaultEmbeddingStoreRegistry(
                Collections.<String, EmbeddingStore<TextSegment>>emptyMap(), "knowledge");

        EmbeddingStoreException exception = assertThrows(EmbeddingStoreException.class, registry::getDefault);

        assertEquals("Default embedding store key is invalid: knowledge", exception.getMessage());
    }

    private static class FakeEmbeddingStore implements EmbeddingStore<TextSegment> {

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
        public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            return new EmbeddingSearchResult<TextSegment>(Collections.emptyList());
        }
    }

    private static class AliasAdapter implements EmbeddingStoreProviderAdapter {
        private final Map<String, EmbeddingStore<TextSegment>> stores;

        private AliasAdapter(Map<String, EmbeddingStore<TextSegment>> stores) {
            this.stores = stores;
        }

        @Override
        public boolean supports(String provider) {
            return "milvus-compatible".equals(provider);
        }

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
            return stores.get(definition.getKey());
        }
    }
}
