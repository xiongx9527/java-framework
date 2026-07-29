package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider.*;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.DefaultEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.ai.langchain4j.adapter.DefaultEmbeddingStoreFactory")
public class LangChain4jEmbeddingStoreAdapterAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "redisStackEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter redisStackEmbeddingStoreProviderAdapter() {
		return new RedisStackEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "milvusEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter milvusEmbeddingStoreProviderAdapter() {
		return new MilvusEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "elasticsearchEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter elasticsearchEmbeddingStoreProviderAdapter() {
		return new ElasticsearchEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "qdrantEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter qdrantEmbeddingStoreProviderAdapter() {
		return new QdrantEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "weaviateEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter weaviateEmbeddingStoreProviderAdapter() {
		return new WeaviateEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "astraDbEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter astraDbEmbeddingStoreProviderAdapter() {
		return new AstraDbEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "azureAiSearchEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter azureAiSearchEmbeddingStoreProviderAdapter(
			ApplicationContext applicationContext) {
		return new AzureAiSearchEmbeddingStoreProviderAdapter(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(name = "azureCosmosMongoVCoreEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter azureCosmosMongoVCoreEmbeddingStoreProviderAdapter() {
		return new AzureCosmosMongoVCoreEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "azureCosmosNoSqlEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter azureCosmosNoSqlEmbeddingStoreProviderAdapter(
			ApplicationContext applicationContext) {
		return new AzureCosmosNoSqlEmbeddingStoreProviderAdapter(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(name = "cassandraEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter cassandraEmbeddingStoreProviderAdapter() {
		return new CassandraEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "chromaEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter chromaEmbeddingStoreProviderAdapter() {
		return new ChromaEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "couchbaseEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter couchbaseEmbeddingStoreProviderAdapter() {
		return new CouchbaseEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "inMemoryEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter inMemoryEmbeddingStoreProviderAdapter() {
		return new InMemoryEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "infinispanEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter infinispanEmbeddingStoreProviderAdapter() {
		return new InfinispanEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "mongoDbEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter mongoDbEmbeddingStoreProviderAdapter() {
		return new MongoDbEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "neo4jEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter neo4jEmbeddingStoreProviderAdapter() {
		return new Neo4jEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "openSearchEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter openSearchEmbeddingStoreProviderAdapter() {
		return new OpenSearchEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "oracleEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter oracleEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
		return new OracleEmbeddingStoreProviderAdapter(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(name = "pgVectorEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter pgVectorEmbeddingStoreProviderAdapter() {
		return new PgVectorEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "pineconeEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter pineconeEmbeddingStoreProviderAdapter() {
		return new PineconeEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "tablestoreEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter tablestoreEmbeddingStoreProviderAdapter(
			ApplicationContext applicationContext) {
		return new TablestoreEmbeddingStoreProviderAdapter(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(name = "vearchEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter vearchEmbeddingStoreProviderAdapter() {
		return new VearchEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "vespaEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter vespaEmbeddingStoreProviderAdapter() {
		return new VespaEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "langChain4jEmbeddingStoreProviderAdapter")
	public EmbeddingStoreProviderAdapter langChain4jEmbeddingStoreProviderAdapter() {
		return new LangChain4jEmbeddingStoreProviderAdapter();
	}

	@Bean
	@ConditionalOnMissingBean(name = "langChain4jAiEmbeddingStoreAdapter")
	public AiEmbeddingStoreAdapter langChain4jAiEmbeddingStoreAdapter(
			List<EmbeddingStoreProviderAdapter> adapters) {
		return new DefaultEmbeddingStoreFactory(adapters);
	}
}
