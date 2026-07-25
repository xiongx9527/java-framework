package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.Test;
import org.ssssssss.script.MagicScriptContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VectorModuleTest {

    @Test
    public void shouldReturnOperationsForExplicitKey() {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        EmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
        VectorModule module = new VectorModule(registry);

        VectorStoreOperations operations = module.getDynamicAttribute("knowledge");

        assertSame(store, operations.getEmbeddingStore());
    }

    @Test
    public void shouldReturnDefaultOperationsWhenKeyIsMissing() {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        EmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
        VectorModule module = new VectorModule(registry);

        VectorStoreOperations operations = module.getDynamicModule(new MagicScriptContext());

        assertSame(store, operations.getEmbeddingStore());
    }

    @Test
    public void shouldRejectUnknownStoreKey() {
        VectorModule module = new VectorModule(registryWith("knowledge", new InMemoryEmbeddingStore<TextSegment>(), "knowledge"));

        try {
            module.getDynamicAttribute("unknown");
            fail("Expected MagicAPIVectorException");
        } catch (MagicAPIVectorException exception) {
            assertTrue(exception.getMessage().contains("unknown"));
        }
    }

    @Test
    public void shouldRejectMissingDefaultStore() {
        VectorModule module = new VectorModule(registryWith(null, null, null));

        try {
            module.getDynamicModule(new MagicScriptContext());
            fail("Expected MagicAPIVectorException");
        } catch (MagicAPIVectorException exception) {
            assertTrue(exception.getMessage().contains("default"));
        }
    }

    private EmbeddingStoreRegistry registryWith(String key, EmbeddingStore<TextSegment> store, String defaultKey) {
        Map<String, EmbeddingStore<TextSegment>> stores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        if (key != null) {
            stores.put(key, store);
        }
        return new TestEmbeddingStoreRegistry(stores, defaultKey);
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
