package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.client;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class McpClientRequestService {

	private final McpSessionService sessionService;

	private final long timeoutMillis;

	public McpClientRequestService(McpSessionService sessionService) {
		this(sessionService, 30000);
	}

	public McpClientRequestService(McpSessionService sessionService, long timeoutMillis) {
		this.sessionService = sessionService;
		this.timeoutMillis = timeoutMillis;
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
		McpClientRequestRecord record = new McpClientRequestRecord();
		record.setId(id);
		record.setSessionId(sessionId);
		record.setMethod(method);
		record.setCapability(capability);
		long now = System.currentTimeMillis();
		record.setCreatedAt(now);
		record.setTimeoutAt(now + timeoutMillis);
		session.getPendingClientRequests().put(id, record);
		return request;
	}

	public boolean resolve(String sessionId, Object requestId, Object result, Object error) {
		McpClientRequestRecord record = record(sessionId, requestId);
		if (record == null) {
			return false;
		}
		record.setResult(result);
		record.setError(error);
		record.setStatus(error == null ? McpClientRequestStatus.RESOLVED : McpClientRequestStatus.FAILED);
		return true;
	}

	public int cleanupTimedOut(String sessionId, long now) {
		McpSession session = sessionService.requireSession(sessionId);
		int count = 0;
		for (McpClientRequestRecord record : session.getPendingClientRequests().values()) {
			if (record.getStatus() == McpClientRequestStatus.PENDING && record.getTimeoutAt() <= now) {
				record.setStatus(McpClientRequestStatus.TIMED_OUT);
				count++;
			}
		}
		return count;
	}

	public boolean cancel(String sessionId, Object requestId) {
		McpClientRequestRecord record = record(sessionId, requestId);
		if (record == null) {
			return false;
		}
		record.setStatus(McpClientRequestStatus.CANCELLED);
		return true;
	}

	private McpClientRequestRecord record(String sessionId, Object requestId) {
		McpSession session = sessionService.requireSession(sessionId);
		return session.getPendingClientRequests().get(requestId);
	}
}
