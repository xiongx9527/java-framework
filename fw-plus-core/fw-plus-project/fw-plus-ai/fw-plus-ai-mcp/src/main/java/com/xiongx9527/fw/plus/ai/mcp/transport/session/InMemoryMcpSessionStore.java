package com.xiongx9527.fw.plus.ai.mcp.transport.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存实现的 {@link McpSessionStore}。
 *
 * <p>会话数据存储在 JVM 内存中，不跨节点共享。
 * 惰性过期：访问时检查是否超时，超时则自动清理。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public class InMemoryMcpSessionStore implements McpSessionStore {

	private static final Logger logger = LoggerFactory.getLogger(InMemoryMcpSessionStore.class);

	private final ConcurrentMap<String, McpSessionData> sessions = new ConcurrentHashMap<>();
	private final long sessionTimeoutMillis;

	public InMemoryMcpSessionStore(long sessionTimeoutMillis) {
		this.sessionTimeoutMillis = sessionTimeoutMillis;
	}

	@Override
	public void save(String sessionId, McpSessionData sessionData) {
		if (sessionId == null || sessionData == null) {
			return;
		}
		sessions.put(sessionId, sessionData);
	}

	@Override
	public McpSessionData load(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		McpSessionData data = sessions.get(sessionId);
		if (data == null) {
			return null;
		}
		if (isExpired(data)) {
			remove(sessionId);
			return null;
		}
		return data;
	}

	@Override
	public void remove(String sessionId) {
		if (sessionId != null) {
			sessions.remove(sessionId);
		}
	}

	@Override
	public void touch(String sessionId) {
		if (sessionId == null) {
			return;
		}
		McpSessionData data = sessions.get(sessionId);
		if (data != null) {
			data.setLastAccess(System.currentTimeMillis());
		}
	}

	/**
	 * 清理所有过期会话。
	 *
	 * @return 被清理的会话数量
	 */
	public int expireSessions() {
		long now = System.currentTimeMillis();
		int count = 0;
		for (String sessionId : sessions.keySet()) {
			McpSessionData data = sessions.get(sessionId);
			if (data != null && now - data.getLastAccess() > sessionTimeoutMillis) {
				sessions.remove(sessionId);
				count++;
			}
		}
		return count;
	}

	/**
	 * 当前存储的会话数量（用于调试/监控）。
	 */
	public int size() {
		return sessions.size();
	}

	private boolean isExpired(McpSessionData data) {
		return System.currentTimeMillis() - data.getLastAccess() > sessionTimeoutMillis;
	}
}
