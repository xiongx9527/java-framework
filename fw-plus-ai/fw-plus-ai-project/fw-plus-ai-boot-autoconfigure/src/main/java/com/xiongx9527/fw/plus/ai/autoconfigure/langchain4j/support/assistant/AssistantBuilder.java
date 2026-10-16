package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticToolProviders;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.aggregate.AggregatingToolProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextAwareChatMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextInjector;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextMaintenance;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentClassifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentRag;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentSupplier;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentTools;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.AiPersistenceStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.aggregate.WeightedScoringContentAggregator;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemoryProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.LengthBoundedAiPersistenceStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryIds;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.PersistToolResultsAiPersistenceStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session.SessionIdResolver;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.skills.Skills;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 助手构建器：把 {@code AiServices} 的装配过程做成脚本可直接链式调用的形态，
 * 一一对应 {@code AiServices} 的能力（模型 / 流式模型 / 记忆 / 系统提示 / 工具 / 检索增强）。
 * 产出是双层结构：内层 {@code TextAssistant}（AiServices 原生管线）+ 外层
 * {@link MultimodalAssistantBridge}（多模态桥接），对外仍表现为 {@link Assistant} 接口。
 *
 * <p>所有依赖都由调用方作为参数传入（不做 Spring 注入）：实例从各类型脚本模块取，
 * 如 {@code langchain4jChatModel['key']}、{@code langchain4jChatMemoryStore['key']}、
 * {@code langchain4jToolProvider['key']}、{@code langchain4jContentRetriever['key']}、
 * {@code langchain4jContentInjector.get(path)}。</p>
 *
 * <p>{@link #chatModel(ChatModel)} 与 {@link #streamingModel(StreamingChatModel)}
 * 至少配一个；其余能力不配即不参与装配。
 * 记忆二选一：配置 {@link #tokenizer(TokenCountEstimator)} 用 {@code TokenWindowChatMemory}（按 token 截断），
 * 否则用 {@code MessageWindowChatMemory}（按条数截断）——都不配时用进程内窗口记忆（默认 20 条）。
 * 无论哪种，内置记忆都包一层 {@link EphemeralChatMemory}：默认按 {@code PERSIST_NON_TOOL_FINAL_ANSWER}
 * 策略把「上一轮未调用工具」的最终回答沉淀进历史、带到下一轮（可用 {@link #aiPersistenceStrategy(AiPersistenceStrategy)}
 * 或 {@link #ephemeralChatMemory(EphemeralChatMemoryProperties)} 切换/自定义策略与插入位置）；
 * RAG 检索段与工具往返不进历史，只在本轮请求里带出。无有效字符的 AI 回复一律不沉淀。
 * 检索段不进历史由 {@code storeRetrievedContentInChatMemory(false)} 保证——langchain4j 该开关默认
 * {@code true}，会把拼了检索内容的整条 user message 写进记忆。
 *
 * <p>agentic 编排接入（agent-as-tool）：{@link #agentic} 把编排产物挂为工具，另有
 * {@link #agenticByKey}（注册中心 key 引用）与 {@link #agenticGraph}（langgraph4j 编译图）两条入口，
 * 内部复用 {@code AgenticToolProviders} 包装为 {@code ToolProvider}。</p>
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

	private ChatModel chatModel;

	private StreamingChatModel streamingModel;

	private LanguageModel languageModel;

	private StreamingLanguageModel streamingLanguageModel;

	private Function<Object, String> systemMessageProvider;

	private ChatMemoryProvider chatMemoryProvider;

	private int memoryMaxMessages = DEFAULT_MEMORY_MAX_MESSAGES;

	/** 按 token 截断记忆时使用的 Tokenizer；配置后记忆改用 TokenWindowChatMemory */
	private TokenCountEstimator tokenizer;

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

/** Agent Skills（langchain4j-skills）：提供 activate_skill / read_skill_resource 工具并注入技能目录 */
	private final List<Skill> skills = new ArrayList<>();

	/** 工具参数错误处理器：缺参/绑定失败回给 LLM 的策略（fork 原生扩展点，缺省用内置默认：缺参反问、格式错修正重试） */
	private ToolArgumentsErrorHandler toolArgumentsErrorHandler = DEFAULT_ARGUMENTS_ERROR_HANDLER;

	/** 工具执行期业务异常处理器：fork 原生扩展点；缺省（{@code null}）时由装配阶段补显式默认（把异常消息回给 LLM） */
	private ToolExecutionErrorHandler toolExecutionErrorHandler;

	private RetrievalAugmentor retrievalAugmentor;

	private ContentRetriever contentRetriever;

	private ContentInjector contentInjector;

	private ScoringModel scoringModel;

	private Double rerankMinScore;

private Integer rerankMaxResults;

	private Double similarityWeight;

	private Double semanticWeight;

	/** 意图分类器：经 {@link #intent(IntentClassifier)} 挂载；同时注入产出的 SimpleAssistant */
	private IntentClassifier<?> intentClassifier;

	/** 上下文组合策略（默认 {@code memory-and-enriched}）；控制是否注入附加来源（摘要等） */
	private ContextStrategy contextStrategy = ContextStrategy.MEMORY_AND_ENRICHED;

	/** 有序上下文来源集合（如 {@code SummaryContextProvider}）；空则不注入 */
	private final List<ContextProvider> contextProviders = new ArrayList<ContextProvider>();

	/** 上下文维护钩子（读时兜底，如 {@code SummaryMaintenance}） */
	private final List<ContextMaintenance> contextMaintenances = new ArrayList<ContextMaintenance>();

	/** 会话标识解析器（默认在 {@link SimpleAssistant} 内为 daily）；配置 {@code session.session-id-resolver} 时覆盖 */
	private SessionIdResolver sessionResolver;

	/** 对话模型（必填）：{@code langchain4jChatModel['key']} 或任意 {@link ChatModel} 实例 */
	public AssistantBuilder chatModel(ChatModel model) {
		this.chatModel = model;
		return this;
	}

	/** 流式模型：{@code langchain4jStreamingChatModel['key']}；不配则助手不支持 {@code stream(...)} */
	public AssistantBuilder streamingModel(StreamingChatModel model) {
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

	/**
	 * Agent Skills（{@code langchain4j-skills}）：注册 {@code activate_skill} /
	 * {@code read_skill_resource} 两个工具实现技能按需加载，并把
	 * {@code formatAvailableSkills()} 目录追加到系统提示末尾。
	 * 技能源：{@code langchain4jSkill['key']}（控制台维护）、
	 * {@code langchain4jSkill.list()}（全部）或 {@code FileSystemSkillLoader} 加载。
	 * 支持数组/集合累积，同名技能后注册覆盖先注册。
	 */
	public AssistantBuilder skills(Skill... items) {
		return skills(items == null ? Collections.<Skill>emptyList() : Arrays.asList(items));
	}

	public AssistantBuilder skills(Collection<Skill> items) {
		if (items == null) {
			return this;
		}
		for (Skill item : items) {
			if (item == null) {
				continue;
			}
			this.skills.removeIf(existing -> Objects.equals(existing.name(), item.name()));
			this.skills.add(item);
		}
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
	public AssistantBuilder tokenizer(TokenCountEstimator tokenizer) {
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

	/** 上下文组合策略：{@code memory} / {@code enriched} / {@code memory-and-enriched}（默认后者） */
	public AssistantBuilder contextStrategy(ContextStrategy strategy) {
		if (strategy != null) {
			this.contextStrategy = strategy;
		}
		return this;
	}

	/** 注册一个有序上下文来源（如 {@code new SummaryContextProvider(summaryStore)}），可传多个 */
	public AssistantBuilder contextProvider(ContextProvider... providers) {
		if (providers != null) {
			for (ContextProvider provider : providers) {
				if (provider != null) {
					this.contextProviders.add(provider);
				}
			}
		}
		return this;
	}

	/** 注册上下文维护钩子（读时兜底，如 {@code new SummaryMaintenance(engine, windowSize)}），可传多个 */
	public AssistantBuilder contextMaintenance(ContextMaintenance... maintenances) {
		if (maintenances != null) {
			for (ContextMaintenance maintenance : maintenances) {
				if (maintenance != null) {
					this.contextMaintenances.add(maintenance);
				}
			}
		}
		return this;
	}

	/** 会话标识解析器：配置 {@code session.session-id-resolver}（daily/explicit）时覆盖默认 daily */
	public AssistantBuilder sessionResolver(SessionIdResolver resolver) {
		if (resolver != null) {
			this.sessionResolver = resolver;
		}
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
	 * 网页抓取工具（{@code langchain4j-community-tool-web-scraper}）：抓取 URL 并转为
	 * Markdown 文本交给大模型。等价于 {@code tools(new WebScraperTool())}（同类型覆盖）。
	 * 需 classpath 上有该 optional 构件，缺失时抛出可操作异常。
	 */
	public AssistantBuilder webScraperTool() {
		try {
			return tools(new dev.langchain4j.community.tool.webscraper.WebScraperTool());
		} catch (NoClassDefFoundError e) {
			throw new IllegalStateException(
					"webScraperTool 需要 classpath 引入 dev.langchain4j:langchain4j-community-tool-web-scraper", e);
		}
	}

	/**
	 * 浏览器操作工具（{@code langchain4j-community-tool-browser-use}）：把动作下发到
	 * browser-use 执行引擎（{@code langchain4j-community-core} 提供
	 * {@code BrowserExecutionEngine}，通常对接外部 browser-use 服务）。同类型覆盖。
	 */
	public AssistantBuilder browserUseTool(dev.langchain4j.community.browser.use.BrowserExecutionEngine engine) {
		return tools(dev.langchain4j.community.tool.browseruse.BrowserUseTool.from(engine));
	}

	/**
	 * 工具提供者：可调用多次累积（如 magic-api 的 {@code langchain4jToolProvider['key']}），
	 * 同一实例重复添加会被忽略。
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

	/**
	 * agent-as-tool：把 agentic 编排产物挂为工具，主 LLM 依据工具描述自主决定何时调用，
	 * 编排结果作为工具返回值回传。可多次调用累积，等价于
	 * {@code toolProvider(AgenticToolProviders.agenticToolProvider(...))}。
	 *
	 * @param pipeline        编排产物（如 {@code AgenticWorkflows.sequence()...build()}）
	 * @param toolName        工具名（LLM 可见）
	 * @param toolDescription 工具描述（LLM 据此决定是否调用）
	 * @param outputKey       从编排产出中提取的字段名；{@code null} 表示原样输出
	 * @param inputKeys       工具入参名，逐个声明为字符串属性并作为编排输入 state
	 */
	public AssistantBuilder agentic(dev.langchain4j.agentic.UntypedAgent pipeline, String toolName,
			String toolDescription, String outputKey, String... inputKeys) {
		if (pipeline == null) {
			throw new IllegalArgumentException("agentic 需要非 null 的编排产物（UntypedAgent）");
		}
		return toolProvider(AgenticToolProviders.agenticToolProvider(pipeline, toolName, toolDescription,
				outputKey, inputKeys));
	}

	/**
	 * agent-as-tool（注册中心版）：按 {@link AgenticPipelineRegistry} 的 key 引用编排，
	 * 调用方无需接触 agentic 类型。key 未注册时抛 {@link IllegalArgumentException}（含已注册 key 列表）。
	 *
	 * @param registry        编排注册中心
	 * @param agenticKey      编排 key
	 * @param toolName        工具名（LLM 可见）
	 * @param toolDescription 工具描述（LLM 据此决定是否调用）
	 * @param outputKey       从编排产出中提取的字段名；{@code null} 表示原样输出
	 * @param inputKeys       工具入参名，逐个声明为字符串属性并作为编排输入 state
	 */
	public AssistantBuilder agenticByKey(AgenticPipelineRegistry registry, String agenticKey, String toolName,
			String toolDescription, String outputKey, String... inputKeys) {
		if (registry == null) {
			throw new IllegalArgumentException("agenticByKey 需要非 null 的 AgenticPipelineRegistry");
		}
		return toolProvider(AgenticToolProviders.agenticToolProviderByKey(registry, agenticKey, toolName,
				toolDescription, outputKey, inputKeys));
	}

	/**
	 * agent-as-tool（langgraph4j 图版）：把编译图挂为工具，工具入参即图的 state，
	 * 图终止后的 state 作为工具返回值回传。等价于
	 * {@code toolProvider(AgenticToolProviders.graphToolProvider(...))}。
	 *
	 * @param compiledGraph   langgraph4j 编译图
	 * @param toolName        工具名（LLM 可见）
	 * @param toolDescription 工具描述（LLM 据此决定是否调用）
	 */
	public AssistantBuilder agenticGraph(
			org.bsc.langgraph4j.CompiledGraph<? extends org.bsc.langgraph4j.state.AgentState> compiledGraph,
			String toolName, String toolDescription) {
		if (compiledGraph == null) {
			throw new IllegalArgumentException("agenticGraph 需要非 null 的 CompiledGraph");
		}
		return toolProvider(AgenticToolProviders.graphToolProvider(compiledGraph, toolName, toolDescription));
	}

	/**
	 * 工具参数错误处理：必填参数缺失/参数绑定失败时，如何回文本给 LLM——
	 * 缺参提示反问用户补齐、格式错误提示修正重试等策略由业务来定；
	 * 不配置时用内置默认（缺参反问、格式错修正重试），写入的是 fork 原生
	 * {@code AiServices.toolArgumentsErrorHandler(...)} 槽位。
	 *
	 * <p>1.20.2 缺参抛的是裸 {@code IllegalArgumentException}（消息前缀
	 * {@code Required parameter ... is missing}），没有自定义缺参类——需要区分缺参与
	 * 格式错时按消息前缀判断，不再是 dev 的 {@code instanceof MissingArgumentException}。</p>
	 *
	 * <p>handler 返回 {@link ToolErrorHandlerResult}（如 {@code ToolErrorHandlerResult.text(...)}），
	 * 缺参/绑定失败不会炸给业务调用方，而是作为 {@code ToolExecutionResultMessage} 回给 LLM。</p>
	 *
	 * @param handler fork 原生 {@link ToolArgumentsErrorHandler}；传 {@code null} 恢复内置默认
	 */
	public AssistantBuilder toolArgumentsErrorHandler(ToolArgumentsErrorHandler handler) {
		this.toolArgumentsErrorHandler = handler != null ? handler : DEFAULT_ARGUMENTS_ERROR_HANDLER;
		return this;
	}

	/**
	 * 工具执行期业务异常处理：工具方法内部抛出的异常如何回文本给 LLM，
	 * 由 fork 原生 {@code AiServices.toolExecutionErrorHandler(...)} 槽位承接。
	 * 不配置时装配阶段会补上显式默认 {@link ToolExecutionErrorHandler#sendExceptionMessageToLlm()}
	 * （把异常消息回给 LLM，等价 fork 现行隐式默认）。
	 *
	 * @param handler fork 原生 {@link ToolExecutionErrorHandler}；传 {@code null} 恢复装配阶段显式默认
	 */
	public AssistantBuilder toolExecutionErrorHandler(ToolExecutionErrorHandler handler) {
		this.toolExecutionErrorHandler = handler;
		return this;
	}

	/** 内置默认参数错误处理器（fork 原生形态）：缺参反问用户补齐、其余格式错提示修正重试 */
	private static final ToolArgumentsErrorHandler DEFAULT_ARGUMENTS_ERROR_HANDLER = (error, context) -> {
		String message = error.getMessage();
		if (message != null && message.startsWith("Required parameter")) {
			return ToolErrorHandlerResult.text(
					"工具调用缺少必要参数，请向用户询问缺失的信息，等用户补齐参数后再调用。详情：" + message);
		}
		String detail = message != null ? message : error.getClass().getSimpleName();
		return ToolErrorHandlerResult.text("工具调用参数格式错误，请修正参数后重试。详情：" + detail);
	};

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

	/** rerank 保留上限：重排后最多保留的条目数（仅在配置了 scoringModel 时生效；不配保留全部） */
	public AssistantBuilder rerankMaxResults(Integer maxResults) {
		this.rerankMaxResults = maxResults;
		return this;
	}


	/** 相似度分权重（0~1，默认 0；与 semanticWeight 配合启用加权混合重排，总分 = 相似度×本值 + 语义分×semanticWeight） */
	public AssistantBuilder similarityWeight(Double weight) {
		this.similarityWeight = weight;
		return this;
	}

	/** 语义分权重（0~1；启用加权混合重排时使用） */
	public AssistantBuilder semanticWeight(Double weight) {
		this.semanticWeight = weight;
		return this;
	}

	/**
	 * 意图识别一站式入口：挂上分类器，并在返回的 {@link IntentRouting} 上按需配置意图化检索
	 * （{@link IntentRag}）与意图化工具（{@link IntentTools}）。
	 *
	 * <p>{@link IntentRouting#done()} / {@link IntentRouting#build()} 会把配好的意图检索器设为
	 * {@code contentRetriever}、意图工具提供者设为 {@code toolProvider}（与直接调用这两个方法同义，
	 * <b>后调用者覆盖先调用者</b>）；只挂分类器、不配路由时，不影响原有检索/工具装配，仅额外提供
	 * {@link SimpleAssistant#classifyIntent(String)}。分类器同时注入产出的 {@link SimpleAssistant}。</p>
	 *
	 * <pre>{@code
	 * AssistantBuilder builder = new AssistantBuilder().chatModel(model);
	 * AssistantBuilder.IntentRouting<MyIntent> intent = builder.intent(classifier);
	 * intent.rag().route(MyIntent.LOGISTICS, logisticsRetriever).fallback(defaultRetriever);
	 * intent.tools().source(aggregatingToolProvider).allow(MyIntent.LOGISTICS, "track_order");
	 * SimpleAssistant assistant = intent.build();
	 * }</pre>
	 */
	public <I extends Enum<I>> IntentRouting<I> intent(IntentClassifier<I> classifier) {
		if (classifier == null) {
			throw new IllegalArgumentException("classifier 不能为 null");
		}
		this.intentClassifier = classifier;
		return new IntentRouting<I>(this, classifier);
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
		// languageModel / streamingLanguageModel 透传给 SimpleAssistant 的直连补全能力
		SimpleAssistant assistant = new SimpleAssistant(assemble(), chatModel, null, languageModel,
				streamingLanguageModel, resolveRetrievalAugmentor());
		// 意图分类器注入：供 SimpleAssistant.classifyIntent 使用（路由由 intent(...) 装配到 contentRetriever/toolProvider）
		if (intentClassifier != null) {
			assistant.intentClassifier(intentClassifier);
		}
		// 检索器支持显式元数据过滤时，接线供 retrieve/chatWithFilter/streamWithFilter 过滤重载使用
		if (contentRetriever instanceof SimpleAssistant.FilterableRetriever filterable) {
			assistant.filterableRetriever(filterable);
		}
		// 会话标识解析器：配置 session.session-id-resolver 时覆盖默认 daily
		if (sessionResolver != null) {
			assistant.sessionResolver(sessionResolver);
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
// 双层结构：内层 TextAssistant（原 Assistant 签名）走 AiServices 原生完整管线
		// （{{context}} 模板、工具循环（原生 toolArgumentsErrorHandler 兜底）、RAG、会话记忆），
		// 外层 MultimodalAssistantBridge 实现 Assistant 新签名——纯文本轮转发内层，
		// 多模态轮自拼 UserMessage 直调模型（记忆写文本占位、不挂工具）
		ChatMemoryProvider memoryProvider = resolveChatMemoryProvider();
		AiServices<TextAssistant> builder = AiServices.builder(TextAssistant.class)
				.chatMemoryProvider(memoryProvider)
				// 检索增强后的 user message 只进请求、不进记忆：默认 true 会把 RAG 检索段整条写进
				// ChatMemoryStore，每轮重复计费且与后续问题无关。置 false 后框架写的是用户原话，
				// 工具续调时由 ToolService.resolveMessagesForNextRequest 把增强后的原话换回请求
				// 末尾（UserMessage.replaceLast），本轮 RAG 仍然只出现一次。
				.storeRetrievedContentInChatMemory(false);

		if (chatModel != null) {
			builder.chatModel(chatModel);
		}
		if (streamingModel != null) {
			builder.streamingChatModel(streamingModel);
		}
		// Agent Skills：注册 activate_skill / read_skill_resource 工具，并把技能目录追加到系统提示
		Function<Object, String> effectiveSystemMessage = systemMessageProvider;
		List<ToolProvider> effectiveToolProviders = toolProviders;
		if (!skills.isEmpty()) {
			Skills aggregated = Skills.from(skills);
			String catalog = aggregated.formatAvailableSkills();
			Function<Object, String> base = systemMessageProvider;
			effectiveSystemMessage = base == null
					? memoryId -> catalog
					: memoryId -> base.apply(memoryId) + "\n\n" + catalog;
			effectiveToolProviders = new ArrayList<>(toolProviders);
			effectiveToolProviders.add(aggregated.toolProvider());
		}
		if (effectiveSystemMessage != null) {
			builder.systemMessageProvider(effectiveSystemMessage);
		}
		if (!tools.isEmpty()) {
			builder.tools(tools);
		}
		if (!effectiveToolProviders.isEmpty()) {
			builder.toolProviders(effectiveToolProviders);
		}
		// 原生扩展点透传：参数错误缺省内置默认（缺参反问/格式错修正）；
		// 工具执行期业务异常：未显式配置时也显式设为「把异常消息回给 LLM」——该值等价
		// langchain4j 现行隐式默认，显式设置既锁定语义、又消除 ToolErrorHandlingNotice 告警
		// （其默认值未来会改为 failInvocationUnlessVisibleToLlm，显式固定可避免升级时的破坏性变更）。
		builder.toolArgumentsErrorHandler(toolArgumentsErrorHandler);
		boolean hasTools = !tools.isEmpty() || !effectiveToolProviders.isEmpty();
		if (toolExecutionErrorHandler != null) {
			builder.toolExecutionErrorHandler(toolExecutionErrorHandler);
		}
		else if (hasTools) {
			builder.toolExecutionErrorHandler(ToolExecutionErrorHandler.sendExceptionMessageToLlm());
		}
		RetrievalAugmentor augmentor = resolveRetrievalAugmentor();
		if (augmentor != null) {
			builder.retrievalAugmentor(augmentor);
		}
		// 上下文注入：记忆读侧在 1.x 时序下看不到本轮用户消息，故注入点前移到请求装配之后、
		// 模型调用之前（chatRequestTransformer），把统一上下文渲染为前置系统消息。
		if (contextStrategy.injectsEnriched() && !contextProviders.isEmpty()) {
			builder.chatRequestTransformer(enrichedContextTransformer());
		}
		return new MultimodalAssistantBridge(builder.build(), chatModel, streamingModel,
				memoryProvider, systemMessageProvider, augmentor);
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
			ChatMemory ephemeral = EphemeralChatMemory.builder(delegate, store)
					.aiPersistenceStrategy(resolvedStrategy)
					.ephemeralSeparators(separators)
					.stripRagFromHistory(ephemeralStripRagFromHistory)
					.includeCurrentUserWithEphemeral(ephemeralIncludeCurrentUserWithEphemeral)
					.sendToolResultsInCurrentTurn(ephemeralSendToolResultsInCurrentTurn)
					.sendToolRequestsInCurrentTurn(ephemeralSendToolRequestsInCurrentTurn)
					.build();
			return wrapContextAware(memoryId, ephemeral);
		};
	}

	/**
	 * 用 {@link ContextAwareChatMemory} 包裹记忆做上下文维护（写时滚动 + 读时兜底）；
	 * 策略为 {@code memory} 或未注册来源时原样返回（不维护、零开销）。会话键即当前
	 * effective memoryId（5 段），userId 由 memoryId 段[3] 解析（跨会话维度）。
	 *
	 * <p>注意：上下文「注入」不在记忆读侧承担——langchain4j 1.x 一轮内先读记忆、后写本轮
	 * 用户消息，读取时看不到本轮输入；注入由 {@link #enrichedContextTransformer()} 挂在
	 * AiServices 的 {@code chatRequestTransformer} 上完成。</p>
	 */
	private ChatMemory wrapContextAware(Object memoryId, ChatMemory delegate) {
		if (!contextStrategy.injectsEnriched() || contextProviders.isEmpty()) {
			return delegate;
		}
		final String sessionKey = memoryId == null ? null : memoryId.toString();
		final String userId = sessionKey == null ? null
				: ChatMemoryIds.parseUserId(sessionKey, ChatMemoryIds.DEFAULT_USER_ID);
		return new ContextAwareChatMemory(delegate,
				latest -> new ContextRequest(sessionKey, sessionKey, userId, latest), contextMaintenances);
	}

	/**
	 * 上下文注入器：挂在 AiServices 的 {@code chatRequestTransformer} 上（请求装配后、模型调用前），
	 * 把 {@link ContextInjector} 渲染的统一上下文作为前置系统消息注入；无内容时不改变请求。
	 *
	 * <p>与记忆读侧注入等价的信息（会话键 / userId / 本轮用户输入）在此处均可获得：本轮用户消息
	 * 已在请求里（取最后一条 {@link UserMessage}），跨会话维度由 memoryId 段[3] 解析，RAG 检索段
	 * 按 {@link EphemeralChatMemory} 同一分隔符口径剥离，保证来源拿到的是用户原话。</p>
	 */
	private BiFunction<ChatRequest, Object, ChatRequest> enrichedContextTransformer() {
		final ContextInjector injector = new ContextInjector(contextProviders, contextStrategy);
		final List<String> separators = ephemeralSeparators != null
				? new ArrayList<String>(ephemeralSeparators)
				: Collections.singletonList(EphemeralChatMemory.DEFAULT_RAG_SEPARATOR);
		return (request, memoryId) -> {
			String sessionKey = memoryId == null ? null : memoryId.toString();
			String userId = sessionKey == null ? null
					: ChatMemoryIds.parseUserId(sessionKey, ChatMemoryIds.DEFAULT_USER_ID);
			String latest = latestUserText(request.messages(), separators);
			String context = injector.render(new ContextRequest(sessionKey, sessionKey, userId, latest));
			if (context == null || context.isEmpty()) {
				return request;
			}
			List<ChatMessage> messages = new ArrayList<ChatMessage>(request.messages().size() + 1);
			messages.add(SystemMessage.from(context));
			messages.addAll(request.messages());
			return request.toBuilder().messages(messages).build();
		};
	}

	/** 请求中最后一条用户消息的文本：无单文本（多模态）回落 {@code null}，并按需剥离 RAG 检索段 */
	private static String latestUserText(List<ChatMessage> messages, List<String> separators) {
		if (messages == null) {
			return null;
		}
		for (int i = messages.size() - 1; i >= 0; i--) {
			ChatMessage message = messages.get(i);
			if (message instanceof UserMessage) {
				UserMessage userMessage = (UserMessage) message;
				return stripRetrievalSegment(
						userMessage.hasSingleText() ? userMessage.singleText() : null, separators);
			}
		}
		return null;
	}

	/** 剥离 RAG 检索段（与 {@link EphemeralChatMemory} 同一分隔符口径），保留用户原话 */
	private static String stripRetrievalSegment(String text, List<String> separators) {
		if (text == null || text.isEmpty() || separators == null) {
			return text;
		}
		for (String separator : separators) {
			if (separator == null || separator.isEmpty()) {
				continue;
			}
			int anchor = text.indexOf(separator);
			if (anchor >= 0) {
				return text.substring(0, anchor);
			}
		}
		return text;
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
			if (similarityWeight != null && semanticWeight != null) {
				// 加权混合模式：向量相似度分与 rerank 语义分按权重融合
				builder.contentAggregator(new WeightedScoringContentAggregator(scoringModel,
						similarityWeight, semanticWeight, rerankMinScore, rerankMaxResults));
			} else {
				ReRankingContentAggregator.ReRankingContentAggregatorBuilder aggregator =
						ReRankingContentAggregator.builder().scoringModel(scoringModel);
				if (rerankMinScore != null) {
					aggregator.minScore(rerankMinScore);
				}
				if (rerankMaxResults != null) {
					aggregator.maxResults(rerankMaxResults);
				}
				builder.contentAggregator(aggregator.build());
			}
		}
		return builder.build();
	}

	/**
	 * {@link #intent(IntentClassifier)} 返回的意图配置视图：复用既有的 {@link IntentRag.Builder}
	 * 与 {@link IntentTools.Builder} 收集意图路由，最后接线回 {@link AssistantBuilder}。
	 * 两个配置入口共享同一条意图来源（同一个分类器实例），避免各建一套。
	 *
	 * @param <I> 使用方自定义的意图枚举类型
	 */
	public static final class IntentRouting<I extends Enum<I>> {

		private final AssistantBuilder builder;

		private final IntentRag.Builder<I> ragBuilder;

		private final IntentTools.Builder<I> toolsBuilder;

		private boolean ragConfigured;

		private boolean toolsConfigured;

		private IntentRouting(AssistantBuilder builder, IntentClassifier<I> classifier) {
			this.builder = builder;
			IntentSupplier<I> supplier = IntentSupplier.of(classifier);
			this.ragBuilder = IntentRag.bySupplier(supplier);
			this.toolsBuilder = IntentTools.bySupplier(supplier);
		}

		/** 意图化检索配置（{@link IntentRag} 的 route / routeNone / fallback） */
		public IntentRag.Builder<I> rag() {
			ragConfigured = true;
			return ragBuilder;
		}

		/** 意图化工具配置（{@link IntentTools} 的 source / allow / allowNone / alwaysInclude / fallback） */
		public IntentTools.Builder<I> tools() {
			toolsConfigured = true;
			return toolsBuilder;
		}

		/**
		 * 把已配置的意图路由接线到 {@link AssistantBuilder} 并返回它，便于继续链式配置。
		 * 未调用 {@link #rag()} / {@link #tools()} 的分支不接线。
		 */
		public AssistantBuilder done() {
			if (ragConfigured) {
				builder.contentRetriever(ragBuilder.build());
			}
			if (toolsConfigured) {
				builder.toolProvider(toolsBuilder.build());
			}
			return builder;
		}

		/** 接线后直接装配助手，等价于 {@code done().build()} */
		public SimpleAssistant build() {
			return done().build();
		}
	}
}
