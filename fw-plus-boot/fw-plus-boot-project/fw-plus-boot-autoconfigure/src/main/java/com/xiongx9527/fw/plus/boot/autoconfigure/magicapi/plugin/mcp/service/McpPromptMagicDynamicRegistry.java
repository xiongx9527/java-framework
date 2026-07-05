package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpPromptInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification.McpNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

@Slf4j
public class McpPromptMagicDynamicRegistry extends AbstractMagicDynamicRegistry<McpPromptInfo> {

	private final McpNotificationService notificationService;

	public McpPromptMagicDynamicRegistry(MagicResourceStorage<McpPromptInfo> magicResourceStorage) {
		this(magicResourceStorage, null);
	}

	public McpPromptMagicDynamicRegistry(MagicResourceStorage<McpPromptInfo> magicResourceStorage,
										 McpNotificationService notificationService) {
		super(magicResourceStorage);
		this.notificationService = notificationService;
	}

	@EventListener(condition = "#event.type == 'mcp-prompt'")
	public void onFileEvent(FileEvent event) {
		try {
			processEvent(event);
		} catch (Exception ignored) {
		}
	}

	@EventListener(condition = "#event.type == 'mcp-prompt'")
	public void onGroupEvent(GroupEvent event) {
		try {
			processEvent(event);
		} catch (Exception ignored) {
		}
	}

	@Override
	public boolean register(McpPromptInfo entity) {
		try {
			boolean registered = super.register(entity);
			if (registered) {
				notifyChanged();
			}
			return registered;
		} catch (Exception ignored) {
			return false;
		}
	}

	@Override
	public boolean unregister(McpPromptInfo entity) {
		try {
			boolean unregistered = super.unregister(entity);
			if (unregistered) {
				notifyChanged();
			}
			return unregistered;
		} catch (Exception ignored) {
			return false;
		}
	}

	public void notifyChanged() {
		if (notificationService != null) {
			notificationService.broadcast(McpNotificationService.promptsListChanged());
		}
	}

	@Override
	protected boolean register(MappingNode<McpPromptInfo> mappingNode) {
		try {
			return super.register(mappingNode);
		} catch (Exception ignored) {
			return false;
		}
	}

	@Override
	protected void unregister(MappingNode<McpPromptInfo> mappingNode) {
		try {
			super.unregister(mappingNode);
		} catch (Exception ignored) {
		}
	}
}
