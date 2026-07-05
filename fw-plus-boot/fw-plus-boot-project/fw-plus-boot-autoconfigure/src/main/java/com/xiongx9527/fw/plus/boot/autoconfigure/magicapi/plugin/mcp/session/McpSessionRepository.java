package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session;

import java.util.Collection;

public interface McpSessionRepository {

	void save(McpSession session);

	McpSession findById(String sessionId);

	void touch(String sessionId, long lastActivityAt);

	void delete(String sessionId);

	Collection<McpSession> findAll();
}
