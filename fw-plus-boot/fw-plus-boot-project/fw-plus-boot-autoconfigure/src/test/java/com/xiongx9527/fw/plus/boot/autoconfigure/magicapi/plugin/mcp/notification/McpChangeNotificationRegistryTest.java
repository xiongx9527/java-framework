package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpResourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service.McpPromptMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service.McpResourceMagicDynamicRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpChangeNotificationRegistryTest {

	@Test
	void shouldNotifyResourceListAndUpdatedWhenResourceChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpResourceMagicDynamicRegistry registry = new McpResourceMagicDynamicRegistry(null, notificationService);
		McpResourceInfo info = new McpResourceInfo();
		info.setUri("mcp://docs/readme");

		registry.notifyChanged(info);

		assertThat(notificationService.methods).contains("notifications/resources/list_changed");
		assertThat(notificationService.updatedUris).containsExactly("mcp://docs/readme");
	}

	@Test
	void shouldNotifyPromptListWhenPromptChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpPromptMagicDynamicRegistry registry = new McpPromptMagicDynamicRegistry(null, notificationService);

		registry.notifyChanged();

		assertThat(notificationService.methods).containsExactly("notifications/prompts/list_changed");
	}

	@Test
	void shouldNotifyToolListWhenApiOrFunctionChanges() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpToolMagicChangeListener listener = new McpToolMagicChangeListener(notificationService);

		listener.notifyChanged();

		assertThat(notificationService.methods).containsExactly("notifications/tools/list_changed");
	}

	private static class RecordingNotificationService extends McpNotificationService {

		private final List<String> methods = new ArrayList<>();

		private final List<String> updatedUris = new ArrayList<>();

		private RecordingNotificationService() {
			super(null, null);
		}

		@Override
		public void broadcast(Map<String, Object> message) {
			methods.add(String.valueOf(message.get("method")));
		}

		@Override
		public void resourceUpdated(String uri) {
			updatedUris.add(uri);
		}
	}
}
