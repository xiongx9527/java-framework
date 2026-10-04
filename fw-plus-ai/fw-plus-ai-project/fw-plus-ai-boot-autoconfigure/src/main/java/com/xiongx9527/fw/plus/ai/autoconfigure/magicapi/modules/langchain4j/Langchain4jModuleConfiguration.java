package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.MagicDynamicChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.module.Langchain4jChatMemoryStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.MagicDynamicContentInjector;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.module.Langchain4jContentInjectorModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLanguageModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicScoringModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicStreamingChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicStreamingLanguageModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicTokenizer;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jScoringModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jTokenizerModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.MagicDynamicContentRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.module.Langchain4jContentRetrieverModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.module.Langchain4jSystemMessageProviderModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicDynamicToolProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.module.Langchain4jToolProviderModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code langchain4j} 脚本模块的装配：注册 magic-api 脚本模块，并按配置注入一个
 * {@link SimpleAssistant} bean。
 *
 * <p>注入条件与 key 全部走配置（{@code fw.plus.magicapi.langchain4j.simple-assistant.*}）：
 * {@code enabled} 注入开关（显式开启才注入）、{@code key} 默认 key（所有资源类型共用）、
 * {@code keys.<类型>} 分类型专属 key（没有就按 {@code use-default-key} / {@code use-default-keys.<类型>}
 * 决定是否回落默认 key）、{@code memory-max-messages} 进程内记忆窗口、{@code max-tokens} token
 * 记忆上限（需配 tokenizer）、{@code rerank-min-score} rerank 生效下限（需配 scoring-model）、
 * {@code simple-assistant-required} langchain4jModule 是否必须依赖 SimpleAssistant。各类型资源
 * 从对应的 {@code Langchain4jXxxModule} 解析，并以 {@code MagicDynamicXxx} 动态代理包装——
 * 每次调用实时解析，magic 实例热更新/注销后不持有失效引用。chat-model 与 streaming-chat-model
 * 至少要有其一，其余类型 key 未配置时跳过不装配。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.service.AiServices")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(Langchain4jSimpleAssistantProperties.class)
public class Langchain4jModuleConfiguration {

	private final Langchain4jSimpleAssistantProperties simpleAssistantProperties;

	public Langchain4jModuleConfiguration(Langchain4jSimpleAssistantProperties simpleAssistantProperties) {
		this.simpleAssistantProperties = simpleAssistantProperties;
	}

	/** 全能力 {@code SimpleAssistant}：按配置 keys 装配，资源以 MagicDynamic 动态代理包装。 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.simple-assistant", name = "enabled",
			havingValue = "true", matchIfMissing = false)
	public SimpleAssistant simpleAssistant(
			ObjectProvider<Langchain4jChatModelModule> chatModelModules,
			ObjectProvider<Langchain4jStreamingChatModelModule> streamingChatModelModules,
			ObjectProvider<Langchain4jLanguageModelModule> languageModelModules,
			ObjectProvider<Langchain4jStreamingLanguageModelModule> streamingLanguageModelModules,
			ObjectProvider<Langchain4jChatMemoryStoreModule> chatMemoryStoreModules,
			ObjectProvider<Langchain4jToolProviderModule> toolProviderModules,
			ObjectProvider<Langchain4jContentRetrieverModule> contentRetrieverModules,
			ObjectProvider<Langchain4jContentInjectorModule> contentInjectorModules,
			ObjectProvider<Langchain4jSystemMessageProviderModule> systemMessageModules,
			ObjectProvider<Langchain4jTokenizerModule> tokenizerModules,
			ObjectProvider<Langchain4jScoringModelModule> scoringModelModules,
			Langchain4jSimpleAssistantProperties properties) {
		AssistantBuilder assistantBuilder = new AssistantBuilder();
		String chatModelKey = typeKey("chatModel", properties);
		if (chatModelKey != null && chatModelModules.getIfAvailable() != null) {
			assistantBuilder.chatModel(new MagicDynamicChatModel(chatModelModules.getIfAvailable(), chatModelKey));
		}
		String streamingModelKey = typeKey("streamingModel", properties);
		if (streamingModelKey != null && streamingChatModelModules.getIfAvailable() != null) {
			assistantBuilder.streamingModel(
					new MagicDynamicStreamingChatModel(streamingChatModelModules.getIfAvailable(), streamingModelKey));
		}
		String languageModelKey = typeKey("languageModel", properties);
		if (languageModelKey != null && languageModelModules.getIfAvailable() != null) {
			assistantBuilder.languageModel(
					new MagicDynamicLanguageModel(languageModelModules.getIfAvailable(), languageModelKey));
		}
		String streamingLanguageModelKey = typeKey("streamingLanguageModel", properties);
		if (streamingLanguageModelKey != null && streamingLanguageModelModules.getIfAvailable() != null) {
			assistantBuilder.streamingLanguageModel(new MagicDynamicStreamingLanguageModel(
					streamingLanguageModelModules.getIfAvailable(), streamingLanguageModelKey));
		}
		String memoryStoreKey = typeKey("chatMemoryStore", properties);
		if (memoryStoreKey != null && chatMemoryStoreModules.getIfAvailable() != null) {
			assistantBuilder.memoryStore(
					new MagicDynamicChatMemoryStore(chatMemoryStoreModules.getIfAvailable(), memoryStoreKey));
		}
		String toolProviderKey = typeKey("toolProvider", properties);
		if (toolProviderKey != null && toolProviderModules.getIfAvailable() != null) {
			assistantBuilder.toolProvider(
					new MagicDynamicToolProvider(toolProviderModules.getIfAvailable(), toolProviderKey));
		}
		String contentRetrieverKey = typeKey("contentRetriever", properties);
		if (contentRetrieverKey != null && contentRetrieverModules.getIfAvailable() != null) {
			assistantBuilder.contentRetriever(new MagicDynamicContentRetriever(
					contentRetrieverModules.getIfAvailable(), contentRetrieverKey));
		}
		String injectorPath = typeKey("contentInjector", properties);
		if (injectorPath != null && contentInjectorModules.getIfAvailable() != null) {
			assistantBuilder.contentInjector(
					new MagicDynamicContentInjector(contentInjectorModules.getIfAvailable(), injectorPath));
		}
		String systemMessagePath = typeKey("systemMessage", properties);
		if (systemMessagePath != null && systemMessageModules.getIfAvailable() != null) {
			Langchain4jSystemMessageProviderModule systemMessageModule = systemMessageModules.getIfAvailable();
			String promptPath = systemMessagePath;
			// systemMessage 的求值天然实时（每次对话按 path 求值），无需代理
			assistantBuilder.systemMessageProvider(
					memoryId -> systemMessageModule.systemMessage(memoryId, promptPath, null));
		}
		String tokenizerKey = typeKey("tokenizer", properties);
		if (tokenizerKey != null && tokenizerModules.getIfAvailable() != null) {
			assistantBuilder.tokenizer(new MagicDynamicTokenizer(tokenizerModules.getIfAvailable(), tokenizerKey));
		}
		String scoringModelKey = typeKey("scoringModel", properties);
		if (scoringModelKey != null && scoringModelModules.getIfAvailable() != null) {
			assistantBuilder.scoringModel(
					new MagicDynamicScoringModel(scoringModelModules.getIfAvailable(), scoringModelKey));
		}
		if (properties.getMemoryMaxMessages() != null) {
			assistantBuilder.memory(properties.getMemoryMaxMessages());
		}
		if (properties.getMaxTokens() != null) {
			assistantBuilder.maxTokens(properties.getMaxTokens());
		}
		if (properties.getRerankMinScore() != null) {
			assistantBuilder.rerankMinScore(properties.getRerankMinScore());
		}
		// Ephemeral 记忆：AI 持久化策略（含位置/长度/工具结果开关）等
		assistantBuilder.ephemeralChatMemory(properties.getEphemeral());
		return assistantBuilder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public Langchain4jModule langchain4jModule(
			ObjectProvider<Langchain4jChatModelModule> chatModelModules,
			ObjectProvider<Langchain4jLanguageModelModule> languageModelModules,
			ObjectProvider<Langchain4jStreamingLanguageModelModule> streamingLanguageModelModules,
			ObjectProvider<Langchain4jContentRetrieverModule> contentRetrieverModules,
			@Autowired(required = false) SimpleAssistant simpleAssistant) {
		if (simpleAssistantProperties.isSimpleAssistantRequired() && simpleAssistant == null) {
			throw new IllegalStateException("simple-assistant.required=true 但 SimpleAssistant 未装配："
					+ "请检查 fw.plus.magicapi.langchain4j.simple-assistant.enabled 与 keys 配置");
		}
		return new Langchain4jModule(simpleAssistantProperties, chatModelModules, languageModelModules,
				streamingLanguageModelModules, contentRetrieverModules, simpleAssistant);
	}

	/** 解析某类型的资源 key：专属 key 优先；没有时按 {@code use-default-key} 开关决定是否回落默认
	 * key（关闭则返回 null，该类型跳过）。 */
	private String typeKey(String type, Langchain4jSimpleAssistantProperties properties) {
		String own = properties.getKeys().get(type);
		if (own != null && !own.isEmpty()) {
			return own;
		}
		// 分类型回落开关（useDefaultKeys.<类型>）优先，未配置回落全局 use-default-key
		boolean useDefault = properties.getUseDefaultKeys()
				.getOrDefault(type, properties.isUseDefaultKey());
		return useDefault ? properties.getKey() : null;
	}
}
