package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicMcpServerInfo;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code mcp-server} 动态注册器：控制台新增/修改/删除资源即新增/更新/移除 MCP server 实例。
 */
public class MagicMcpServerMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MagicMcpServerInfo> {

	private final MagicDynamicMcpServer dataSource;

	private final MagicApiMcpProperties properties;

	public MagicMcpServerMagicDynamicRegistry(MagicResourceStorage<MagicMcpServerInfo> magicResourceStorage,
			MagicDynamicMcpServer dataSource, MagicApiMcpProperties properties) {
		super(magicResourceStorage);
		this.dataSource = dataSource;
		this.properties = properties;
	}

	@EventListener(condition = "#event.type == 'mcp-server'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'mcp-server'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}

	@Override
	protected boolean register(MappingNode<MagicMcpServerInfo> mappingNode) {
		MagicMcpServerInfo entity = mappingNode.getEntity();
		dataSource.destroyClientByKey(entity.getKey());
		if (!entity.isEnabled()) {
			return true;
		}
		boolean created = dataSource.createClient(entity);
		return created && super.register(mappingNode);
	}

	@Override
	protected void unregister(MappingNode<MagicMcpServerInfo> mappingNode) {
		dataSource.destroyClientByKey(mappingNode.getEntity().getKey());
		super.unregister(mappingNode);
	}

	/**
	 * 默认映射只用于控制台资源树展示：只列配置项驱动、尚未落盘为文件的实例
	 * （{@code id == null}）。文件创建的实例已由控制器 {@code listFiles → simple()} 列出，
	 * 这里再返回会造成同一 JSON 在树上重复。
	 *
	 * <p>不能在此触发创建——{@code defaultMappings} 仅在打开控制台资源树时被调用，
	 * 配置项驱动的实例已由 {@code MagicDynamicMcpServer#afterSingletonsInstantiated()}
	 * 在启动期创建。</p>
	 */
	@Override
	public List<MagicMcpServerInfo> defaultMappings() {
		List<MagicMcpServerInfo> result = new ArrayList<>();
		for (MagicMcpServerInfo info : dataSource.serverInfos()) {
			if (info.getId() == null) {
				result.add(info);
			}
		}
		return result;
	}
}
