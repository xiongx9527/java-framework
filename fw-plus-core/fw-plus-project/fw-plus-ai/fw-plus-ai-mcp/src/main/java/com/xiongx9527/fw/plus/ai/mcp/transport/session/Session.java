package com.xiongx9527.fw.plus.ai.mcp.transport.session;

import com.xiongx9527.fw.plus.ai.mcp.transport.SseSink;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * MCP 会话运行时状态。
 *
 * <p>包含 SSE/WebSocket 传输通道引用和事件缓冲。
 * 不包含可序列化的协议状态（由 {@link McpSessionData} 承载）。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public class Session {

	public final String id;
	public final Deque<McpSessionData.SseEvent> events = new ArrayDeque<>();
	public volatile SseSink sink;
	public long eventSeq;
	public volatile long lastAccess;

	public Session(String id) {
		this.id = id;
	}

	/**
	 * 从持久化数据恢复运行时会话（sink 需后续 attach）
	 */
	public Session(McpSessionData data) {
		this.id = data.getSessionId();
		this.eventSeq = data.getEventSeq();
		this.lastAccess = data.getLastAccess();
		if (data.getEvents() != null) {
			this.events.addAll(data.getEvents());
		}
	}

	/**
	 * 转换为可序列化数据
	 */
	public McpSessionData toData() {
		Deque<McpSessionData.SseEvent> eventsCopy = new ArrayDeque<>(events);
		return McpSessionData.of(id, eventSeq, lastAccess, eventsCopy);
	}
}
