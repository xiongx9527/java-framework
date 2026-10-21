package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessContract;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.LangChain4jIntentAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.logging.ChatContextLogger;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryTenantResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.Mem0ChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts.FactStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts.Mem0FactStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.InMemorySummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag.DifyContentRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag.DifyRagProperties;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.utils.DatasetBuilder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

/**
 * LangChain4j 支撑组件装配。
 *
 * <p>按 {@code fw.plus.ai.langchain4j.chat-memory.type} 条件注入一个
 * {@link ChatMemoryStore} Bean（含进程内实现）：{@code in-memory}（默认兜底）/
 * {@code mem0}（mem0 兼容 REST 服务，连接参数见 {@link ChatMemoryStoreProperties.Mem0}，
 * 以 {@code @ConditionalOnMissingBean} 优先使用方自定义 Bean）。
 * mem0 服务地址错误或不可达时调用期快速失败，避免静默降级。</p>
 *
 * <p>按 {@code fw.plus.ai.langchain4j.dify-rag.enabled=true} 条件注入
 * {@link ContentRetriever} Bean（{@link DifyContentRetriever}，调用 dify 知识库检索接口，
 * 配置见 {@link DifyRagProperties}），检索故障降级空列表不打断对话。</p>
 *
 * <p>两个存储配置类与 DifyRagProperties 配置类经 {@code @Import} 显式导入
 * （而非依赖嵌套 member 自动发现）；DifyRagProperties 因 {@code fw-plus-dify} 是
 * optional 依赖而按类存在性条件注册——消费方没有 dify 类时跳过，
 * 避免配置类方法签名 introspection 时 {@code NoClassDefFoundError} 拉垮整个上下文。</p>
 *
 * <p>信封编排（{@link EnvelopeRuntime}）的装配同样经 {@code @Import} 收口
 * （{@code AssistantEnvelopeConfiguration}，条件为
 * {@code fw.plus.ai.langchain4j.assistant.envelope.enabled=true}），
 * 不改本类的注册文件与既有 {@code @Bean} 签名。</p>
 *
 * <p>意图识别自动配置 {@link LangChain4jIntentAutoConfiguration} 亦在此 {@code @Import}
 * 收口，其自身条件与本源一致，无需在 {@code AutoConfiguration.imports} 中单独登记。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.Experimental")
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChatMemoryStoreProperties.class)
	@Import({LangChain4jSupportAutoConfiguration.Mem0ChatMemoryStoreConfiguration.class,
			LangChain4jSupportAutoConfiguration.InMemoryChatMemoryStoreConfiguration.class,
			LangChain4jSupportAutoConfiguration.DifyRagPropertiesConfiguration.class,
			LangChain4jSupportAutoConfiguration.AssistantEnvelopeConfiguration.class,
			LangChain4jSupportAutoConfiguration.SummaryMemoryConfiguration.class,
			LangChain4jSupportAutoConfiguration.FactMemoryConfiguration.class,
			LangChain4jIntentAutoConfiguration.class})
public class LangChain4jSupportAutoConfiguration {

	@Bean
	public ChatContextLogger chatContextLogger() {
		return new ChatContextLogger();
	}

	/** 默认租户解析器：返回配置的 default-tenant-id；使用方自定义 Bean 即自动覆盖 */
	@Bean
	@ConditionalOnMissingBean(ChatMemoryTenantResolver.class)
	public ChatMemoryTenantResolver chatMemoryTenantResolver(ChatMemoryStoreProperties properties) {
		return () -> properties.getDefaultTenantId();
	}

	/**
	 * {@code type} 取值校验：未知取值（如旧配置残留 {@code type=redis/jdbc}）启动即失败，
	 * 避免条件全不匹配导致 ChatMemoryStore 静默缺失、会话记忆无声消失。
	 */
	@Bean
	public InitializingBean chatMemoryStoreTypeValidator(ChatMemoryStoreProperties properties) {
		return () -> {
			String type = properties.getType();
			if (type == null) {
				return;
			}
			String normalized = type.trim();
			if (!ChatMemoryStoreProperties.TYPE_IN_MEMORY.equals(normalized)
					&& !ChatMemoryStoreProperties.TYPE_MEM0.equals(normalized)) {
				throw new IllegalStateException(String.format(
						"%s 取值非法: '%s'，仅支持 %s / %s；使用自定义 ChatMemoryStore Bean 时请移除该配置",
						ChatMemoryStoreProperties.PREFIX + ".type", type,
						ChatMemoryStoreProperties.TYPE_IN_MEMORY, ChatMemoryStoreProperties.TYPE_MEM0));
			}
		};
	}

	/**
	 * Dify 知识库检索器：{@code fw.plus.ai.langchain4j.dify-rag.enabled=true} 时装配，
	 * 多知识库并行检索后按分数降序合并截断；以 {@code @ConditionalOnMissingBean}
	 * 优先使用方自定义 {@link ContentRetriever}。base-url / api-key / dataset-ids
	 * 任一缺失即启动快速失败（配置了却不生效比不配更危险），元数据过滤段名同步校验。
	 */
	@Bean
	@ConditionalOnProperty(prefix = DifyRagProperties.PREFIX, name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean(ContentRetriever.class)
	public ContentRetriever difyContentRetriever(DifyRagProperties properties) {
		requireDifyProperty(properties.getBaseUrl(), "base-url");
		requireDifyProperty(properties.getApiKey(), "api-key");
		if (properties.getDatasetIds() == null || properties.getDatasetIds().isEmpty()) {
			throw new IllegalStateException(I18nMessageResolver.resolveKey(
					"fw.ai.LangChain4jSupportAutoConfiguration.difyDatasetIdsMissing", DifyRagProperties.PREFIX));
		}
		properties.getMetaFields().validate();
		IDatasetClient client = DatasetBuilder
				.builder(properties.toBaseUrl(), properties.getApiKey().trim())
				.build();
		return new DifyContentRetriever(client, properties);
	}

	private static void requireDifyProperty(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalStateException(I18nMessageResolver.resolveKey(
					"fw.ai.LangChain4jSupportAutoConfiguration.difyPropertyMissing", DifyRagProperties.PREFIX, field));
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Mem0ChatMemoryStoreConfiguration {

		@Bean
		@ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
				havingValue = ChatMemoryStoreProperties.TYPE_MEM0)
		@ConditionalOnMissingBean(ChatMemoryStore.class)
		public ChatMemoryStore mem0ChatMemoryStore(ChatMemoryTenantResolver tenantResolver,
				ChatMemoryStoreProperties properties) {
			ChatMemoryStoreProperties.Mem0 mem0 = properties.getMem0();
			if (mem0.getBaseUrl() == null || mem0.getBaseUrl().trim().isEmpty()) {
				throw new IllegalStateException(String.format(
						"%s=%s，但 %s.mem0.base-url 未配置，请填写 mem0 服务地址",
						ChatMemoryStoreProperties.PREFIX + ".type", ChatMemoryStoreProperties.TYPE_MEM0,
						ChatMemoryStoreProperties.PREFIX));
			}
			return new Mem0ChatMemoryStore(tenantResolver, properties.getDefaultAppId(),
					mem0.getBaseUrl(), mem0.getApiKey(), mem0.getConnectTimeout(), mem0.getReadTimeout());
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class InMemoryChatMemoryStoreConfiguration {

		/** 声明在最后：仅当 type=in-memory（或缺省）且容器中没有其他 ChatMemoryStore 时兜底 */
		@Bean
		@ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
				havingValue = ChatMemoryStoreProperties.TYPE_IN_MEMORY, matchIfMissing = true)
		@ConditionalOnMissingBean(ChatMemoryStore.class)
		public ChatMemoryStore inMemoryChatMemoryStore() {
			return new InMemoryChatMemoryStore();
		}
	}

	/**
	 * Dify 检索配置类：按 {@code fw-plus-dify}（optional 依赖）类存在性条件注册。
	 * <p>dify 类缺席的消费方（如样例工程）直接跳过——DifyRagProperties 的方法签名引用了
	 * {@code RetrievalModel}，无条件注册会在 introspection 时 NoClassDefFoundError。</p>
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel")
	@EnableConfigurationProperties(DifyRagProperties.class)
	static class DifyRagPropertiesConfiguration {
	}

	/**
	 * 信封编排装配：{@code fw.plus.ai.langchain4j.assistant.envelope.enabled=true} 时注册
	 * {@link EnvelopeRuntime} 与安装器；配置属性类在此按需注册（沿用本类既有嵌套注册模式，
	 * 不改外层 {@code @EnableConfigurationProperties}，也不动注册文件）。
	 *
	 * <p>开关关闭时不注册 runtime Bean，安装器随之无事可做——既有无 mapper 流式入口的行为一字不变。</p>
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(Langchain4jAssistantProperties.class)
	static class AssistantEnvelopeConfiguration {

		/**
		 * 信封编排件：模型与记忆存储<b>都从容器取、不新建</b>。
		 *
		 * <p>结构化轮次直连 {@link ChatLanguageModel}；会话历史必须与既有对话通道共用同一个
		 * {@link ChatMemoryStore}（否则两条通道各写各的历史）。任一缺失即启动快速失败——
		 * 开关开着却拿不到模型/存储属于配置没配对，静默降级只会更难查。</p>
		 *
		 * <p>业务契约从容器收集（{@link BusinessContract} Bean，可为 0 个：此时所有轮次落兜底 chat）。
		 * 容器里存在多个 {@code ChatLanguageModel} / {@code ChatMemoryStore} Bean 时按 Spring 语义报错
		 * （无法确定用哪一个），需要时请用 {@code @Primary} 指定一个。</p>
		 */
		@Bean
		@ConditionalOnProperty(prefix = Langchain4jAssistantProperties.PREFIX, name = "envelope.enabled",
				havingValue = "true")
		@ConditionalOnMissingBean(EnvelopeRuntime.class)
		public EnvelopeRuntime envelopeRuntime(Langchain4jAssistantProperties properties,
				ObjectProvider<ChatLanguageModel> chatModels, ObjectProvider<ChatMemoryStore> chatMemoryStores,
				ObjectProvider<BusinessContract> contracts) {
			ChatLanguageModel chatModel = chatModels.getIfAvailable();
			if (chatModel == null) {
				throw new IllegalStateException(String.format(
						"%s.envelope.enabled=true，但容器中没有 ChatLanguageModel Bean："
								+ "结构化轮次与业务分类需要直连模型，请先装配对话模型或关闭该开关",
						Langchain4jAssistantProperties.PREFIX));
			}
			ChatMemoryStore chatMemoryStore = chatMemoryStores.getIfAvailable();
			if (chatMemoryStore == null) {
				throw new IllegalStateException(String.format(
						"%s.envelope.enabled=true，但容器中没有 ChatMemoryStore Bean："
								+ "信封编排的会话历史必须与既有对话通道共用同一份存储",
						Langchain4jAssistantProperties.PREFIX));
			}
			BusinessRegistry registry = new BusinessRegistry();
			for (BusinessContract contract : contracts) {
				registry.register(contract);
			}
			return new EnvelopeRuntime(chatModel, chatMemoryStore, registry, properties);
		}

		/**
		 * 安装器：启动收尾时把 {@link EnvelopeRuntime} 装进容器里每个 {@link SimpleAssistant}。
		 *
		 * <p>两侧都在才安装，缺失任一侧静默跳过（样例或自定义装配可能只有其中一侧，不该因此启动失败）。
		 * 只覆盖启动时已实例化的 {@code SimpleAssistant}；懒加载 Bean 不在此列（需要时自行调用 setter）。</p>
		 */
		@Bean
		public EnvelopeRuntimeInstaller envelopeRuntimeInstaller(ObjectProvider<SimpleAssistant> assistants,
				ObjectProvider<EnvelopeRuntime> runtime) {
			return new EnvelopeRuntimeInstaller(assistants, runtime);
		}
	}

	/**
	 * 会话摘要存储装配：默认注册进程内 {@link SummaryStore}（{@link InMemorySummaryStore}），
	 * 使用方自定义 {@code SummaryStore} Bean 即自动覆盖（Jdbc 实现可另加依赖后注入）。
	 *
	 * <p>摘要来源/维护钩子不在此装配：它们需要按 magic-api 运行时构造的对话模型与 tokenizer
	 * 现场组装，收口在 {@code Langchain4jModuleConfiguration#simpleAssistant} 的
	 * {@code SummaryMemoryAssembler.apply(...)}。</p>
	 */
	@Configuration(proxyBeanMethods = false)
	static class SummaryMemoryConfiguration {

		/** 默认进程内摘要存储：仅当使用方未提供 {@link SummaryStore} 时兜底 */
		@Bean
		@ConditionalOnProperty(prefix = Langchain4jAssistantProperties.PREFIX, name = "session.summary.enabled",
				havingValue = "true", matchIfMissing = true)
		@ConditionalOnMissingBean(SummaryStore.class)
		public SummaryStore inMemorySummaryStore() {
			return new InMemorySummaryStore();
		}
	}

	/**
	 * 跨会话事实存储装配：默认注册独立 mem0 实例的 {@link FactStore}（{@link Mem0FactStore}），
	 * 使用方自定义 {@code FactStore} Bean 即自动覆盖。
	 *
	 * <p>事实来源/维护钩子不在此装配：它们需要按 magic-api 运行时构造的模型现场组装，
	 * 收口在 {@code Langchain4jModuleConfiguration#simpleAssistant} 的
	 * {@code FactMemoryAssembler.apply(...)}（先于摘要装配）。</p>
	 */
	@Configuration(proxyBeanMethods = false)
	static class FactMemoryConfiguration {

		/** 默认独立 mem0 事实存储：enabled=true 且使用方未提供 {@link FactStore} 时兜底；base-url 缺失即启动失败 */
		@Bean
		@ConditionalOnProperty(prefix = Langchain4jAssistantProperties.PREFIX, name = "session.facts.enabled",
				havingValue = "true")
		@ConditionalOnMissingBean(FactStore.class)
		public FactStore mem0FactStore(Langchain4jAssistantProperties properties) {
			Langchain4jAssistantProperties.Session session = properties.getSession();
			Langchain4jAssistantProperties.Session.Facts facts = session == null ? null : session.getFacts();
			if (facts == null || facts.getBaseUrl() == null || facts.getBaseUrl().trim().isEmpty()) {
				throw new IllegalStateException(String.format(
						"%s.session.facts.enabled=true，但 session.facts.base-url 未配置（且无自定义 FactStore Bean）："
								+ "请填写独立 mem0 服务地址",
						Langchain4jAssistantProperties.PREFIX));
			}
			return new Mem0FactStore(facts.getBaseUrl().trim(), facts.getApiKey(),
					facts.getConnectTimeout(), facts.getReadTimeout());
		}
	}

	/** 把信封处理器装进容器里既有的 {@link SimpleAssistant}（见 {@link AssistantEnvelopeConfiguration}） */
	static final class EnvelopeRuntimeInstaller implements SmartInitializingSingleton {

		private final ObjectProvider<SimpleAssistant> assistants;

		private final ObjectProvider<EnvelopeRuntime> runtime;

		EnvelopeRuntimeInstaller(ObjectProvider<SimpleAssistant> assistants, ObjectProvider<EnvelopeRuntime> runtime) {
			this.assistants = assistants;
			this.runtime = runtime;
		}

		@Override
		public void afterSingletonsInstantiated() {
			EnvelopeRuntime envelopeRuntime = runtime.getIfAvailable();
			if (envelopeRuntime == null) {
				return;
			}
			for (SimpleAssistant assistant : assistants) {
				assistant.envelopeRuntime(envelopeRuntime);
			}
		}
	}
}
