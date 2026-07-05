package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources.McpResourceSubscriptionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpSseStreamService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McpNotificationService {

	private final McpSseStreamService sseStreamService;

	private final McpResourceSubscriptionService subscriptionService;

	public McpNotificationService(McpSseStreamService sseStreamService,
								  McpResourceSubscriptionService subscriptionService) {
		this.sseStreamService = sseStreamService;
		this.subscriptionService = subscriptionService;
	}

	public boolean send(String sessionId, Map<String, Object> message) {
		return sseStreamService.send(sessionId, message);
	}

	public void broadcast(Map<String, Object> message) {
		sseStreamService.broadcast(message);
	}

	public void resourceUpdated(String uri) {
		List<Map<String, Object>> notifications = subscriptionService.updated(uri);
		for (Map<String, Object> notification : notifications) {
			Object params = notification.get("params");
			if (params instanceof Map) {
				Object sessionId = ((Map<?, ?>) params).get("sessionId");
				if (sessionId != null) {
					Map<String, Object> cleanNotification = new HashMap<>(notification);
					Map<String, Object> cleanParams = new HashMap<>((Map<String, Object>) params);
					cleanParams.remove("sessionId");
					cleanNotification.put("params", cleanParams);
					send(String.valueOf(sessionId), cleanNotification);
				}
			}
		}
	}

	public static Map<String, Object> toolsListChanged() {
		return notification("notifications/tools/list_changed");
	}

	public static Map<String, Object> resourcesListChanged() {
		return notification("notifications/resources/list_changed");
	}

	public static Map<String, Object> promptsListChanged() {
		return notification("notifications/prompts/list_changed");
	}

	private static Map<String, Object> notification(String method) {
		Map<String, Object> notification = new HashMap<>();
		notification.put("jsonrpc", "2.0");
		notification.put("method", method);
		return notification;
	}
}
