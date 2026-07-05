package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

@Slf4j
public class McpResourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<McpResourceInfo> {

	public McpResourceMagicDynamicRegistry(MagicResourceStorage<McpResourceInfo> magicResourceStorage) {
		super(magicResourceStorage);
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
			return super.register(entity);
		} catch (Exception ignored) {
			return false;
		}
	}

	@Override
	public boolean unregister(McpResourceInfo entity) {
		try {
			return super.unregister(entity);
		} catch (Exception ignored) {
			return false;
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
