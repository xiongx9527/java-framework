package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;

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
    public void shouldRejectUnknownStoreKey() {
        EmbeddingStoreRegistry registry = new DefaultEmbeddingStoreRegistry(
                Collections.<String, EmbeddingStore<TextSegment>>emptyMap(), null);

        EmbeddingStoreException exception = assertThrows(EmbeddingStoreException.class, () -> registry.get("unknown"));

        assertEquals("Embedding store key is not registered: unknown", exception.getMessage());
    }

    @Test
    public void shouldRejectMissingDefaultStoreConfiguration() {
        EmbeddingStoreRegistry registry = new DefaultEmbeddingStoreRegistry(
                Collections.<String, EmbeddingStore<TextSegment>>emptyMap(), "");

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
    }
}
