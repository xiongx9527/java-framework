package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 让会话记忆只沉淀「系统提示 + 用户原话」的 {@link ChatMemory} 装饰器。
 *
 * <p>三类数据只属于本轮，不该留在历史里：</p>
 * <ul>
 *   <li><b>RAG 检索内容</b>——按当前 query 临时检索，与后续问题无关，却每轮重复计费。
 *       主防线在装配侧：{@code AssistantBuilder} 把 {@code storeRetrievedContentInChatMemory}
 *       置为 {@code false}，框架写进记忆的就是用户原话；</li>
 *   <li><b>工具往返</b>——{@code AiMessage.toolExecutionRequests} 与
 *       {@code ToolExecutionResultMessage} 是同一次调用内的过程数据；</li>
 *   <li><b>AI 回复</b>——模型的最终回答只是本轮输出。留在历史里会让模型凭上一轮的结论直接作答、
 *       不再重新调用工具。</li>
 * </ul>
 *
 * <p><b>框架在两个时刻读取记忆，两者只能靠 store 尾部形态区分</b>
 * （{@code DefaultAiServices.assembleMessages} 与
 * {@code ToolService.persistToolResultsAndResolveMessagesSync} 都调 {@code messages()}）：</p>
 * <ol>
 *   <li><b>新一轮的第一个请求</b>——此时上一轮已经收尾，store 尾部是上一轮的过程数据。
 *       这些内容必须剔除，否则模型看到现成的工具结果就直接作答，本轮不再调用工具。
 *       注意此时框架尚未写入本轮用户消息（{@code assembleMessages} 先取历史、后写用户消息），
 *       轮次清理只能靠 {@link #messages()} 自己判断；</li>
 *   <li><b>工具续调请求</b>——此时本轮尚未收尾，store 尾部是「工具请求 + 工具结果」，
 *       必须原样带出，模型才知道结果是什么。</li>
 * </ol>
 *
 * <p><b>不持有任何实例状态</b>：框架对同一 memoryId 只调用一次 {@code ChatMemoryProvider.get}
 * （{@code AiServiceContext.chatMemories} 的 {@code computeIfAbsent}），实例在全进程复用，
 * 挂在实例上的暂存会跨轮、跨并发请求串台。所以轮次边界只能从 store 内容推断：
 * 最后一条用户消息之后若出现<b>不含工具请求的 AI 回复</b>，即视为本轮已收尾。
 * 工具请求与工具结果必须成对出现才算「进行中」——工具抛错提前中断时不会留下半截往返。</p>
 *
 * <p><b>RAG 靠锚点而非类型</b>：injector 把检索内容拼在 user message 尾部，框架随后用
 * {@code UserMessage.from(text)} 重建消息，类型到不了这里；但文本里的分隔串仍在。
 * {@link #RAG_SEPARATOR} 与框架默认 injector 的默认模板分隔段一致，作为兜底把历史用户消息里的
 * 检索段剥掉。锚点失配时整体透传，不报错。</p>
 */
public class EphemeralChatMemory implements ChatMemory {

	/**
	 * 检索内容注入 user message 的分隔串。
	 *
	 * <p>值与框架默认 injector（{@code DefaultContentInjector} 的
	 * {@code DEFAULT_PROMPT_TEMPLATE}）的分隔段一致。装配侧已把
	 * {@code storeRetrievedContentInChatMemory} 置为 {@code false}，正常链路里记忆中的用户消息
	 * 不含检索段，这里只是兜底：上游改默认模板或记忆里已有增强过的消息时仍能剥掉。</p>
	 */
	public static final String RAG_SEPARATOR = "\n\nAnswer using the following information:\n";

	private final ChatMemory delegate;
	private final ChatMemoryStore store;

	public EphemeralChatMemory(ChatMemory delegate, ChatMemoryStore store) {
		this.delegate = delegate;
		this.store = store;
	}

	@Override
	public Object id() {
		return delegate.id();
	}

	@Override
	public void add(ChatMessage message) {
		if (message.type() == ChatMessageType.USER) {
			// 用户消息标记一轮的开始：清掉上一轮的过程数据，只留系统提示与历史原话。
			// 必须在 delegate.add 之前做，之后 delegate 会基于清理过的 store 追加
			store.updateMessages(id(), durableOnly(store.getMessages(id())));
		}
		delegate.add(message);
	}

	@Override
	public List<ChatMessage> messages() {
		List<ChatMessage> stored = delegate.messages();

		int lastUserIndex = lastUserIndex(stored);
		boolean roundUnfinished = roundUnfinished(stored, lastUserIndex);

		List<ChatMessage> sendable = new ArrayList<>(stored.size());
		for (int i = 0; i < stored.size(); i++) {
			ChatMessage message = stored.get(i);
			ChatMessageType type = message.type();
			if (type == ChatMessageType.SYSTEM) {
				sendable.add(message);
			} else if (type == ChatMessageType.USER) {
				// 用户消息只留原话；本轮增强后的版本由框架接到请求末尾，这里不重复输出
				sendable.add(stripRag((UserMessage) message));
			} else if (roundUnfinished && i > lastUserIndex) {
				// 工具往返属于本轮，原样带出。不含工具请求的 AI 消息是上一轮的最终回答，只是输出
				sendable.add(message);
			}
		}
		return sendable;
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	/** 最后一条用户消息的下标，没有则 -1。 */
	private static int lastUserIndex(List<ChatMessage> messages) {
		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i).type() == ChatMessageType.USER) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 最后一条用户消息之后，本轮是否仍未收尾（即当前处于工具续调中）。
	 *
	 * <p>判据：出现<b>不含工具请求的 AI 回复</b>即收尾；工具请求与工具结果成对出现且没有最终
	 * 回复，才算进行中。中断在半截（工具抛错、请求数与结果数对不上）时按收尾处理，避免把
	 * 残缺往返发给模型。</p>
	 */
	private static boolean roundUnfinished(List<ChatMessage> messages, int lastUserIndex) {
		boolean seenToolRequest = false;
		int pendingResults = 0;
		for (int i = lastUserIndex + 1; i < messages.size(); i++) {
			ChatMessage message = messages.get(i);
			if (message.type() == ChatMessageType.AI) {
				AiMessage aiMessage = (AiMessage) message;
				if (!aiMessage.hasToolExecutionRequests()) {
					return false;
				}
				seenToolRequest = true;
				pendingResults += aiMessage.toolExecutionRequests().size();
			} else if (message.type() == ChatMessageType.TOOL_EXECUTION_RESULT) {
				pendingResults--;
			}
		}
		return seenToolRequest && pendingResults <= 0;
	}

	/** 只留能沉淀为历史的消息：系统提示，以及剥掉检索段的用户原话。 */
	private static List<ChatMessage> durableOnly(List<ChatMessage> messages) {
		List<ChatMessage> kept = new ArrayList<>(messages.size());
		for (ChatMessage message : messages) {
			if (message.type() == ChatMessageType.SYSTEM) {
				kept.add(message);
			} else if (message.type() == ChatMessageType.USER) {
				kept.add(stripRag((UserMessage) message));
			}
		}
		return kept;
	}

	/** 去掉 user message 尾部由 retrievalAugmentor 拼入的检索段。 */
	private static UserMessage stripRag(UserMessage message) {
		int anchor = message.singleText().indexOf(RAG_SEPARATOR);
		return anchor < 0 ? message : UserMessage.from(message.singleText().substring(0, anchor));
	}
}