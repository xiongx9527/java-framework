package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LangChain4jEmbeddingStoreArchitectureTest {

    private static final String CORE_PACKAGE = "com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.";
    private static final String AUTOCONFIGURE_PACKAGE =
            "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.";

    @Test
    public void shouldExposeOnlyTheNeutralLangChain4jStoreChain() throws Exception {
        assertPresent(CORE_PACKAGE + "DefaultLangChain4jEmbeddingStoreAdapter");
        assertPresent(CORE_PACKAGE + "LangChain4jEmbeddingStoreProviderAdapter");
        assertPresent(CORE_PACKAGE + "LangChain4jAiEmbeddingStore");

        for (String legacyType : Arrays.asList(
                "DefaultEmbeddingStoreFactory",
                "DefaultEmbeddingStoreRegistry",
                "EmbeddingStoreDefinition",
                "EmbeddingStoreException",
                "EmbeddingStoreFactory",
                "EmbeddingStoreProperties",
                "EmbeddingStoreProviderAdapter",
                "EmbeddingStoreRegistry")) {
            assertAbsent(CORE_PACKAGE + legacyType);
        }
    }

    @Test
    public void shouldKeepTheStoreAutoConfigurationPackageDeep() throws Exception {
        assertPresent(AUTOCONFIGURE_PACKAGE + "LangChain4jEmbeddingStoreAdapterAutoConfiguration");
        assertPresent(AUTOCONFIGURE_PACKAGE + "LangChain4jEmbeddingStoreProviderSpec");
        assertPresent(AUTOCONFIGURE_PACKAGE + "ReflectiveLangChain4jEmbeddingStoreProviderAdapter");
        assertPresent(AUTOCONFIGURE_PACKAGE + "ProviderAdapterSupport");

        List<String> removedProviderTypes = Arrays.asList(
                "AstraDbEmbeddingStoreProviderAdapter",
                "AzureAiSearchEmbeddingStoreProviderAdapter",
                "AzureCosmosMongoVCoreEmbeddingStoreProviderAdapter",
                "AzureCosmosNoSqlEmbeddingStoreProviderAdapter",
                "CassandraEmbeddingStoreProviderAdapter",
                "ChromaEmbeddingStoreProviderAdapter",
                "CouchbaseEmbeddingStoreProviderAdapter",
                "ElasticsearchEmbeddingStoreProviderAdapter",
                "InMemoryEmbeddingStoreProviderAdapter",
                "InfinispanEmbeddingStoreProviderAdapter",
                "LangChain4jEmbeddingStoreProviderAdapter",
                "MilvusEmbeddingStoreProviderAdapter",
                "MongoDbEmbeddingStoreProviderAdapter",
                "Neo4jEmbeddingStoreProviderAdapter",
                "OpenSearchEmbeddingStoreProviderAdapter",
                "OracleEmbeddingStoreProviderAdapter",
                "PgVectorEmbeddingStoreProviderAdapter",
                "PineconeEmbeddingStoreProviderAdapter",
                "QdrantEmbeddingStoreProviderAdapter",
                "RedisStackEmbeddingStoreProviderAdapter",
                "ReflectiveEmbeddingStoreProviderAdapter",
                "TablestoreEmbeddingStoreProviderAdapter",
                "VearchEmbeddingStoreProviderAdapter",
                "VespaEmbeddingStoreProviderAdapter",
                "WeaviateEmbeddingStoreProviderAdapter");
        for (String removedProviderType : removedProviderTypes) {
            assertAbsent(AUTOCONFIGURE_PACKAGE + removedProviderType);
        }
    }

    private void assertPresent(String className) {
        Class<?> type = assertDoesNotThrow(
                () -> Class.forName(className, false, getClass().getClassLoader()), className);
        assertNotNull(type, className);
    }

    private void assertAbsent(String className) {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName(className, false, getClass().getClassLoader()), className);
    }
}
