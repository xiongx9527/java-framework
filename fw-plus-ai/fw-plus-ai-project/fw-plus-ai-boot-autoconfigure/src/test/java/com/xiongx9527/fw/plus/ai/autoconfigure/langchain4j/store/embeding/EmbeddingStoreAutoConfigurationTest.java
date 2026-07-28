package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProperties;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

@ExtendWith(OutputCaptureExtension.class)
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
    public void shouldKeepNeutralInfrastructureWhenLegacyConfigurationIsDisabled() {
        this.contextRunner
                .withPropertyValues("fw-plus.ai.langchain4j.embedding-store.enabled=false")
                .run(context -> {
                    assertFalse(context.containsBean("embeddingStoreRegistry"));
                    assertTrue(context.containsBean("aiEmbeddingStoreRegistry"));
                    assertTrue(context.containsBean("aiEmbeddingStoreFactory"));
                    assertFalse(context.getBeansOfType(EmbeddingStoreProviderAdapter.class).isEmpty());
                    assertFalse(context.getBeansOfType(EmbeddingStoreFactory.class).isEmpty());
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
    public void shouldMapLegacyPropertiesToNeutralRegistry() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.default-store=knowledge",
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> {
                    AiEmbeddingStoreRegistry registry = context.getBean(AiEmbeddingStoreRegistry.class);
                    assertTrue(registry.contains("knowledge"));
                    assertEquals("knowledge", registry.getDefaultKey());
                });
    }

    @Test
    public void shouldCreateNeutralRegistryFromNeutralPropertiesOnly() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.embedding-store.default-store=knowledge",
                        "fw-plus.ai.embedding-store.stores.knowledge.framework=langchain4j",
                        "fw-plus.ai.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.embedding-store.stores.knowledge.type=vector")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> assertTrue(context.getBean(AiEmbeddingStoreRegistry.class).contains("knowledge")));
    }

    @Test
    public void shouldCreateNeutralRegistryWhenLegacyConfigurationIsDisabled() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.enabled=false",
                        "fw-plus.ai.embedding-store.default-store=knowledge",
                        "fw-plus.ai.embedding-store.stores.knowledge.framework=langchain4j",
                        "fw-plus.ai.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.embedding-store.stores.knowledge.type=vector")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertFalse(context.containsBean("embeddingStoreRegistry"));
                    assertTrue(context.getBean(AiEmbeddingStoreRegistry.class).contains("knowledge"));
                });
    }

    @Test
    public void shouldFailForDuplicateEnabledNeutralStoreKeyWithoutLeakingSecrets(CapturedOutput output) {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.embedding-store.stores.primary.key=shared",
                        "fw-plus.ai.embedding-store.stores.primary.framework=langchain4j",
                        "fw-plus.ai.embedding-store.stores.primary.provider=fake",
                        "fw-plus.ai.embedding-store.stores.primary.type=vector",
                        "fw-plus.ai.embedding-store.stores.primary.properties.apiKey=primary-store-secret",
                        "fw-plus.ai.embedding-store.stores.secondary.key=shared",
                        "fw-plus.ai.embedding-store.stores.secondary.framework=langchain4j",
                        "fw-plus.ai.embedding-store.stores.secondary.provider=fake",
                        "fw-plus.ai.embedding-store.stores.secondary.type=vector",
                        "fw-plus.ai.embedding-store.stores.secondary.properties.apiKey=secondary-store-secret")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> {
                    assertTrue(hasCause(context.getStartupFailure(), "Duplicate enabled store key: key=shared"));
                    assertFalse(output.getAll().contains("primary-store-secret"));
                    assertFalse(output.getAll().contains("secondary-store-secret"));
                });
    }

    @Test
    public void shouldPreferNeutralStoreForSameKey() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.default-store=knowledge",
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.embedding-store.default-store=knowledge",
                        "fw-plus.ai.embedding-store.stores.knowledge.framework=langchain4j",
                        "fw-plus.ai.embedding-store.stores.knowledge.provider=neutral",
                        "fw-plus.ai.embedding-store.stores.knowledge.type=vector")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> assertEquals("neutral",
                        context.getBean(AiEmbeddingStoreRegistry.class).get("knowledge").getProvider()));
    }

    @Test
    public void shouldNotRestoreDisabledNeutralStoreFromLegacyProperties() {
        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.embedding-store.stores.knowledge.enabled=false",
                        "fw-plus.ai.embedding-store.stores.knowledge.framework=langchain4j",
                        "fw-plus.ai.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.embedding-store.stores.knowledge.type=vector")
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> assertFalse(context.getBean(AiEmbeddingStoreRegistry.class).contains("knowledge")));
    }

    @Test
    public void shouldUseInjectedNeutralFactoryWhenCreatingRegistry() {
        final AiEmbeddingStore expected = mock(AiEmbeddingStore.class);
        AiEmbeddingStoreFactory factory = new AiEmbeddingStoreFactory() {
            @Override
            public AiEmbeddingStore create(AiEmbeddingStoreDefinition ignored,
                                           List<AiEmbeddingStoreAdapter> adapters) {
                return expected;
            }
        };

        this.contextRunner
                .withPropertyValues(
                        "fw-plus.ai.embedding-store.stores.knowledge.framework=custom",
                        "fw-plus.ai.embedding-store.stores.knowledge.provider=custom",
                        "fw-plus.ai.embedding-store.stores.knowledge.type=vector")
                .withBean(AiEmbeddingStoreFactory.class, () -> factory)
                .run(context -> assertSame(expected,
                        context.getBean(AiEmbeddingStoreRegistry.class).get("knowledge")));
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
                    assertTrue(hasCause(context.getStartupFailure(), "provider=unknown"));
                });
    }

    private boolean hasCause(Throwable throwable, String message) {
        Throwable current = throwable;
        while (current != null) {
            if ((current instanceof EmbeddingStoreException || current instanceof AiEmbeddingStoreException)
                    && current.getMessage().contains(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static class FakeEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

        @Override
        public boolean supports(String provider) {
            return "fake".equals(provider) || "neutral".equals(provider);
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
