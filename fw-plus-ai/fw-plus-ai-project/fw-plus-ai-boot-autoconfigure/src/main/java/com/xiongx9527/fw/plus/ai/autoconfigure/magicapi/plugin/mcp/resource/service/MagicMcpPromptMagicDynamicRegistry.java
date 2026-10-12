package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.Objects;

/**
 * {@code mcp-prompt} 动态注册表：提示词定义变更后热更新映射并通知 {@code prompts/list_changed}。
 */
@Slf4j
public class MagicMcpPromptMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MagicMcpPromptInfo> {

	private final MagicMcpNotifier notifier;

	private final MagicApiMcpCapabilityRegistrar capabilityRegistrar;

	public MagicMcpPromptMagicDynamicRegistry(MagicResourceStorage<MagicMcpPromptInfo> magicResourceStorage,
			MagicMcpNotifier notifier, MagicApiMcpCapabilityRegistrar capabilityRegistrar) {
		super(magicResourceStorage);
		this.notifier = Objects.requireNonNull(notifier, "notifier");
		this.capabilityRegistrar = capabilityRegistrar;
	}

	@Override
	protected boolean register(MappingNode<MagicMcpPromptInfo> mappingNode) {
		boolean registered = super.register(mappingNode);
		if (registered) {
			refreshCapabilities();
			notifier.publishPromptsChanged();
		}
		return registered;
	}

	@Override
	protected void unregister(MappingNode<MagicMcpPromptInfo> mappingNode) {
		super.unregister(mappingNode);
		refreshCapabilities();
		notifier.publishPromptsChanged();
	}

	private void refreshCapabilities() {
		if (capabilityRegistrar != null) {
			capabilityRegistrar.refresh();
		}
	}

	@EventListener(condition = "#event.type == 'mcp-prompt'")
	public void onFileEvent(FileEvent event) {
		try {
			processEvent(event);
		} catch (Exception e) {
			log.warn("处理 MCP 提示词文件事件失败，运行态提示词可能未更新，异常类型: {}", e.getClass().getSimpleName());
		}
	}

	@EventListener(condition = "#event.type == 'mcp-prompt'")
	public void onGroupEvent(GroupEvent event) {
		try {
			processEvent(event);
		} catch (Exception e) {
			log.warn("处理 MCP 提示词分组事件失败，运行态提示词可能未更新，异常类型: {}", e.getClass().getSimpleName());
		}
	}
}
