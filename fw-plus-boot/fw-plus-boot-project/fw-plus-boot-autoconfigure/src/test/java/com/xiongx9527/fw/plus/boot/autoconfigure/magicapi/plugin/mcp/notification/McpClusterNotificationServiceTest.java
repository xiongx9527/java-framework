package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources.McpResourceSubscriptionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpSseStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpClusterNotificationServiceTest {

	@Test
	void shouldUseLocalPublisherWhenClusterPublisherIsMissing() {
		RecordingEmitter emitter = new RecordingEmitter();
		McpSseStreamService sseStreamService = new McpSseStreamService(new MagicAPIMcpProperties(), null, () -> emitter);
		sseStreamService.open("text/event-stream", "session-1");
		McpNotificationService notificationService = new McpNotificationService(sseStreamService,
				new McpResourceSubscriptionService(), null);

		notificationService.broadcast(McpNotificationService.toolsListChanged());

		assertThat(emitter.events).hasSize(1);
		assertThat(emitter.events.get(0)).contains("notifications/tools/list_changed");
	}

	@Test
	void shouldPublishClusterEventAndDeliverToLocalEmitters() {
		RecordingEmitter emitter = new RecordingEmitter();
		RecordingPublisher publisher = new RecordingPublisher("node-a");
		McpSseStreamService sseStreamService = new McpSseStreamService(new MagicAPIMcpProperties(), null, () -> emitter);
		sseStreamService.open("text/event-stream", "session-1");
		McpNotificationService notificationService = new McpNotificationService(sseStreamService,
				new McpResourceSubscriptionService(), publisher);

		notificationService.broadcast(McpNotificationService.promptsListChanged());

		assertThat(publisher.events).hasSize(1);
		assertThat(publisher.events.get(0).getOriginId()).isEqualTo("node-a");
		assertThat(publisher.events.get(0).getMethod()).isEqualTo("notifications/prompts/list_changed");
		assertThat(emitter.events).hasSize(1);
	}

	@Test
	void shouldIgnoreSelfLoopClusterEvent() {
		RecordingEmitter emitter = new RecordingEmitter();
		RecordingPublisher publisher = new RecordingPublisher("node-a");
		McpSseStreamService sseStreamService = new McpSseStreamService(new MagicAPIMcpProperties(), null, () -> emitter);
		sseStreamService.open("text/event-stream", "session-1");
		McpNotificationService notificationService = new McpNotificationService(sseStreamService,
				new McpResourceSubscriptionService(), publisher);

		notificationService.receiveClusterEvent(new McpChangeEvent("node-a", McpNotificationService.toolsListChanged()));

		assertThat(emitter.events).isEmpty();
	}

	@Test
	void shouldDeliverRemoteClusterEventOnlyToLocalEmitters() {
		RecordingEmitter subscribed = new RecordingEmitter();
		RecordingEmitter other = new RecordingEmitter();
		McpResourceSubscriptionService subscriptionService = new McpResourceSubscriptionService();
		subscriptionService.subscribe("session-1", "mcp://docs/readme");
		McpSseStreamService sseStreamService = new McpSseStreamService(new MagicAPIMcpProperties(), null,
				new QueueEmitterFactory(subscribed, other));
		sseStreamService.open("text/event-stream", "session-1");
		sseStreamService.open("text/event-stream", "session-2");
		McpNotificationService notificationService = new McpNotificationService(sseStreamService,
				subscriptionService, new RecordingPublisher("node-a"));

		notificationService.receiveClusterEvent(new McpChangeEvent("node-b",
				McpNotificationService.resourceUpdatedNotification("mcp://docs/readme")));

		assertThat(subscribed.events).hasSize(1);
		assertThat(other.events).isEmpty();
	}

	private static class RecordingPublisher implements McpChangeEventPublisher {

		private final String originId;

		private final List<McpChangeEvent> events = new ArrayList<>();

		private RecordingPublisher(String originId) {
			this.originId = originId;
		}

		@Override
		public String getOriginId() {
			return originId;
		}

		@Override
		public void publish(McpChangeEvent event) {
			events.add(event);
		}
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
