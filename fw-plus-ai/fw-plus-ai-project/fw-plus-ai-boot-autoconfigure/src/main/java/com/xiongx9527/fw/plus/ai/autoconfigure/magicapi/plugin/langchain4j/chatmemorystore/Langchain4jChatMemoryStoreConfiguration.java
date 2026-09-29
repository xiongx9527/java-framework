package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.MagicDynamicLangchain4jChatMemoryStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.module.Langchain4jChatMemoryStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.registry.Langchain4jChatMemoryStoreMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.resource.Langchain4jChatMemoryStoreMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.ScriptManagerMagicToolInvoker;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code langchain4j-chat-memory-store} 数据源的装配：实例持有者、资源存储、动态注册器与脚本模块
 * {@code langchain4jChatMemoryStore['key']}。
 *
 * <p>把 magic-api 函数包装成 LangChain4j {@code ChatMemoryStore}：get/update/delete 三个动作
 * 分别委托给数据源配置的三个函数执行。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.store.memory.chat.ChatMemoryStore")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.chat-memory-store", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jChatMemoryStoreConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-chat-memory-store.1.0.0.iife.js";

	public Langchain4jChatMemoryStoreConfiguration() {
		super("langchain4j-chat-memory-store", "MagicLangchain4jChatMemoryStore", JAVASCRIPT_FILENAME);
	}

	/** 数据源实例持有者：按 key 持有多个 ChatMemoryStore 实例。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicLangchain4jChatMemoryStoreDataSource langchain4jChatMemoryStoreDataSource(
			ObjectProvider<MagicToolInvoker> invokerProvider) {
		MagicToolInvoker invoker = invokerProvider.getIfAvailable(ScriptManagerMagicToolInvoker::new);
		return new MagicDynamicLangchain4jChatMemoryStoreDataSource(invoker);
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
