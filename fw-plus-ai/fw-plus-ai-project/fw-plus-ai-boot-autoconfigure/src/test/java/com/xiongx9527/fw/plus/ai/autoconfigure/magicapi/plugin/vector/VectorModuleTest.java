package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VectorModuleTest {

    @Test
    public void shouldReturnOperationsForExplicitKey() {
        AiEmbeddingStore store = store("knowledge");
        AiEmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
        DynamicAttribute<VectorModule, VectorStoreOperations> module = new VectorModule(registry);

        VectorModule keyedModule = module.getDynamicAttribute("knowledge");

        assertStore(keyedModule.getOperations().getEmbeddingStore(), "knowledge");
        assertSame(store, keyedModule.getOperations().getEmbeddingStore());
    }

    @Test
    public void shouldResolveExplicitKeyThroughMagicScriptDynamicAttributeAccess() {
        AiEmbeddingStore store = store("knowledge");
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
        AiEmbeddingStore store = store("knowledge");
        AiEmbeddingStoreRegistry registry = registryWith("knowledge", store, "knowledge");
        VectorModule module = new VectorModule(registry);

        VectorStoreOperations operations = module.getDynamicModule(new MagicScriptContext());

        assertStore(operations.getEmbeddingStore(), "knowledge");
    }

    @Test
    public void shouldExposeSpringBootDefaultStoreThroughGetEmbeddingStore() {
        AiEmbeddingStore store = store("knowledge");
        VectorModule module = new VectorModule(registryWith("knowledge", store, "knowledge"));

        assertStore(module.getEmbeddingStore(), "knowledge");
    }

    @Test
    public void shouldExposeExplicitStoreThroughDynamicAttributeGetEmbeddingStore() {
        AiEmbeddingStore store = store("knowledge");
        VectorModule module = new VectorModule(registryWith("knowledge", store, "knowledge"));

        assertStore(module.getDynamicAttribute("knowledge").getEmbeddingStore(), "knowledge");
    }

    @Test
    public void shouldRejectUnknownStoreKey() {
        VectorModule module = new VectorModule(registryWith("knowledge", store("knowledge"), "knowledge"));

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
        assertEquals("neutral-test", store.getFramework());
        assertEquals("in-memory", store.getProvider());
        assertEquals(AiEmbeddingStoreType.VECTOR, store.getType());
    }

    private AiEmbeddingStoreRegistry registryWith(String key, AiEmbeddingStore store, String defaultKey) {
        Map<String, AiEmbeddingStore> stores = new LinkedHashMap<String, AiEmbeddingStore>();
        if (key != null) {
            stores.put(key, store);
        }
        return new TestEmbeddingStoreRegistry(stores, defaultKey);
    }

    private AiEmbeddingStore store(final String key) {
        return new AiEmbeddingStore() {
            @Override public String getKey() { return key; }
            @Override public String getFramework() { return "neutral-test"; }
            @Override public String getProvider() { return "in-memory"; }
            @Override public AiEmbeddingStoreType getType() { return AiEmbeddingStoreType.VECTOR; }
            @Override public String add(AiEmbeddingDocument document) { return "id"; }
            @Override public java.util.List<String> addAll(java.util.List<AiEmbeddingDocument> documents) {
                return Collections.singletonList("id");
            }
            @Override public void remove(String id) { }
            @Override public void removeAll(java.util.List<String> ids) { }
            @Override public AiEmbeddingSearchResult search(AiEmbeddingQuery query) { return null; }
        };
    }

    private static class TestEmbeddingStoreRegistry implements AiEmbeddingStoreRegistry {

        private final Map<String, AiEmbeddingStore> stores;
        private final String defaultKey;

        private TestEmbeddingStoreRegistry(Map<String, AiEmbeddingStore> stores, String defaultKey) {
            this.stores = stores;
            this.defaultKey = defaultKey;
        }

        @Override
        public AiEmbeddingStore get(String key) {
            AiEmbeddingStore store = stores.get(key);
            if (store == null) {
                throw new IllegalArgumentException("Unknown embedding store: " + key);
            }
            return store;
        }

        @Override
        public AiEmbeddingStore getDefault() {
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

        @Override
        public String getDefaultKey() {
            return defaultKey;
        }
    }
}
