package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.service.VectorDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.service.VectorDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.starter.MagicAPIVectorConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding.AiEmbeddingStoreAutoConfiguration;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MagicAPIVectorAutoConfigurationIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class,
                    AiEmbeddingStoreAutoConfiguration.class));

    @Test
    public void shouldCreateRegistryAndVectorPluginThroughAutoConfigurations() {
        List<String> autoConfigurations = SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
                getClass().getClassLoader());
        assertTrue(autoConfigurations.contains(MagicAPIBaseAutoConfiguration.class.getName()));
        assertTrue(autoConfigurations.contains(AiEmbeddingStoreAutoConfiguration.class.getName()));

        this.contextRunner
                .withPropertyValues(
                        "fw.plus.magicapi.enabled=true",
                        "magic-api.plugin.aia.enabled=false",
                        "fw-plus.ai.embedding-store.default-store=knowledge",
                        "fw-plus.ai.embedding-store.stores.knowledge.provider=fake",
                        "fw-plus.ai.embedding-store.stores.knowledge.type=vector")
                .withBean(MagicAPIProperties.class, MagicAPIProperties::new)
                .withBean(AiEmbeddingStoreAdapter.class, FakeEmbeddingStoreAdapter::new)
                .run(context -> {
                    AiEmbeddingStoreRegistry registry = context.getBean(AiEmbeddingStoreRegistry.class);
                    assertTrue(registry.contains("knowledge"));
                    assertNotNull(context.getBean(MagicDynamicVectorDataSource.class));
                    assertNotNull(context.getBean(MagicDynamicVectorDataSource.class).get("knowledge"));
                    assertNotNull(context.getBean(VectorDataSourceInfoMagicResourceStorage.class));
                    assertNotNull(context.getBean(VectorDataSourceMagicDynamicRegistry.class));
                    assertNotNull(context.getBean(VectorModule.class));
                    MagicAPIVectorConfiguration configuration = context.getBean(MagicAPIVectorConfiguration.class);
                    Plugin plugin = configuration.plugin();
                    assertEquals("vector", plugin.getName());
                });
    }

    private static class FakeEmbeddingStoreAdapter implements AiEmbeddingStoreAdapter {

        @Override
        public String getFramework() {
            return "fake-framework";
        }

        @Override
        public boolean supports(AiEmbeddingStoreDefinition definition) {
            return "fake".equals(definition.getProvider());
        }

        @Override
        public AiEmbeddingStore create(final AiEmbeddingStoreDefinition definition) {
            return new AiEmbeddingStore() {
                @Override public String getKey() { return definition.getKey(); }
                @Override public String getFramework() { return "fake-framework"; }
                @Override public String getProvider() { return definition.getProvider(); }
                @Override public AiEmbeddingStoreType getType() { return definition.getType(); }
                @Override public String add(AiEmbeddingDocument document) { return "id"; }
                @Override public List<String> addAll(List<AiEmbeddingDocument> documents) {
                    return java.util.Collections.singletonList("id");
                }
                @Override public void remove(String id) { }
                @Override public void removeAll(List<String> ids) { }
                @Override public AiEmbeddingSearchResult search(AiEmbeddingQuery query) { return null; }
            };
        }
    }
}
