package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryIds;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 会话上下文请求变换器：挂在 AiServices 的 {@code chatRequestTransformer} 上（请求装配后、
 * 模型调用前），把 {@link ContextInjector} 渲染的统一上下文作为前置系统消息注入；无内容时不改变请求。
 *
 * <p>助手（{@code AssistantBuilder}）与 agentic（{@code AgenticBuilder}）共用本实现，保证注入口径一致：
 * 本轮用户消息已在请求里（取最后一条 {@link UserMessage} 的文本，按 {@link EphemeralChatMemory}
 * 同一分隔符口径剥离 RAG 检索段，保证来源拿到的是用户原话）；会话键（memoryId）与 userId（段[3]）
 * 由 {@link ChatMemoryIds} 解析，与记忆读侧（{@link ContextAwareChatMemory} 的 requestFactory）同源。</p>
 */
public class EnrichedContextTransformer implements BiFunction<ChatRequest, Object, ChatRequest> {

	private final ContextInjector injector;

	private final List<String> separators;

	/**
	 * @param contextProviders 有序上下文来源
	 * @param contextStrategy  上下文组合策略（{@link ContextStrategy#MEMORY} 时不注入）
	 * @param separators       RAG 检索段分隔符（与 {@link EphemeralChatMemory} 同口径）；null 表示不剥离
	 */
	public EnrichedContextTransformer(List<ContextProvider> contextProviders, ContextStrategy contextStrategy,
			List<String> separators) {
		this.injector = new ContextInjector(contextProviders, contextStrategy);
		this.separators = separators == null
				? Collections.<String>emptyList()
				: new ArrayList<String>(separators);
	}

	@Override
	public ChatRequest apply(ChatRequest request, Object memoryId) {
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

}
