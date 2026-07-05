package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpResourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.McpNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

@Slf4j
public class McpResourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<McpResourceInfo> {

	private final McpNotificationService notificationService;

	public McpResourceMagicDynamicRegistry(MagicResourceStorage<McpResourceInfo> magicResourceStorage) {
		this(magicResourceStorage, null);
	}

	public McpResourceMagicDynamicRegistry(MagicResourceStorage<McpResourceInfo> magicResourceStorage,
										   McpNotificationService notificationService) {
		super(magicResourceStorage);
		this.notificationService = notificationService;
	}

	@EventListener(condition = "#event.type == 'mcp-resource'")
	public void onFileEvent(FileEvent event) {
		try {
			processEvent(event);
		} catch (Exception ignored) {
		}
	}

	@EventListener(condition = "#event.type == 'mcp-resource'")
	public void onGroupEvent(GroupEvent event) {
		try {
			processEvent(event);
		} catch (Exception ignored) {
		}
	}

	@Override
	public boolean register(McpResourceInfo entity) {
		try {
			boolean registered = super.register(entity);
			if (registered) {
				notifyChanged(entity);
			}
			return registered;
		} catch (Exception ignored) {
			return false;
		}
	}

	@Override
	public boolean unregister(McpResourceInfo entity) {
		try {
			boolean unregistered = super.unregister(entity);
			if (unregistered) {
				notifyChanged(entity);
			}
			return unregistered;
		} catch (Exception ignored) {
			return false;
		}
	}

	public void notifyChanged(McpResourceInfo entity) {
		if (notificationService == null) {
			return;
		}
		notificationService.broadcast(McpNotificationService.resourcesListChanged());
		if (entity != null && entity.getUri() != null) {
			notificationService.resourceUpdated(entity.getUri());
		}
	}

	@Override
	protected boolean register(MappingNode<McpResourceInfo> mappingNode) {
		try {
			return super.register(mappingNode);
		} catch (Exception ignored) {
			return false;
		}
	}

	@Override
	protected void unregister(MappingNode<McpResourceInfo> mappingNode) {
		try {
			super.unregister(mappingNode);
		} catch (Exception ignored) {
		}
	}
}
