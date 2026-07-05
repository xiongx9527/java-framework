package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class McpSessionService {

	private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();

	private final long sessionTimeoutMillis;

	public McpSessionService(long sessionTimeoutSeconds) {
		this.sessionTimeoutMillis = sessionTimeoutSeconds <= 0 ? 0 : sessionTimeoutSeconds * 1000L;
	}

	public McpSession createSession(McpPrincipal principal, Map<String, Object> clientCapabilities, String protocolVersion) {
		long now = System.currentTimeMillis();
		McpSession session = new McpSession();
		session.setId(UUID.randomUUID().toString());
		session.setPrincipal(principal);
		session.setClientCapabilities(clientCapabilities == null ? Collections.emptyMap() : clientCapabilities);
		session.setProtocolVersion(protocolVersion == null ? "2025-06-18" : protocolVersion);
		session.setState(McpSessionState.INITIALIZED);
		session.setCreatedAt(now);
		session.setLastActivityAt(now);
		sessions.put(session.getId(), session);
		return session;
	}

	public McpSession requireSession(String sessionId) {
		McpSession session = sessions.get(sessionId);
		if (session == null || session.getState() == McpSessionState.DELETED || isExpired(session)) {
			if (session != null) {
				sessions.remove(sessionId);
			}
			throw new McpSessionNotFoundException(sessionId);
		}
		session.setLastActivityAt(System.currentTimeMillis());
		return session;
	}

	public void markReady(String sessionId) {
		McpSession session = requireSession(sessionId);
		session.setState(McpSessionState.READY);
	}

	public void delete(String sessionId) {
		McpSession session = sessions.get(sessionId);
		if (session != null) {
			session.setState(McpSessionState.DELETED);
			sessions.remove(sessionId);
		}
	}

	private boolean isExpired(McpSession session) {
		return sessionTimeoutMillis > 0 && System.currentTimeMillis() - session.getLastActivityAt() > sessionTimeoutMillis;
	}
}
