package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
}
