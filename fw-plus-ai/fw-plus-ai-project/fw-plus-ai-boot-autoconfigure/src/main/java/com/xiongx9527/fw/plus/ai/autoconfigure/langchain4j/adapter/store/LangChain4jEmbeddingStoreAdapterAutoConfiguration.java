package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.DefaultLangChain4jEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.absent;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.all;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.any;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.bean;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.defined;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.present;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.undefined;
import static com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderSpec.property;

@Configuration
@ConditionalOnClass(DefaultLangChain4jEmbeddingStoreAdapter.class)
public class LangChain4jEmbeddingStoreAdapterAutoConfiguration {

    private static final String PREFIX = "dev.langchain4j.store.embedding.";

    @Bean
    @ConditionalOnMissingBean(name = "redisStackEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter redisStackEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("redisstack", PREFIX + "redis.RedisEmbeddingStore", "redis")
                .require("host", "port", "indexName", "dimension")
                .builderPlan()
                .property("host", "host")
                .property("port", "port")
                .property("user", "username")
                .property("password", "password")
                .property("indexName", "indexName")
                .property("dimension", "dimension")
                .property("metadataKeys", "metadataKeys")
                .end().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "milvusEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter milvusEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("milvus", PREFIX + "milvus.MilvusEmbeddingStore")
                .requiredClasses("io.milvus.common.clientenum.ConsistencyLevelEnum",
                        "io.milvus.param.IndexType", "io.milvus.param.MetricType")
                .requireAny("host", "uri")
                .requireWhen(defined("host"), "host")
                .require("collectionName", "dimension")
                .builderPlan()
                .property("host", "host")
                .property("port", "port")
                .property("uri", "uri")
                .property("collectionName", "collectionName")
                .property("dimension", "dimension")
                .property("token", "apiKey", "token")
                .property("username", "username")
                .property("password", "password")
                .property("databaseName", "databaseName")
                .property("indexType", "indexType")
                .property("metricType", "metricType")
                .property("consistencyLevel", "consistencyLevel")
                .property("retrieveEmbeddingsOnSearch", "retrieveEmbeddingsOnSearch")
                .property("autoFlushOnInsert", "autoFlushOnInsert")
                .end().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "elasticsearchEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter elasticsearchEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("elasticsearch", PREFIX + "elasticsearch.ElasticsearchEmbeddingStore")
                .require("uri", "indexName", "dimension")
                .builderPlan()
                .property("serverUrl", "uri")
                .property("apiKey", "apiKey")
                .property("userName", "username")
                .property("password", "password")
                .property("indexName", "indexName")
                .property("dimension", "dimension")
                .end().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "qdrantEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter qdrantEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("qdrant", PREFIX + "qdrant.QdrantEmbeddingStore")
                .require("host", "port", "collectionName")
                .builderPlan()
                .property("host", "host")
                .property("port", "port")
                .property("collectionName", "collectionName")
                .property("apiKey", "apiKey")
                .property("payloadTextKey", "payloadTextKey")
                .property("useTls", "useTls", "utls")
                .end().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "weaviateEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter weaviateEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("weaviate", PREFIX + "weaviate.WeaviateEmbeddingStore")
                .require("scheme", "host", "port", "className")
                .constructorPlan(property("apiKey"), property("scheme"), property("host"),
                        property("port"), property("useGrpcForInserts"), property("securedGrpc"),
                        property("grpcPort"), property("className"), property("avoidDups"),
                        property("consistencyLevel"), property("metadataKeys"), property("textFieldName"))
                .build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "astraDbEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter astraDbEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("astra-db", PREFIX + "astradb.AstraDbEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "azureAiSearchEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter azureAiSearchEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("azure-ai-search", PREFIX + "azure.search.AzureAiSearchEmbeddingStore")
                .requiredClasses("dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchFilterMapper",
                        "com.azure.core.credential.TokenCredential",
                        "com.azure.search.documents.indexes.models.SearchIndex")
                .require("endpoint", "indexName")
                .requireAny("dimensions", "dimension")
                .builderPlan()
                .property("endpoint", "endpoint")
                .property("apiKey", "apiKey")
                .property("indexName", "indexName")
                .property("dimensions", "dimensions", "dimension")
                .property("createOrUpdateIndex", "createOrUpdateIndex")
                .beanProperty("tokenCredential", "tokenCredentialBean")
                .beanProperty("index", "indexBean")
                .beanProperty("filterMapper", "filterMapperBean")
                .end().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "azureCosmosMongoVCoreEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter azureCosmosMongoVCoreEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("azure-cosmos-mongo-vcore",
                PREFIX + "azure.cosmos.mongo.vcore.AzureCosmosDbMongoVCoreEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "azureCosmosNoSqlEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter azureCosmosNoSqlEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("azure-cosmos-nosql",
                        PREFIX + "azure.cosmos.nosql.AzureCosmosDbNoSqlEmbeddingStore")
                .requireAny("cosmosClient", "cosmosClientBean")
                .require("databaseName", "containerName")
                .builderPlan()
                .passthrough()
                .beanProperty("cosmosClient", "cosmosClient", "cosmosClientBean")
                .beanProperty("cosmosVectorEmbeddingPolicy", "cosmosVectorEmbeddingPolicy",
                        "cosmosVectorEmbeddingPolicyBean")
                .beanProperty("cosmosVectorIndexes", "cosmosVectorIndexes", "cosmosVectorIndexesBean")
                .beanProperty("containerProperties", "containerProperties", "containerPropertiesBean")
                .end().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "cassandraEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter cassandraEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("cassandra", PREFIX + "cassandra.CassandraEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "chromaEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter chromaEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("chroma", PREFIX + "chroma.ChromaEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "couchbaseEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter couchbaseEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("couchbase", PREFIX + "couchbase.CouchbaseEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "inMemoryEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter inMemoryEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("in-memory", PREFIX + "inmemory.InMemoryEmbeddingStore", context,
                "inmemory", "memory");
    }

    @Bean
    @ConditionalOnMissingBean(name = "infinispanEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter infinispanEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("infinispan", PREFIX + "infinispan.InfinispanEmbeddingStore")
                .require("cacheName", "dimension")
                .auto().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mongoDbEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter mongoDbEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("mongodb-atlas", PREFIX + "mongodb.MongoDbEmbeddingStore", context, "mongodb");
    }

    @Bean
    @ConditionalOnMissingBean(name = "neo4jEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter neo4jEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("neo4j", PREFIX + "neo4j.Neo4jEmbeddingStore")
                .require("dimension")
                .requireWhen(undefined("driver"), "uri", "username", "password")
                .auto().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "openSearchEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter openSearchEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("opensearch", PREFIX + "opensearch.OpenSearchEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "oracleEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter oracleEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("oracle", PREFIX + "oracle.OracleEmbeddingStore")
                .require("dataSourceBean", "embeddingTable")
                .builderPlan()
                .beanProperty("dataSource", "dataSourceBean")
                .invoke("embeddingTable", absent("embeddingTableCreateOption"), property("embeddingTable"))
                .invoke("embeddingTable", present("embeddingTableCreateOption"),
                        property("embeddingTable"), property("embeddingTableCreateOption"))
                .invoke("vectorIndex", present("vectorIndex"), property("vectorIndex"))
                .property("exactSearch", "exactSearch")
                .end().build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "pgVectorEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter pgVectorEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("pgvector", PREFIX + "pgvector.PgVectorEmbeddingStore", context,
                "postgres", "postgresql");
    }

    @Bean
    @ConditionalOnMissingBean(name = "pineconeEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter pineconeEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("pinecone", PREFIX + "pinecone.PineconeEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "tablestoreEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter tablestoreEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec
                .builder("tablestore", PREFIX + "tablestore.TablestoreEmbeddingStore")
                .requireWhen(absent("constructorArgs"), "clientBean", "dimension")
                .requireWhen(all(absent("constructorArgs"), present("tableName")),
                        "searchIndexName", "pkName", "textField", "embeddingField")
                .rawConstructorArgsPlan(present("constructorArgs"))
                .constructorPlan(all(absent("constructorArgs"), absent("tableName"),
                                absent("metadataSchemaList"), absent("metadataSchemaListBean")),
                        bean("clientBean"), property("dimension"))
                .constructorPlan(all(absent("constructorArgs"), absent("tableName"),
                                any(present("metadataSchemaList"), present("metadataSchemaListBean"))),
                        bean("clientBean"), property("dimension"),
                        bean("metadataSchemaList", "metadataSchemaListBean"))
                .constructorPlan(all(absent("constructorArgs"), present("tableName")),
                        bean("clientBean"), property("tableName"),
                        property("searchIndexName"), property("pkName"), property("textField"),
                        property("embeddingField"), property("dimension"), property("vectorMetricType"),
                        bean("metadataSchemaList", "metadataSchemaListBean"))
                .build(), context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "vearchEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter vearchEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("vearch", PREFIX + "vearch.VearchEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "vespaEmbeddingStoreProviderAdapter")
    public LangChain4jEmbeddingStoreProviderAdapter vespaEmbeddingStoreProviderAdapter(
            ApplicationContext context) {
        return automatic("vespa", PREFIX + "vespa.VespaEmbeddingStore", context);
    }

    @Bean
    @ConditionalOnMissingBean(name = "langChain4jAiEmbeddingStoreAdapter")
    public AiEmbeddingStoreAdapter langChain4jAiEmbeddingStoreAdapter(
            Map<String, LangChain4jEmbeddingStoreProviderAdapter> providerAdapters) {
        return new DefaultLangChain4jEmbeddingStoreAdapter(providerAdapters);
    }

    private LangChain4jEmbeddingStoreProviderAdapter automatic(
            String provider, String className, ApplicationContext context, String... aliases) {
        return adapter(LangChain4jEmbeddingStoreProviderSpec.builder(provider, className, aliases)
                .auto().build(), context);
    }

    private LangChain4jEmbeddingStoreProviderAdapter adapter(
            LangChain4jEmbeddingStoreProviderSpec spec, ApplicationContext context) {
        return new ReflectiveLangChain4jEmbeddingStoreProviderAdapter(spec, context);
    }
}
