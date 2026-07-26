package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

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
    public void shouldSupportAllRemainingLangChain4jEmbeddingStoreProviders() {
        LangChain4jEmbeddingStoreProviderAdapter adapter = new LangChain4jEmbeddingStoreProviderAdapter();

        assertTrue(adapter.supports("azure-ai-search"));
        assertTrue(adapter.supports("azure-cosmos-mongo-vcore"));
        assertTrue(adapter.supports("azure-cosmos-nosql"));
        assertTrue(adapter.supports("cassandra"));
        assertTrue(adapter.supports("astra-db"));
        assertTrue(adapter.supports("chroma"));
        assertTrue(adapter.supports("couchbase"));
        assertTrue(adapter.supports("infinispan"));
        assertTrue(adapter.supports("mongodb-atlas"));
        assertTrue(adapter.supports("mongodb"));
        assertTrue(adapter.supports("neo4j"));
        assertTrue(adapter.supports("opensearch"));
        assertTrue(adapter.supports("oracle"));
        assertTrue(adapter.supports("pgvector"));
        assertTrue(adapter.supports("postgres"));
        assertTrue(adapter.supports("postgresql"));
        assertTrue(adapter.supports("pinecone"));
        assertTrue(adapter.supports("tablestore"));
        assertTrue(adapter.supports("vearch"));
        assertTrue(adapter.supports("vespa"));
        assertFalse(adapter.supports("unknown"));
    }

    @Test
    public void shouldCreateGenericStoreByMappingPropertiesToBuilderSetters() {
        LangChain4jEmbeddingStoreProviderAdapter adapter = new LangChain4jEmbeddingStoreProviderAdapter(testProviderMappings());
        EmbeddingStoreDefinition definition = definition("knowledge", "fake");
        definition.setDimension(768);
        definition.getMetadataKeys().add("tenant_id");
        definition.getProperties().put("host", "127.0.0.1");
        definition.getProperties().put("port", "9000");
        definition.getProperties().put("enabled", "true");
        definition.getProperties().put("scoreThreshold", "0.72");
        definition.getProperties().put("mode", "cosine");
        definition.getProperties().put("tags", Arrays.asList("rag", "vector"));

        FakeGenericEmbeddingStore store = (FakeGenericEmbeddingStore) adapter.create(definition);

        assertEquals("127.0.0.1", store.builder.host);
        assertEquals(Integer.valueOf(9000), store.builder.port);
        assertEquals(Boolean.TRUE, store.builder.enabled);
        assertEquals(Double.valueOf(0.72D), store.builder.scoreThreshold);
        assertEquals(FakeMode.COSINE, store.builder.mode);
        assertEquals(Arrays.asList("rag", "vector"), store.builder.tags);
        assertEquals(Integer.valueOf(768), store.builder.dimension);
        assertEquals(Arrays.asList("tenant_id"), store.builder.metadataKeys);
    }

    @Test
    public void shouldCreateGenericStoreWithConstructorArgumentsWhenBuilderIsAbsent() {
        LangChain4jEmbeddingStoreProviderAdapter adapter = new LangChain4jEmbeddingStoreProviderAdapter(testProviderMappings());
        EmbeddingStoreDefinition definition = definition("knowledge", "constructor-only");
        definition.getProperties().put("constructorArgs", Arrays.asList("127.0.0.1", "9000", "true"));

        ConstructorOnlyEmbeddingStore store = (ConstructorOnlyEmbeddingStore) adapter.create(definition);

        assertEquals("127.0.0.1", store.host);
        assertEquals(Integer.valueOf(9000), store.port);
        assertEquals(Boolean.TRUE, store.enabled);
    }

    @Test
    public void shouldWrapGenericStoreCreationFailureWithProviderAndKey() {
        LangChain4jEmbeddingStoreProviderAdapter adapter = new LangChain4jEmbeddingStoreProviderAdapter(testProviderMappings());
        EmbeddingStoreDefinition definition = definition("knowledge", "failing");

        try {
            adapter.create(definition);
            fail("Expected EmbeddingStoreException");
        } catch (EmbeddingStoreException ex) {
            assertTrue(ex.getMessage().contains("provider=failing"));
            assertTrue(ex.getMessage().contains("key=knowledge"));
        }
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

    private Map<String, String> testProviderMappings() {
        Map<String, String> mappings = new LinkedHashMap<String, String>();
        mappings.put("fake", FakeGenericEmbeddingStore.class.getName());
        mappings.put("failing", FailingGenericEmbeddingStore.class.getName());
        mappings.put("constructor-only", ConstructorOnlyEmbeddingStore.class.getName());
        return mappings;
    }

    public enum FakeMode {
        COSINE
    }

    public static class FakeGenericEmbeddingStore implements EmbeddingStore<TextSegment> {

        private final Builder builder;

        FakeGenericEmbeddingStore(Builder builder) {
            this.builder = builder;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String host;
            private Integer port;
            private Boolean enabled;
            private Double scoreThreshold;
            private FakeMode mode;
            private Collection<String> tags;
            private Integer dimension;
            private Collection<String> metadataKeys;

            public Builder host(String host) {
                this.host = host;
                return this;
            }

            public Builder port(Integer port) {
                this.port = port;
                return this;
            }

            public Builder enabled(Boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Builder scoreThreshold(Double scoreThreshold) {
                this.scoreThreshold = scoreThreshold;
                return this;
            }

            public Builder mode(FakeMode mode) {
                this.mode = mode;
                return this;
            }

            public Builder tags(Collection<String> tags) {
                this.tags = tags;
                return this;
            }

            public Builder dimension(Integer dimension) {
                this.dimension = dimension;
                return this;
            }

            public Builder metadataKeys(Collection<String> metadataKeys) {
                this.metadataKeys = metadataKeys;
                return this;
            }

            public FakeGenericEmbeddingStore build() {
                return new FakeGenericEmbeddingStore(this);
            }
        }

        @Override
        public String add(Embedding embedding) {
            return "id";
        }

        @Override
        public void add(String id, Embedding embedding) {
        }

        @Override
        public String add(Embedding embedding, TextSegment embedded) {
            return "id";
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            return Collections.emptyList();
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
            return Collections.emptyList();
        }
    }

    public static class FailingGenericEmbeddingStore implements EmbeddingStore<TextSegment> {

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            public FailingGenericEmbeddingStore build() {
                throw new IllegalStateException("failed");
            }
        }

        @Override
        public String add(Embedding embedding) {
            return "id";
        }

        @Override
        public void add(String id, Embedding embedding) {
        }

        @Override
        public String add(Embedding embedding, TextSegment embedded) {
            return "id";
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            return Collections.emptyList();
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
            return Collections.emptyList();
        }
    }

    public static class ConstructorOnlyEmbeddingStore implements EmbeddingStore<TextSegment> {

        private final String host;
        private final Integer port;
        private final Boolean enabled;

        public ConstructorOnlyEmbeddingStore(String host, Integer port, Boolean enabled) {
            this.host = host;
            this.port = port;
            this.enabled = enabled;
        }

        @Override
        public String add(Embedding embedding) {
            return "id";
        }

        @Override
        public void add(String id, Embedding embedding) {
        }

        @Override
        public String add(Embedding embedding, TextSegment embedded) {
            return "id";
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            return Collections.emptyList();
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
            return Collections.emptyList();
        }
    }
}
