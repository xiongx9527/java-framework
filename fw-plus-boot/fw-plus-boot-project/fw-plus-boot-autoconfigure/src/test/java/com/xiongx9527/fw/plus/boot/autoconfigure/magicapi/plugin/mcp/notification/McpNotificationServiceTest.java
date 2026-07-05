package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources.McpResourceSubscriptionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpSseStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpNotificationServiceTest {

	@Test
	void shouldSendJsonRpcNotificationToRegisteredEmitter() {
		RecordingEmitter emitter = new RecordingEmitter();
		McpSseStreamService sseStreamService = new McpSseStreamService(new MagicAPIMcpProperties(), null, () -> emitter);
		sseStreamService.open("text/event-stream", "session-1");
		McpNotificationService notificationService = new McpNotificationService(sseStreamService, new McpResourceSubscriptionService());

		notificationService.send("session-1", McpNotificationService.toolsListChanged());

		assertThat(emitter.events).hasSize(1);
		assertThat(emitter.events.get(0)).contains("notifications/tools/list_changed");
	}

	@Test
	void shouldSendResourceUpdatedOnlyToSubscribedSessions() {
		RecordingEmitter subscribed = new RecordingEmitter();
		RecordingEmitter other = new RecordingEmitter();
		McpSseStreamService sseStreamService = new McpSseStreamService(new MagicAPIMcpProperties(), null,
				new QueueEmitterFactory(subscribed, other));
		sseStreamService.open("text/event-stream", "session-1");
		sseStreamService.open("text/event-stream", "session-2");
		McpResourceSubscriptionService subscriptionService = new McpResourceSubscriptionService();
		subscriptionService.subscribe("session-1", "mcp://docs/readme");
		McpNotificationService notificationService = new McpNotificationService(sseStreamService, subscriptionService);

		notificationService.resourceUpdated("mcp://docs/readme");

		assertThat(subscribed.events).hasSize(1);
		assertThat(subscribed.events.get(0)).contains("notifications/resources/updated");
		assertThat(other.events).isEmpty();
	}

	@Test
	void shouldRemoveEmitterWhenSessionIsDeleted() {
		RecordingEmitter emitter = new RecordingEmitter();
		McpSseStreamService sseStreamService = new McpSseStreamService(new MagicAPIMcpProperties(), null, () -> emitter);
		sseStreamService.open("text/event-stream", "session-1");

		sseStreamService.remove("session-1");
		boolean sent = sseStreamService.send("session-1", McpNotificationService.toolsListChanged());

		assertThat(sent).isFalse();
		assertThat(emitter.events).isEmpty();
	}

	@Test
	void shouldReturnRegisteredEmitterFromOpen() {
		RecordingEmitter emitter = new RecordingEmitter();
		McpSseStreamService sseStreamService = new McpSseStreamService(new MagicAPIMcpProperties(), null, () -> emitter);

		ResponseEntity<?> response = sseStreamService.open("text/event-stream", "session-1");

		assertThat(response.getBody()).isSameAs(emitter);
		assertThat(sseStreamService.hasEmitter("session-1")).isTrue();
	}

	private static class RecordingEmitter extends SseEmitter {

		private final List<String> events = new ArrayList<>();

		@Override
		public void send(Object object) throws IOException {
			events.add(String.valueOf(object));
		}
	}

	private static class QueueEmitterFactory implements McpSseStreamService.EmitterFactory {

		private final SseEmitter[] emitters;

		private int index;

		private QueueEmitterFactory(SseEmitter... emitters) {
			this.emitters = emitters;
		}

		@Override
		public SseEmitter create() {
			return emitters[index++];
		}
	}
}
