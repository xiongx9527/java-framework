package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionNotFoundException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class McpLifecycleHandlers {

	private final McpSessionService sessionService;

	public McpLifecycleHandlers(McpSessionService sessionService) {
		this.sessionService = sessionService;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> initialize(McpRequestContext context, Map<String, Object> params) {
		Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;
		String protocolVersion = stringValue(safeParams.get("protocolVersion"), "2025-06-18");
		Object capabilities = safeParams.get("capabilities");
		Map<String, Object> clientCapabilities = capabilities instanceof Map ? (Map<String, Object>) capabilities : Collections.emptyMap();
		McpSession session = sessionService.createSession(context.getPrincipal(), clientCapabilities, protocolVersion);
		context.setSessionId(session.getId());

		Map<String, Object> result = new HashMap<>();
		result.put("protocolVersion", protocolVersion);
		result.put("capabilities", serverCapabilities());
		result.put("serverInfo", serverInfo());
		return result;
	}

	public Object initialized(McpRequestContext context, Object params) {
		sessionService.markReady(context.getSessionId());
		return Collections.emptyMap();
	}

	public Object ping(McpRequestContext context, Object params) {
		return Collections.emptyMap();
	}

	public Object cancelled(McpRequestContext context, Map<String, Object> params) {
		McpSession session = sessionService.requireSession(context.getSessionId());
		if (params != null && params.get("requestId") != null) {
			session.getCancelledRequests().add(params.get("requestId"));
		}
		return Collections.emptyMap();
	}

	public Object progress(McpRequestContext context, Map<String, Object> params) {
		sessionService.requireSession(context.getSessionId());
		return Collections.emptyMap();
	}

	public void requireReady(McpRequestContext context) {
		McpSession session;
		try {
			session = sessionService.requireSession(context == null ? null : context.getSessionId());
		} catch (McpSessionNotFoundException e) {
			throw new McpJsonRpcException(McpJsonRpcError.INVALID_REQUEST, "Session is not ready");
		}
		if (session.getState() != McpSessionState.READY) {
			throw new McpJsonRpcException(McpJsonRpcError.INVALID_REQUEST, "Session is not ready");
		}
	}

	private Map<String, Object> serverCapabilities() {
		Map<String, Object> capabilities = new HashMap<>();
		capabilities.put("tools", Collections.emptyMap());
		capabilities.put("resources", Collections.emptyMap());
		capabilities.put("prompts", Collections.emptyMap());
		return capabilities;
	}

	private Map<String, Object> serverInfo() {
		Map<String, Object> serverInfo = new HashMap<>();
		serverInfo.put("name", "fw-plus-magicapi-mcp");
		serverInfo.put("version", "0.1.1-SNAPSHOT");
		return serverInfo;
	}

	private String stringValue(Object value, String defaultValue) {
		return value == null ? defaultValue : String.valueOf(value);
	}
}
