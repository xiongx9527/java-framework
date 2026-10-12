package com.xiongx9527.fw.plus.ai.nl2sql;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

/**
 * NL2SQL 自动装配。开关：fw-plus.ai.nl2sql.enabled=true。
 */
@AutoConfiguration
@EnableConfigurationProperties(Nl2SqlProperties.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.nl2sql", name = "enabled", havingValue = "true")
public class Nl2SqlAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ChatModel nl2sqlChatModel(Nl2SqlProperties props) {
		return OllamaChatModel.builder()
				.baseUrl(props.getChat().getBaseUrl())
				.modelName(props.getChat().getModelName())
				.temperature(props.getChat().getTemperature())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public EmbeddingModel nl2sqlEmbeddingModel(Nl2SqlProperties props) {
		return OllamaEmbeddingModel.builder()
				.baseUrl(props.getEmbedding().getBaseUrl())
				.modelName(props.getEmbedding().getModelName())
				.build();
	}

	@Bean(destroyMethod = "")
	@ConditionalOnMissingBean
	public EmbeddingStore<TextSegment> nl2sqlEmbeddingStore(Nl2SqlProperties props) {
		Nl2SqlProperties.Qdrant qdrant = props.getQdrant();
		return QdrantEmbeddingStore.builder()
				.host(qdrant.getHost())
				.port(qdrant.getPort())
				.apiKey(qdrant.getApiKey())
				.collectionName(qdrant.getCollectionName())
				.useTls(qdrant.isUseTls())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public SqlSchemaTrainer nl2sqlSchemaTrainer(DataSource dataSource,
			EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, Nl2SqlProperties props) {
		return new SqlSchemaTrainer(dataSource, embeddingStore, embeddingModel,
				props.getTable(), true);
	}

	@Bean
	@ConditionalOnMissingBean
	public SchemaRetriever nl2sqlSchemaRetriever(EmbeddingStore<TextSegment> embeddingStore,
			EmbeddingModel embeddingModel, Nl2SqlProperties props) {
		return new SchemaRetriever(embeddingStore, embeddingModel,
				props.getRag().getTopK(), props.getRag().getMinScore());
	}

	@Bean
	@ConditionalOnMissingBean
	public PromptTemplate nl2sqlPromptTemplate() {
		return PromptTemplate.from("""
				你是一名 MySQL 数据库专家，负责把用户的自然语言问题翻译成可执行的 SELECT 语句。

				数据库方言: {{sqlDialect}}
				以下是本次查询可用的表结构（只允许使用这些表与列，禁止臆造）：
				{{databaseStructure}}

				规则：
				1. 只输出一条 SELECT 语句，不要输出任何解释、注释或 markdown 围栏；
				2. 不要生成 INSERT/UPDATE/DELETE/ALTER/DROP 等写语句；
				3. 涉及相对时间（如"最近一个月""今年"）时，以当前日期 2026-10-10 为基准计算，不要凭记忆造年份；
				4. 尽量使用表注释中给出的业务语义；列名模糊时优先选择注释或命名最贴切的列；
				5. 只要一条语句能回答，就不要写多条。
				""");
	}

	@Bean
	@ConditionalOnMissingBean
	public Nl2SqlService nl2SqlService(DataSource dataSource, ChatModel chatModel,
			SchemaRetriever schemaRetriever, SqlSchemaTrainer schemaTrainer,
			PromptTemplate promptTemplate, Nl2SqlProperties props) {
		return new Nl2SqlService(dataSource, chatModel, schemaRetriever, schemaTrainer,
				promptTemplate, props);
	}
}