package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts.FactMemoryAssembler;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts.FactStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.ChatLanguageModelSummaryModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryMemoryAssembler;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session.SessionIdResolver;
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
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.scoring.ScoringModel;
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
			ObjectProvider<Langchain4jTokenizerModule> tokenizerModules,
			ObjectProvider<Langchain4jScoringModelModule> scoringModelModules,
			Langchain4jSimpleAssistantProperties properties,
			ObjectProvider<Langchain4jAssistantProperties> assistantProperties,
			ObjectProvider<SummaryStore> summaryStores,
			ObjectProvider<SummaryModel> summaryModels,
			ObjectProvider<FactStore> factStores,
			ObjectProvider<SessionIdResolver> sessionResolvers) {
		AssistantBuilder assistantBuilder = new AssistantBuilder();
		String chatModelKey = typeKey("chatModel", properties);
		ChatLanguageModel chatModel = null;
		if (chatModelKey != null && chatModelModules.getIfAvailable() != null) {
			chatModel = new MagicDynamicChatModel(chatModelModules.getIfAvailable(), chatModelKey);
			assistantBuilder.chatModel(chatModel);
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
		String tokenizerKey = typeKey("tokenizer", properties);
		Tokenizer tokenizer = null;
		if (tokenizerKey != null && tokenizerModules.getIfAvailable() != null) {
			tokenizer = new MagicDynamicTokenizer(tokenizerModules.getIfAvailable(), tokenizerKey);
			assistantBuilder.tokenizer(tokenizer);
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
		// 会话/摘要配置：把 fw.plus.ai.langchain4j.assistant.session.* 落到 builder
		// （session.enabled=false 或属性类缺席时不动，保持既有非会话行为）
		applySessionMemory(assistantBuilder, assistantProperties, summaryStores, summaryModels, factStores,
				sessionResolvers, chatModelModules, scoringModelModules, tokenizer);
		return assistantBuilder.build();
	}

	/**
	 * 装配会话/摘要/事实：把 {@code fw.plus.ai.langchain4j.assistant.session.*} 配置落到 builder。
	 * 属性类 / 相关 Bean 缺席时静默跳过（不改变既有行为）；事实来源先于摘要来源注册，
	 * 保证上下文注入顺序为 事实 → 摘要。
	 */
	private void applySessionMemory(AssistantBuilder assistantBuilder,
			ObjectProvider<Langchain4jAssistantProperties> assistantProperties,
			ObjectProvider<SummaryStore> summaryStores, ObjectProvider<SummaryModel> summaryModels,
			ObjectProvider<FactStore> factStores, ObjectProvider<SessionIdResolver> sessionResolvers,
			ObjectProvider<Langchain4jChatModelModule> chatModelModules,
			ObjectProvider<Langchain4jScoringModelModule> scoringModelModules, Tokenizer tokenizer) {
		Langchain4jAssistantProperties properties = assistantProperties.getIfAvailable();
		if (properties == null) {
			return;
		}
		// 先事实、后摘要：提供者注册顺序即上下文渲染顺序
		FactMemoryAssembler.apply(assistantBuilder, properties, factStores.getIfAvailable(),
				resolveConfiguredFactExtractModel(properties, chatModelModules),
				resolveConfiguredFactRerankModel(properties, scoringModelModules));
		SummaryModel summaryModel = summaryModels.getIfAvailable();
		if (summaryModel == null) {
			summaryModel = resolveConfiguredSummaryModel(properties, chatModelModules);
		}
		SummaryMemoryAssembler.apply(assistantBuilder, properties, summaryStores.getIfAvailable(), summaryModel,
				sessionResolvers.getIfAvailable(), tokenizer);
	}

	/** 解析 {@code session.facts.extract-model}：模型 key（动态解析）或 ChatLanguageModel FQCN 反射构造。 */
	private ChatLanguageModel resolveConfiguredFactExtractModel(Langchain4jAssistantProperties properties,
			ObjectProvider<Langchain4jChatModelModule> chatModelModules) {
		String name = factModelName(properties, true);
		if (name == null) {
			return null;
		}
		// 优先按 langchain4j 模型 key 动态解析（与对话/摘要模型同一套 key 机制）
		if (chatModelModules.getIfAvailable() != null) {
			return new MagicDynamicChatModel(chatModelModules.getIfAvailable(), name);
		}
		try {
			Class<?> clazz = Class.forName(name);
			if (ChatLanguageModel.class.isAssignableFrom(clazz)) {
				return (ChatLanguageModel) clazz.getDeclaredConstructor().newInstance();
			}
		} catch (Exception ignored) {
			// 反射失败：等同未配置（llm 跳过本轮 / hybrid 退化为 rule）
		}
		return null;
	}

	/** 解析 {@code session.facts.rerank-model}：模型 key（动态解析）或 ScoringModel FQCN 反射构造。 */
	private ScoringModel resolveConfiguredFactRerankModel(Langchain4jAssistantProperties properties,
			ObjectProvider<Langchain4jScoringModelModule> scoringModelModules) {
		String name = factModelName(properties, false);
		if (name == null) {
			return null;
		}
		if (scoringModelModules.getIfAvailable() != null) {
			return new MagicDynamicScoringModel(scoringModelModules.getIfAvailable(), name);
		}
		try {
			Class<?> clazz = Class.forName(name);
			if (ScoringModel.class.isAssignableFrom(clazz)) {
				return (ScoringModel) clazz.getDeclaredConstructor().newInstance();
			}
		} catch (Exception ignored) {
			// 反射失败：等同未配置（rerank 退回本地关键词重叠重排）
		}
		return null;
	}

	/** 取 facts 模型配置项（trim；空视为未配置）。 */
	private String factModelName(Langchain4jAssistantProperties properties, boolean extract) {
		Langchain4jAssistantProperties.Session session = properties.getSession();
		Langchain4jAssistantProperties.Session.Facts facts = session == null ? null : session.getFacts();
		if (facts == null) {
			return null;
		}
		String name = extract ? facts.getExtractModel() : facts.getRerankModel();
		return name == null || name.trim().isEmpty() ? null : name.trim();
	}

	/** 解析 {@code session.summary.summarize-model}：模型 key（经 langchain4j 模块动态解析）或 FQCN 反射构造。 */
	private SummaryModel resolveConfiguredSummaryModel(Langchain4jAssistantProperties properties,
			ObjectProvider<Langchain4jChatModelModule> chatModelModules) {
		Langchain4jAssistantProperties.Session session = properties.getSession();
		if (session == null || session.getSummary() == null) {
			return null;
		}
		String name = session.getSummary().getSummarizeModel();
		if (name == null || name.trim().isEmpty()) {
			return null;
		}
		String key = name.trim();
		// 优先按 langchain4j 模型 key 动态解析（与对话模型同一套 key 机制）
		if (chatModelModules.getIfAvailable() != null) {
			ChatLanguageModel model = new MagicDynamicChatModel(chatModelModules.getIfAvailable(), key);
			return new ChatLanguageModelSummaryModel(model);
		}
		// 退回 FQCN 反射（自定义 SummaryModel 实现）
		try {
			Class<?> clazz = Class.forName(key);
			if (SummaryModel.class.isAssignableFrom(clazz)) {
				return (SummaryModel) clazz.getDeclaredConstructor().newInstance();
			}
		} catch (Exception ignored) {
			// 反射失败：回退主模型（返回 null 由装配器走 fallback）
		}
		return null;
	}

	@Bean
	@ConditionalOnMissingBean
	public Langchain4jModule langchain4jModule(
			ObjectProvider<Langchain4jChatModelModule> chatModelModules,
			ObjectProvider<Langchain4jLanguageModelModule> languageModelModules,
			ObjectProvider<Langchain4jStreamingLanguageModelModule> streamingLanguageModelModules,
			@Autowired(required = false) SimpleAssistant simpleAssistant) {
		if (simpleAssistantProperties.isSimpleAssistantRequired() && simpleAssistant == null) {
			throw new IllegalStateException("simple-assistant.required=true 但 SimpleAssistant 未装配："
					+ "请检查 fw.plus.magicapi.langchain4j.simple-assistant.enabled 与 keys 配置");
		}
		return new Langchain4jModule(simpleAssistantProperties, chatModelModules, languageModelModules,
				streamingLanguageModelModules, simpleAssistant);
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
