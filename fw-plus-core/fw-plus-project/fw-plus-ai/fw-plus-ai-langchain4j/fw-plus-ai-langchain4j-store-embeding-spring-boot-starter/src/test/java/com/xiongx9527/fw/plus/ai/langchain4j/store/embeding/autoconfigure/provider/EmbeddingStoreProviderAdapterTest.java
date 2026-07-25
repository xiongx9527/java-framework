package com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.autoconfigure.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    public void shouldMapQdrantPayloadTextKeyFromProperties() {
        EmbeddingStoreDefinition definition = definition("knowledge", "qdrant");
        definition.getProperties().put("host", "127.0.0.1");
        definition.getProperties().put("port", 6334);
        definition.getProperties().put("collectionName", "knowledge");
        definition.getProperties().put("payloadTextKey", "content");

        Object builder = new QdrantEmbeddingStoreProviderAdapter().configureBuilder(definition);

        assertEquals("content", ReflectionTestUtils.getField(builder, "payloadTextKey"));
    }

    @Test
    public void shouldMapMilvusLegacyTokenWhenApiKeyIsAbsent() {
        EmbeddingStoreDefinition definition = definition("knowledge", "milvus");
        definition.setDimension(768);
        definition.getProperties().put("uri", "http://127.0.0.1:19530");
        definition.getProperties().put("collectionName", "knowledge");
        definition.getProperties().put("token", "legacy-token");

        Object builder = new MilvusEmbeddingStoreProviderAdapter().configureBuilder(definition);

        assertEquals("legacy-token", ReflectionTestUtils.getField(builder, "token"));
    }

    @Test
    public void shouldPreferMilvusApiKeyOverLegacyToken() {
        EmbeddingStoreDefinition definition = definition("knowledge", "milvus");
        definition.setDimension(768);
        definition.getProperties().put("uri", "http://127.0.0.1:19530");
        definition.getProperties().put("collectionName", "knowledge");
        definition.getProperties().put("token", "legacy-token");
        definition.getProperties().put("apiKey", "new-api-key");

        Object builder = new MilvusEmbeddingStoreProviderAdapter().configureBuilder(definition);

        assertEquals("new-api-key", ReflectionTestUtils.getField(builder, "token"));
    }

    @Test
    public void shouldMapQdrantLegacyUtlsWhenUseTlsIsAbsent() {
        EmbeddingStoreDefinition definition = definition("knowledge", "qdrant");
        definition.getProperties().put("host", "127.0.0.1");
        definition.getProperties().put("port", 6334);
        definition.getProperties().put("collectionName", "knowledge");
        definition.getProperties().put("utls", true);

        Object builder = new QdrantEmbeddingStoreProviderAdapter().configureBuilder(definition);

        assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(builder, "useTls"));
    }

    @Test
    public void shouldPreferQdrantUseTlsOverLegacyUtls() {
        EmbeddingStoreDefinition definition = definition("knowledge", "qdrant");
        definition.getProperties().put("host", "127.0.0.1");
        definition.getProperties().put("port", 6334);
        definition.getProperties().put("collectionName", "knowledge");
        definition.getProperties().put("utls", true);
        definition.getProperties().put("useTls", false);

        Object builder = new QdrantEmbeddingStoreProviderAdapter().configureBuilder(definition);

        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(builder, "useTls"));
    }

    @Test
    public void shouldMapWeaviateLegacyOptionsFromProperties() {
        EmbeddingStoreDefinition definition = definition("knowledge", "weaviate");
        definition.getProperties().put("scheme", "http");
        definition.getProperties().put("host", "127.0.0.1");
        definition.getProperties().put("port", 8080);
        definition.getProperties().put("className", "Knowledge");
        definition.getProperties().put("avoidDups", true);
        definition.getProperties().put("consistencyLevel", "ALL");
        definition.getProperties().put("textFieldName", "content");

        WeaviateEmbeddingStore store = (WeaviateEmbeddingStore) new WeaviateEmbeddingStoreProviderAdapter().create(definition);

        assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(store, "avoidDups"));
        assertEquals("ALL", ReflectionTestUtils.getField(store, "consistencyLevel"));
        assertEquals("content", ReflectionTestUtils.getField(store, "textFieldName"));
    }

    @Test
    public void shouldMapMilvusLegacyOptionsFromProperties() {
        EmbeddingStoreDefinition definition = definition("knowledge", "milvus");
        definition.setDimension(768);
        definition.getProperties().put("uri", "http://127.0.0.1:19530");
        definition.getProperties().put("collectionName", "knowledge");
        definition.getProperties().put("indexType", "HNSW");
        definition.getProperties().put("metricType", "COSINE");
        definition.getProperties().put("consistencyLevel", "EVENTUALLY");
        definition.getProperties().put("retrieveEmbeddingsOnSearch", false);
        definition.getProperties().put("autoFlushOnInsert", false);

        Object builder = new MilvusEmbeddingStoreProviderAdapter().configureBuilder(definition);

        assertEquals(IndexType.HNSW, ReflectionTestUtils.getField(builder, "indexType"));
        assertEquals(MetricType.COSINE, ReflectionTestUtils.getField(builder, "metricType"));
        assertEquals(ConsistencyLevelEnum.EVENTUALLY, ReflectionTestUtils.getField(builder, "consistencyLevel"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(builder, "retrieveEmbeddingsOnSearch"));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.getField(builder, "autoFlushOnInsert"));
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
