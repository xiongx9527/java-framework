package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.starter.MagicAPIVectorConfiguration;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.EmbeddingStoreAutoConfiguration;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MagicAPIVectorAutoConfigurationIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class,
                    EmbeddingStoreAutoConfiguration.class));

    @Test
    public void shouldCreateRegistryAndVectorPluginThroughAutoConfigurations() {
        List<String> autoConfigurations = SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
                getClass().getClassLoader());
        assertTrue(autoConfigurations.contains(MagicAPIBaseAutoConfiguration.class.getName()));
        assertTrue(autoConfigurations.contains(EmbeddingStoreAutoConfiguration.class.getName()));

        this.contextRunner
                .withPropertyValues(
                        "fw.plus.magicapi.enabled=true",
                        "magic-api.plugin.aia.enabled=false",
                        "magic-api.plugin.qdrant.enabled=false",
                        "magic-api.plugin.redisstack.enabled=false",
                        "magic-api.plugin.weaviate.enabled=false",
                        "fw-plus.ai.langchain4j.embedding-store.default-store=knowledge",
                        "fw-plus.ai.langchain4j.embedding-store.stores.knowledge.provider=fake")
                .withBean(MagicAPIProperties.class, MagicAPIProperties::new)
                .withBean(EmbeddingStoreProviderAdapter.class, FakeEmbeddingStoreProviderAdapter::new)
                .run(context -> {
                    assertNotNull(context.getBean(EmbeddingStoreRegistry.class));
                    assertNotNull(context.getBean(VectorModule.class));
                    MagicAPIVectorConfiguration configuration = context.getBean(MagicAPIVectorConfiguration.class);
                    Plugin plugin = configuration.plugin();
                    assertEquals("vector", plugin.getName());
                });
    }

    private static class FakeEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

        @Override
        public boolean supports(String provider) {
            return "fake".equals(provider);
        }

        @Override
        public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
            return new InMemoryEmbeddingStore<TextSegment>();
        }
    }
}
