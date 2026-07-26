package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.VectorDataSourceInfo;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MagicDynamicVectorDataSourceTest {

    @Test
    public void shouldUseSpringBootRegistryAsDefaultDataSource() {
        EmbeddingStore<TextSegment> bootStore = new InMemoryEmbeddingStore<TextSegment>();
        MagicDynamicVectorDataSource dataSource = new MagicDynamicVectorDataSource(
                registryWith("boot", bootStore, "boot"), new RecordingFactory(), Collections.<EmbeddingStoreProviderAdapter>emptyList());

        assertSame(bootStore, dataSource.getDefault());
        assertSame(bootStore, dataSource.get("boot"));
    }

    @Test
    public void shouldRegisterMagicVectorDatasourceAtRuntime() {
        EmbeddingStore<TextSegment> bootStore = new InMemoryEmbeddingStore<TextSegment>();
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicVectorDataSource dataSource = new MagicDynamicVectorDataSource(
                registryWith("boot", bootStore, "boot"), factory, Collections.<EmbeddingStoreProviderAdapter>emptyList());
        VectorDataSourceInfo info = new VectorDataSourceInfo();
        info.setKey("runtime");
        info.setProvider("in-memory");

        dataSource.createClient(info);

        assertSame(factory.createdStore, dataSource.get("runtime"));
        assertTrue(dataSource.datasourceNodes().size() >= 2);
    }

    @Test
    public void shouldExposeSpringBootRegistryAsSingleDefaultDatasourceNode() {
        Map<String, EmbeddingStore<TextSegment>> stores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        stores.put("boot", new InMemoryEmbeddingStore<TextSegment>());
        stores.put("other", new InMemoryEmbeddingStore<TextSegment>());
        MagicDynamicVectorDataSource dataSource = new MagicDynamicVectorDataSource(
                new TestEmbeddingStoreRegistry(stores, "boot"),
                new RecordingFactory(),
                Collections.<EmbeddingStoreProviderAdapter>emptyList());
        VectorDataSourceInfo info = new VectorDataSourceInfo();
        info.setKey("runtime");
        info.setName("运行时向量库");
        info.setProvider("in-memory");
        dataSource.createClient(info);

        List<VectorDataSourceInfo> nodes = dataSource.datasourceNodes();

        assertEquals(2, nodes.size());
        assertTrue(hasNode(nodes, "runtime"));
        assertTrue(hasNode(nodes, "default"));
        assertFalse(hasNode(nodes, "boot"));
        assertFalse(hasNode(nodes, "other"));
    }

    private EmbeddingStoreRegistry registryWith(String key, EmbeddingStore<TextSegment> store, String defaultKey) {
        Map<String, EmbeddingStore<TextSegment>> stores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        stores.put(key, store);
        return new TestEmbeddingStoreRegistry(stores, defaultKey);
    }

    private boolean hasNode(List<VectorDataSourceInfo> nodes, String key) {
        for (VectorDataSourceInfo node : nodes) {
            if (key.equals(node.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static class RecordingFactory implements EmbeddingStoreFactory {
        private final EmbeddingStore<TextSegment> createdStore = new InMemoryEmbeddingStore<TextSegment>();

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition, List<EmbeddingStoreProviderAdapter> adapters) {
            return createdStore;
        }
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
            return stores.get(key);
        }

        @Override
        public EmbeddingStore<TextSegment> getDefault() {
            return stores.get(defaultKey);
        }

        @Override
        public boolean contains(String key) {
            return stores.containsKey(key);
        }

        @Override
        public Set<String> keys() {
            return stores.keySet();
        }
    }
}
