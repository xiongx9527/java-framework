package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.ssssssss.script.MagicScript;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.MagicScriptEngine;
import org.ssssssss.script.MagicScriptEngineFactory;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VectorModuleTest {

    @Test
    public void shouldReturnOperationsForExplicitKey() {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        EmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
        DynamicAttribute<VectorModule, VectorStoreOperations> module = new VectorModule(registry);

        VectorModule keyedModule = module.getDynamicAttribute("knowledge");

        assertStore(keyedModule.getOperations().getEmbeddingStore(), "knowledge");
        assertNotSame(store, keyedModule.getOperations().getEmbeddingStore());
    }

    @Test
    public void shouldResolveExplicitKeyThroughMagicScriptDynamicAttributeAccess() {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        VectorModule module = new VectorModule(registryWith("knowledge", store, "knowledge"));
        MagicScriptContext context = new MagicScriptContext();
        context.set("vector", module);

        Object result = MagicScript.create("return vector['knowledge'];", new MagicScriptEngine(new MagicScriptEngineFactory()))
                .execute(context);

        assertTrue(result instanceof VectorModule);
        assertStore(((VectorModule) result).getOperations().getEmbeddingStore(), "knowledge");
    }

    @Test
    public void shouldReturnDefaultOperationsWhenKeyIsMissing() {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        EmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
        VectorModule module = new VectorModule(registry);

        VectorStoreOperations operations = module.getDynamicModule(new MagicScriptContext());

        assertStore(operations.getEmbeddingStore(), "default");
    }

    @Test
    public void shouldExposeSpringBootDefaultStoreThroughGetEmbeddingStore() {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        VectorModule module = new VectorModule(registryWith("knowledge", store, "knowledge"));

        assertStore(module.getEmbeddingStore(), "default");
    }

    @Test
    public void shouldExposeExplicitStoreThroughDynamicAttributeGetEmbeddingStore() {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<TextSegment>();
        VectorModule module = new VectorModule(registryWith("knowledge", store, "knowledge"));

        assertStore(module.getDynamicAttribute("knowledge").getEmbeddingStore(), "knowledge");
    }

    @Test
    public void shouldRejectUnknownStoreKey() {
        VectorModule module = new VectorModule(registryWith("knowledge", new InMemoryEmbeddingStore<TextSegment>(), "knowledge"));

        MagicAPIVectorException exception = assertThrows(MagicAPIVectorException.class,
                () -> module.getDynamicAttribute("unknown"));
        assertTrue(exception.getMessage().contains("unknown"));
    }

    @Test
    public void shouldRejectMissingDefaultStore() {
        VectorModule module = new VectorModule(registryWith(null, null, null));

        MagicAPIVectorException exception = assertThrows(MagicAPIVectorException.class,
                () -> module.getDynamicModule(new MagicScriptContext()));
        assertTrue(exception.getMessage().contains("default"));
    }

    private void assertStore(AiEmbeddingStore store, String key) {
        assertEquals(key, store.getKey());
        assertEquals("langchain4j", store.getFramework());
        assertEquals("langchain4j", store.getProvider());
        assertEquals(AiEmbeddingStoreType.VECTOR, store.getType());
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
