package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.VectorDataSourceInfo;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MagicDynamicVectorDataSourceTest {

    private GenericApplicationContext applicationContext;

    @AfterEach
    public void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
            new SpringUtils().setApplicationContext(null);
        }
    }

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

    @Test
    public void shouldUseVectorConfigurationPropertiesInsteadOfDatasourceDefaults() {
        configure("magic.api.vector.runtime.properties={\"host\":\"configured-host\",\"custom\":\"configured\"}",
                "magic.api.vector.runtime.apiKey=configured-api-key");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicVectorDataSource dataSource = new MagicDynamicVectorDataSource(
                registryWith("boot", new InMemoryEmbeddingStore<TextSegment>(), "boot"), factory,
                Collections.<EmbeddingStoreProviderAdapter>emptyList());
        VectorDataSourceInfo info = new VectorDataSourceInfo();
        info.setKey("runtime");
        info.setProvider("in-memory");
        Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        defaults.put("host", "default-host");
        defaults.put("legacyOnly", "default-value");
        info.setProperties(defaults);

        dataSource.createClient(info);

        assertEquals("configured-host", factory.definition.getProperties().get("host"));
        assertEquals("configured", factory.definition.getProperties().get("custom"));
        assertEquals("configured-api-key", factory.definition.getProperties().get("apiKey"));
        assertFalse(factory.definition.getProperties().containsKey("legacyOnly"));
    }

    private void configure(String... properties) {
        applicationContext = new GenericApplicationContext();
        TestPropertyValues.of(properties).applyTo(applicationContext);
        applicationContext.refresh();
        new SpringUtils().setApplicationContext(applicationContext);
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
        private EmbeddingStoreDefinition definition;

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition, List<EmbeddingStoreProviderAdapter> adapters) {
            this.definition = definition;
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
