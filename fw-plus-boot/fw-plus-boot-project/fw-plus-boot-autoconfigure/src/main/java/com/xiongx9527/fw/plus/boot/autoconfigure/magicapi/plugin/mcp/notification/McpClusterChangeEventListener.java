package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.service.MagicNotifyListener;
import org.ssssssss.magicapi.core.model.MagicNotify;

public class McpClusterChangeEventListener implements MagicNotifyListener {

	private final McpNotificationService notificationService;

	public McpClusterChangeEventListener(McpNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
	public void onNotify(MagicNotify notify) {
		McpChangeEvent event = MagicNotifyMcpChangeEventPublisher.fromNotify(notify);
		if (event != null) {
			notificationService.receiveClusterEvent(event);
		}
	}
}
