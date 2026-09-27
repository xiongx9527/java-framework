package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.Objects;

/**
 * {@code mcp-resource} 动态注册表：资源定义变更后热更新映射，并按变更类型通知客户端。
 *
 * <p>新增/删除 uri 走 {@code notifications/resources/list_changed}；同名 uri 内容更新走
 * {@code notifications/resources/updated}（内核只投递给订阅了该 uri 的客户端）。</p>
 */
@Slf4j
public class MagicMcpResourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MagicMcpResourceInfo> {

	private final MagicMcpNotifier notifier;

	private final MagicApiMcpCapabilityRegistrar capabilityRegistrar;

	/**
	 * 同名 uri 的保存事件标记：为 {@code true} 时基类会先卸载旧节点，此时不应再发
	 * {@code list_changed}，改由随后 register 发的 {@code resources/updated} 表达。
	 */
	private volatile boolean updating;

	public MagicMcpResourceMagicDynamicRegistry(MagicResourceStorage<MagicMcpResourceInfo> magicResourceStorage,
			MagicMcpNotifier notifier, MagicApiMcpCapabilityRegistrar capabilityRegistrar) {
		super(magicResourceStorage);
		this.notifier = Objects.requireNonNull(notifier, "notifier");
		this.capabilityRegistrar = capabilityRegistrar;
	}

	@EventListener(condition = "#event.type == 'mcp-resource'")
	public void onFileEvent(FileEvent event) {
		updating = EventAction.SAVE == event.getAction() && event.getEntity() instanceof MagicMcpResourceInfo
				&& hasSameUri((MagicMcpResourceInfo) event.getEntity());
		try {
			processEvent(event);
		} catch (Exception e) {
			log.warn("处理 MCP 资源文件事件失败，运行态资源可能未更新，异常类型: {}", e.getClass().getSimpleName());
		} finally {
			updating = false;
		}
	}

	@EventListener(condition = "#event.type == 'mcp-resource'")
	public void onGroupEvent(GroupEvent event) {
		try {
			processEvent(event);
		} catch (Exception e) {
			log.warn("处理 MCP 资源分组事件失败，运行态资源可能未更新，异常类型: {}", e.getClass().getSimpleName());
		}
	}

	@Override
	public boolean register(MagicMcpResourceInfo entity) {
		// 必须在 super.register 之前取：基类在同 uri 更新时会先卸载旧节点再从表里移除
		boolean existed = hasSameUri(entity);
		boolean registered = super.register(entity);
		if (registered) {
			refreshCapabilities();
			if (existed) {
				notifier.publishResourceUpdated(entity.getUri());
			} else {
				notifier.publishResourcesChanged();
			}
		}
		return registered;
	}

	@Override
	protected void unregister(MappingNode<MagicMcpResourceInfo> mappingNode) {
		super.unregister(mappingNode);
		refreshCapabilities();
		if (!updating) {
			notifier.publishResourcesChanged();
		}
	}

	private void refreshCapabilities() {
		if (capabilityRegistrar != null) {
			capabilityRegistrar.refresh();
		}
	}

	private boolean hasSameUri(MagicMcpResourceInfo entity) {
		for (MagicMcpResourceInfo it : mappings()) {
			if (Objects.equals(it.getUri(), entity.getUri())) {
				return true;
			}
		}
		return false;
	}
}
