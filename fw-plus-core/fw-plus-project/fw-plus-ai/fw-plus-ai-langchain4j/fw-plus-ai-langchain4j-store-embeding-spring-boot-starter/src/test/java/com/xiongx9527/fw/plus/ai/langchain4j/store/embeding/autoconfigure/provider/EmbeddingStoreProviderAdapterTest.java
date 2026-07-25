package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EmbeddingStoreProviderAdapterTest {

    @Test
    public void shouldMatchProviderNamesCaseInsensitively() {
        assertTrue(new RedisStackEmbeddingStoreProviderAdapter().supports("redisstack"));
        assertTrue(new RedisStackEmbeddingStoreProviderAdapter().supports("redis"));
        assertTrue(new MilvusEmbeddingStoreProviderAdapter().supports("MILVUS"));
        assertTrue(new ElasticsearchEmbeddingStoreProviderAdapter().supports("elasticsearch"));
        assertTrue(new QdrantEmbeddingStoreProviderAdapter().supports("qdrant"));
        assertTrue(new WeaviateEmbeddingStoreProviderAdapter().supports("weaviate"));
    }

    @Test
    public void shouldWrapInvalidMilvusConfigurationWithProviderAndKey() {
        EmbeddingStoreDefinition definition = definition("knowledge", "milvus");
        definition.getProperties().put("host", "");

        try {
            new MilvusEmbeddingStoreProviderAdapter().create(definition);
            fail("Expected EmbeddingStoreException");
        } catch (EmbeddingStoreException ex) {
            assertTrue(ex.getMessage().contains("provider=milvus"));
            assertTrue(ex.getMessage().contains("key=knowledge"));
        }
    }

    @Test
    public void shouldWrapIncompleteProviderConfigurationWithoutConnecting() {
        assertIncompleteConfiguration(new RedisStackEmbeddingStoreProviderAdapter(), "redis", "redisstack");
        assertIncompleteConfiguration(new MilvusEmbeddingStoreProviderAdapter(), "milvus", "milvus");
        assertIncompleteConfiguration(new ElasticsearchEmbeddingStoreProviderAdapter(), "elasticsearch", "elasticsearch");
        assertIncompleteConfiguration(new QdrantEmbeddingStoreProviderAdapter(), "qdrant", "qdrant");
        assertIncompleteConfiguration(new WeaviateEmbeddingStoreProviderAdapter(), "weaviate", "weaviate");
    }

    private EmbeddingStoreDefinition definition(String key, String provider) {
        EmbeddingStoreDefinition definition = new EmbeddingStoreDefinition();
        definition.setKey(key);
        definition.setProvider(provider);
        return definition;
    }

    private void assertIncompleteConfiguration(com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter adapter,
                                               String key, String provider) {
        try {
            adapter.create(definition(key, provider));
            fail("Expected EmbeddingStoreException");
        } catch (EmbeddingStoreException ex) {
            assertTrue(ex.getMessage().contains("provider=" + provider));
            assertTrue(ex.getMessage().contains("key=" + key));
        }
    }
}
