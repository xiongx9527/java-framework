package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.MagicNotify;

import static org.assertj.core.api.Assertions.assertThat;

class MagicNotifyMcpChangeEventPublisherTest {

	@Test
	void shouldRoundTripMcpChangeEventThroughMagicNotifyContent() {
		RecordingNotifyService notifyService = new RecordingNotifyService();
		MagicNotifyMcpChangeEventPublisher publisher = new MagicNotifyMcpChangeEventPublisher(notifyService, "node-a");

		publisher.publish(new McpChangeEvent("node-a", McpNotificationService.toolsListChanged()));

		McpChangeEvent event = MagicNotifyMcpChangeEventPublisher.fromNotify(notifyService.notify);
		assertThat(event.getOriginId()).isEqualTo("node-a");
		assertThat(event.getMethod()).isEqualTo("notifications/tools/list_changed");
	}

	@Test
	void shouldDispatchReceivedNotifyToNotificationService() {
		RecordingNotificationService notificationService = new RecordingNotificationService();
		McpClusterChangeEventListener listener = new McpClusterChangeEventListener(notificationService);
		RecordingNotifyService notifyService = new RecordingNotifyService();
		new MagicNotifyMcpChangeEventPublisher(notifyService, "node-a")
				.publish(new McpChangeEvent("node-a", McpNotificationService.promptsListChanged()));

		listener.onNotify(notifyService.notify);

		assertThat(notificationService.event.getOriginId()).isEqualTo("node-a");
		assertThat(notificationService.event.getMethod()).isEqualTo("notifications/prompts/list_changed");
	}

	private static class RecordingNotifyService implements org.ssssssss.magicapi.core.service.MagicNotifyService {

		private MagicNotify notify;

		@Override
		public void sendNotify(MagicNotify magicNotify) {
			this.notify = magicNotify;
		}
	}

	private static class RecordingNotificationService extends McpNotificationService {

		private McpChangeEvent event;

		private RecordingNotificationService() {
			super(null, null);
		}

		@Override
		public void receiveClusterEvent(McpChangeEvent event) {
			this.event = event;
		}
	}
}
