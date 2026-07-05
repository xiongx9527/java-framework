package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMcpSessionRepository implements McpSessionRepository {

	private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();

	@Override
	public void save(McpSession session) {
		if (session != null && session.getId() != null) {
			sessions.put(session.getId(), session);
		}
	}

	@Override
	public McpSession findById(String sessionId) {
		return sessionId == null ? null : sessions.get(sessionId);
	}

	@Override
	public void touch(String sessionId, long lastActivityAt) {
		McpSession session = findById(sessionId);
		if (session != null) {
			session.setLastActivityAt(lastActivityAt);
		}
	}

	@Override
	public void delete(String sessionId) {
		if (sessionId != null) {
			sessions.remove(sessionId);
		}
	}

	@Override
	public Collection<McpSession> findAll() {
		return Collections.unmodifiableCollection(sessions.values());
	}
}
