package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources.McpResourceSubscriptionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpSseStreamService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McpNotificationService {

	private final McpSseStreamService sseStreamService;

	private final McpResourceSubscriptionService subscriptionService;

	private final McpChangeEventPublisher changeEventPublisher;

	public McpNotificationService(McpSseStreamService sseStreamService,
								  McpResourceSubscriptionService subscriptionService) {
		this(sseStreamService, subscriptionService, null);
	}

	public McpNotificationService(McpSseStreamService sseStreamService,
								  McpResourceSubscriptionService subscriptionService,
								  McpChangeEventPublisher changeEventPublisher) {
		this.sseStreamService = sseStreamService;
		this.subscriptionService = subscriptionService;
		this.changeEventPublisher = changeEventPublisher;
	}

	public boolean send(String sessionId, Map<String, Object> message) {
		return sseStreamService.send(sessionId, message);
	}

	public void broadcast(Map<String, Object> message) {
		if (changeEventPublisher != null) {
			changeEventPublisher.publish(new McpChangeEvent(changeEventPublisher.getOriginId(), message));
		}
		broadcastLocal(message);
	}

	public void receiveClusterEvent(McpChangeEvent event) {
		if (event == null) {
			return;
		}
		if (changeEventPublisher != null && changeEventPublisher.getOriginId() != null
				&& changeEventPublisher.getOriginId().equals(event.getOriginId())) {
			return;
		}
		Map<String, Object> message = event.toMessage();
		if ("notifications/resources/updated".equals(event.getMethod())) {
			Object uri = event.getParams() == null ? null : event.getParams().get("uri");
			if (uri != null) {
				resourceUpdatedLocal(String.valueOf(uri));
				return;
			}
		}
		broadcastLocal(message);
	}

	private void broadcastLocal(Map<String, Object> message) {
		sseStreamService.broadcast(message);
	}

	public void resourceUpdated(String uri) {
		Map<String, Object> message = resourceUpdatedNotification(uri);
		if (changeEventPublisher != null) {
			changeEventPublisher.publish(new McpChangeEvent(changeEventPublisher.getOriginId(), message));
		}
		resourceUpdatedLocal(uri);
	}

	private void resourceUpdatedLocal(String uri) {
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

	public static Map<String, Object> resourceUpdatedNotification(String uri) {
		Map<String, Object> params = new HashMap<>();
		params.put("uri", uri);
		Map<String, Object> notification = notification("notifications/resources/updated");
		notification.put("params", params);
		return notification;
	}

	private static Map<String, Object> notification(String method) {
		Map<String, Object> notification = new HashMap<>();
		notification.put("jsonrpc", "2.0");
		notification.put("method", method);
		return notification;
	}
}
