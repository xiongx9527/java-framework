package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.TextFileContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.service.TokenStream;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link Assistant} 的多模态桥接实现（外层），与内层 {@link TextAssistant} 组成双层结构：
 *
 * <ul>
 *   <li><b>纯文本轮</b>（contents 全为 {@link TextContent}）：拆出文本后原样转发内层，
 *       走 LangChain4j 原生完整管线——{@code {{context}}} 模板渲染、工具循环
 *       （含 {@link GuardedToolExecutor}）、RAG、会话记忆，行为与改造前逐字节一致；</li>
 *   <li><b>多模态轮</b>（含图片/文档等内容）：自拼 {@code UserMessage.from(contents)} 直调模型。
 *       RAG 照走（query 取文本部分，检索结果以附加 TextContent 注入），会话记忆写
 *       <b>文本占位</b>（{@code [图片]} / {@code [文档:x.pdf]} + 文本）而非原文，
 *       不挂工具（多模态轮不进工具循环）。</li>
 * </ul>
 *
 * <p>{@code context} 参数为 {@link Object}：{@code String} 原样当一句话背景，
 * {@code Map} 经 fastjson2 序列化为 JSON，{@code null} 视为无背景；其他类型抛
 * {@link IllegalArgumentException}。两种形态最终统一为一段文本——纯文本轮随
 * {@code {{context}}} 模板语义进入（无占位则忽略，与历史行为一致），
 * 多模态轮以 TextContent 附加在消息头部。</p>
 *
 * <p>包内可见：由 {@link AssistantBuilder} 装配，业务侧经 {@link Assistant} 接口引用调用。</p>
 */
class MultimodalAssistantBridge implements Assistant {

	private final TextAssistant textAssistant;

	private final ChatLanguageModel chatModel;

	private final StreamingChatLanguageModel streamingModel;

	private final ChatMemoryProvider memoryProvider;

	private final Function<Object, String> systemMessageProvider;

	private final RetrievalAugmentor retrievalAugmentor;

	MultimodalAssistantBridge(TextAssistant textAssistant, ChatLanguageModel chatModel,
			StreamingChatLanguageModel streamingModel, ChatMemoryProvider memoryProvider,
			Function<Object, String> systemMessageProvider, RetrievalAugmentor retrievalAugmentor) {
		this.textAssistant = textAssistant;
		this.chatModel = chatModel;
		this.streamingModel = streamingModel;
		this.memoryProvider = memoryProvider;
		this.systemMessageProvider = systemMessageProvider;
		this.retrievalAugmentor = retrievalAugmentor;
	}

	@Override
	public String chat(String memoryId, Object context, List<Content> contents) {
		String ctx = toContextText(context);
		List<Content> media = checkedContents(contents);
		if (isAllText(media)) {
			return textAssistant.chat(memoryId, ctx, joinTexts(media));
		}
		requireChatModel();
		Turn turn = prepareTurn(memoryId, ctx, media);
		turn.memory.add(turn.placeholder);
		Response<AiMessage> response = chatModel.generate(turn.messages);
		turn.memory.add(response.content());
		return response.content().text();
	}

	@Override
	public TokenStream stream(String memoryId, Object context, List<Content> contents) {
		String ctx = toContextText(context);
		List<Content> media = checkedContents(contents);
		if (isAllText(media)) {
			return textAssistant.stream(memoryId, ctx, joinTexts(media));
		}
		requireStreamingModel();
		Turn turn = prepareTurn(memoryId, ctx, media);
		turn.memory.add(turn.placeholder);
		return new MediaTokenStream(turn);
	}

	// -------------------------------------------------- 多模态轮：拼消息 + 记忆

	/**
	 * 多模态轮准备：系统提示入记忆（对齐原生每轮 add，{@code MessageWindowChatMemory}
	 * 自带单系统消息去重/替换）→ 取历史 → RAG 检索 → 拼本轮完整多模态用户消息。
	 * 记忆里<b>不</b>放完整消息——占位文本由调用方在生成前写入。
	 */
	private Turn prepareTurn(String memoryId, String ctx, List<Content> contents) {
		ChatMemory memory = memoryProvider.get(memoryId);

		String systemText = systemMessageProvider == null ? null : systemMessageProvider.apply(memoryId);
		if (systemText != null && !systemText.trim().isEmpty()) {
			memory.add(SystemMessage.from(systemText));
		}
		List<ChatMessage> messages = new ArrayList<>(memory.messages());

		List<Content> userContents = new ArrayList<>();
		if (ctx != null && !ctx.trim().isEmpty()) {
			userContents.add(TextContent.from(ctx));
		}

		List<dev.langchain4j.rag.content.Content> retrieved = retrieve(memoryId, contents, memory, userContents);

		userContents.addAll(contents);
		messages.add(UserMessage.from(userContents));

		return new Turn(memory, messages, UserMessage.from(placeholderText(contents)), retrieved);
	}

	/**
	 * RAG（多模态轮）：query 取 contents 的文本部分，检索结果拍平成一段参考资料，
	 * 以 TextContent 插在 context 与用户内容之间；未配置检索或无文本时跳过。
	 *
	 * @return 命中的检索内容（供流式 {@code onRetrieved} 回调），无则空列表
	 */
	private List<dev.langchain4j.rag.content.Content> retrieve(String memoryId, List<Content> contents,
			ChatMemory memory, List<Content> userContents) {
		if (retrievalAugmentor == null) {
			return Collections.emptyList();
		}
		String query = joinTexts(contents);
		if (query.trim().isEmpty()) {
			return Collections.emptyList();
		}
		UserMessage queryMessage = UserMessage.from(query);
		AugmentationResult result = retrievalAugmentor.augment(new AugmentationRequest(queryMessage,
				new Metadata(queryMessage, memoryId, memory.messages())));
		List<dev.langchain4j.rag.content.Content> retrieved =
				result.contents() == null ? Collections.<dev.langchain4j.rag.content.Content>emptyList() : result.contents();
		StringBuilder ragText = new StringBuilder();
		for (dev.langchain4j.rag.content.Content content : retrieved) {
			if (content == null || content.textSegment() == null) {
				continue;
			}
			if (ragText.length() > 0) {
				ragText.append("\n\n");
			}
			ragText.append(content.textSegment().text());
		}
		if (ragText.length() > 0) {
			userContents.add(TextContent.from(ragText.toString()));
		}
		return retrieved;
	}

	// -------------------------------------------------- 参数归一化与内容工具

	/** {@code context} 归一化：null→空串、String→原样、Map→fastjson2 JSON，其余类型拒绝 */
	static String toContextText(Object context) {
		if (context == null) {
			return "";
		}
		if (context instanceof String) {
			return (String) context;
		}
		if (context instanceof Map) {
			return JSON.toJSONString(context);
		}
		throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
				"fw.ai.MultimodalAssistantBridge.contextTypeUnsupported", context.getClass().getName()));
	}

	/** contents 校验：null 或空列表视为调用错误 */
	private static List<Content> checkedContents(List<Content> contents) {
		if (contents == null || contents.isEmpty()) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.MultimodalAssistantBridge.contentsRequired"));
		}
		return contents;
	}

	private static boolean isAllText(List<Content> contents) {
		for (Content content : contents) {
			if (!(content instanceof TextContent)) {
				return false;
			}
		}
		return true;
	}

	/** 取全部 TextContent 的文本，按换行拼接（多条文本与单条在模型侧等价） */
	private static String joinTexts(List<Content> contents) {
		StringBuilder text = new StringBuilder();
		for (Content content : contents) {
			if (!(content instanceof TextContent)) {
				continue;
			}
			if (text.length() > 0) {
				text.append('\n');
			}
			text.append(((TextContent) content).text());
		}
		return text.toString();
	}

	/**
	 * 记忆占位文本：文本原样保留，媒体内容压成占位标记——历史可读且不存原文
	 * （图片 base64 / 文档 base64 一律不进记忆，store 零改动）。
	 */
	static String placeholderText(List<Content> contents) {
		StringBuilder placeholder = new StringBuilder();
		for (Content content : contents) {
			if (placeholder.length() > 0) {
				placeholder.append('\n');
			}
			placeholder.append(placeholderOf(content));
		}
		return placeholder.toString();
	}

	private static String placeholderOf(Content content) {
		if (content instanceof TextContent) {
			return ((TextContent) content).text();
		}
		if (content instanceof ImageContent) {
			return "[图片]";
		}
		if (content instanceof PdfFileContent) {
			return filePlaceholder("PDF", ((PdfFileContent) content).pdfFile() == null
					? null : ((PdfFileContent) content).pdfFile().url());
		}
		if (content instanceof TextFileContent) {
			return filePlaceholder("文档", ((TextFileContent) content).textFile() == null
					? null : ((TextFileContent) content).textFile().url());
		}
		ContentType type = content.type();
		if (type == ContentType.AUDIO) {
			return "[音频]";
		}
		if (type == ContentType.VIDEO) {
			return "[视频]";
		}
		return "[附件]";
	}

	/** 文件占位：能从 URL 取到文件名则带名（如 {@code [PDF:report.pdf]}），否则只留类型 */
	private static String filePlaceholder(String tag, URI url) {
		if (url != null) {
			String path = url.getPath();
			if (path != null && !path.isEmpty()) {
				int slash = path.lastIndexOf('/');
				String name = slash >= 0 ? path.substring(slash + 1) : path;
				if (!name.isEmpty()) {
					return "[" + tag + ":" + name + "]";
				}
			}
		}
		return "[" + tag + "]";
	}

	private void requireChatModel() {
		if (chatModel == null) {
			throw new IllegalStateException(
					I18nMessageResolver.resolveKey("fw.ai.MultimodalAssistantBridge.chatModelNotConfigured"));
		}
	}

	private void requireStreamingModel() {
		if (streamingModel == null) {
			throw new IllegalStateException(
					I18nMessageResolver.resolveKey("fw.ai.MultimodalAssistantBridge.streamingModelNotConfigured"));
		}
	}

	// -------------------------------------------------- 多模态轮的流式实现

	/** 一轮多模态请求的现场：发给模型的消息、记忆引用、写回占位、检索结果 */
	private static final class Turn {

		private final ChatMemory memory;

		private final List<ChatMessage> messages;

		private final UserMessage placeholder;

		private final List<dev.langchain4j.rag.content.Content> retrieved;

		private Turn(ChatMemory memory, List<ChatMessage> messages, UserMessage placeholder,
				List<dev.langchain4j.rag.content.Content> retrieved) {
			this.memory = memory;
			this.messages = messages;
			this.placeholder = placeholder;
			this.retrieved = retrieved;
		}
	}

	/**
	 * 多模态轮直连流式：不经 AiServices，驱动 {@link StreamingChatLanguageModel} 逐段回调；
	 * 检索结果在 start() 时经 {@code onRetrieved} 回调，完整回复写回记忆（占位已先写入）。
	 * 结构与 {@link SimpleAssistant.DirectTokenStream} 对齐。
	 */
	private final class MediaTokenStream implements TokenStream {

		private final Turn turn;

		private final List<Consumer<String>> nextHandlers = new ArrayList<>();

		private final List<Consumer<List<dev.langchain4j.rag.content.Content>>> retrievedHandlers = new ArrayList<>();

		private Consumer<Response<AiMessage>> completeHandler;

		private Consumer<Throwable> errorHandler;

		private MediaTokenStream(Turn turn) {
			this.turn = turn;
		}

		@Override
		public TokenStream onNext(Consumer<String> handler) {
			nextHandlers.add(handler);
			return this;
		}

		@Override
		public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> handler) {
			retrievedHandlers.add(handler);
			return this;
		}

		@Override
		public TokenStream onComplete(Consumer<Response<AiMessage>> handler) {
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
			for (Consumer<List<dev.langchain4j.rag.content.Content>> handler : retrievedHandlers) {
				handler.accept(turn.retrieved);
			}
			streamingModel.generate(turn.messages, new StreamingResponseHandler<AiMessage>() {
				@Override
				public void onNext(String token) {
					for (Consumer<String> handler : nextHandlers) {
						handler.accept(token);
					}
				}

				@Override
				public void onComplete(Response<AiMessage> response) {
					turn.memory.add(response.content());
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
}
