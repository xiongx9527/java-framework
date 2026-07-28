package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultEmbeddingStoreFactoryTest {

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
