package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.VectorDataSourceInfo;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void shouldSelectUniqueAdapterWhenFrameworkIsAbsent() {
        MagicDynamicVectorDataSource dataSource = dataSource(new FakeStoreAdapter("first"));
        VectorDataSourceInfo info = info("runtime");

        dataSource.createClient(info);

        assertStore(dataSource.get("runtime"), "runtime", "first", "in-memory");
    }

    @Test
    public void shouldReportCandidateFrameworksWhenMultipleAdaptersMatch() {
        MagicDynamicVectorDataSource dataSource = dataSource(
                new FakeStoreAdapter("first"), new FakeStoreAdapter("second"));

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> dataSource.createClient(info("runtime")));

        assertTrue(exception.getMessage().contains("first"));
        assertTrue(exception.getMessage().contains("second"));
        assertFalse(dataSource.contains("runtime"));
    }

    @Test
    public void shouldSelectExplicitFrameworkWhenMultipleAdaptersMatch() {
        MagicDynamicVectorDataSource dataSource = dataSource(
                new FakeStoreAdapter("first"), new FakeStoreAdapter("second"));
        VectorDataSourceInfo info = info("runtime");
        info.setFramework("second");

        dataSource.createClient(info);

        assertStore(dataSource.get("runtime"), "runtime", "second", "in-memory");
    }

    @Test
    public void runtimeContractsShouldNotExposeNativeTypes() {
        assertNeutralContract(MagicDynamicVectorDataSource.class);
        assertNeutralContract(VectorDataSourceInfo.class);
    }

    @Test
    public void shouldUseSpringBootRegistryAsDefaultDataSource() {
        AiEmbeddingStore bootStore = store("boot", "langchain4j", "in-memory");
        MagicDynamicVectorDataSource dataSource = new MagicDynamicVectorDataSource(
                registryWith("boot", bootStore, "boot"), new RecordingFactory(),
                Collections.<AiEmbeddingStoreAdapter>emptyList());

        assertStore(dataSource.getDefault(), "boot", "langchain4j", "in-memory");
        assertStore(dataSource.get("boot"), "boot", "langchain4j", "in-memory");
    }

    @Test
    public void shouldRegisterMagicVectorDatasourceAtRuntime() {
        AiEmbeddingStore bootStore = store("boot", "langchain4j", "in-memory");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicVectorDataSource dataSource = new MagicDynamicVectorDataSource(
                registryWith("boot", bootStore, "boot"), factory, Collections.<AiEmbeddingStoreAdapter>emptyList());
        VectorDataSourceInfo info = new VectorDataSourceInfo();
        info.setKey("runtime");
        info.setProvider("in-memory");

        dataSource.createClient(info);

        assertStore(dataSource.get("runtime"), "runtime", null, "in-memory");
        assertTrue(dataSource.datasourceNodes().size() >= 2);
    }

    @Test
    public void shouldExposeSpringBootRegistryAsSingleDefaultDatasourceNode() {
        Map<String, AiEmbeddingStore> stores = new LinkedHashMap<String, AiEmbeddingStore>();
        stores.put("boot", store("boot", "langchain4j", "in-memory"));
        stores.put("other", store("other", "langchain4j", "in-memory"));
        MagicDynamicVectorDataSource dataSource = new MagicDynamicVectorDataSource(
                new TestEmbeddingStoreRegistry(stores, "boot"),
                new RecordingFactory(),
                Collections.<AiEmbeddingStoreAdapter>emptyList());
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
                registryWith("boot", store("boot", "langchain4j", "in-memory"), "boot"), factory,
                Collections.<AiEmbeddingStoreAdapter>emptyList());
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

    private VectorDataSourceInfo info(String key) {
        VectorDataSourceInfo info = new VectorDataSourceInfo();
        info.setKey(key);
        info.setProvider("in-memory");
        return info;
    }

    private MagicDynamicVectorDataSource dataSource(AiEmbeddingStoreAdapter... adapters) {
        return new MagicDynamicVectorDataSource(
                registryWith("boot", store("boot", "boot-framework", "in-memory"), "boot"),
                new DefaultAiEmbeddingStoreFactory(), java.util.Arrays.asList(adapters));
    }

    private void assertNeutralContract(Class<?> type) {
        for (java.lang.reflect.Field field : type.getDeclaredFields()) {
            assertFalse(field.getGenericType().getTypeName().contains("langchain4j"));
        }
        for (java.lang.reflect.Constructor<?> constructor : type.getDeclaredConstructors()) {
            assertFalse(constructor.toGenericString().contains("langchain4j"));
        }
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
            assertFalse(method.toGenericString().contains("langchain4j"));
        }
    }

    private AiEmbeddingStoreRegistry registryWith(String key, AiEmbeddingStore store, String defaultKey) {
        Map<String, AiEmbeddingStore> stores = new LinkedHashMap<String, AiEmbeddingStore>();
        stores.put(key, store);
        return new TestEmbeddingStoreRegistry(stores, defaultKey);
    }

    private static AiEmbeddingStore store(String key, String framework, String provider) {
        AiEmbeddingStore store = mock(AiEmbeddingStore.class);
        when(store.getKey()).thenReturn(key);
        when(store.getFramework()).thenReturn(framework);
        when(store.getProvider()).thenReturn(provider);
        when(store.getType()).thenReturn(AiEmbeddingStoreType.VECTOR);
        return store;
    }

    private void assertStore(AiEmbeddingStore store, String key, String framework, String provider) {
        assertEquals(key, store.getKey());
        assertEquals(framework, store.getFramework());
        assertEquals(provider, store.getProvider());
        assertEquals(AiEmbeddingStoreType.VECTOR, store.getType());
    }

    private boolean hasNode(List<VectorDataSourceInfo> nodes, String key) {
        for (VectorDataSourceInfo node : nodes) {
            if (key.equals(node.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static class RecordingFactory implements AiEmbeddingStoreFactory {
        private AiEmbeddingStoreDefinition definition;

        @Override
        public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition, List<AiEmbeddingStoreAdapter> adapters) {
            this.definition = definition;
            AiEmbeddingStore store = store(definition.getKey(), definition.getFramework(), definition.getProvider());
            when(store.getType()).thenReturn(definition.getType());
            return store;
        }
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
            return stores.get(key);
        }

        @Override
        public AiEmbeddingStore getDefault() {
            return stores.get(defaultKey);
        }

        @Override
        public String getDefaultKey() {
            return defaultKey;
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

    private static class FakeStoreAdapter implements AiEmbeddingStoreAdapter {
        private final String framework;

        private FakeStoreAdapter(String framework) {
            this.framework = framework;
        }

        @Override
        public String getFramework() {
            return framework;
        }

        @Override
        public boolean supports(AiEmbeddingStoreDefinition definition) {
            return "in-memory".equals(definition.getProvider());
        }

        @Override
        public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition) {
            return store(definition.getKey(), framework, definition.getProvider());
        }
    }
}
