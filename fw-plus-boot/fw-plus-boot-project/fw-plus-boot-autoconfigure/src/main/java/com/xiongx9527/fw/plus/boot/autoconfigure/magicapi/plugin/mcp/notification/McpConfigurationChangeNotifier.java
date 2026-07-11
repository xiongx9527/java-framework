package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.OnConfigChange;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class McpConfigurationChangeNotifier {

	private static final String MCP_PREFIX = "magic-api.plugin.mcp";
	private static final String FUNCTIONS_TOOLS_PREFIX = MCP_PREFIX + ".tools.functions.";
	private static final String API_TOOLS_PREFIX = MCP_PREFIX + ".tools.api.";
	private static final String PROTOCOL_PREFIX = MCP_PREFIX + ".protocol.";
	private static final String AUTH_PREFIX = MCP_PREFIX + ".auth.";
	private static final String HEADERS_PREFIX = MCP_PREFIX + ".headers.";
	private static final String ENDPOINT_KEY = MCP_PREFIX + ".endpoint";
	private static final String SESSION_TIMEOUT_SECONDS_KEY = MCP_PREFIX + ".session-timeout-seconds";
	private static final String SSE_STREAM_ENABLED_KEY = MCP_PREFIX + ".sse-stream-enabled";
	private static final String TRUSTED_ORIGINS_KEY = MCP_PREFIX + ".trusted-origins";

	private static final String TOOLS_LIST_CHANGED = "notifications/tools/list_changed";
	private static final String RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";
	private static final String PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

	private final McpNotificationService notificationService;

	public McpConfigurationChangeNotifier(McpNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@OnConfigChange(prefix = MCP_PREFIX)
	public void onMcpConfigurationChanged(Map<String, Object> properties) {
		if (properties == null || properties.isEmpty()) {
			return;
		}
		Set<String> methods = new LinkedHashSet<>();
		for (String key : properties.keySet()) {
			collectNotificationMethods(key, methods);
		}
		for (String method : methods) {
			broadcast(method);
		}
	}

	private void collectNotificationMethods(String key, Set<String> methods) {
		if (key == null) {
			return;
		}
		if (key.startsWith(FUNCTIONS_TOOLS_PREFIX) || key.startsWith(API_TOOLS_PREFIX)) {
			methods.add(TOOLS_LIST_CHANGED);
			return;
		}
		if (key.startsWith(PROTOCOL_PREFIX) || key.startsWith(AUTH_PREFIX) || key.startsWith(HEADERS_PREFIX)
				|| isCapabilityViewKey(key)) {
			methods.add(TOOLS_LIST_CHANGED);
			methods.add(RESOURCES_LIST_CHANGED);
			methods.add(PROMPTS_LIST_CHANGED);
		}
	}

	private boolean isCapabilityViewKey(String key) {
		return ENDPOINT_KEY.equals(key)
				|| SESSION_TIMEOUT_SECONDS_KEY.equals(key)
				|| SSE_STREAM_ENABLED_KEY.equals(key)
				|| TRUSTED_ORIGINS_KEY.equals(key);
	}

	private void broadcast(String method) {
		if (TOOLS_LIST_CHANGED.equals(method)) {
			notificationService.broadcast(McpNotificationService.toolsListChanged());
			return;
		}
		if (RESOURCES_LIST_CHANGED.equals(method)) {
			notificationService.broadcast(McpNotificationService.resourcesListChanged());
			return;
		}
		if (PROMPTS_LIST_CHANGED.equals(method)) {
			notificationService.broadcast(McpNotificationService.promptsListChanged());
		}
	}
}
