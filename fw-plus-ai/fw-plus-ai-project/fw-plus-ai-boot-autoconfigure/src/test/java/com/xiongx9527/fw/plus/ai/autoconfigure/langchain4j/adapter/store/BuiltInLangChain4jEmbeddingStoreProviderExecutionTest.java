package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuiltInLangChain4jEmbeddingStoreProviderExecutionTest {

    private final LangChain4jEmbeddingStoreAdapterAutoConfiguration configuration =
            new LangChain4jEmbeddingStoreAdapterAutoConfiguration();
    private final StaticApplicationContext context = new StaticApplicationContext();

    @Test
    public void shouldCreateAnInMemoryStoreUsingTheDefaultConstructor() {
        Object store = configuration.inMemoryEmbeddingStoreProviderAdapter(context)
                .create(definition("memory", "in-memory", Collections.<String, Object>emptyMap()));

        assertInstanceOf(InMemoryEmbeddingStore.class, store);
    }

    @Test
    public void shouldMapQdrantPropertiesAndPreferExplicitUseTls() {
        Map<String, Object> values = values("host", "127.0.0.1", "port", 6334,
                "collectionName", "knowledge", "payloadTextKey", "content",
                "utls", true, "useTls", false);

        Object builder = builder(configuration.qdrantEmbeddingStoreProviderAdapter(context),
                definition("knowledge", "qdrant", values));

        assertEquals("content", ReflectionTestUtils.getField(builder, "payloadTextKey"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(builder, "useTls"));
    }

    @Test
    public void shouldMapMilvusFallbacksEnumsAndBooleans() {
        Map<String, Object> values = values("uri", "http://127.0.0.1:19530",
                "collectionName", "knowledge", "dimension", 768,
                "token", "legacy-token", "apiKey", "new-api-key",
                "indexType", "HNSW", "metricType", "COSINE",
                "consistencyLevel", "EVENTUALLY",
                "retrieveEmbeddingsOnSearch", false, "autoFlushOnInsert", false);

        Object builder = builder(configuration.milvusEmbeddingStoreProviderAdapter(context),
                definition("knowledge", "milvus", values));

        assertEquals("new-api-key", ReflectionTestUtils.getField(builder, "token"));
        assertEquals(IndexType.HNSW, ReflectionTestUtils.getField(builder, "indexType"));
        assertEquals(MetricType.COSINE, ReflectionTestUtils.getField(builder, "metricType"));
        assertEquals(ConsistencyLevelEnum.EVENTUALLY,
                ReflectionTestUtils.getField(builder, "consistencyLevel"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(builder, "retrieveEmbeddingsOnSearch"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(builder, "autoFlushOnInsert"));
    }

    @Test
    public void shouldRejectAnExplicitBlankMilvusHostEvenWhenUriIsConfigured() {
        Map<String, Object> values = values("host", "", "uri", "http://127.0.0.1:19530",
                "collectionName", "knowledge", "dimension", 768);

        AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                () -> configuration.milvusEmbeddingStoreProviderAdapter(context)
                        .create(definition("knowledge", "milvus", values)));

        assertTrue(exception.getMessage().contains("host"));
    }

    @Test
    public void shouldCreateWeaviateWithTheLegacyOptions() {
        Map<String, Object> values = values("scheme", "http", "host", "127.0.0.1",
                "port", 8080, "className", "Knowledge", "avoidDups", true,
                "consistencyLevel", "ALL", "metadataKeys", Arrays.asList("tenantId"),
                "textFieldName", "content");

        WeaviateEmbeddingStore store = (WeaviateEmbeddingStore)
                configuration.weaviateEmbeddingStoreProviderAdapter(context)
                        .create(definition("knowledge", "weaviate", values));

        assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(store, "avoidDups"));
        assertEquals("ALL", ReflectionTestUtils.getField(store, "consistencyLevel"));
        assertEquals("content", ReflectionTestUtils.getField(store, "textFieldName"));
    }

    @Test
    public void shouldRejectIncompleteProviderConfigurationBeforeConnecting() {
        Map<String, LangChain4jEmbeddingStoreProviderAdapter> providers =
                new LinkedHashMap<String, LangChain4jEmbeddingStoreProviderAdapter>();
        providers.put("redis", configuration.redisStackEmbeddingStoreProviderAdapter(context));
        providers.put("milvus", configuration.milvusEmbeddingStoreProviderAdapter(context));
        providers.put("elasticsearch", configuration.elasticsearchEmbeddingStoreProviderAdapter(context));
        providers.put("qdrant", configuration.qdrantEmbeddingStoreProviderAdapter(context));
        providers.put("weaviate", configuration.weaviateEmbeddingStoreProviderAdapter(context));
        providers.put("azure-ai-search", configuration.azureAiSearchEmbeddingStoreProviderAdapter(context));
        providers.put("azure-cosmos-nosql", configuration.azureCosmosNoSqlEmbeddingStoreProviderAdapter(context));
        providers.put("infinispan", configuration.infinispanEmbeddingStoreProviderAdapter(context));
        providers.put("neo4j", configuration.neo4jEmbeddingStoreProviderAdapter(context));
        providers.put("oracle", configuration.oracleEmbeddingStoreProviderAdapter(context));
        providers.put("tablestore", configuration.tablestoreEmbeddingStoreProviderAdapter(context));

        for (Map.Entry<String, LangChain4jEmbeddingStoreProviderAdapter> entry : providers.entrySet()) {
            AiEmbeddingStoreException exception = assertThrows(AiEmbeddingStoreException.class,
                    () -> entry.getValue().create(definition(entry.getKey(), entry.getKey(),
                            Collections.<String, Object>emptyMap())), entry.getKey());
            assertTrue(exception.getMessage().contains("provider=" + entry.getKey()), entry.getKey());
            assertTrue(exception.getMessage().contains("key=" + entry.getKey()), entry.getKey());
        }
    }

    private Object builder(LangChain4jEmbeddingStoreProviderAdapter adapter,
                           AiEmbeddingStoreDefinition definition) {
        Object spec = ReflectionTestUtils.getField(adapter, "spec");
        LangChain4jEmbeddingStoreProviderSpec.CreationPlan plan =
                ((LangChain4jEmbeddingStoreProviderSpec) spec).getCreationPlans().get(0);
        try {
            Class<?> storeClass = Class.forName(
                    ((LangChain4jEmbeddingStoreProviderSpec) spec).getStoreClassName());
            Object target = ProviderAdapterSupport.invokeStaticNoArg(storeClass, plan.getFactoryMethod());
            for (LangChain4jEmbeddingStoreProviderSpec.Invocation invocation : plan.getInvocations()) {
                if (ProviderAdapterSupport.matches(invocation.getCondition(), definition)) {
                    target = ProviderAdapterSupport.invoke(target, invocation.getMethod(),
                            invocation.getArguments(), definition, context);
                }
            }
            return target;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private AiEmbeddingStoreDefinition definition(String key, String provider,
                                                  Map<String, Object> properties) {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey(key);
        definition.setFramework("langchain4j");
        definition.setProvider(provider);
        definition.setType(AiEmbeddingStoreType.VECTOR);
        definition.setEnabled(true);
        definition.setProperties(properties);
        return definition;
    }

    private Map<String, Object> values(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
