package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryTenantResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.ChatMemoryStoreProviderFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.Langchain4jChatMemoryStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.MagicDynamicLangchain4jChatMemoryStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.module.Langchain4jChatMemoryStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.registry.Langchain4jChatMemoryStoreMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.resource.Langchain4jChatMemoryStoreMagicResourceStorage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

	/**
	 * 提供商工厂：按资源 type 与 datasourceKey 构建 ChatMemoryStore。
	 *
	 * <p>签名只保留本仓库内的类型；可选客户端（JdbcTemplate / StringRedisTemplate /
	 * MongoTemplate / ElasticsearchClient / RestClient）与 magic 插件持有者经
	 * {@link ApplicationContext} 按需懒解析——{@code ObjectProvider<T>} 签名里的 T 会被
	 * Spring 反射解析，对应 optional 依赖不在 classpath 时会抛 TypeNotPresentException
	 * 导致应用启动失败（如样例未引 spring-data-redis 时）。</p>
	 */
	@Bean
	@ConditionalOnMissingBean
	public ChatMemoryStoreProviderFactory chatMemoryStoreProviderFactory(
			ObjectProvider<ChatMemoryTenantResolver> tenantResolver,
			ObjectProvider<ChatMemoryStoreProperties> properties,
			ApplicationContext context) {
		return new ChatMemoryStoreProviderFactory(context, tenantResolver, properties);
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
