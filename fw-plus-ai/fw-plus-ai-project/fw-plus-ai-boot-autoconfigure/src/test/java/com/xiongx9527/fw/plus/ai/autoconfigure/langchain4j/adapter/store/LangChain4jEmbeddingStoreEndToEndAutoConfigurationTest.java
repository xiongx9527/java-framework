package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding.AiEmbeddingStoreAutoConfiguration;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LangChain4jEmbeddingStoreEndToEndAutoConfigurationTest {

    @Test
    public void shouldCreateAConfiguredStoreThroughTheNeutralRegistry() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        LangChain4jAdapterAutoConfiguration.class,
                        AiEmbeddingStoreAutoConfiguration.class))
                .withPropertyValues(
                        "fw-plus.ai.embedding-store.default-store=memory",
                        "fw-plus.ai.embedding-store.stores.memory.framework=langchain4j",
                        "fw-plus.ai.embedding-store.stores.memory.provider=in-memory",
                        "fw-plus.ai.embedding-store.stores.memory.type=vector",
                        "fw-plus.ai.embedding-store.stores.memory.enabled=true")
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    AiEmbeddingStoreRegistry registry = context.getBean(AiEmbeddingStoreRegistry.class);
                    AiEmbeddingStore store = registry.getDefault();
                    assertEquals("memory", store.getKey());
                    assertEquals("langchain4j", store.getFramework());
                    assertEquals("in-memory", store.getProvider());
                    assertEquals(AiEmbeddingStoreType.VECTOR, store.getType());
                });
    }
}
