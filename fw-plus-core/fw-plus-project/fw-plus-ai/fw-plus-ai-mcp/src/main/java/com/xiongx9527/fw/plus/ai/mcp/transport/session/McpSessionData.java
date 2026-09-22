package com.xiongx9527.fw.plus.ai.mcp.transport.session;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * MCP 会话数据（可序列化）。
 *
 * <p>存储会话的核心状态，供 {@link McpSessionStore} 持久化。
 * 不包含传输层专属对象（如 SSE sink），仅包含协议状态。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public class McpSessionData {

	private String sessionId;
	private long eventSeq;
	private long lastAccess;
	private Deque<SseEvent> events;

	public McpSessionData() {
	}

	public McpSessionData(String sessionId, long eventSeq, long lastAccess, Deque<SseEvent> events) {
		this.sessionId = sessionId;
		this.eventSeq = eventSeq;
		this.lastAccess = lastAccess;
		this.events = events;
	}

	public static McpSessionData of(String sessionId, long eventSeq, long lastAccess, Deque<SseEvent> events) {
		return new McpSessionData(sessionId, eventSeq, lastAccess, events);
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public long getEventSeq() {
		return eventSeq;
	}

	public void setEventSeq(long eventSeq) {
		this.eventSeq = eventSeq;
	}

	public long getLastAccess() {
		return lastAccess;
	}

	public void setLastAccess(long lastAccess) {
		this.lastAccess = lastAccess;
	}

	public Deque<SseEvent> getEvents() {
		return events;
	}

	public void setEvents(Deque<SseEvent> events) {
		this.events = events;
	}

	/**
	 * SSE 事件数据
	 */
	public static class SseEvent {
		private String id;
		private String payload;

		public SseEvent() {
		}

		public SseEvent(String id, String payload) {
			this.id = id;
			this.payload = payload;
		}

		public static SseEvent of(String id, String payload) {
			return new SseEvent(id, payload);
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getPayload() {
			return payload;
		}

		public void setPayload(String payload) {
			this.payload = payload;
		}
	}
}
