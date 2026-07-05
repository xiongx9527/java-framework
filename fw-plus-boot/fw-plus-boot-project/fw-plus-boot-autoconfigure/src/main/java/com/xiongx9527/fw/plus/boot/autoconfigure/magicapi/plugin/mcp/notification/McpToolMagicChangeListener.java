package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;

public class McpToolMagicChangeListener {

	private final McpNotificationService notificationService;

	public McpToolMagicChangeListener(McpNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@EventListener(condition = "#event.type == 'api' || #event.type == 'function'")
	public void onFileEvent(FileEvent event) {
		notifyChanged();
	}

	@EventListener(condition = "#event.type == 'api' || #event.type == 'function'")
	public void onGroupEvent(GroupEvent event) {
		notifyChanged();
	}

	public void notifyChanged() {
		if (notificationService != null) {
			notificationService.broadcast(McpNotificationService.toolsListChanged());
		}
	}
}
