package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpLogService {

	private final Map<String, String> levels = new ConcurrentHashMap<>();

	public void setLevel(String sessionId, String level) {
		if (sessionId != null && level != null) {
			levels.put(sessionId, level);
		}
	}

	public String getLevel(String sessionId) {
		return levels.get(sessionId);
	}

	public Map<String, Object> message(String sessionId, String level, String message) {
		if (!isEnabled(getLevel(sessionId), level)) {
			return null;
		}
		Map<String, Object> params = new HashMap<>();
		params.put("level", level);
		params.put("logger", "fw-plus-mcp");
		params.put("data", message);
		Map<String, Object> notification = new HashMap<>();
		notification.put("jsonrpc", "2.0");
		notification.put("method", "notifications/message");
		notification.put("params", params);
		return notification;
	}

	private boolean isEnabled(String configuredLevel, String eventLevel) {
		return severity(eventLevel) >= severity(configuredLevel == null ? "info" : configuredLevel);
	}

	private int severity(String level) {
		if ("debug".equalsIgnoreCase(level)) {
			return 10;
		}
		if ("info".equalsIgnoreCase(level)) {
			return 20;
		}
		if ("notice".equalsIgnoreCase(level)) {
			return 25;
		}
		if ("warning".equalsIgnoreCase(level)) {
			return 30;
		}
		if ("error".equalsIgnoreCase(level)) {
			return 40;
		}
		if ("critical".equalsIgnoreCase(level)) {
			return 50;
		}
		if ("alert".equalsIgnoreCase(level)) {
			return 60;
		}
		if ("emergency".equalsIgnoreCase(level)) {
			return 70;
		}
		return 20;
	}
}
