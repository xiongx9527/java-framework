package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.web;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.DefaultEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MagicVectorControllerTest {

    @Test
    public void datasourceTestShouldRejectMissingProvider() {
        EmbeddingStoreDefinition definition = new EmbeddingStoreDefinition();
        definition.setKey("knowledge");

        JsonBean<Object> result = controller().test(definition);

        assertEquals(-1, result.getCode());
        assertTrue(result.getMessage().contains("provider"));
    }

    @Test
    public void datasourceTestShouldRejectMissingKey() {
        EmbeddingStoreDefinition definition = new EmbeddingStoreDefinition();
        definition.setProvider("fake");

        JsonBean<Object> result = controller().test(definition);

        assertEquals(-1, result.getCode());
        assertTrue(result.getMessage().contains("key"));
    }

    @Test
    public void datasourceTestShouldReturnOkWhenProviderCreatesStore() {
        EmbeddingStoreDefinition definition = new EmbeddingStoreDefinition();
        definition.setKey("knowledge");
        definition.setProvider("fake");

        JsonBean<Object> result = controller().test(definition);

        assertEquals(1, result.getCode());
        assertEquals("ok", result.getData());
    }

    private MagicVectorController controller() {
        return new MagicVectorController(new MagicConfiguration(), new DefaultEmbeddingStoreFactory(),
                Collections.<EmbeddingStoreProviderAdapter>singletonList(new FakeEmbeddingStoreProviderAdapter()));
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
