package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEvent;
import com.xiongx9527.fw.plus.boot.autoconfigure.config.event.ConfigChangeEventListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpConfigurationChangeNotifierTest {

	@Test
	void shouldNotifyToolsListWhenFunctionToolConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.tools.functions.enabled", true);

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly("notifications/tools/list_changed");
	}

	@Test
	void shouldNotifyToolsListWhenApiToolConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.tools.api.exposure-default", "disabled");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly("notifications/tools/list_changed");
	}

	@Test
	void shouldNotifyAllCapabilityListsWhenProtocolConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.protocol.default-version", "2025-11-25");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldNotifyAllCapabilityListsWhenAuthConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.auth.enabled", false);

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldNotifyAllCapabilityListsWhenHeaderConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.headers.session-id", "Mcp-Session-Id");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldNotifyAllCapabilityListsWhenEndpointConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.endpoint", "/mcp-v2");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldNotifyAllCapabilityListsWhenSessionAndTransportConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.session-timeout-seconds", 60);
		properties.put("magic-api.plugin.mcp.sse-stream-enabled", false);

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldNotifyAllCapabilityListsWhenTrustedOriginsConfigChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.trusted-origins", "https://example.com");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldDeduplicateNotificationMethodsWithinOneRefresh() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("magic-api.plugin.mcp.tools.functions.enabled", true);
		properties.put("magic-api.plugin.mcp.tools.api.enabled", true);
		properties.put("magic-api.plugin.mcp.auth.enabled", false);
		properties.put("magic-api.plugin.mcp.endpoint", "/mcp-v2");
		properties.put("magic-api.plugin.mcp.sse-stream-enabled", false);
		properties.put("magic-api.plugin.mcp.max-request-bytes", 1024);

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).containsExactly(
				"notifications/tools/list_changed",
				"notifications/resources/list_changed",
				"notifications/prompts/list_changed");
	}

	@Test
	void shouldIgnoreNonClientVisibleAndUnknownMcpConfigKeys() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		Map<String, Object> properties = new HashMap<>();
		properties.put("server.port", 8080);
		properties.put("magic-api.plugin.mcp.enabled", true);
		properties.put("magic-api.plugin.mcp.max-request-bytes", 1024);
		properties.put("magic-api.plugin.mcp.max-response-chars", 4096);
		properties.put("magic-api.plugin.mcp.logging.level", "debug");

		notifier.onMcpConfigurationChanged(properties);

		assertThat(notificationService.methods).isEmpty();
	}

	@Test
	void shouldReceiveFilteredPropertiesThroughOnConfigChangeListener() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		ConfigChangeEventListener listener = new ConfigChangeEventListener();
		listener.postProcessAfterInitialization(notifier, "mcpConfigurationChangeNotifier");
		Map<String, Object> properties = new HashMap<>();
		properties.put("server.port", 8080);
		properties.put("magic-api.plugin.mcp.tools.functions.enabled", true);

		listener.onApplicationEvent(new ConfigChangeEvent(this, properties));

		assertThat(notificationService.methods).containsExactly("notifications/tools/list_changed");
	}

	@Test
	void shouldIgnoreConfigChangeEventWhenFilteredMcpMapIsEmpty() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);
		ConfigChangeEventListener listener = new ConfigChangeEventListener();
		listener.postProcessAfterInitialization(notifier, "mcpConfigurationChangeNotifier");

		listener.onApplicationEvent(new ConfigChangeEvent(this,
				Collections.<String, Object>singletonMap("server.port", 8080)));

		assertThat(notificationService.methods).isEmpty();
	}

	@Test
	void shouldIgnoreNullOrEmptyProperties() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpConfigurationChangeNotifier notifier = new McpConfigurationChangeNotifier(notificationService);

		notifier.onMcpConfigurationChanged(null);
		notifier.onMcpConfigurationChanged(new HashMap<String, Object>());

		assertThat(notificationService.methods).isEmpty();
	}

	private static class RecordingNotificationService extends McpNotificationService {

		private final List<String> methods = new ArrayList<>();

		private RecordingNotificationService() {
			super(null, null);
		}

		@Override
		public void broadcast(Map<String, Object> message) {
			methods.add(String.valueOf(message.get("method")));
		}
	}
}
