package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 消息持久化策略（Spring AI 版）——与 LangChain4j 链路的框架
 * {@code AiPersistenceStrategy} 同名同语义，消息类型换为 Spring AI 的 {@link Message}。
 *
 * <p>设计约定（与框架版一致）：</p>
 * <ul>
 *   <li>持久化时自动过滤「无有效字符」的 AssistantMessage（{@link #hasEffectiveText} 为 false）；</li>
 *   <li>默认不持久化工具往返（带 toolCalls 的 AssistantMessage 与 {@link ToolResponseMessage}）；</li>
 *   <li>持久化发生在新一轮开始（{@code EphemeralChatMemory} 收到新的 UserMessage）时，
 *       同一策略也用于读取时回放「最后一轮输出」，保证 prompt 视图与持久视图一致。</li>
 * </ul>
 *
 * <p><b>与框架版的差异</b>：框架版可按消息粒度指定插入位置（HEAD/MIDDLE/TAIL 等
 * {@code PersistedAiItem}）；Spring AI 版简化为「选中项按原序追加在历史用户消息之后」
 * ——Spring AI 的记忆里没有 SystemMessage（系统提示每轮由服务层直接携带），无复杂排版需求。</p>
 */
public interface AiPersistenceStrategy {

	/**
	 * 从「最后一轮输出」（最后一条 UserMessage 之后的消息）中筛选需要持久化/回放的消息。
	 *
	 * @param previousTurnMessages 最后一条 UserMessage 之后的消息（AI 与工具往返）
	 * @return 需要保留的消息，按原序返回
	 */
	List<Message> selectForPersistence(List<Message> previousTurnMessages);

	// ==================== 有效字符判断 ====================

	/** AI 回复去除所有空白后仍有内容才算有效（空回复不沉淀，避免把空白帧带进下一轮）。 */
	static boolean hasEffectiveText(AssistantMessage ai) {
		if (ai == null) {
			return false;
		}
		String text = ai.getText();
		if (text == null) {
			return false;
		}
		return !text.replaceAll("\\s+", "").isEmpty();
	}

	/** 该 AssistantMessage 是否带工具调用（工具中间步，非最终回答）。 */
	static boolean isToolCallMessage(Message message) {
		return message instanceof AssistantMessage
				&& ((AssistantMessage) message).getToolCalls() != null
				&& !((AssistantMessage) message).getToolCalls().isEmpty();
	}

	/** 该消息是否为工具执行结果。 */
	static boolean isToolResultMessage(Message message) {
		return message instanceof ToolResponseMessage;
	}

	/** 最终回答：AssistantMessage、不带工具调用、文本有效。 */
	static boolean isFinalAnswer(Message message) {
		return message instanceof AssistantMessage
				&& !isToolCallMessage(message)
				&& hasEffectiveText((AssistantMessage) message);
	}

	/**
	 * 判断「上一轮输出」里是否发生过工具调用往返。
	 *
	 * <p>用于识别工具派生的结论——实时数据（天气、行情、库存）过期后仍留在上下文里，
	 * 会让模型凭旧值直接作答、不再重新调用工具。</p>
	 */
	static boolean containsToolRoundTrip(List<Message> messages) {
		if (messages == null || messages.isEmpty()) {
			return false;
		}
		for (Message m : messages) {
			if (isToolResultMessage(m) || isToolCallMessage(m)) {
				return true;
			}
		}
		return false;
	}

	// ==================== 内置策略 ====================

	/** 什么 AI 都不沉淀（记忆里只剩用户原话）。 */
	AiPersistenceStrategy DROP_ALL_AI = previousTurnMessages -> Collections.emptyList();

	/**
	 * <b>默认策略</b>：只沉淀「本轮没有调用工具」的最后一个最终回答。
	 *
	 * <p>取舍说明（与框架版一致）——连续性与数据新鲜度天然冲突：无条件沉淀能保住上下文
	 * 连续性，但工具派生的结论会变成「现成答案」，模型据此直接作答、不再重新调用工具；
	 * 本策略判定上一轮用过工具就整轮不沉淀，实时数据每轮重查，纯聊天与 RAG 问答照常
	 * 保留上一轮结论。</p>
	 */
	AiPersistenceStrategy PERSIST_NON_TOOL_FINAL_ANSWER = previousTurnMessages -> {
		if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
			return Collections.emptyList();
		}
		// 上一轮用过工具：其结论可能已过期，沉淀会让模型跳过工具调用
		if (containsToolRoundTrip(previousTurnMessages)) {
			return Collections.emptyList();
		}
		for (int i = previousTurnMessages.size() - 1; i >= 0; i--) {
			Message m = previousTurnMessages.get(i);
			if (isFinalAnswer(m)) {
				return Collections.singletonList(m);
			}
		}
		return Collections.emptyList();
	};

	/** 只保留最后一个最终回答（不管上一轮是否用过工具）。 */
	AiPersistenceStrategy PERSIST_LAST_FINAL_ANSWER = previousTurnMessages -> {
		if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
			return Collections.emptyList();
		}
		for (int i = previousTurnMessages.size() - 1; i >= 0; i--) {
			Message m = previousTurnMessages.get(i);
			if (isFinalAnswer(m)) {
				return Collections.singletonList(m);
			}
		}
		return Collections.emptyList();
	};

	/** 所有最终回答（过滤工具往返与空白回复）。 */
	AiPersistenceStrategy PERSIST_ALL_FINAL_ANSWERS = previousTurnMessages -> {
		if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
			return Collections.emptyList();
		}
		List<Message> selected = new ArrayList<>();
		for (Message m : previousTurnMessages) {
			if (isFinalAnswer(m)) {
				selected.add(m);
			}
		}
		return selected;
	};

	/**
	 * 所有有效 AI 回复（含带工具调用的中间步），但丢弃工具执行结果消息。
	 * 对应框架版 {@code PERSIST_ALL_AI_DROP_TOOL_MESSAGES}。
	 */
	AiPersistenceStrategy PERSIST_ALL_AI_DROP_TOOL_MESSAGES = previousTurnMessages -> {
		if (previousTurnMessages == null || previousTurnMessages.isEmpty()) {
			return Collections.emptyList();
		}
		List<Message> selected = new ArrayList<>();
		for (Message m : previousTurnMessages) {
			if (m instanceof AssistantMessage && hasEffectiveText((AssistantMessage) m)
					&& !isToolResultMessage(m)) {
				selected.add(m);
			}
		}
		return selected;
	};
}
