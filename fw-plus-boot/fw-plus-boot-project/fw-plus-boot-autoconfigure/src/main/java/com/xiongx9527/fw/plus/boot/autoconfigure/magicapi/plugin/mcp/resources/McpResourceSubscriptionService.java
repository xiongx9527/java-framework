package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class McpResourceSubscriptionService {

	private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

	public void subscribe(String sessionId, String uri) {
		if (sessionId == null || uri == null) {
			return;
		}
		subscriptions.computeIfAbsent(uri, key -> Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()))
				.add(sessionId);
	}

	public void unsubscribe(String sessionId, String uri) {
		Set<String> sessionIds = subscriptions.get(uri);
		if (sessionIds != null) {
			sessionIds.remove(sessionId);
			if (sessionIds.isEmpty()) {
				subscriptions.remove(uri);
			}
		}
	}

	public List<Map<String, Object>> updated(String uri) {
		Set<String> sessionIds = subscriptions.get(uri);
		if (sessionIds == null || sessionIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> notifications = new ArrayList<>();
		for (String sessionId : sessionIds) {
			Map<String, Object> params = new HashMap<>();
			params.put("uri", uri);
			params.put("sessionId", sessionId);
			Map<String, Object> notification = new HashMap<>();
			notification.put("jsonrpc", "2.0");
			notification.put("method", "notifications/resources/updated");
			notification.put("params", params);
			notifications.add(notification);
		}
		return notifications;
	}

	public Map<String, Object> listChanged() {
		Map<String, Object> notification = new HashMap<>();
		notification.put("jsonrpc", "2.0");
		notification.put("method", "notifications/resources/list_changed");
		return notification;
	}
}
