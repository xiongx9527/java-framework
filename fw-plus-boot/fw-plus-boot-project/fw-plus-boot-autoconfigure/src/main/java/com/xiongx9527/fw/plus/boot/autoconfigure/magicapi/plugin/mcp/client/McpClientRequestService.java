package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.client;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class McpClientRequestService {

	private final McpSessionService sessionService;

	public McpClientRequestService(McpSessionService sessionService) {
		this.sessionService = sessionService;
	}

	public Map<String, Object> rootsList(String sessionId) {
		return createRequest(sessionId, "roots", "roots/list", null);
	}

	public Map<String, Object> samplingCreateMessage(String sessionId, Map<String, Object> params) {
		return createRequest(sessionId, "sampling", "sampling/createMessage", params);
	}

	public Map<String, Object> elicitationCreate(String sessionId, Map<String, Object> params) {
		return createRequest(sessionId, "elicitation", "elicitation/create", params);
	}

	private Map<String, Object> createRequest(String sessionId, String capability, String method, Map<String, Object> params) {
		McpSession session = sessionService.requireSession(sessionId);
		if (!session.getClientCapabilities().containsKey(capability)) {
			throw new McpClientCapabilityException("Client capability not supported: " + capability);
		}
		String id = UUID.randomUUID().toString();
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", id);
		request.put("method", method);
		if (params != null) {
			request.put("params", params);
		}
		session.getClientRequests().put(id, request);
		return request;
	}
}
