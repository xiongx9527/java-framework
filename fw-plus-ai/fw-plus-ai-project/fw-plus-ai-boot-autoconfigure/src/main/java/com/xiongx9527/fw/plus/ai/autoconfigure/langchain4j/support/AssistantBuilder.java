package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
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
import java.util.Arrays;
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
 * 无论哪种，内置记忆都包一层 {@link EphemeralChatMemory}：历史只沉淀系统提示与用户原话，
 * RAG 检索段与工具往返不进历史。
 */
public class AssistantBuilder {

	/** 进程内记忆的默认窗口（条） */
	private static final int DEFAULT_MEMORY_MAX_MESSAGES = 20;

	private ChatLanguageModel chatModel;

	private StreamingChatLanguageModel streamingModel;

	private Function<Object, String> systemMessageProvider;

	private ChatMemoryProvider chatMemoryProvider;

	private int memoryMaxMessages = DEFAULT_MEMORY_MAX_MESSAGES;

	/** 按 token 截断记忆时使用的 Tokenizer；配置后记忆改用 TokenWindowChatMemory */
	private Tokenizer tokenizer;

	/** TokenWindowChatMemory 的 token 上限（仅在配置了 tokenizer 时生效，默认 1000） */
	private Integer maxTokens;

	private ChatMemoryStore memoryStore;

	private final List<Object> tools = new ArrayList<>();

	private ToolProvider toolProvider;

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

	/** 工具：带 {@code @Tool} 方法的对象，可传多个 */
	public AssistantBuilder tools(Object... toolObjects) {
		if (toolObjects != null) {
			this.tools.addAll(Arrays.asList(toolObjects));
		}
		return this;
	}

	/** 工具：{@code langchain4jToolProvider['key']}（magic-api 的 api/function 暴露成 ToolProvider） */
	public AssistantBuilder toolProvider(ToolProvider provider) {
		this.toolProvider = provider;
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
	public SimpleAssistant build() {
		// 第 4 参是文本补全的 StreamingLanguageModel，builder 不配置（对话流式走 assistant.stream）
		return new SimpleAssistant(assemble(), chatModel, null, null);
	}

	/** 需要底层 {@link Assistant} 类型（如继续挂 AiServices 能力）时取用 */
	public Assistant toAssistant() {
		return assemble();
	}

	private Assistant assemble() {
		if (chatModel == null && streamingModel == null) {
			throw new IllegalArgumentException("构建助手缺少模型：请至少调用 chatModel(...) 或 streamingModel(...) 之一指定");
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
		if (toolProvider != null) {
			builder.toolProvider(toolProvider);
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
		// 内置记忆包一层 EphemeralChatMemory：历史只沉淀系统提示与用户原话，
		// RAG 检索段与工具往返不进历史（与样例 ChatAssistantConfig 的行为一致）
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
			return new EphemeralChatMemory(delegate, store);
		};
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
