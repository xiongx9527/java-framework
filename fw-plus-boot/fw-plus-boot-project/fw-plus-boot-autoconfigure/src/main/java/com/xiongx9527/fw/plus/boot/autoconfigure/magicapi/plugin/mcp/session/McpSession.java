package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import lombok.Data;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class McpSession {

	private String id;

	private McpPrincipal principal;

	private String protocolVersion;

	private Map<String, Object> clientCapabilities = Collections.emptyMap();

	private McpSessionState state = McpSessionState.NEW;

	private long createdAt;

	private long lastActivityAt;

	private Map<Object, Map<String, Object>> clientRequests = new ConcurrentHashMap<>();

	private Set<Object> cancelledRequests = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());
}
