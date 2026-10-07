package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.MagicDynamicLangchain4jContentRetrieverDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.module.Langchain4jContentRetrieverModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.AggregatingContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.AzureAiSearchContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.EmbeddingStoreContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.FilteringContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.NebulaContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.Neo4jContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider.WebSearchContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbKnowledgeState;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.registry.Langchain4jContentRetrieverMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource.Langchain4jContentRetrieverMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.web.Langchain4jContentRetrieverController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.ScriptManagerMagicToolInvoker;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.web.search.WebSearchEngine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code langchain4j-content-retriever} 数据源的装配：provider 分发表、实例持有者、资源存储、
 * 动态注册器与脚本模块 {@code langchain4jContentRetriever['key']}。
 *
 * <p>五种 provider 即五种 {@link ContentRetriever} 实现：embedding-store / neo4j /
 * azure-ai-search / web-search / filtering。被引用的向量库与模型按各自 datasource key 取用，
 * 这些类型可独立停用，故一律用 {@link ObjectProvider} 装配，缺失时在构建期给出可操作提示。</p>
 *
 * <p>插件管理员可见性（adminEnabled）跟随知识库装配：KnowledgeConfiguration 生效
 * （SqlSessionFactory + auth-core 就绪且 knowledge.enabled 未关闭）时该插件仅管理员可见，
 * 否则普通可见。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.rag.content.retriever.ContentRetriever")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.content-retriever", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jContentRetrieverConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-content-retriever.1.0.0.iife.js";

	/** 惰性依赖：两个 bean 都由本类声明，构造期尚未就绪，注册 controller 时才取用 */
	private final ObjectProvider<ContentRetrieverProviderRegistry> providers;

	private final ObjectProvider<ContentRetrieverContext> context;

	/** 知识库装配标志：KnowledgeConfiguration 装配时强制注册，缺失即知识库未启动 */
	private final ObjectProvider<KbKnowledgeState> knowledgeStates;

	public Langchain4jContentRetrieverConfiguration(ObjectProvider<ContentRetrieverProviderRegistry> providers,
			ObjectProvider<ContentRetrieverContext> context,
			ObjectProvider<KbKnowledgeState> knowledgeStates) {
		super("langchain4j-content-retriever", "MagicLangchain4jContentRetriever", JAVASCRIPT_FILENAME,
				knowledgeStates.getIfAvailable() != null);
		this.providers = providers;
		this.context = context;
		this.knowledgeStates = knowledgeStates;
	}

	/** 连接测试 Controller 由本类承载：路径 {@code /langchain4j/content-retriever/datasource/test}。 */
	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> mapping.registerController(new Langchain4jContentRetrieverController(
				configuration, providers.getObject(), context.getObject()));
	}

	/** provider 分发表：新增 provider 在此登记；也可直接注册 {@link ContentRetrieverProvider} bean 扩展。 */
	@Bean
	@ConditionalOnMissingBean
	public ContentRetrieverProviderRegistry langchain4jContentRetrieverProviderRegistry(
			ObjectProvider<ContentRetrieverProvider> providerProvider) {
		ContentRetrieverProviderRegistry registry = new ContentRetrieverProviderRegistry();
		registry.register(new EmbeddingStoreContentRetrieverProvider());
		registry.register(new Neo4jContentRetrieverProvider());
		registry.register(new NebulaContentRetrieverProvider());
		registry.register(new AzureAiSearchContentRetrieverProvider());
		registry.register(new WebSearchContentRetrieverProvider());
		registry.register(new FilteringContentRetrieverProvider());
		registry.register(new AggregatingContentRetrieverProvider());
		for (ContentRetrieverProvider provider : providerProvider) {
			if (provider != null) {
				registry.register(provider);
			}
		}
		return registry;
	}

	/** 数据源实例持有者：按 key 持有多个 ContentRetriever 实例。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicLangchain4jContentRetrieverDataSource langchain4jContentRetrieverDataSource() {
		return new MagicDynamicLangchain4jContentRetrieverDataSource();
	}

	/** provider 构建时的依赖视图：被引用的向量库 / 模型 / 另一个 retriever 都从这里取。 */
	@Bean
	@ConditionalOnMissingBean
	public ContentRetrieverContext langchain4jContentRetrieverContext(
			ObjectProvider<Langchain4jEmbeddingStoreModule> embeddingStores,
			ObjectProvider<Langchain4jEmbeddingModelModule> embeddingModels,
			ObjectProvider<Langchain4jChatModelModule> chatModels,
			Langchain4jContentRetrieverModule retrievers,
			ObjectProvider<WebSearchEngine> webSearchEngines,
			ObjectProvider<MagicToolInvoker> invokerProvider) {
		MagicToolInvoker invoker = invokerProvider.getIfAvailable(ScriptManagerMagicToolInvoker::new);
		return new ContentRetrieverContext(embeddingStores, embeddingModels, chatModels, retrievers,
				webSearchEngines, invoker);
	}

	/** langchain4j-content-retriever 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jContentRetrieverMagicResourceStorage langchain4jContentRetrieverMagicResourceStorage(
			ContentRetrieverProviderRegistry providers) {
		return new Langchain4jContentRetrieverMagicResourceStorage(providers.providers());
	}

	/** langchain4j-content-retriever 动态注册器：资源变更即增删 ContentRetriever 实例。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jContentRetrieverMagicDynamicRegistry langchain4jContentRetrieverMagicDynamicRegistry(
			Langchain4jContentRetrieverMagicResourceStorage storage,
			MagicDynamicLangchain4jContentRetrieverDataSource dataSource,
			ContentRetrieverProviderRegistry providers,
			ContentRetrieverContext context,
			ObjectProvider<ContentRetriever> defaultInstance) {
		return new Langchain4jContentRetrieverMagicDynamicRegistry(storage, dataSource, providers, context,
				defaultInstance);
	}

	/** 脚本模块：langchain4jContentRetriever['key'] 按 key 取 ContentRetriever。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jContentRetrieverModule langchain4jContentRetrieverModule(
			MagicDynamicLangchain4jContentRetrieverDataSource dataSource,
			ObjectProvider<ContentRetriever> defaultInstance) {
		return new Langchain4jContentRetrieverModule(dataSource, defaultInstance);
	}
}
