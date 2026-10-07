package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.aggregate.AggregatingToolProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.AiPersistenceStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemoryProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.LengthBoundedAiPersistenceStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.PersistToolResultsAiPersistenceStrategy;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 助手构建器：把 {@code AiServices<Assistant>} 的装配过程做成脚本可直接链式调用的形态，
 * 一一对应 {@code AiServices} 的能力（模型 / 流式模型 / 记忆 / 系统提示 / 工具 / 检索增强）。
 *
 * <p>所有依赖都由调用方作为参数传入（不做 Spring 注入）：实例从各类型脚本模块取，
 * 如 {@code langchain4jChatModel['key']}、{@code langchain4jChatMemoryStore['key']}、
 * {@code langchain4jToolProvider['key']}、{@code langchain4jContentRetriever['key']}、
 * {@code langchain4jContentInjector.get(path)}。</p>
 *
 * <p>{@link #chatModel(ChatLanguageModel)} 与 {@link #streamingModel(StreamingChatLanguageModel)}
 * 至少配一个；其余能力不配即不参与装配。
 * 记忆二选一：配置 {@link #tokenizer(Tokenizer)} 用 {@code TokenWindowChatMemory}（按 token 截断），
 * 否则用 {@code MessageWindowChatMemory}（按条数截断）——都不配时用进程内窗口记忆（默认 20 条）。
 * 无论哪种，内置记忆都包一层 {@link EphemeralChatMemory}：默认按 {@code PERSIST_NON_TOOL_FINAL_ANSWER}
 * 策略把「上一轮未调用工具」的最终回答沉淀进历史、带到下一轮（可用 {@link #aiPersistenceStrategy(AiPersistenceStrategy)}
 * 或 {@link #ephemeralChatMemory(EphemeralChatMemoryProperties)} 切换/自定义策略与插入位置）；
 * RAG 检索段与工具往返不进历史，只在本轮请求里带出。无有效字符的 AI 回复一律不沉淀。
 */
public class AssistantBuilder {

	/** 只检索模式：不装配对话大模型，build() 产出仅支持 retrieve(...) 的 {@link SimpleAssistant} */
	private final boolean retrievalOnly;

	/** 进程内记忆的默认窗口（条） */
	private static final int DEFAULT_MEMORY_MAX_MESSAGES = 20;

	/** 常规构建：对话 + 检索增强（默认） */
	public AssistantBuilder() {
		this(false);
	}

	/**
	 * 构造方式参数：{@code retrievalOnly=true} 时为只检索模式——
	 * 不装配对话大模型（无 AiServices），build() 产出仅支持 {@code retrieve(...)} 的 SimpleAssistant，
	 * 必须配置 {@link #contentRetriever(ContentRetriever)} 或 {@link #retrievalAugmentor(RetrievalAugmentor)}。
	 */
	public AssistantBuilder(boolean retrievalOnly) {
		this.retrievalOnly = retrievalOnly;
	}

	/** 只检索模式的便捷入口，等价于 {@code new AssistantBuilder(true)} */
	public static AssistantBuilder retrievalOnly() {
		return new AssistantBuilder(true);
	}

	private ChatLanguageModel chatModel;

	private StreamingChatLanguageModel streamingModel;

	private LanguageModel languageModel;

	private StreamingLanguageModel streamingLanguageModel;

	private Function<Object, String> systemMessageProvider;

	private ChatMemoryProvider chatMemoryProvider;

	private int memoryMaxMessages = DEFAULT_MEMORY_MAX_MESSAGES;

	/** 按 token 截断记忆时使用的 Tokenizer；配置后记忆改用 TokenWindowChatMemory */
	private Tokenizer tokenizer;

	/** TokenWindowChatMemory 的 token 上限（仅在配置了 tokenizer 时生效，默认 1000） */
	private Integer maxTokens;

	/** AI 持久化策略对象（优先级最高：对象 > Key > 默认） */
	private AiPersistenceStrategy aiPersistenceStrategy;

	/** AI 持久化策略 Key（name 或 class，全限定名） */
	private String aiPersistenceStrategyKey;

	/** 是否严格校验 AI 持久化策略 Key */
	private boolean strictAiPersistenceStrategy = false;

	/** Ephemeral：是否限制 AI 消息长度 */
	private boolean ephemeralLimitAiLength = false;

	/** Ephemeral：单条 AI 最大字符数（-1 不限制） */
	private int ephemeralMaxAiCharsPerMessage = -1;

	/** Ephemeral：AI 总字符数限制（-1 不限制） */
	private int ephemeralMaxAiTotalChars = -1;

	/** Ephemeral：AI 最大条数（<=0 表示不限制） */
	private int ephemeralMaxAiMessages = Integer.MAX_VALUE;

	/** Ephemeral：超长是否截断（false：超长直接丢弃） */
	private boolean ephemeralTruncateAiIfExceeds = false;

	/** Ephemeral：是否持久化 ToolExecutionResultMessage */
	private boolean ephemeralPersistToolResults = false;

	/** Ephemeral：ToolExecutionResult 最大字符数 */
	private int ephemeralMaxToolResultChars = 512;

	/** Ephemeral：ToolExecutionResult 最大条数 */
	private int ephemeralMaxToolResults = 0;

	/** Ephemeral：临时内容分隔符 */
	private java.util.List<String> ephemeralSeparators;

	/** Ephemeral：历史 User 是否剥离 RAG 临时段 */
	private boolean ephemeralStripRagFromHistory = true;

	/** Ephemeral：当前轮 User 是否保留临时段 */
	private boolean ephemeralIncludeCurrentUserWithEphemeral = true;

	/** Ephemeral：当前轮是否发送 ToolExecutionResult */
	private boolean ephemeralSendToolResultsInCurrentTurn = true;

	/** Ephemeral：当前轮是否发送带工具请求的 AiMessage（中间步） */
	private boolean ephemeralSendToolRequestsInCurrentTurn = true;

	private ChatMemoryStore memoryStore;

	private final List<Object> tools = new ArrayList<>();

	private final List<ToolProvider> toolProviders = new ArrayList<>();

	private RetrievalAugmentor retrievalAugmentor;

	private ContentRetriever contentRetriever;

	private ContentInjector contentInjector;

	private ScoringModel scoringModel;

	private Double rerankMinScore;

	/** 对话模型（必填）：{@code langchain4jChatModel['key']} 或任意 {@link ChatLanguageModel} 实例 */
	public AssistantBuilder chatModel(ChatLanguageModel model) {
		this.chatModel = model;
		return this;
	}

	/** 流式模型：{@code langchain4jStreamingChatModel['key']}；不配则助手不支持 {@code stream(...)} */
	public AssistantBuilder streamingModel(StreamingChatLanguageModel model) {
		this.streamingModel = model;
		return this;
	}

	/**
	 * 文本补全模型：{@code langchain4jLanguageModel['key']}；不配则助手不支持
	 * {@code generateText(...)} 系列直连补全。经 {@code SimpleAssistant} 的
	 * {@code languageModel} 字段直连使用，不进 AiServices、无记忆。
	 */
	public AssistantBuilder languageModel(LanguageModel model) {
		this.languageModel = model;
		return this;
	}

	/**
	 * 流式文本补全模型：{@code langchain4jStreamingLanguageModel['key']}；不配则助手不支持
	 * {@code generateTextToSse(...)}。直连使用，不进 AiServices。
	 */
	public AssistantBuilder streamingLanguageModel(StreamingLanguageModel model) {
		this.streamingLanguageModel = model;
		return this;
	}

	/** 系统提示：固定文本（每轮相同） */
	public AssistantBuilder systemMessage(String text) {
		this.systemMessageProvider = memoryId -> text;
		return this;
	}

	/** 系统提示：自定义 provider（入参为 memoryId），如包装 {@code langchain4jSystemMessageProvider} 的求值结果 */
	public AssistantBuilder systemMessageProvider(Function<Object, String> provider) {
		this.systemMessageProvider = provider;
		return this;
	}

	/** 进程内记忆：每个 {@code memoryId} 一份窗口记忆（默认包 EphemeralChatMemory），窗口大小 {@code maxMessages}（默认 20） */
	public AssistantBuilder memory(Integer maxMessages) {
		this.memoryMaxMessages = maxMessages == null || maxMessages < 1 ? DEFAULT_MEMORY_MAX_MESSAGES : maxMessages;
		return this;
	}

	/**
	 * 按 token 截断记忆：配置后记忆改用 {@link TokenWindowChatMemory}（忽略 {@link #memory(Integer)}），
	 * 上限由 {@link #maxTokens(Integer)} 控制（默认 1000）。
	 *
	 * @param tokenizer Tokenizer，如 {@code langchain4jTokenizer['openai']}
	 */
	public AssistantBuilder tokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
		return this;
	}

	/** TokenWindowChatMemory 的 token 上限（仅在配置了 tokenizer 时生效，默认 1000） */
	public AssistantBuilder maxTokens(Integer maxTokens) {
		this.maxTokens = maxTokens;
		return this;
	}

	/** 记忆存储：{@code langchain4jChatMemoryStore['key']}（记忆由 magic-api 函数托管，可持久化） */
	public AssistantBuilder memoryStore(ChatMemoryStore store) {
		this.memoryStore = store;
		return this;
	}

	/** 记忆：直接用 {@link ChatMemoryProvider}（想保留工具往返等完整历史时用这条） */
	public AssistantBuilder chatMemoryProvider(ChatMemoryProvider provider) {
		this.chatMemoryProvider = provider;
		return this;
	}

	/** 工具：带 {@code @Tool} 方法的对象，可传多个。同类型工具按“后注册覆盖先注册”去重，避免重复调用注册出多份 */
	public AssistantBuilder tools(Object... toolObjects) {
		if (toolObjects != null) {
			for (Object tool : toolObjects) {
				if (tool == null) {
					continue;
				}
				this.tools.removeIf(tool.getClass()::isInstance);
				this.tools.add(tool);
			}
		}
		return this;
	}

	/**
	 * 工具提供者：可调用多次累积（如同时挂 magic-api 的 {@code langchain4jToolProvider['key']} 与
	 * MCP 客户端等），同一实例重复添加会被忽略。0.35 的 AiServices 只接受单个 toolProvider，
	 * 多个时由 {@link AggregatingToolProvider} 聚合成一个。
	 */
	public AssistantBuilder toolProvider(ToolProvider... providers) {
		if (providers != null) {
			for (ToolProvider provider : providers) {
				if (provider != null && !this.toolProviders.contains(provider)) {
					this.toolProviders.add(provider);
				}
			}
		}
		return this;
	}

	/** 检索增强：{@code langchain4jContentRetriever['key']}（自动包成 DefaultRetrievalAugmentor） */
	public AssistantBuilder contentRetriever(ContentRetriever retriever) {
		this.contentRetriever = retriever;
		return this;
	}

	/** 检索增强的注入器：{@code langchain4jContentInjector.get(path)}；不配则用 langchain4j 默认注入器 */
	public AssistantBuilder contentInjector(ContentInjector injector) {
		this.contentInjector = injector;
		return this;
	}

	/** 检索增强：直接给装配好的 {@link RetrievalAugmentor}（优先级高于 contentRetriever/contentInjector） */
	public AssistantBuilder retrievalAugmentor(RetrievalAugmentor augmentor) {
		this.retrievalAugmentor = augmentor;
		return this;
	}

	/**
	 * rerank 重排模型：{@code langchain4jScoringModel['key']}（jina / cohere）。
	 * 配置后用 {@link ReRankingContentAggregator} 对检索结果重排，配合 {@link #rerankMinScore(Double)} 使用。
	 */
	public AssistantBuilder scoringModel(ScoringModel scoringModel) {
		this.scoringModel = scoringModel;
		return this;
	}

	/** rerank 生效下限：低于该分数的条目丢弃（仅在配置了 scoringModel 时生效） */
	public AssistantBuilder rerankMinScore(Double minScore) {
		this.rerankMinScore = minScore;
		return this;
	}

	/**
	 * 按当前配置装配 {@link AiServices}，返回易用封装 {@link SimpleAssistant}
	 * （{@code chat(message)} 两参即可、{@code streamToSse(...)} 直接推流）；
	 * 需要底层 {@link Assistant} 类型时用 {@link #toAssistant()}。
	 */

	/**
	 * 设置 AI 持久化策略（对象实例，优先级最高：对象 > Key > 默认）。
	 */
	public AssistantBuilder aiPersistenceStrategy(AiPersistenceStrategy strategy) {
		this.aiPersistenceStrategy = strategy;
		this.aiPersistenceStrategyKey = null;
		return this;
	}

	/**
	 * 设置 AI 持久化策略（名称或全限定类名，优先级次于策略对象）。
	 * <p>支持内置策略名、自定义注册名、全限定类名。</p>
	 */
	public AssistantBuilder aiPersistenceStrategy(String keyOrClass) {
		if (keyOrClass == null || keyOrClass.trim().isEmpty()) {
			this.aiPersistenceStrategyKey = null;
			return this;
		}
		this.aiPersistenceStrategyKey = keyOrClass.trim();
		this.aiPersistenceStrategy = null;
		return this;
	}

	/**
	 * 注册自定义 AI 持久化策略。
	 */
	public AssistantBuilder registerAiPersistenceStrategy(String name, AiPersistenceStrategy strategy) {
		AiPersistenceStrategy.Registry.register(name, strategy);
		return this;
	}

	/**
	 * 注册自定义 AI 持久化策略并立即使用。
	 */
	public AssistantBuilder registerAndUseAiPersistenceStrategy(String name, AiPersistenceStrategy strategy) {
		AiPersistenceStrategy.Registry.register(name, strategy);
		this.aiPersistenceStrategyKey = name.trim();
		this.aiPersistenceStrategy = null;
		return this;
	}

	/**
	 * 使用已注册的 AI 持久化策略（按名称或类名）。
	 */
	public AssistantBuilder useAiPersistenceStrategy(String nameOrClass) {
		return aiPersistenceStrategy(nameOrClass);
	}

	/**
	 * 是否严格校验 AI 持久化策略 Key。
	 */
	public AssistantBuilder strictAiPersistenceStrategy(boolean strict) {
		this.strictAiPersistenceStrategy = strict;
		return this;
	}

	public AssistantBuilder ephemeralLimitAiLength(boolean limit) {
		this.ephemeralLimitAiLength = limit;
		return this;
	}

	public AssistantBuilder ephemeralMaxAiCharsPerMessage(int max) {
		this.ephemeralMaxAiCharsPerMessage = max;
		return this;
	}

	public AssistantBuilder ephemeralMaxAiTotalChars(int max) {
		this.ephemeralMaxAiTotalChars = max;
		return this;
	}

	public AssistantBuilder ephemeralMaxAiMessages(int max) {
		this.ephemeralMaxAiMessages = max;
		return this;
	}

	public AssistantBuilder ephemeralTruncateAiIfExceeds(boolean truncate) {
		this.ephemeralTruncateAiIfExceeds = truncate;
		return this;
	}

	public AssistantBuilder ephemeralPersistToolResults(boolean persist) {
		this.ephemeralPersistToolResults = persist;
		return this;
	}

	public AssistantBuilder ephemeralMaxToolResultChars(int max) {
		this.ephemeralMaxToolResultChars = max;
		return this;
	}

	public AssistantBuilder ephemeralMaxToolResults(int max) {
		this.ephemeralMaxToolResults = max;
		return this;
	}

	public AssistantBuilder ephemeralSeparators(String... separators) {
		if (separators != null && separators.length > 0) {
			this.ephemeralSeparators = java.util.Arrays.asList(separators);
		}
		return this;
	}

	public AssistantBuilder ephemeralSeparators(java.util.List<String> separators) {
		if (separators != null && !separators.isEmpty()) {
			this.ephemeralSeparators = new java.util.ArrayList<>(separators);
		}
		return this;
	}

	public AssistantBuilder ephemeralStripRagFromHistory(boolean strip) {
		this.ephemeralStripRagFromHistory = strip;
		return this;
	}

	public AssistantBuilder ephemeralIncludeCurrentUserWithEphemeral(boolean include) {
		this.ephemeralIncludeCurrentUserWithEphemeral = include;
		return this;
	}

	public AssistantBuilder ephemeralSendToolResultsInCurrentTurn(boolean send) {
		this.ephemeralSendToolResultsInCurrentTurn = send;
		return this;
	}

	public AssistantBuilder ephemeralSendToolRequestsInCurrentTurn(boolean send) {
		this.ephemeralSendToolRequestsInCurrentTurn = send;
		return this;
	}

	/**
	 * 用 {@link EphemeralChatMemoryProperties} 一次性配置 Ephemeral 记忆。
	 * <p>策略解析走 {@link EphemeralChatMemoryProperties#resolveAiPersistenceStrategy()}，
	 * 已包含「长度限制」与「工具结果持久化」装饰器；同时注册其中的自定义策略。</p>
	 */
	public AssistantBuilder ephemeralChatMemory(EphemeralChatMemoryProperties properties) {
		if (properties == null || !properties.isEnabled()) {
			return this;
		}
		// 直接解析成策略对象（优先级最高），避免与 key 双通道歧义
		this.aiPersistenceStrategy = properties.resolveAiPersistenceStrategy();
		this.aiPersistenceStrategyKey = null;
		this.ephemeralSeparators = new java.util.ArrayList<>(properties.getEphemeralSeparators());
		this.ephemeralStripRagFromHistory = properties.isStripRagFromHistory();
		this.ephemeralIncludeCurrentUserWithEphemeral = properties.isIncludeCurrentUserWithEphemeral();
		this.ephemeralSendToolResultsInCurrentTurn = properties.isSendToolResultsInCurrentTurn();
		this.ephemeralSendToolRequestsInCurrentTurn = properties.isSendToolRequestsInCurrentTurn();
		return this;
	}

	public SimpleAssistant build() {
		// RetrievalAugmentor 从 builder 装配结果传入：常规模式随 AiServices 一起走检索增强，
		// 只检索模式（retrievalOnly）则产出仅支持 retrieve(...) 的 SimpleAssistant；
		// streamingModel 透传给 SimpleAssistant 的直连流式能力（streamWithFilter 用），
		// languageModel / streamingLanguageModel 透传给 SimpleAssistant 的直连补全能力
		SimpleAssistant assistant = new SimpleAssistant(assemble(), chatModel, streamingModel, languageModel,
				streamingLanguageModel, resolveRetrievalAugmentor());
		// 检索器支持显式元数据过滤时，接线供 retrieve/chatWithFilter/streamWithFilter 过滤重载使用
		if (contentRetriever instanceof SimpleAssistant.FilterableRetriever) {
			assistant.filterableRetriever((SimpleAssistant.FilterableRetriever) contentRetriever);
		}
		return assistant;
	}

	/** 需要底层 {@link Assistant} 类型（如继续挂 AiServices 能力）时取用 */
	public Assistant toAssistant() {
		return assemble();
	}

	private Assistant assemble() {
		if (retrievalOnly) {
			// 只检索模式：不装配对话大模型
			return null;
		}
		if (chatModel == null && streamingModel == null) {
			// 无对话模型时允许只装配直连补全/检索能力：AiServices 侧不参与
			if (languageModel == null && streamingLanguageModel == null && retrievalAugmentor == null) {
				throw new IllegalArgumentException("构建助手缺少模型：请至少调用 chatModel(...) 或 "
						+ "streamingModel(...) 之一指定；仅补全/检索形态请调用 languageModel(...)、"
						+ "streamingLanguageModel(...)、retrievalAugmentor(...) 或 contentRetriever(...)");
			}
			return null;
		}
		AiServices<Assistant> builder = AiServices.builder(Assistant.class)
				.chatMemoryProvider(resolveChatMemoryProvider());

		if (chatModel != null) {
			builder.chatLanguageModel(chatModel);
		}
		if (streamingModel != null) {
			builder.streamingChatLanguageModel(streamingModel);
		}
		if (systemMessageProvider != null) {
			builder.systemMessageProvider(systemMessageProvider);
		}
		if (!tools.isEmpty()) {
			builder.tools(tools);
		}
		if (toolProviders.size() == 1) {
			builder.toolProvider(toolProviders.get(0));
		} else if (toolProviders.size() > 1) {
			builder.toolProvider(new AggregatingToolProvider(toolProviders));
		}
		RetrievalAugmentor augmentor = resolveRetrievalAugmentor();
		if (augmentor != null) {
			builder.retrievalAugmentor(augmentor);
		}
		return builder.build();
	}

	/** 记忆：显式 provider → 传入的 store → 进程内 store；每个 memoryId 一份窗口记忆 */
	private ChatMemoryProvider resolveChatMemoryProvider() {
		if (chatMemoryProvider != null) {
			return chatMemoryProvider;
		}
		final ChatMemoryStore store = memoryStore == null ? new InMemoryChatMemoryStore() : memoryStore;
		final AiPersistenceStrategy resolvedStrategy = resolveEphemeralStrategy();
		final java.util.List<String> separators = ephemeralSeparators != null
				? ephemeralSeparators
				: java.util.Collections.singletonList(EphemeralChatMemory.DEFAULT_RAG_SEPARATOR);
		return memoryId -> {
			ChatMemory delegate;
			if (tokenizer != null) {
				int limit = maxTokens == null ? 1000 : maxTokens;
				delegate = TokenWindowChatMemory.builder()
						.id(memoryId)
						.maxTokens(limit, tokenizer)
						.chatMemoryStore(store)
						.build();
			} else {
				delegate = MessageWindowChatMemory.builder()
						.id(memoryId)
						.maxMessages(memoryMaxMessages)
						.chatMemoryStore(store)
						.build();
			}
			return EphemeralChatMemory.builder(delegate, store)
					.aiPersistenceStrategy(resolvedStrategy)
					.ephemeralSeparators(separators)
					.stripRagFromHistory(ephemeralStripRagFromHistory)
					.includeCurrentUserWithEphemeral(ephemeralIncludeCurrentUserWithEphemeral)
					.sendToolResultsInCurrentTurn(ephemeralSendToolResultsInCurrentTurn)
					.sendToolRequestsInCurrentTurn(ephemeralSendToolRequestsInCurrentTurn)
					.build();
		};
	}

	/**
	 * 解析 AI 持久化策略，优先级：策略对象 &gt; 策略 Key（name/class）&gt; 默认
	 * {@link AiPersistenceStrategy#PERSIST_NON_TOOL_FINAL_ANSWER}。
	 * <p>随后依次叠加「长度限制」与「工具结果持久化」两个装饰器（按开关）。</p>
	 */
	private AiPersistenceStrategy resolveEphemeralStrategy() {
		AiPersistenceStrategy strategy;
		if (aiPersistenceStrategy != null) {
			// 优先级 1：显式策略对象
			strategy = aiPersistenceStrategy;
		} else if (aiPersistenceStrategyKey != null && !aiPersistenceStrategyKey.trim().isEmpty()) {
			// 优先级 2：策略 Key（注册名或全限定类名）
			AiPersistenceStrategy resolved = AiPersistenceStrategy.Registry.tryResolve(aiPersistenceStrategyKey);
			if (resolved == null) {
				if (strictAiPersistenceStrategy) {
					throw new IllegalArgumentException("未知的 AI 持久化策略：'" + aiPersistenceStrategyKey
							+ "'（未在 AiPersistenceStrategy.Registry 注册，也无法作为 AiPersistenceStrategy 类实例化）");
				}
				resolved = AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER;
			}
			strategy = resolved;
		} else {
			// 优先级 3：默认
			strategy = AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER;
		}

		if (ephemeralLimitAiLength) {
			strategy = LengthBoundedAiPersistenceStrategy.builder(strategy)
					.maxCharsPerMessage(ephemeralMaxAiCharsPerMessage)
					.maxTotalChars(ephemeralMaxAiTotalChars)
					.maxMessages(ephemeralMaxAiMessages <= 0 ? Integer.MAX_VALUE : ephemeralMaxAiMessages)
					.truncateIfExceeds(ephemeralTruncateAiIfExceeds)
					.build();
		}

		if (ephemeralPersistToolResults) {
			strategy = PersistToolResultsAiPersistenceStrategy.builder(strategy)
					.persistToolResults(true)
					.maxToolResultChars(ephemeralMaxToolResultChars)
					.maxToolResults(ephemeralMaxToolResults)
					.build();
		}

		return strategy;
	}

	/** 检索增强：显式 augmentor → 传入的检索器/注入器组装；都没给则不启用检索 */
	private RetrievalAugmentor resolveRetrievalAugmentor() {
		if (retrievalAugmentor != null) {
			return retrievalAugmentor;
		}
		if (contentRetriever == null && contentInjector == null) {
			return null;
		}
		DefaultRetrievalAugmentor.DefaultRetrievalAugmentorBuilder builder = DefaultRetrievalAugmentor.builder();
		if (contentRetriever != null) {
			builder.contentRetriever(contentRetriever);
		}
		if (contentInjector != null) {
			builder.contentInjector(contentInjector);
		}
		if (scoringModel != null) {
			ReRankingContentAggregator.ReRankingContentAggregatorBuilder aggregator =
					ReRankingContentAggregator.builder().scoringModel(scoringModel);
			if (rerankMinScore != null) {
				aggregator.minScore(rerankMinScore);
			}
			builder.contentAggregator(aggregator.build());
		}
		return builder.build();
	}
}
