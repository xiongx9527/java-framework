package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import dev.langchain4j.data.message.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话级轮次缓冲（{@code batched} / {@code session-end} 共用）：按会话键累积
 * 「用户维度 + 本轮真实消息」，在达阈值（batched）或会话切换（两种模式）时整体冲空。
 *
 * <p>并发语义：{@code ConcurrentHashMap} + 每会话桶内 {@code synchronized}——同会话
 * 追加/冲空串行化，跨会话互不阻塞。冲空<b>不删除</b>桶映射（空桶常驻，量级 = 会话数），
 * 消除「追加已取到桶引用、冲空随后移除映射」造成的静默丢轮窗口。</p>
 *
 * <p>缓冲为内存态，进程退出即丢（v1 接受的取舍，见设计 §10）。</p>
 */
public class FactTurnBuffer {

	private final Map<String, Bucket> buckets = new ConcurrentHashMap<String, Bucket>();

	/** 追加一轮（会话键为空白时忽略）。 */
	public void append(String sessionKey, String userId, List<ChatMessage> messages) {
		String key = normalizedKey(sessionKey);
		if (key == null) {
			return;
		}
		Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
		synchronized (bucket) {
			bucket.turns.add(new BufferedTurn(userId, messages));
		}
	}

	/** 当前缓冲轮数（会话键为空白时为 0）。 */
	public int size(String sessionKey) {
		String key = normalizedKey(sessionKey);
		if (key == null) {
			return 0;
		}
		Bucket bucket = buckets.get(key);
		if (bucket == null) {
			return 0;
		}
		synchronized (bucket) {
			return bucket.turns.size();
		}
	}

	/** 取出并清空指定会话的全部缓冲轮（按入队顺序）；无缓冲返回空列表。 */
	public List<BufferedTurn> drain(String sessionKey) {
		String key = normalizedKey(sessionKey);
		if (key == null) {
			return new ArrayList<BufferedTurn>();
		}
		Bucket bucket = buckets.get(key);
		if (bucket == null) {
			return new ArrayList<BufferedTurn>();
		}
		synchronized (bucket) {
			List<BufferedTurn> drained = new ArrayList<BufferedTurn>(bucket.turns);
			bucket.turns.clear();
			return drained;
		}
	}

	private static String normalizedKey(String sessionKey) {
		if (sessionKey == null || sessionKey.trim().isEmpty()) {
			return null;
		}
		return sessionKey.trim();
	}

	/** 每会话桶：桶对象兼作锁；桶常驻（冲空只清空、不删映射，保证并发追加不落到孤儿列表）。 */
	private static final class Bucket {

		private final List<BufferedTurn> turns = new ArrayList<BufferedTurn>();
	}

	/** 会话级缓冲轮：用户维度 + 本轮真实消息（构造时拷贝）。 */
	public static final class BufferedTurn {

		private final String userId;

		private final List<ChatMessage> messages;

		public BufferedTurn(String userId, List<ChatMessage> messages) {
			this.userId = userId;
			this.messages = messages == null
					? Collections.<ChatMessage>emptyList()
					: Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
		}

		public String getUserId() {
			return userId;
		}

		public List<ChatMessage> getMessages() {
			return messages;
		}
	}
}
