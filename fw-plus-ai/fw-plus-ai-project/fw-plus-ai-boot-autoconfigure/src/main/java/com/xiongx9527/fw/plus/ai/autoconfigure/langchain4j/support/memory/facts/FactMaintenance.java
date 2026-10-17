package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextMaintenance;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryIds;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 写路径维护钩子（{@link ContextMaintenance} 3 参）：AI 最终回复落库后触发事实抽取/写入。
 *
 * <p>守卫（按序）：读时兜底（{@code turnMessage == null}）不抽取；工具循环中间态
 * （{@code hasToolExecutionRequests()}）跳过；无显式用户段（memId 段[3]）一次性 WARN 后跳过；
 * 本轮消息全空跳过。</p>
 *
 * <p>失败隔离：抽取异常在此边界被吞掉并记日志，绝不中断主对话；异步模式经
 * {@link FactWriteScheduler} 提交，队列满时丢弃本轮。</p>
 */
@Slf4j
public class FactMaintenance implements ContextMaintenance {

	private final FactExtractor extractor;

	private final FactWriteMode writeMode;

	private final int batchSize;

	private final boolean extractAsync;

	private final FactTurnBuffer buffer;

	/** 用户维度 → 最近会话键（会话切换检测：同一用户出现新会话键即视为上一会话结束） */
	private final Map<String, String> lastSessionKeys = new ConcurrentHashMap<String, String>();

	/** 无显式用户段仅提示一次 */
	private final AtomicBoolean scopeWarned = new AtomicBoolean();

	public FactMaintenance(FactExtractor extractor, FactWriteMode writeMode, int batchSize,
			boolean extractAsync, FactTurnBuffer buffer) {
		if (extractor == null) {
			throw new IllegalArgumentException("FactMaintenance: extractor 不能为空");
		}
		this.extractor = extractor;
		this.writeMode = writeMode == null ? FactWriteMode.PER_TURN : writeMode;
		this.batchSize = batchSize < 1 ? 1 : batchSize;
		this.extractAsync = extractAsync;
		this.buffer = buffer == null ? new FactTurnBuffer() : buffer;
	}

	/** 读时兜底入口：无当轮消息，不触发抽取（2 参为接口既有抽象方法）。 */
	@Override
	public void maintain(ContextRequest request, List<ChatMessage> currentMessages) {
		// 读时不抽取：全部逻辑在 3 参入口
	}

	@Override
	public void maintain(ContextRequest request, List<ChatMessage> currentMessages, ChatMessage turnMessage) {
		if (!(turnMessage instanceof AiMessage)) {
			return;
		}
		AiMessage aiMessage = (AiMessage) turnMessage;
		if (aiMessage.hasToolExecutionRequests()) {
			// 工具循环中间态：只对最终回复触发
			return;
		}
		if (request == null) {
			return;
		}
		String userId = ChatMemoryIds.parseUserId(request.getEffectiveMemoryId(), null);
		if (userId == null) {
			if (scopeWarned.compareAndSet(false, true)) {
				log.warn("事实通道缺少显式用户段（memId 段[3]），本轮起跳过（仅提示一次）：{}",
						request.getEffectiveMemoryId());
			}
			return;
		}
		List<ChatMessage> turnMessages = assembleTurn(request.getLatestUserMessage(), aiMessage);
		if (turnMessages.isEmpty()) {
			return;
		}
		String sessionKey = request.getSessionKey();
		switch (writeMode) {
			case BATCHED:
				handleSessionSwitch(userId, sessionKey);
				buffer.append(sessionKey, userId, turnMessages);
				if (buffer.size(sessionKey) >= batchSize) {
					drainAndSubmit(sessionKey);
				}
				break;
			case SESSION_END:
				handleSessionSwitch(userId, sessionKey);
				buffer.append(sessionKey, userId, turnMessages);
				break;
			case PER_TURN:
			default:
				submit(userId, turnMessages);
		}
	}

	/** 显式冲空指定会话缓冲（batched / session-end；供自定义装配方与测试）。 */
	public void flush(String sessionKey) {
		drainAndSubmit(sessionKey);
	}

	// -------------------------------------------------- 内部实现

	private void handleSessionSwitch(String userId, String sessionKey) {
		String key = sessionKey == null || sessionKey.trim().isEmpty() ? null : sessionKey.trim();
		if (key == null) {
			return;
		}
		String previous = lastSessionKeys.put(userId, key);
		if (previous != null && !previous.equals(key)) {
			// 同一用户出现新会话：上一会话视为结束，集中写入其缓冲
			drainAndSubmit(previous);
		}
	}

	private void drainAndSubmit(String sessionKey) {
		List<FactTurnBuffer.BufferedTurn> drained = buffer.drain(sessionKey);
		if (drained.isEmpty()) {
			return;
		}
		// 按用户分组（正常同会话同用户），消息保持时间序扁平
		Map<String, List<ChatMessage>> grouped = new LinkedHashMap<String, List<ChatMessage>>();
		for (FactTurnBuffer.BufferedTurn turn : drained) {
			List<ChatMessage> merged = grouped.get(turn.getUserId());
			if (merged == null) {
				merged = new ArrayList<ChatMessage>();
				grouped.put(turn.getUserId(), merged);
			}
			merged.addAll(turn.getMessages());
		}
		for (Map.Entry<String, List<ChatMessage>> entry : grouped.entrySet()) {
			submit(entry.getKey(), entry.getValue());
		}
	}

	private static List<ChatMessage> assembleTurn(String latestUserMessage, AiMessage aiMessage) {
		List<ChatMessage> messages = new ArrayList<ChatMessage>(2);
		if (latestUserMessage != null && !latestUserMessage.trim().isEmpty()) {
			messages.add(UserMessage.from(latestUserMessage.trim()));
		}
		String aiText = aiMessage.text();
		if (aiText != null && !aiText.trim().isEmpty()) {
			messages.add(aiMessage);
		}
		return messages;
	}

	private void submit(final String userId, final List<ChatMessage> turnMessages) {
		final List<ChatMessage> messages = Collections
				.unmodifiableList(new ArrayList<ChatMessage>(turnMessages));
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					extractor.extract(userId, messages);
				} catch (Throwable t) {
					// 失败隔离：抽取失败（含 Error 级）丢弃本轮，绝不中断/反压主对话
					log.warn("事实抽取失败，本轮丢弃（不中断对话）：{}", t.toString());
				}
			}
		};
		if (extractAsync) {
			if (!FactWriteScheduler.submit(task)) {
				log.warn("事实写入队列已满，丢弃本轮抽取");
			}
			return;
		}
		task.run();
	}
}
