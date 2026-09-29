package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

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
 *   <li><b>RAG 检索内容</b>——框架的 injector 把它拼在 user message 尾部。它按当前 query 临时检索，
 *       与后续问题无关，却每轮重复计费；</li>
 *   <li><b>工具往返</b>——{@code AiMessage.toolExecutionRequests} 与
 *       {@code ToolExecutionResultMessage} 是同一次调用内的过程数据，框架无条件写入；</li>
 *   <li><b>AI 回复</b>——模型的最终回答只是本轮输出。留在历史里会让模型凭上一轮的结论直接作答、
 *       不再重新调用工具。</li>
 * </ul>
 *
 * <p><b>不持有任何实例状态</b>：框架对同一 memoryId 只调用一次 {@code ChatMemoryProvider.get}
 * （{@code AiServiceContext.chatMemories} 的 {@code computeIfAbsent}），实例在全进程复用，
 * 挂在实例上的暂存会跨轮、跨并发请求串台。所以这里只用两个无状态动作：</p>
 * <ol>
 *   <li>{@link #add} 收到本轮的<b>用户消息</b>——框架给出的轮次边界——时，先把 store 里上一轮的
 *       AI 回复与工具往返清掉，并剥掉历史用户消息的检索段；</li>
 *   <li>{@link #messages()} 以最后一条用户消息为界：它本身（带检索段）与其后的工具往返属于本轮，
 *       原样带出；本轮的最终回答与更早的历史都不进请求——更早的部分只保留系统提示与用户原话。</li>
 * </ol>
 *
 * <p><b>RAG 靠锚点而非类型</b>：injector 把检索内容拼在 user message 尾部，框架随后用
 * {@code UserMessage.from(text)} 重建消息，类型到不了这里；但文本里的分隔串仍在。
 * {@link #RAG_SEPARATOR} 与框架默认 injector 的默认模板分隔段一致。锚点失配时整体透传
 * （退化为「RAG 也跟着进记忆」），不报错。</p>
 */
public class EphemeralChatMemory implements ChatMemory {

	/**
	 * 检索内容注入 user message 的分隔串。
	 *
	 * <p>值与框架默认 injector（{@code DefaultContentInjector} 的
	 * {@code DEFAULT_PROMPT_TEMPLATE}）的分隔段一致：RAG 走默认 injector 时，
	 * 注入结果就是「用户原话 + 本分隔串 + 检索内容」。上游若改默认模板，这里需同步，
	 * 否则锚点失配、检索段重新流入记忆。</p>
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

		int lastUserIndex = -1;
		for (int i = stored.size() - 1; i >= 0; i--) {
			if (stored.get(i).type() == ChatMessageType.USER) {
				lastUserIndex = i;
				break;
			}
		}

		List<ChatMessage> sendable = new ArrayList<>(stored.size());
		for (int i = 0; i < stored.size(); i++) {
			ChatMessage message = stored.get(i);
			if (i >= lastUserIndex) {
				// 本轮的用户提问（带检索段）与工具往返：原样带出。
				// 不含工具请求的 AI 消息是本轮的最终回答，只是输出，一并剔除
				if (message.type() != ChatMessageType.AI
						|| ((AiMessage) message).hasToolExecutionRequests()) {
					sendable.add(message);
				}
			} else if (message.type() == ChatMessageType.SYSTEM) {
				sendable.add(message);
			} else if (message.type() == ChatMessageType.USER) {
				// 历史用户消息：检索段剥掉，只留原话
				sendable.add(stripRag((UserMessage) message));
			}
			// 历史里的 AI 回复与工具往返：丢弃
		}
		return sendable;
	}

	@Override
	public void clear() {
		delegate.clear();
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
