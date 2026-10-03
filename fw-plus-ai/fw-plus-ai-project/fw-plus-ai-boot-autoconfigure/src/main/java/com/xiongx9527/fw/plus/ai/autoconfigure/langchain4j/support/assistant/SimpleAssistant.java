package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;




/**
 * {@link Assistant} 的易用封装：常用参数给了默认值，省掉样板代码。
 *
 * <p>{@code Assistant} 的三个参数（memoryId / context / message）里，{@code context} 在
 * 不做动态系统提示定制时恒为空串；这里的重载直接把它省掉：</p>
 *
 * <pre>{@code
 * SimpleAssistant assistant = new SimpleAssistant(assistant);
 * assistant.chat("用一句话介绍杭州");                       // memoryId=default，context=""
 * assistant.chat("session-1", "杭州天气怎么样");             // memoryId 自定，context=""
 *
 * // SSE 推流：start 事件先带 requestId 表示已受理，token 事件逐段推送；
 * // 推理模型思考时推 thinking（增量）/ thinking-done（本轮完整思考文本）；
 * // 工具调用时推 tool-call（执行前，name/arguments）与 tool-result（执行后，name/arguments/result）；
 * // RAG 检索后推 retrieved（JSON 数组，元素为检索片段文本）；
 * // done 事件带完整回复文本；出错发 error 事件（data 为错误信息）后 completeWithError。
 * // 客户端断开时自动取消模型流式调用（langchain4j 1.20.2 StreamingHandle）。
 * SseEmitter emitter = assistant.streamToSse("session-1", "讲个故事");
 * }</pre>
 *
 * <p>模型（{@code ChatModel} / {@code LanguageModel} / {@code StreamingLanguageModel}）
 * 一律经构造方法传入，方法内部直接使用对应字段。直连调用用 generate 系列（无记忆、不经 AiServices）：
 * {@code generate(message)} 直连对话、{@code generateText(prompt)} 文本补全、
 * {@code generateTextToSse(prompt)} 补全 SSE 推流；{@code chat / stream / streamToSse} 走 assistant 记忆对话。</p>
 *
 * <pre>{@code
 * SimpleAssistant simple = new SimpleAssistant(chatModel);   // 或四参构造一次给全
 * simple.generate("你好");                                   // 直连一次性对话（无记忆）
 * simple.generate("你是助手", "你好");                        // 直连 + 系统提示
 * }</pre>
 */
public class SimpleAssistant {

	/** memoryId 缺省值，与 LangChain4j {@code AiServices.DEFAULT} 语义一致 */
	public static final String DEFAULT_MEMORY_ID = "default";

	/** 上下文参数的缺省值：不做动态系统提示定制时恒为空串 */
	public static final String DEFAULT_CONTEXT = "";

	/** done 事件的兜底数据：模型没有产出文本时使用 */
	public static final String DONE_FALLBACK = "[DONE]";

	/** SSE 事件名：开始（data 为 requestId） */
	public static final String EVENT_START = "start";

	/** SSE 事件名：逐段 token */
	public static final String EVENT_TOKEN = "token";

	/** SSE 事件名：结束（data 为完整回复文本或 {@value #DONE_FALLBACK}） */
	public static final String EVENT_DONE = "done";

	/** SSE 事件名：出错（data 为错误信息，发送后 completeWithError 结束） */
	public static final String EVENT_ERROR = "error";

	/** SSE 事件名：思考中（data 为思考文本增量，仅推理模型产生） */
	public static final String EVENT_THINKING = "thinking";

	/** SSE 事件名：思考完成（data 为本轮完整思考文本） */
	public static final String EVENT_THINKING_DONE = "thinking-done";

	/** SSE 事件名：工具调用中（工具执行前，data 为 JSON：name、arguments） */
	public static final String EVENT_TOOL_CALL = "tool-call";

	/** SSE 事件名：工具调用完成（data 为 JSON：name、arguments、result） */
	public static final String EVENT_TOOL_RESULT = "tool-result";

	/** SSE 事件名：RAG 检索完成（data 为 JSON 数组，元素为检索到的文本片段） */
	public static final String EVENT_RETRIEVED = "retrieved";

	private final Assistant assistant;

	private final ChatModel chatModel;

	private final StreamingChatModel streamingTextModel;

	private final LanguageModel languageModel;

	private final StreamingLanguageModel streamingLanguageModel;

	private final RetrievalAugmentor retrievalAugmentor;

	/**
	 * 全量构造：底层 {@link Assistant} 与各模型、检索增强都由调用方给定（不需要的可传 null），
	 * 便于把外部已有的模型实例装配进同一个封装对象。
	 */
	public SimpleAssistant(Assistant assistant, ChatModel chatModel, StreamingChatModel streamingTextModel,
						   LanguageModel languageModel, StreamingLanguageModel streamingLanguageModel,
						   RetrievalAugmentor retrievalAugmentor) {
		this.assistant = assistant;
		this.chatModel = chatModel;
		this.streamingTextModel = streamingTextModel;
		this.languageModel = languageModel;
		this.streamingLanguageModel = streamingLanguageModel;
		this.retrievalAugmentor = retrievalAugmentor;
	}

	/** 全能来源：{@code AiServices<Assistant>}（chat / stream / 记忆 / 工具 / 检索增强） */
	public SimpleAssistant(Assistant assistant) {
		this(assistant, null, null, null, null, null);
	}

	/** 仅同步对话：{@code ChatModel}，无记忆、无流式 */
	public SimpleAssistant(ChatModel chatModel) {
		this(null, chatModel, null, null, null, null);
	}

	/** 仅流式对话：{@code StreamingChatModel}，逐段推送 */
	public SimpleAssistant(StreamingChatModel streamingTextModel) {
		this(null, null, streamingTextModel, null, null, null);
	}

	/** 仅文本补全：{@code LanguageModel} */
	public SimpleAssistant(LanguageModel languageModel) {
		this(null, null, null, languageModel, null, null);
	}

	/** 仅流式补全：{@code StreamingLanguageModel}，逐段推送 */
	public SimpleAssistant(StreamingLanguageModel streamingLanguageModel) {
		this(null, null, null, null, streamingLanguageModel, null);
	}

	/** 仅检索：{@code RetrievalAugmentor}（不聊天、不调用对话大模型） */
	public SimpleAssistant(RetrievalAugmentor retrievalAugmentor) {
		this(null, null, null, null, null, retrievalAugmentor);
	}

	/** 需要底层 {@link Assistant} 类型（如再挂别的 AiServices 能力）时取用 */
	public Assistant toAssistant() {
		return assistant;
	}

	// -------------------------------------------------- 只检索（不调用对话大模型）

	/**
	 * 只检索：对问题走检索增强链（contentRetriever / 配置了 scoringModel 时含 rerank 重排），
	 * 返回命中的内容列表；不调用对话大模型、不产生回复、不写会话记忆。
	 * 仅检索构造（{@link #SimpleAssistant(RetrievalAugmentor)} 或
	 * {@code AssistantBuilder.retrievalOnly()}）下可用。
	 */
	public List<Content> retrieve(String message) {
		return retrieve(DEFAULT_MEMORY_ID, message);
	}

	/** 只检索：memoryId 自定（可做检索侧会话区分），返回命中的内容列表 */
	public List<Content> retrieve(String memoryId, String message) {
		return retrievalAugmentor().augment(new AugmentationRequest(
				UserMessage.from(message),
				new dev.langchain4j.rag.query.Metadata(UserMessage.from(message), memoryId,
						Collections.<ChatMessage>emptyList()))).contents();
	}

	private RetrievalAugmentor retrievalAugmentor() {
		if (retrievalAugmentor == null) {
			throw new IllegalStateException(
					"未配置检索增强：请用 contentRetriever(...) / retrievalAugmentor(...) 配置，"
							+ "或使用 AssistantBuilder.retrievalOnly() 只检索模式构建");
		}
		return retrievalAugmentor;
	}

	// -------------------------------------------------- 带元数据过滤的检索与对话（显式参数）

	/**
	 * 显式元数据过滤检索回调：由带知识库检索的构建方注入（如 {@code KnowledgeAssistantFactory}），
	 * 实现方负责将传入条件与自身固定条件叠加（AND）。
	 */
	public interface FilterableRetriever {

		/**
		 * 检索：{@code metadataCondition} 为 Dify metadata_condition 格式（可 null）。
		 */
		List<Content> retrieve(String message, com.alibaba.fastjson2.JSONObject metadataCondition);
	}

	private FilterableRetriever filterableRetriever;

	/** 注入显式过滤检索回调（链式），供 {@link #retrieve(String, String, String)} /
	 * {@link #chatWithFilter(String, String, String)} 使用 */
	public SimpleAssistant filterableRetriever(FilterableRetriever filterableRetriever) {
		this.filterableRetriever = filterableRetriever;
		return this;
	}

	private FilterableRetriever filterableRetriever() {
		if (filterableRetriever == null) {
			throw new IllegalStateException(
					"未注入显式过滤检索回调：仅 KnowledgeAssistantFactory 构建的助手"
							+ "支持带过滤参数的 retrieve / chatWithFilter");
		}
		return filterableRetriever;
	}

	/**
	 * 只检索（带元数据过滤）：{@code metadataCondition} 为 Dify metadata_condition 格式 JSON
	 * 字符串（键须为数据集已定义的元数据字段），仅本次调用生效，与构建期固定条件叠加（AND）。
	 */
	public List<Content> retrieve(String memoryId, String metadataCondition, String message) {
		return filterableRetriever().retrieve(message, parseCondition(metadataCondition));
	}

	/**
	 * 同步对话（带元数据过滤）：检索经 {@code metadataCondition} 过滤，结果拼入上下文后由
	 * 底层对话模型直连生成（不经 AiServices 检索增强链，避免重复召回）；未配置 ChatModel 时
	 * 退回底层 {@link Assistant} 会话对话。
	 */
	public String chatWithFilter(String memoryId, String metadataCondition, String message) {
		List<Content> contents = retrieve(memoryId, metadataCondition, message);
		String prompt = filteredPrompt(contents, message);
		return chatModel != null
				? chatModel.chat(prompt)
				: requireAssistant().chat(memoryId, "", message);
	}

	/**
	 * 流式对话（带元数据过滤）：检索经 {@code metadataCondition} 过滤，结果拼入上下文后由
	 * 流式对话模型直连生成（不经 AiServices 检索增强链，避免重复召回；与会话记忆一样不经过——
	 * 直连路径与 {@link #chatWithFilter} 语义一致）。需构建时配置 StreamingChatModel。
	 * 直连流式拿不到 {@code StreamingHandle}，取消语义为停止消费（模型侧流自然结束）。
	 */
	public TokenStream streamWithFilter(String memoryId, String metadataCondition, String message) {
		if (streamingTextModel == null) {
			throw new IllegalStateException(
					"未配置流式对话模型：streamWithFilter 需构建时配置 streamingTextModel(...) / StreamingChatModel");
		}
		List<Content> contents = retrieve(memoryId, metadataCondition, message);
		return new DirectTokenStream(filteredPrompt(contents, message), contents);
	}

	/** SSE 推流（带元数据过滤）：事件链与 {@link #streamToSse(String, String)} 一致 */
	public SseEmitter streamToSseWithFilter(String memoryId, String metadataCondition, String message) {
		return streamTokenStreamToSse(streamWithFilter(memoryId, metadataCondition, message),
				null, null, null, null);
	}

	/**
	 * SSE 推流（带元数据过滤，挂钩形态）：参数语义与 {@link #streamToSse(String, String, Object,
	 * java.util.function.Function, Consumer, Consumer)} 一致（startData / doneDataMapper /
	 * onStarted / onDone 均可为 null）。
	 */
	public SseEmitter streamToSseWithFilter(String memoryId, String metadataCondition, String message,
			Object startData, java.util.function.Function<String, Object> doneDataMapper,
			Consumer<String> onStarted, Consumer<String> onDone) {
		return streamTokenStreamToSse(streamWithFilter(memoryId, metadataCondition, message),
				startData, doneDataMapper, onStarted, onDone);
	}

	/** 把检索结果拼成带参考资料的提示词（chatWithFilter / streamWithFilter 共用） */
	private static String filteredPrompt(List<Content> contents, String message) {
		StringBuilder context = new StringBuilder();
		for (Content content : contents) {
			if (context.length() > 0) {
				context.append("\n\n");
			}
			context.append(content.textSegment().text());
		}
		return context.length() > 0
				? "请根据以下参考资料回答用户问题。\n\n参考资料：\n" + context + "\n\n用户问题：" + message
				: message;
	}

	/**
	 * 直连流式 TokenStream：不经 AiServices，直接驱动 {@link StreamingChatModel} 逐段回调。
	 * 无会话记忆、无工具/思考事件（onToolExecuted / thinking 回调为 no-op）；
	 * 检索结果在 start() 时经 {@code onRetrieved} 回调（显式检索已在 streamWithFilter 完成）。
	 */
	private final class DirectTokenStream implements TokenStream {

		private final String prompt;

		private final List<Content> contents;

		private final List<Consumer<String>> partialHandlers = new ArrayList<>();

		private final List<Consumer<List<Content>>> retrievedHandlers = new ArrayList<>();

		private Consumer<dev.langchain4j.model.chat.response.ChatResponse> completeHandler;

		private Consumer<Throwable> errorHandler;

		private DirectTokenStream(String prompt, List<Content> contents) {
			this.prompt = prompt;
			this.contents = contents;
		}

		@Override
		public TokenStream onPartialResponse(Consumer<String> handler) {
			partialHandlers.add(handler);
			return this;
		}

		@Override
		public TokenStream onPartialResponseWithContext(
				java.util.function.BiConsumer<dev.langchain4j.model.chat.response.PartialResponse,
						dev.langchain4j.model.chat.response.PartialResponseContext> handler) {
			return onPartialResponse(token -> handler.accept(
					new dev.langchain4j.model.chat.response.PartialResponse(token), null));
		}

		@Override
		public TokenStream onPartialThinkingWithContext(
				java.util.function.BiConsumer<dev.langchain4j.model.chat.response.PartialThinking,
						dev.langchain4j.model.chat.response.PartialThinkingContext> handler) {
			// 直连路径无思考事件
			return this;
		}

		@Override
		public TokenStream beforeToolExecution(
				java.util.function.Consumer<dev.langchain4j.service.tool.BeforeToolExecution> handler) {
			// 直连路径无工具调用
			return this;
		}

		@Override
		public TokenStream onRetrieved(Consumer<List<Content>> handler) {
			retrievedHandlers.add(handler);
			return this;
		}

		@Override
		public TokenStream onToolExecuted(
				java.util.function.Consumer<dev.langchain4j.service.tool.ToolExecution> handler) {
			// 直连路径无工具调用
			return this;
		}

		@Override
		public TokenStream onCompleteResponse(
				Consumer<dev.langchain4j.model.chat.response.ChatResponse> handler) {
			completeHandler = handler;
			return this;
		}

		@Override
		public TokenStream onError(Consumer<Throwable> handler) {
			errorHandler = handler;
			return this;
		}

		@Override
		public TokenStream ignoreErrors() {
			return this;
		}

		@Override
		public void start() {
			// 先回调检索结果（SSE 链会发 retrieved 事件），再启动直连流式生成
			for (Consumer<List<Content>> handler : retrievedHandlers) {
				handler.accept(contents);
			}
			streamingTextModel.chat(prompt, new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
				@Override
				public void onPartialResponse(String token) {
					for (Consumer<String> handler : partialHandlers) {
						handler.accept(token);
					}
				}

				@Override
				public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
					if (completeHandler != null) {
						completeHandler.accept(response);
					}
				}

				@Override
				public void onError(Throwable throwable) {
					if (errorHandler != null) {
						errorHandler.accept(throwable);
					}
				}
			});
		}
	}

	private static com.alibaba.fastjson2.JSONObject parseCondition(String metadataCondition) {
		if (metadataCondition == null || metadataCondition.trim().isEmpty()) {
			return null;
		}
		try {
			return com.alibaba.fastjson2.JSON.parseObject(metadataCondition);
		} catch (Exception e) {
			throw new IllegalArgumentException("metadataCondition 不是合法的 JSON 对象: " + e.getMessage());
		}
	}

	/** 一次性对话：memoryId 取 {@value #DEFAULT_MEMORY_ID}，context 为空 */
	public String chat(String message) {
		return chat(DEFAULT_MEMORY_ID, message);
	}

	/** 会话对话：memoryId 自定（决定会话记忆），context 为空 */
	public String chat(String memoryId, String message) {
		return chat(memoryId, DEFAULT_CONTEXT, message);
	}

	/** 完整形态：透传给底层 {@link Assistant} */
	public String chat(String memoryId, String context, String message) {
		return requireAssistant().chat(memoryId, normalizeContext(context), message);
	}

	/** 流式：memoryId 取 {@value #DEFAULT_MEMORY_ID}，context 为空，返回原始 {@link TokenStream} */
	public TokenStream stream(String message) {
		return stream(DEFAULT_MEMORY_ID, message);
	}

	/** 流式：返回原始 {@link TokenStream}，需要自定义回调时用 */
	public TokenStream stream(String memoryId, String message) {
		return requireAssistant().stream(memoryId, DEFAULT_CONTEXT, message);
	}

	/** 流式完整形态：透传给底层 {@link Assistant}，可自行挂回调（如 {@code onRetrieved}） */
	public TokenStream stream(String memoryId, String context, String message) {
		return requireAssistant().stream(memoryId, normalizeContext(context), message);
	}

	// -------------------------------------------------- 直连对话（generate 系列，无记忆）

	/** 直连对话：用构造传入的 {@code ChatModel} 发一条用户消息并返回回复文本（无记忆） */
	public String generate(String message) {
		return chatModel().chat(UserMessage.from(message)).aiMessage().text();
	}

	/** 直连对话（带系统提示）：第一参数为系统提示 */
	public String generate(String systemPrompt, String message) {
		return chatModel().chat(Arrays.asList(SystemMessage.from(systemPrompt), UserMessage.from(message)))
				.aiMessage().text();
	}

	/** 直连对话（自定义消息序列）：用构造传入的 {@code ChatModel}，脚本里拼好上下文再发一次 */
	public String generate(List<ChatMessage> messages) {
		return chatModel().chat(messages).aiMessage().text();
	}

	// -------------------------------------------------- 文本补全（语言模型直连）

	/** 一次性文本补全：用构造传入的 {@code LanguageModel}（非对话模型）直接生成文本 */
	public String generateText(String prompt) {
		return languageModel().generate(prompt).content();
	}

	/**
	 * 流式文本补全：用构造传入的 {@code StreamingLanguageModel} 逐段回调 {@code onToken}，
	 * 阻塞等待完成后返回完整文本。
	 *
	 * @param onToken 每段文本回调，可为 null
	 */
	public String generateText(String prompt, java.util.function.Consumer<String> onToken) {
		StringBuilder full = new StringBuilder();
		java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
		streamingLanguageModel().generate(prompt, new StreamingResponseHandler<String>() {
			@Override
			public void onNext(String token) {
				full.append(token);
				if (onToken != null) {
					onToken.accept(token);
				}
			}

			@Override
			public void onComplete(Response<String> response) {
				done.countDown();
			}

			@Override
			public void onError(Throwable throwable) {
				error.set(throwable);
				done.countDown();
			}
		});
		try {
			done.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("流式文本补全被中断", e);
		}
		if (error.get() != null) {
			throw new IllegalStateException("流式文本补全失败", error.get());
		}
		return full.toString();
	}

	// -------------------------------------------------- 流式文本补全的 SSE 推流

	/**
	 * 流式文本补全的 SSE 推流：调用模型前先发 {@code start} 事件把 requestId 交给前端，
	 * 逐段推 {@code token}；推理模型思考时推 {@code thinking}（增量）与 {@code thinking-done}
	 * （本轮完整思考文本）；结束时发 {@code done} 事件（data 为完整文本，未产出时 [DONE]）并完成，
	 * 异常自动 completeWithError。SseEmitter 不设超时（0L），由生成速度决定。
	 * 客户端断开时自动取消模型流式调用。
	 */
	public SseEmitter generateTextToSse(String prompt) {
		return doGenerateTextToSse(prompt, null, null, null, null);
	}

	/** 同上（start 事件 data 由调用方提供，如自己的聊天记录ID；null 时用自动生成的 requestId） */
	public SseEmitter generateTextToSse(String prompt, Object startData) {
		return doGenerateTextToSse(prompt, startData, null, null, null);
	}

	/** 同上（经 {@code onStarted} 拿到本轮 requestId） */
	public SseEmitter generateTextToSse(String prompt, java.util.function.Consumer<String> onStarted) {
		return doGenerateTextToSse(prompt, null, null, onStarted, null);
	}

	/**
	 * 流式文本补全 SSE 推流（start/done 事件 data 均由调用方提供，均非必须）：
	 * {@code startData} 为 start 事件 data，null 时用自动生成的 requestId；
	 * {@code doneDataMapper} 收到完整回复文本（或 [DONE]），返回值作为 done 事件 data，
	 * null 时用完整回复文本——可在这里顺带保存聊天记录。
	 * 注意：与 {@code (prompt, onStarted)} 重载并存，传 null 时请显式转型避免二义性。
	 */
	public SseEmitter generateTextToSse(String prompt, Object startData,
			java.util.function.Function<String, Object> doneDataMapper) {
		return doGenerateTextToSse(prompt, startData, doneDataMapper, null, null);
	}

	/** 同上，并回调挂钩：onStarted 在 start 事件后回调（requestId），onDone 在 done 事件后回调（完整文本或 [DONE]），均可为 null */
	public SseEmitter generateTextToSse(String prompt,
			java.util.function.Consumer<String> onStarted, java.util.function.Consumer<String> onDone) {
		return doGenerateTextToSse(prompt, null, null, onStarted, onDone);
	}

	private SseEmitter doGenerateTextToSse(String prompt, Object startData,
			java.util.function.Function<String, Object> doneDataMapper,
			java.util.function.Consumer<String> onStarted, java.util.function.Consumer<String> onDone) {
		SseEmitter emitter = new SseEmitter(0L);
		String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
		// 客户端断开/超时后置位：后续 token 不再推送也不再消费。
		// StreamingLanguageModel 的流式 API 无取消句柄，模型侧只能放弃消费（HTTP 流会自然结束）
		java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
		emitter.onCompletion(() -> cancelled.set(true));
		emitter.onError(t -> cancelled.set(true));
		emitter.onTimeout(() -> cancelled.set(true));
		try {
			// 调用模型前先告知前端「已受理」；start 事件 data 默认为 requestId，调用方提供 startData 时用调用方的
			Object start = startData != null ? startData : requestId;
			emitter.send(SseEmitter.event().name(EVENT_START).id(requestId).data(start));
		} catch (Exception e) {
			emitter.completeWithError(e);
			return emitter;
		} finally {
			// finally 保证调用：即使 start 事件 send 失败（客户端已断开）也通知调用方
			if (onStarted != null) {
				onStarted.accept(requestId);
			}
		}

		streamingLanguageModel().generate(prompt, new StreamingResponseHandler<String>() {
			@Override
			public void onNext(String token) {
				if (cancelled.get()) {
					return;
				}
				sendEvent(emitter, cancelled, EVENT_TOKEN, requestId, token);
			}

			@Override
			public void onComplete(Response<String> response) {
				// done 事件带上完整回复文本：前端流式中途丢段时可兜底
				String text = response == null || response.content() == null || response.content().isEmpty()
						? DONE_FALLBACK : response.content();
				try {
					if (!cancelled.get()) {
						Object data = doneDataMapper != null ? doneDataMapper.apply(text) : text;
						emitter.send(SseEmitter.event().name(EVENT_DONE).id(requestId).data(data));
						emitter.complete();
					}
				} catch (Exception e) {
					cancelled.set(true);
					emitter.completeWithError(e);
				} finally {
					// finally 保证调用：即使客户端已断开（cancelled）也用模型完整回复补齐聊天记录
					if (onDone != null) {
						onDone.accept(text);
					}
				}
			}

			@Override
			public void onError(Throwable throwable) {
				try {
					if (!cancelled.get()) {
						// 出错也告知前端：先发 error 事件（data 为错误信息）
						emitter.send(SseEmitter.event().name(EVENT_ERROR).id(requestId)
								.data(throwable.getMessage() == null
										? throwable.getClass().getSimpleName() : throwable.getMessage()));
					}
				} catch (Exception ignored) {
					// 客户端已断开等场景：发送失败不影响按错误结束
				} finally {
					emitter.completeWithError(throwable);
				}
			}
		});
		return emitter;
	}

	/**
	 * SSE 推流（自动生成 requestId）：调用模型前先发 {@code start} 事件把 requestId 交给前端，
	 * 再逐段推 {@code token}；推理模型思考时推 {@code thinking}/{@code thinking-done}，
	 * 工具调用推 {@code tool-call}/{@code tool-result}，RAG 检索推 {@code retrieved}；
	 * 结束时发 {@code done} 事件并完成，异常自动 completeWithError；客户端断开时取消模型调用。
	 * 每个事件都带 SSE 标准 id 字段（值为本轮 requestId），前端可用 {@code lastEventId} 取到。
	 * SseEmitter 不设超时（0L），由生成速度决定。
	 */
	public SseEmitter streamToSse(String message) {
		return streamToSse(DEFAULT_MEMORY_ID, message);
	}

	public SseEmitter streamToSse(String memoryId, String message) {
		return doStreamToSse(memoryId, DEFAULT_CONTEXT, message, null, null, null, null);
	}

	/** 同上（带 context，动态系统提示定制）；第三参传 null 时需显式转型，避免与 Object/Consumer 重载二义 */
	public SseEmitter streamToSse(String memoryId, String context, String message) {
		return doStreamToSse(memoryId, normalizeContext(context), message, null, null, null, null);
	}

	/** 同上（start 事件 data 由调用方提供，如自己的聊天记录ID；null 时用自动生成的 requestId） */
	public SseEmitter streamToSse(String memoryId, String message, Object startData) {
		return doStreamToSse(memoryId, DEFAULT_CONTEXT, message, startData, null, null, null);
	}

	/** 同上（经 {@code onStarted} 拿到本轮 requestId） */
	public SseEmitter streamToSse(String memoryId, String message, Consumer<String> onStarted) {
		return doStreamToSse(memoryId, DEFAULT_CONTEXT, message, null, null, onStarted, null);
	}

	/**
	 * SSE 推流（start/done 事件 data 均由调用方提供，均非必须）：
	 * {@code startData} 为 start 事件 data，null 时用自动生成的 requestId；
	 * {@code doneDataMapper} 收到完整回复文本（或 [DONE]），返回值作为 done 事件 data，
	 * null 时用完整回复文本——可在这里顺带保存聊天记录。
	 * 注意：与 {@code (memoryId, message, onStarted)} 重载并存，传 null 时请显式转型避免二义性。
	 */
	public SseEmitter streamToSse(String memoryId, String message,
			Object startData, java.util.function.Function<String, Object> doneDataMapper) {
		return doStreamToSse(memoryId, DEFAULT_CONTEXT, message, startData, doneDataMapper, null, null);
	}

	/**
	 * SSE 推流（挂钩形态）：onStarted 在 start 事件后回调（入参为本轮 requestId），
	 * onDone 在 done 事件后回调（入参为完整回复文本或 [DONE]，finally 保证调用，可用于保存聊天记录）；
	 * 均可为 null。start/done 事件 data 用默认值（requestId / 完整回复文本）。
	 */
	public SseEmitter streamToSse(String memoryId, String message,
			Consumer<String> onStarted, Consumer<String> onDone) {
		return doStreamToSse(memoryId, DEFAULT_CONTEXT, message, null, null, onStarted, onDone);
	}

	private SseEmitter doStreamToSse(String memoryId, String context, String message, Object startData,
			java.util.function.Function<String, Object> doneDataMapper,
			Consumer<String> onStarted, Consumer<String> onDone) {
		return streamTokenStreamToSse(stream(memoryId, context, message),
				startData, doneDataMapper, onStarted, onDone);
	}

	/**
	 * 将给定 {@link TokenStream} 以 SSE 推流：start/token/thinking/tool-call/tool-result/retrieved/
	 * done/error 事件链与客户端断开取消逻辑与常规流式完全一致，
	 * 供常规流式与带过滤直连流式（{@link #streamToSseWithFilter}）共用。
	 */
	private SseEmitter streamTokenStreamToSse(TokenStream tokenStream, Object startData,
			java.util.function.Function<String, Object> doneDataMapper,
			Consumer<String> onStarted, Consumer<String> onDone) {
		SseEmitter emitter = new SseEmitter(0L);
		String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
		// 客户端断开/超时后置位：后续事件不再推送；捕获到 StreamingHandle 后同时取消模型流式调用
		// （langchain4j 1.20.2 起 TokenStream 的 WithContext 回调可拿到 StreamingHandle）
		java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
		AtomicReference<StreamingHandle> handleRef = new AtomicReference<>();
		Runnable cancelStreaming = () -> {
			StreamingHandle handle = handleRef.get();
			if (handle != null && !handle.isCancelled()) {
				handle.cancel();
			}
		};
		emitter.onCompletion(() -> {
			cancelled.set(true);
			cancelStreaming.run();
		});
		emitter.onError(t -> {
			cancelled.set(true);
			cancelStreaming.run();
		});
		emitter.onTimeout(() -> {
			cancelled.set(true);
			cancelStreaming.run();
		});
		ThinkingSseTracker thinking = new ThinkingSseTracker();
		try {
			// 调用模型前先告知前端「已受理」；start 事件 data 默认为 requestId，调用方提供 startData 时用调用方的
			Object start = startData != null ? startData : requestId;
			emitter.send(SseEmitter.event().name(EVENT_START).id(requestId).data(start));
		} catch (Exception e) {
			emitter.completeWithError(e);
			return emitter;
		} finally {
			// finally 保证调用：即使 start 事件 send 失败（客户端已断开）也通知调用方
			if (onStarted != null) {
				onStarted.accept(requestId);
			}
		}

		tokenStream
				.onPartialThinkingWithContext((partial, ctx) -> {
				captureHandle(handleRef, ctx == null ? null : ctx.streamingHandle(), cancelled);
					if (!cancelled.get()) {
						thinking.delta(partial.text(), emitter, cancelled, requestId);
					}
				})
				.onPartialResponseWithContext((partial, ctx) -> {
				captureHandle(handleRef, ctx == null ? null : ctx.streamingHandle(), cancelled);
					if (!cancelled.get()) {
						// 从思考转入回答：先结束思考轮
						thinking.finish(emitter, cancelled, requestId);
						sendEvent(emitter, cancelled, EVENT_TOKEN, requestId, partial.text());
					}
				})
				.onRetrieved(contents -> {
					if (cancelled.get() || contents == null || contents.isEmpty()) {
						return;
					}
					List<String> texts = contents.stream()
							.map(content -> content.textSegment().text())
							.collect(Collectors.toList());
					sendEvent(emitter, cancelled, EVENT_RETRIEVED, requestId, toJson(texts));
				})
				.beforeToolExecution(before -> {
					if (!cancelled.get()) {
						// 思考后发起工具调用：先结束思考轮
						thinking.finish(emitter, cancelled, requestId);
						sendEvent(emitter, cancelled, EVENT_TOOL_CALL, requestId,
								toJson(toolCallData(before.request(), null)));
					}
				})
				.onToolExecuted(execution -> {
					if (!cancelled.get()) {
						sendEvent(emitter, cancelled, EVENT_TOOL_RESULT, requestId,
								toJson(toolCallData(execution.request(), execution.result())));
					}
				})
				.onCompleteResponse(response -> {
					// done 事件带上完整回复文本：前端流式中途丢段时可兜底
					String text = doneData(response.aiMessage());
					try {
						if (!cancelled.get()) {
							Object data = doneDataMapper != null ? doneDataMapper.apply(text) : text;
							emitter.send(SseEmitter.event().name(EVENT_DONE).id(requestId).data(data));
							emitter.complete();
						}
					} catch (Exception e) {
						cancelled.set(true);
						emitter.completeWithError(e);
					} finally {
						// finally 保证调用：即使客户端已断开（cancelled）也用模型完整回复补齐聊天记录
						if (onDone != null) {
							onDone.accept(text);
						}
					}
				})
				.onError(throwable -> {
					try {
						if (!cancelled.get()) {
							// 出错也告知前端：先发 error 事件（data 为错误信息）
							emitter.send(SseEmitter.event().name(EVENT_ERROR).id(requestId)
									.data(throwable.getMessage() == null
											? throwable.getClass().getSimpleName() : throwable.getMessage()));
						}
					} catch (Exception ignored) {
						// 客户端已断开等场景：发送失败不影响按错误结束
					} finally {
						emitter.completeWithError(throwable);
					}
				})
				.start();
		return emitter;
	}

	/**
	 * done 事件的数据：完整回复文本；模型没有产出文本（如纯工具调用轮、response 为空）时
	 * 退化为 {@code [DONE]} 标记。
	 */
	static String doneData(AiMessage response) {
		if (response == null || response.text() == null || response.text().isEmpty()) {
			return DONE_FALLBACK;
		}
		return response.text();
	}

	// -------------------------------------------------- SSE 事件辅助

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	/** 序列化为 JSON 字符串（SSE data 统一发文本）；失败时退化为 toString */
	static String toJson(Object value) {
		try {
			return JSON_MAPPER.writeValueAsString(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	/**
	 * 发送 SSE 事件：cancelled 时跳过；发送失败置位 cancelled 并 completeWithError。
	 */
	private static void sendEvent(SseEmitter emitter, java.util.concurrent.atomic.AtomicBoolean cancelled,
			String event, String requestId, Object data) {
		if (cancelled.get()) {
			return;
		}
		try {
			emitter.send(SseEmitter.event().name(event).id(requestId).data(data));
		} catch (Exception e) {
			cancelled.set(true);
			emitter.completeWithError(e);
		}
	}

	/** 捕获流式句柄供断开时取消：客户端已断开则立即取消 */
	private static void captureHandle(AtomicReference<StreamingHandle> handleRef, StreamingHandle handle,
			java.util.concurrent.atomic.AtomicBoolean cancelled) {
		if (handle == null) {
			return;
		}
		handleRef.set(handle);
		if (cancelled.get()) {
			handle.cancel();
		}
	}

	/** 工具事件 data：name、arguments，执行完成后附 result */
	static Map<String, Object> toolCallData(ToolExecutionRequest request, String result) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("name", request.name());
		data.put("arguments", request.arguments());
		if (result != null) {
			data.put("result", result);
		}
		return data;
	}

	/**
	 * 思考事件跟踪：首个思考增量开始新一轮并发送 thinking（增量）；
	 * 思考转入回答或工具调用时发送 thinking-done（data 为本轮完整思考文本）。
	 */
	private static final class ThinkingSseTracker {

		private final StringBuilder text = new StringBuilder();

		private boolean active;

		/** 收到思考增量：必要时开始新一轮，随后发送 thinking 事件 */
		void delta(String delta, SseEmitter emitter, java.util.concurrent.atomic.AtomicBoolean cancelled,
				String requestId) {
			if (!active) {
				text.setLength(0);
				active = true;
			}
			text.append(delta);
			sendEvent(emitter, cancelled, EVENT_THINKING, requestId, delta);
		}

		/** 结束当前思考轮并发送 thinking-done；无进行中的思考轮时忽略 */
		void finish(SseEmitter emitter, java.util.concurrent.atomic.AtomicBoolean cancelled, String requestId) {
			if (!active) {
				return;
			}
			active = false;
			sendEvent(emitter, cancelled, EVENT_THINKING_DONE, requestId, text.toString());
		}
	}

	// -------------------------------------------------- 内部取用：模型经构造方法传入

	/** context 为空（null 或纯空白）时归一化为 {@value #DEFAULT_CONTEXT}，避免 null/空白进入系统提示模板。 */
	private static String normalizeContext(String context) {
		return context == null || context.trim().isEmpty() ? DEFAULT_CONTEXT : context;
	}

	private Assistant requireAssistant() {
		if (assistant == null) {
			throw new IllegalStateException("未配置 Assistant：请用含 Assistant 参数的构造方法构建");
		}
		return assistant;
	}

	private ChatModel chatModel() {
		if (chatModel == null) {
			throw new IllegalStateException("未配置 ChatModel：请用构造方法传入");
		}
		return chatModel;
	}

	private StreamingChatModel streamingTextModel() {
		if (streamingTextModel == null) {
			throw new IllegalStateException("未配置 StreamingChatModel：请用构造方法传入");
		}
		return streamingTextModel;
	}

	private LanguageModel languageModel() {
		if (languageModel == null) {
			throw new IllegalStateException("未配置 LanguageModel：请用构造方法传入");
		}
		return languageModel;
	}

	private StreamingLanguageModel streamingLanguageModel() {
		if (streamingLanguageModel == null) {
			throw new IllegalStateException("未配置 StreamingLanguageModel：请用构造方法传入");
		}
		return streamingLanguageModel;
	}
}
