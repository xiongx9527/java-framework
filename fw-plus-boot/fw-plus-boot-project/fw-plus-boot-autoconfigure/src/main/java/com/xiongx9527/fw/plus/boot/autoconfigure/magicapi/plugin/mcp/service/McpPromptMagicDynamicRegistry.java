package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpPromptInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

@Slf4j
public class McpPromptMagicDynamicRegistry extends AbstractMagicDynamicRegistry<McpPromptInfo> {

	public McpPromptMagicDynamicRegistry(MagicResourceStorage<McpPromptInfo> magicResourceStorage) {
		super(magicResourceStorage);
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
			return super.register(entity);
		} catch (Exception ignored) {
			return false;
		}
	}

	@Override
	public boolean unregister(McpPromptInfo entity) {
		try {
			return super.unregister(entity);
		} catch (Exception ignored) {
			return false;
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
