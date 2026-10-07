package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryTenantResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.ChatMemoryStoreProviderFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.Langchain4jChatMemoryStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.MagicDynamicLangchain4jChatMemoryStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.module.Langchain4jChatMemoryStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.registry.Langchain4jChatMemoryStoreMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.resource.Langchain4jChatMemoryStoreMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

/**
 * {@code langchain4j-chat-memory-store} 数据源的装配：实例持有者、资源存储、动态注册器与脚本模块
 * {@code langchain4jChatMemoryStore['key']}。
 *
 * <p>每个资源定义通过 {@code type} 选择内置提供商（in-memory / jdbc / redis / mongodb /
 * elasticsearch），实例由 {@link ChatMemoryStoreProviderFactory} 构建：定义了
 * {@code datasourceKey} 时使用 magic-api 同名命名数据源实例，否则回退 Spring 容器默认客户端 Bean。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.store.memory.chat.ChatMemoryStore")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.chat-memory-store", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jChatMemoryStoreConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-chat-memory-store.1.0.0.iife.js";

	public Langchain4jChatMemoryStoreConfiguration() {
		super("langchain4j-chat-memory-store", "MagicLangchain4jChatMemoryStore", JAVASCRIPT_FILENAME, false);
	}

	/** 提供商工厂：按资源 type 与 datasourceKey 构建 ChatMemoryStore。 */
	@Bean
	@ConditionalOnMissingBean
	public ChatMemoryStoreProviderFactory chatMemoryStoreProviderFactory(
			ObjectProvider<MagicDynamicDataSource> dynamicDataSource,
			ObjectProvider<MagicDynamicRedisDataSource> redisDataSource,
			ObjectProvider<MagicDynamicMongoDataSource> mongoDataSource,
			ObjectProvider<MagicDynamicElasticSearchDataSource> elasticSearchDataSource,
			ObjectProvider<JdbcTemplate> jdbcTemplate, ObjectProvider<StringRedisTemplate> redisTemplate,
			ObjectProvider<MongoTemplate> mongoTemplate, ObjectProvider<ElasticsearchClient> elasticsearchClient,
			ObjectProvider<RestClient> restClient, ObjectProvider<ChatMemoryTenantResolver> tenantResolver,
			ObjectProvider<ChatMemoryStoreProperties> properties) {
		return new ChatMemoryStoreProviderFactory(dynamicDataSource, redisDataSource, mongoDataSource,
				elasticSearchDataSource, jdbcTemplate, redisTemplate, mongoTemplate, elasticsearchClient,
				restClient, tenantResolver, properties);
	}

	/** 数据源实例持有者：按 key 持有多个 ChatMemoryStore 实例。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicLangchain4jChatMemoryStoreDataSource langchain4jChatMemoryStoreDataSource(
			ChatMemoryStoreProviderFactory providerFactory) {
		return new MagicDynamicLangchain4jChatMemoryStoreDataSource(providerFactory);
	}

	/** langchain4j-chat-memory-store 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jChatMemoryStoreMagicResourceStorage langchain4jChatMemoryStoreMagicResourceStorage() {
		return new Langchain4jChatMemoryStoreMagicResourceStorage();
	}

	/** langchain4j-chat-memory-store 动态注册器：资源变更即增删 ChatMemoryStore 实例。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jChatMemoryStoreMagicDynamicRegistry langchain4jChatMemoryStoreMagicDynamicRegistry(
			Langchain4jChatMemoryStoreMagicResourceStorage storage,
			MagicDynamicLangchain4jChatMemoryStoreDataSource dataSource,
			ObjectProvider<ChatMemoryStore> defaultInstance) {
		return new Langchain4jChatMemoryStoreMagicDynamicRegistry(storage, dataSource, defaultInstance);
	}

	/** 脚本模块：langchain4jChatMemoryStore['key'] 按 key 取 ChatMemoryStore。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jChatMemoryStoreModule langchain4jChatMemoryStoreModule(
			MagicDynamicLangchain4jChatMemoryStoreDataSource dataSource,
			ObjectProvider<ChatMemoryStore> defaultInstance) {
		return new Langchain4jChatMemoryStoreModule(dataSource, defaultInstance);
	}
}
