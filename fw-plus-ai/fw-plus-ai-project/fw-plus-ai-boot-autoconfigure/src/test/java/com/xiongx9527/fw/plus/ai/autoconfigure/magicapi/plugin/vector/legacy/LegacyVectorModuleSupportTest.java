package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.legacy;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyVectorModuleSupportTest {

    @Test
    public void shouldResolveLegacyProviderKeyThroughUnifiedRegistry() {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        EmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
        LegacyVectorModuleSupport support = new LegacyVectorModuleSupport(registry, "milvus");

        VectorStoreOperations operations = support.get("knowledge");

        assertSame(store, operations.getEmbeddingStore());
    }

    @Test
    public void shouldTellUserToMigrateWhenLegacyKeyIsUnknown() {
        LegacyVectorModuleSupport support = new LegacyVectorModuleSupport(emptyRegistry(), "qdrant");

        try {
            support.get("missing");
            fail("Expected MagicAPIVectorException");
        } catch (MagicAPIVectorException ex) {
            assertTrue(ex.getMessage().contains("import vector"));
            assertTrue(ex.getMessage().contains("missing"));
            assertTrue(ex.getMessage().contains("qdrant"));
        }
    }

    private EmbeddingStoreRegistry registryWith(String key, EmbeddingStore<TextSegment> store, String defaultKey) {
        Map<String, EmbeddingStore<TextSegment>> stores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        stores.put(key, store);
        return new TestEmbeddingStoreRegistry(stores, defaultKey);
    }

    private EmbeddingStoreRegistry emptyRegistry() {
        return new TestEmbeddingStoreRegistry(Collections.<String, EmbeddingStore<TextSegment>>emptyMap(), null);
    }

    private static class TestEmbeddingStoreRegistry implements EmbeddingStoreRegistry {

        private final Map<String, EmbeddingStore<TextSegment>> stores;
        private final String defaultKey;

        private TestEmbeddingStoreRegistry(Map<String, EmbeddingStore<TextSegment>> stores, String defaultKey) {
            this.stores = stores;
            this.defaultKey = defaultKey;
        }

        @Override
        public EmbeddingStore<TextSegment> get(String key) {
            EmbeddingStore<TextSegment> store = stores.get(key);
            if (store == null) {
                throw new IllegalArgumentException("Unknown embedding store: " + key);
            }
            return store;
        }

        @Override
        public EmbeddingStore<TextSegment> getDefault() {
            if (defaultKey == null) {
                throw new IllegalStateException("Default embedding store is not configured");
            }
            return get(defaultKey);
        }

        @Override
        public boolean contains(String key) {
            return stores.containsKey(key);
        }

        @Override
        public Set<String> keys() {
            return Collections.unmodifiableSet(stores.keySet());
        }
    }
}
