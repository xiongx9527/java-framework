package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProperties;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

public class EmbeddingStoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmbeddingStoreAutoConfiguration.class);

    @Test
    public void shouldNotCreateRegistryWhenDisabled() {
        this.contextRunner
                .withPropertyValues("fw-plus.ai.langchain4j.embedding-store.enabled=false")
                .run(context -> assertFalse(context.containsBean("embeddingStoreRegistry")));
    }

    @Test
    public void shouldNotCreateStarterBeansWhenDisabled() {
        this.contextRunner
                .withPropertyValues("fw-plus.ai.langchain4j.embedding-store.enabled=false")
                .run(context -> {
                    assertFalse(context.containsBean("embeddingStoreRegistry"));
                    assertFalse(context.containsBean("embeddingStoreFactory"));
                    assertTrue(context.getBeansOfType(EmbeddingStoreProviderAdapter.class).isEmpty());
                    assertTrue(context.getBeansOfType(EmbeddingStoreFactory.class).isEmpty());
                });
    }

    @Test
    public void shouldStartWithoutOptionalTypedProviderSdks() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(
                        "dev.langchain4j.store.embedding.redis",
                        "dev.langchain4j.store.embedding.milvus",
                        "dev.langchain4j.store.embedding.elasticsearch",
                        "dev.langchain4j.store.embedding.qdrant",
                        "dev.langchain4j.store.embedding.weaviate",
                        "dev.langchain4j.store.embedding.azure.search"))
                .withUserConfiguration(EmbeddingStoreAutoConfiguration.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.containsBean("embeddingStoreRegistry"));
                    assertTrue(context.containsBean("inMemoryEmbeddingStoreProviderAdapter"));
                    assertFalse(context.containsBean("redisStackEmbeddingStoreProviderAdapter"));
                    assertFalse(context.containsBean("milvusEmbeddingStoreProviderAdapter"));
                    assertFalse(context.containsBean("elasticsearchEmbeddingStoreProviderAdapter"));
                    assertFalse(context.containsBean("qdrantEmbeddingStoreProviderAdapter"));
                    assertFalse(context.containsBean("weaviateEmbeddingStoreProviderAdapter"));
                    assertFalse(context.containsBean("azureAiSearchEmbeddingStoreProviderAdapter"));
                });
    }

    @Test
    public void shouldCreateRedisStoreWhenCustomAdapterExists() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.stores.redis-cache.provider=redis",
                        "fw-plus.ai.langchain4j.embedding-store.stores.redis-cache.dimension=384",
                        "fw-plus.ai.langchain4j.embedding-store.stores.redis-cache.properties.host=127.0.0.1",
                        "fw-plus.ai.langchain4j.embedding-store.stores.redis-cache.properties.port=6379",
                        "fw-plus.ai.langchain4j.embedding-store.stores.redis-cache.properties.indexName=knowledge")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .withBean(EmbeddingStoreFactory.class, RedisAwareEmbeddingStoreFactory::new)
                .run(context -> {
                    assertTrue(context.containsBean("redisStackEmbeddingStoreProviderAdapter"));
                    assertTrue(context.getBean(EmbeddingStoreRegistry.class).contains("redis-cache"));
                });
    }

    @Test
    public void shouldRegisterDedicatedEmbeddingStoreProviderAdapters() {
        this.contextRunner
                .run(context -> {
                    assertTrue(context.containsBean("astraDbEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("azureAiSearchEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("azureCosmosMongoVCoreEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("azureCosmosNoSqlEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("cassandraEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("chromaEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("couchbaseEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("inMemoryEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("infinispanEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("mongoDbEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("neo4jEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("openSearchEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("oracleEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("pgVectorEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("pineconeEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("tablestoreEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("vearchEmbeddingStoreProviderAdapter"));
                    assertTrue(context.containsBean("vespaEmbeddingStoreProviderAdapter"));
                });
    }

    @Test
    public void shouldCreateRegistryWithMultipleStoresAndDefaultKey() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.enabled=true",
                        "fw-plus.ai.langchain4j.embedding-store.default-store=knowledge",
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.langchain4j.embedding-store.stores.local-cache.provider=fake")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> {
                    assertTrue(context.containsBean("embeddingStoreRegistry"));
                    EmbeddingStoreRegistry registry = context.getBean(EmbeddingStoreRegistry.class);
                    assertTrue(registry.contains("knowledge"));
                    assertTrue(registry.contains("local-cache"));
                    assertSame(registry.get("knowledge"), registry.getDefault());
                });
    }

    @Test
    public void shouldBindProviderPropertiesUnderPropertiesMap() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.properties.collectionName=knowledge")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> assertEquals("knowledge", context.getBean(EmbeddingStoreProperties.class)
                        .getStores().get("knowledge").getProperties().get("collectionName")));
    }

    @Test
    public void shouldFailWhenMultipleStoresResolveToTheSameKey() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.key=shared",
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.langchain4j.embedding-store.stores.local-cache.key=shared",
                        "fw-plus.ai.langchain4j.embedding-store.stores.local-cache.provider=fake")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> {
                    assertTrue(hasCause(context.getStartupFailure(), "Duplicate embedding store key"));
                });
    }

    @Test
    public void shouldFailWhenDefaultStoreIsInvalid() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.default-store=missing",
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> {
                    assertTrue(hasCause(context.getStartupFailure(), "Default embedding store key is invalid"));
                });
    }

    @Test
    public void shouldFailWhenProviderIsUnknown() {
        this.contextRunner
                .withPropertyValues("fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=unknown")
                .run(context -> {
                    assertTrue(hasCause(context.getStartupFailure(), "Embedding store provider is not supported"));
                });
    }

    private boolean hasCause(Throwable throwable, String message) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof EmbeddingStoreException && current.getMessage().contains(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static class FakeEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

        @Override
        public boolean supports(String provider) {
            return "fake".equals(provider);
        }

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
            return mock(EmbeddingStore.class);
        }
    }

    private static class RedisAwareEmbeddingStoreFactory implements EmbeddingStoreFactory {

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition,
                                                  List<EmbeddingStoreProviderAdapter> adapters) {
            for (EmbeddingStoreProviderAdapter adapter : adapters) {
                if (adapter.supports("redis")) {
                    return mock(EmbeddingStore.class);
                }
            }
            throw new EmbeddingStoreException("Embedding store provider is not supported: redis");
        }
    }
}
